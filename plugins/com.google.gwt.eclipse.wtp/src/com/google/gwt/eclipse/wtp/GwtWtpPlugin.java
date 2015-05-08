/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.wtp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
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
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModule2;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.osgi.framework.BundleContext;

import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.GwtSuperDevModeLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

/**
 * Google GWT WTP plug-in life-cycle.
 */
public final class GwtWtpPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.gwt.eclipse.wtp";
  public static final String USE_MAVEN_DEPS_PROPERTY_NAME = PLUGIN_ID + ".useMavenDependencies";

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

  public GwtWtpPlugin() {
    INSTANCE = this;
  }

  /**
   * When a Server runtime server is started and terminated, and the project has
   * a GWT Facet, start and stop the GWT Super Dev Mode Code Server with runtime
   * server.
   *
   * TODO if sdm starts, start the wtp server? <br/>
   * TODO if the sdm server stops on its own, stop the wtp server?
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
    ILaunchConfigurationType launchType = launchConfig.getType();

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType sdmCodeServerType = launchManager
        .getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);

    if (launchType.equals(sdmCodeServerType) && event.getKind() == DebugEvent.CREATE) {
      // Start: Observe the console for SDM launching
      processSdmCodeServerLauncher(event);

    } else if (launchType.equals(sdmCodeServerType) && event.getKind() == DebugEvent.TERMINATE) {
      // Stop: Observe the console for SDM terminating
      processSdmCodeServerTerminate(event);

    } else {

      if (event.getKind() == DebugEvent.CREATE) {
        // Start: Delineate event for Server Launching and then possibly launch SDM 
        posiblyLaunchGwtSuperDevModeCodeServer(event);

      } else if (event.getKind() == DebugEvent.TERMINATE) {
        // Stop: Delineate event for Server Launching and then possibly terminate SDM 
        possiblyTerminateLaunchConfiguration(event);
      }
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

  // TODO fire gwt sdm start/stop event
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
    if (server.getName().contains("Google")) {
      GaeServer gaeServer = GaeServer.getGaeServer(server);
      String gaeHost = server.getHost();
      ServerPort gaePort = gaeServer.getMainPort();
      String gaeUrl = String.format("http://%s:%s/_ah/admin", gaeHost, gaePort.getPort());
      launchUrls.add(gaeUrl);
    }
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
    if (serverLaunch == null) {
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
        logMessage("possiblyRemoveLaunchConfiguration: Launches is empty or null. Can't find the GWT sdm launch config");
        return;
      }

      for (ILaunch launch : launches) {
        ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
        String launcherId = launchConfig.getAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, (String) null);
        // If its the sdm code server
        if (sdmcodeServerType.equals(serverType)) {
          // TODO ? remove listener on console
        } else if (!sdmcodeServerType.equals(serverType)
            && sdmcodeServerType.equals(launch.getLaunchConfiguration().getType()) && serverLaunchId.equals(launcherId)) {
          // Skip if it's the Super Dev Mode Code Server terminating, so it
          // doesn't
          // Terminate if the server terminated and they both have the same
          // launcher id.
          launch.terminate();
        }
      }
    } catch (CoreException e) {
      e.printStackTrace();
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
   * This starts as separate process, which allows for custom args modification.
   * It adds a launcher id to both processes for reference.
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
      logError("posiblyLaunchGwtSuperDevModeCodeServer: Getting the WTP server error.", e);
      return;
    }

    if (server == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No WTP server found.");
      return;
    }

    IProject project = getProject(server);
    if (project == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Couldn't find project.");
      return;
    }

    if (!hasGwtFacet(project)) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Does not have a GWT Facet.");
      return;
    }

    if (GWTProjectProperties.getFacetSyncCodeServer(project) != null
        && GWTProjectProperties.getFacetSyncCodeServer(project) == false) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: GWT Facet project properties, the code server sync is off.");
      return;
    }

    // Also used GaeServerBehaviour.setupLaunchConfig(...)
    String launcherDir = null;
    try {
      launcherDir = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String) null);
    } catch (CoreException e) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Couldn't get working directory for launcherDir.");
    }

    // Classic launch config isn't providing a launcherDir, lets check the server properties
    if (launcherDir == null) {
      launcherDir = getLauncherDirFromServerLaunchConfigAttributes(server, launchConfig);
    }

    if (launcherDir == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No -launcherDir arg is available, exiting.");
      return;
    }

    // Add server urls to DevMode view for easy clicking on
    addServerUrlsToDevModeView(launch);

    // LauncherId used to reference and terminate the the process
    String launcherId = setLauncherIdToWtpRunTimeLaunchConfig(launchConfig);

    logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Launching GWT Super Dev Mode CodeServer.");
    
    // Just in case
    if (launchMode == null) {
      launchMode = "run";
    }
    
    if (launcherId == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No launcherId.");
    }
    
    // Creates ore launches an existing Super Dev Mode Code Server process
    GwtSuperDevModeCodeServerLaunchUtil.launch(project, launchMode, launcherDir, launcherId);
  }

  /**
   * Try to get the war output path from server launch config.
   * 
   * @param server
   * @param serverLaunchConfig
   * @return
   */
  private String getLauncherDirFromServerLaunchConfigAttributes(IServer server, ILaunchConfiguration serverLaunchConfig) {
    if (server == null || serverLaunchConfig == null) {
      return null;
    }

    // First get wtp.deploy from the server attributes
    Map<String, Object> launchConfigAttributes = null;
    try {
      launchConfigAttributes = serverLaunchConfig.getAttributes();
    } catch (CoreException e) {
      logError(
          "posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: can't find launcher directory in ATTR_VM_ARGUMENTS.",
          e);
    }

    // Get the vm arguments in the launch configs vm args
    String vmArgsString = (String) launchConfigAttributes.get("org.eclipse.jdt.launching.VM_ARGUMENTS");
    if (vmArgsString == null || vmArgsString.isEmpty()) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: can't get org.eclipse.jdt.launching.VM_ARGUMENTS from the launch config.");
      return null;
    }

    // Create an array from the vm args string
    String[] vmArgsArray = DebugPlugin.parseArguments(vmArgsString);
    if (vmArgsArray == null || vmArgsString.isEmpty()) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: can't parse vm args into an array.");
      return null;
    }

    String wtpDeployArg = null;
    for (int i = 0; i < vmArgsArray.length; i++) {
      String arg = vmArgsArray[i];
      if (arg != null && arg.startsWith("-Dwtp.deploy")) {
        wtpDeployArg = arg.replaceFirst("-Dwtp.deploy=", "");
        wtpDeployArg = wtpDeployArg.replace("\"", "").trim();
        break;
      }
    }

    if (wtpDeployArg == null || wtpDeployArg.isEmpty()) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: can't get \"-Dwtp.deploy=\" (w/no spaces) arg from vm args list.");
      return null;
    }

    // Next get the server project deploy name or deploy context relative path
    String launcherDir = null;
    String deployName = null;
    IModule[] modules = server.getModules();
    if (modules != null && modules.length > 0) {
      if (modules.length > 0) {
        logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: launcherDir will use the first module for the deploy path.");
      }
      IModule2 module = (IModule2) modules[0];
      deployName = module.getProperty(IModule2.PROP_DEPLOY_NAME);
      if (deployName == null) {
        logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: Couldn't get the deploy path for the module.");
      } else {
        launcherDir = wtpDeployArg + File.separator + deployName;
      }
    } else {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: the modules are empty, add a wtp module.");
    }

    if (launcherDir == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: couldn't construct the launcherDir Path from server launch config.");
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: wtpDeployArg="
          + wtpDeployArg);
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: wtpDeployArg="
          + deployName);
    }

    logMessage("posiblyLaunchGwtSuperDevModeCodeServer > getLauncherDirFromServerLaunchConfigAttributes: Success, found the launcherDir="
        + launcherDir);

    return launcherDir;
  }

  private IProject getProject(IServer server) {
    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      return null;
    }

    return modules[0].getProject();
  }

  private boolean hasGwtFacet(IProject project) {
    boolean hasFacet = false;
    try {
      hasFacet = FacetedProjectFramework.hasProjectFacet(project, IGwtFacetConstants.GWT_FACET_ID);
    } catch (CoreException e) {
      logError("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }

  @Override
  public void stop(BundleContext v) throws Exception {
    DebugPlugin.getDefault().removeDebugEventListener(processListener);

    for (String[] command : commandsToExecuteAtExit) {
      try {
        logError(">>> " + command[0], null);
        BufferedReader input = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command)
            .getInputStream()));
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
