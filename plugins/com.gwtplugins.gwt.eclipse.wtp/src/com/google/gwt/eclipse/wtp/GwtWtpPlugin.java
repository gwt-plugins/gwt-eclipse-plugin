/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gwt.eclipse.wtp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.osgi.framework.BundleContext;

import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.GwtSuperDevModeLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;
import com.google.gwt.eclipse.wtp.utils.GwtFacetUtils;

/**
 * GWT WTP plug-in life-cycle.
 * 
 * TODO extract process listening TODO after sdm compile, publish bits. Either override CodeServer or proxy the web
 * server requests to CodeServer.
 * 
 * Note: For now, this will sync the code server with web server runtime, but won't figure out the launcher directory.
 */
public final class GwtWtpPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.gwtplugins.gwt.eclipse.wtp";


  private static GwtWtpPlugin INSTANCE;
  private static final String LOCAL_URL = "http://%s:%s";
  private static HashSet<String[]> commandsToExecuteAtExit = new HashSet<String[]>();
  private static List<String> launchUrls = new ArrayList<String>();

  public static IStatus createErrorStatus(String mess, Exception e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, -1, mess, e);
  }

  public static GwtWtpPlugin getInstance() {
    return INSTANCE;
  }

  public static void logMessage(String msg) {
    msg = msg == null ? "GWT" : "GWT: " + msg;
    Status status = new Status(IStatus.INFO, PLUGIN_ID, 1, msg, null);
    getInstance().getLog().log(status);
  }

  public static void logError(String msg) {
    logError(msg, null);
  }

  public static void logError(String msg, Throwable e) {
    msg = msg == null ? "GWT Error" : "GWT: " + msg;
    Status status = new Status(IStatus.ERROR, PLUGIN_ID, 1, msg, e);
    getInstance().getLog().log(status);
  }

  public static void logMessage(Throwable e) {
    logError(null, e);
  }

  private IDebugEventSetListener processListener;
  private IStreamListener consoleStreamListener;

  /**
   * Observe the console to notify SDM has launched
   */
  private IStreamMonitor streamMonitor;
  
  private IServerListener serverListener;
  private DebugEvent debugEvent;
  private boolean startedCodeServer;

  public GwtWtpPlugin() {
    INSTANCE = this;
  }

  /**
   * When a Server runtime server is started and terminated, and the project has a GWT Facet, start and stop the GWT
   * Super Dev Mode Code Server with runtime server.
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    // Observe launch events, that are for the WTP server
    processListener = new IDebugEventSetListener() {
      @Override
      public void handleDebugEvents(DebugEvent[] events) {
        if (events != null) {
          processProcessorEvents(events);
        }
      }
    };
    DebugPlugin.getDefault().addDebugEventListener(processListener);
  }

  protected void processProcessorEvents(DebugEvent[] events) {
    for (int i = 0; i < events.length; i++) {
      try {
        processLauncherEvent(events[i]);
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Start or stop processes automatically. Start or stop the CodeServer.
   */
  private void processLauncherEvent(DebugEvent event) throws CoreException {
    if (!(event.getSource() instanceof RuntimeProcess)) {
      return;
    }

    RuntimeProcess runtimeProcess = (RuntimeProcess) event.getSource();
    ILaunch launch = runtimeProcess.getLaunch();
    ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
    if (launchConfig == null) {
      return;
    }

    IServer server = getServerFromLaunchConfig(launch);
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType sdmCodeServerType = launchManager
        .getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);

    // CodeServer Start/Stop
    if (launchConfig.getType().equals(sdmCodeServerType) && event.getKind() == DebugEvent.CREATE) {
      // CodeServer Start: Observe the console for SDM launching
      processSdmCodeServerLauncher(event);
    } else if (launchConfig.getType().equals(sdmCodeServerType) && event.getKind() == DebugEvent.TERMINATE) {
      // CodeServer Stop: Observe the console for SDM terminating
      processSdmCodeServerTerminate(event);
    }

    // pass along the event to server started. 
    if (server != null && server.getServerState() == IServer.STATE_STARTING) {
      debugEvent = event;
      startedCodeServer = false; // safety reset
    }
    
    // WTP Server Start/Stop
    if (server != null && serverListener == null) { // listen for server started. It throws two events...
      serverListener = new IServerListener() {
        @Override
        public void serverChanged(ServerEvent event) {
          if (event.getState() == IServer.STATE_STARTED && !startedCodeServer) {
            startedCodeServer = true;
            posiblyLaunchGwtSuperDevModeCodeServer(debugEvent); // TODO rewrite so it can use the ServerEvent?
          }
        }
      };
      server.addServerListener(serverListener);
    }

    if (server != null && server.getServerState() == IServer.STATE_STOPPING
        && event.getKind() == DebugEvent.TERMINATE) {
      // Server Stop: Delineate event for Server Launching and then possibly terminate SDM
      possiblyTerminateLaunchConfiguration(event);
      serverListener = null;
      startedCodeServer = false;
    }
  }

  private void processSdmCodeServerTerminate(DebugEvent event) {
    if (streamMonitor == null) {
      return;
    }
    streamMonitor.removeListener(consoleStreamListener);
  }

  private void processSdmCodeServerLauncher(DebugEvent event) {
    RuntimeProcess runtimeProcess = (RuntimeProcess) event.getSource();
    final ILaunch launch = runtimeProcess.getLaunch();
    IProcess[] processes = launch.getProcesses();
    final IProcess process = processes[0];

    // Look for the links in the sdm console output
    consoleStreamListener = new IStreamListener() {
      @Override
      public void streamAppended(String text, IStreamMonitor monitor) {
        displayCodeServerUrlInDevMode(launch, text);
      }
    };

    // Listen to Console output
    streamMonitor = process.getStreamsProxy().getOutputStreamMonitor();
    streamMonitor.addListener(consoleStreamListener);
  }

  private void addServerUrlsToDevModeView(ILaunch launch) {
    IServer server = getServerFromLaunchConfig(launch);
    if (server == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No WTP server found.");
      return;
    }

    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      return;
    }

    IModule rootMod = modules[0];
    if (rootMod == null) {
      return;
    }

    // First clear the previous urls, before adding new ones
    launchUrls.clear();

    String host = server.getHost();
    ServerPort[] ports = null;
    try {
      ports = server.getServerPorts(new NullProgressMonitor());
    } catch (Exception e1) {
    }

    // Add server urls to DevModeViewer
    if (ports != null) {
      for (ServerPort port : ports) {
        String baseUrl = String.format(LOCAL_URL, host, port.getPort());
        String path = getPath(server, rootMod);
        String fullUrl = baseUrl + path;
        launchUrls.add(fullUrl);
      }
    }

    // TODO extract, consider a plugin that does this, that adds the appengine dashboard url
    // TODO or possibly do a extension query
    // Add App Engine url to DevModeView
    // See OpenLocalAdminConsoleHandler

    // TODO fix
    // if (server.getName().contains("Google")) {
    // GaeServer gaeServer = GaeServer.getGaeServer(server);
    // String gaeHost = server.getHost();
    // ServerPort gaePort = gaeServer.getMainPort();
    // String gaeUrl = String.format("http://%s:%s/_ah/admin", gaeHost, gaePort.getPort());
    // launchUrls.add(gaeUrl);
    // }
  }

  private String getPath(IServer server, IModule rootMod) {
    URL url = ((IURLProvider) server.loadAdapter(IURLProvider.class, null)).getModuleRootURL(rootMod);
    String surl = "";
    try {
      surl = url.toURI().getPath().toString();
    } catch (URISyntaxException e) {
    }
    return surl;
  }

  private void displayCodeServerUrlInDevMode(final ILaunch launch, String text) {
    if (!text.contains("http")) {
      return;
    }

    // Extract URL http://localhost:9876/
    String url = text.replaceAll(".*(http.*).*?", "$1").trim();
    if (url.matches(".*/")) {
      url = url.substring(0, url.length() - 1);
      launchUrls.add(url);
    }

    // Dev Mode View, add url
    LaunchConfiguration lc = WebAppDebugModel.getInstance().addOrReturnExistingLaunchConfiguration(launch, "", null);
    lc.setLaunchUrls(launchUrls);
  }

  private IServer getServerFromLaunchConfig(ILaunch launch) {
    ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
    if (launchConfig == null) {
      return null;
    }

    IServer server = null;
    try {
      server = ServerUtil.getServer(launchConfig);
    } catch (CoreException e) {
      logError("getServerFromLaunchConfig: Getting the WTP server error.", e);
      return null;
    }
    return server;
  }

  protected void possiblyTerminateLaunchConfiguration(DebugEvent event) {
    logMessage("posiblyTerminateGwtSuperDevModeCodeServer: Stopping GWT Super Dev Mode Code Server.");

    RuntimeProcess serverRuntimeProcess = (RuntimeProcess) event.getSource();
    ILaunch serverLaunch = serverRuntimeProcess.getLaunch();
    ILaunchConfiguration launchConfig = serverLaunch.getLaunchConfiguration();

    IServer server = null;
    try {
      server = ServerUtil.getServer(launchConfig);
    } catch (CoreException e) {
      logError("posiblyLaunchGwtSuperDevModeCodeServer: Could get the WTP server.", e);
      return;
    }

    if (server == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No WTP server found.");
      return;
    }

    IFacetedProject gwtFacetedProject = GwtFacetUtils.getGwtFacetedProject(server);

    // If one of the server modules has a gwt facet
    if (gwtFacetedProject == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Does not have a GWT Facet.");
      return;
    }

    // Sync Option - If the sync is off, ignore stopping the server
    if (!GWTProjectProperties.getFacetSyncCodeServer(gwtFacetedProject.getProject())) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: GWT Facet project properties, the code server sync is off.");
      return;
    }

    ILaunchConfiguration serverLaunchConfig = serverLaunch.getLaunchConfiguration();
    if (serverLaunchConfig == null) {
      return;
    }

    ILaunchConfigurationType serverType = null;
    try {
      serverType = serverLaunchConfig.getType();
    } catch (CoreException e) {
      logError("possiblyRemoveLaunchConfiguration: Could not retrieve the launch config type.", e);
    }

    String serverLaunchId;
    try {
      serverLaunchId = serverLaunchConfig.getAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, "NoId");
    } catch (CoreException e1) {
      serverLaunchId = "NoId";
    }

    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType sdmcodeServerType = launchManager
          .getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);
      ILaunch[] launches = launchManager.getLaunches();

      if (launches == null || launches.length == 0) {
        logMessage(
            "possiblyRemoveLaunchConfiguration: Launches is empty or null. Can't find the GWT sdm launch config");
        return;
      }

      for (ILaunch launch : launches) {
        launchConfig = launch.getLaunchConfiguration();
        String launcherId = launchConfig.getAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, (String) null);
        // If its the sdm code server
        if (sdmcodeServerType.equals(serverType)) {
          // TODO ? remove listener on console
        } else if (!sdmcodeServerType.equals(serverType)
            && sdmcodeServerType.equals(launch.getLaunchConfiguration().getType())
            && serverLaunchId.equals(launcherId)) {
          // Skip if it's the Super Dev Mode Code Server terminating, so it doesn't
          // Terminate if the server terminated and they both have the same launcher id.
          launch.terminate();
        }
      }
    } catch (CoreException e) {
      logError("possiblyRemoveLaunchConfiguration: Couldn't stop the Super Dev Mode Code Server.", e);
    }
  }

  private String setLauncherIdToWtpRunTimeLaunchConfig(ILaunchConfiguration launchConfig) {
    String launcherId = getLaunchId();

    logMessage("setLauncherIdToWtpRunTimeLaunchConfig: Adding server launcherId id=" + getLaunchId());

    try {
      ILaunchConfigurationWorkingCopy launchConfigWorkingCopy = launchConfig.getWorkingCopy();
      launchConfigWorkingCopy.setAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, launcherId);
      launchConfigWorkingCopy.doSave();
    } catch (CoreException e) {
      logError("posiblyLaunchGwtSuperDevModeCodeServer: Couldn't add server Launcher Id attribute.", e);
    }

    return launcherId;
  }

  private String getLaunchId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Possibly start the GWT Super Dev Mode CodeServer. <br/>
   * <br/>
   * This starts as separate process, which allows for custom args modification. It adds a launcher id to both processes
   * for reference. 1. Get it from classic launch config 2. Get it from server VM properties
   */
  protected void posiblyLaunchGwtSuperDevModeCodeServer(DebugEvent event) {
    RuntimeProcess runtimeProcess = (RuntimeProcess) event.getSource();
    ILaunch launch = runtimeProcess.getLaunch();
    ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
    String launchMode = launch.getLaunchMode();

    IServer server = null;
    try {
      server = ServerUtil.getServer(launchConfig);
    } catch (CoreException e) {
      logError("posiblyLaunchGwtSuperDevModeCodeServer: Could get the WTP server.", e);
      return;
    }

    if (server == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No WTP server runtime found.");
      return;
    }

    IFacetedProject gwtFacetedProject = GwtFacetUtils.getGwtFacetedProject(server);

    // If one of the server modules has a gwt facet
    if (gwtFacetedProject == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Does not have a GWT Facet.");
      return;
    }

    // Sync Option - the sync is off, ignore stopping the server
    if (!GWTProjectProperties.getFacetSyncCodeServer(gwtFacetedProject.getProject())) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: GWT Facet project properties, the code server sync is off.");
      return;
    }

    /**
     * Get the war output path for the `-launcherDir` in SDM launcher
     */
    String launcherDir = getLauncherDirectory(server, launchConfig, gwtFacetedProject);

    // LauncherId used to reference and terminate the the process
    String launcherId = setLauncherIdToWtpRunTimeLaunchConfig(launchConfig);

    logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Launching GWT Super Dev Mode CodeServer. launcherId="
        + launcherId + " launcherDir=" + launcherDir);

    // Just in case
    if (launchMode == null) {
      // run the code server, no need to debug it
      launchMode = "run";
    }

    if (launcherId == null) { // ids to link two processes together
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No launcherId.");
    }

    // Add server urls to DevMode view for easy clicking on
    addServerUrlsToDevModeView(launch);

    // Creates ore launches an existing Super Dev Mode Code Server process
    GwtSuperDevModeCodeServerLaunchUtil.launch(gwtFacetedProject.getProject(), launchMode, launcherDir, launcherId);
  }

  /**
   * TODO find a generic method to get the server publish/deploy path. TODO or put bits into a directory and use
   * PublishHelper
   * 
   * The -launcherDir war/output/path is the war deployment directory
   * 
   * @param server
   * @param launchConfig
   * @param gwtFacetedProject
   * @return the launcher directory or war output path
   */
  private String getLauncherDirectory(IServer server, ILaunchConfiguration launchConfig,
      IFacetedProject gwtFacetedProject) {
    String launcherDir = null;

    // Get the the war output path from classic launch configuration working directory
    // Also used GaeServerBehaviour.setupLaunchConfig(...)
    try {
      launcherDir = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String) null);
    } catch (CoreException e) {
      logMessage(
          "posiblyLaunchGwtSuperDevModeCodeServer: Couldn't get working directory from launchConfig IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY.");
    }

    return launcherDir;
  }

  @Override
  public void stop(BundleContext v) throws Exception {
    DebugPlugin.getDefault().removeDebugEventListener(processListener);

    for (String[] command : commandsToExecuteAtExit) {
      try {
        logError(">>> " + command[0], null);
        BufferedReader input = new BufferedReader(
            new InputStreamReader(Runtime.getRuntime().exec(command).getInputStream()));
        String line = null;
        while ((line = input.readLine()) != null) {
          logError(">>> " + line, null);
        }
        input.close();
      } catch (Throwable ex) {
        logMessage("Error executing process:\n" + ex);
      }
    }
    super.stop(v);
  }

}
