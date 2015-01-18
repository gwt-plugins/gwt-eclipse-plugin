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

import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.GwtSuperDevModeLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.osgi.framework.BundleContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.UUID;

/**
 * Google GWT WTP plug-in life-cycle.
 */
public final class GwtWtpPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.gwt.eclipse.wtp";
  public static final String USE_MAVEN_DEPS_PROPERTY_NAME = PLUGIN_ID + ".useMavenDependencies";

  private static HashSet<String[]> commandsToExecuteAtExit = new HashSet<String[]>();
  private static GwtWtpPlugin INSTANCE;

  public static IStatus createErrorStatus(String mess, Exception e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, -1, mess, e);
  }

  public static GwtWtpPlugin getInstance() {
    return INSTANCE;
  }

  public static void logMessage(String mess) {
    logMessage(mess, null);
  }

  public static void logMessage(String msg, Throwable e) {
    msg = msg == null ? "Google GWT Error" : "Google GWT: " + msg;
    Status status = new Status(IStatus.ERROR, PLUGIN_ID, 1, msg, e);
    getInstance().getLog().log(status);
  }

  public static void logMessage(Throwable e) {
    logMessage(null, e);
  }

  private IDebugEventSetListener processListener;

  public GwtWtpPlugin() {
    INSTANCE = this;
  }

  /**
   * When a Server runtime server is started and terminated, and the project has a GWT Facet, start
   * and stop the GWT Super Dev Mode Code Server with runtime server.
   *
   * TODO if sdm starts, start the wtp server?
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
          for (int i = 0; i < events.length; i++) {
            if (events[i].getSource() instanceof RuntimeProcess
                && events[i].getKind() == DebugEvent.CREATE) {
              // Start
              posiblyLaunchGwtSuperDevModeCodeServer(events[i]);
            } else if (events[i].getSource() instanceof RuntimeProcess
                && events[i].getKind() == DebugEvent.TERMINATE) {
              // Terminate
              possiblyRemoveLaunchConfiguration(events[i]);
            }
          }
        }
      }
    };
    DebugPlugin.getDefault().addDebugEventListener(processListener);
  }

  protected void possiblyRemoveLaunchConfiguration(DebugEvent event) {
    logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Stopping GWT Super Dev Mode Code Server.");

    RuntimeProcess serverRuntimeProcess = (RuntimeProcess) event.getSource();
    ILaunch serverLaunch = serverRuntimeProcess.getLaunch();
    ILaunchConfiguration serverLaunchConfig = serverLaunch.getLaunchConfiguration();

    ILaunchConfigurationType serverType = null;
    try {
      serverType = serverLaunchConfig.getType();
    } catch (CoreException e) {
      logMessage("possiblyRemoveLaunchConfiguration: Could not retrieve the launch config type.", e);
    }

    String serverLaunchId;
    try {
      serverLaunchId = serverLaunchConfig.getAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, "NoId");
    } catch (CoreException e1) {
      serverLaunchId = "NoId";
    }

    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType typeId =
          launchManager.getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);
      ILaunch[] launches = launchManager.getLaunches();

      for (ILaunch launch : launches) {
        ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
        String launcherId =
            launchConfig.getAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, (String) null);
        // Skip if it's the Super Dev Mode Code Server terminating, so it doesn't
        // Terminate if the server terminated and they both have the same launcher id.
        if (!typeId.equals(serverType) && typeId.equals(launch.getLaunchConfiguration().getType())
            && serverLaunchId.equals(launcherId)) {
          launch.terminate();
        }
      }
    } catch (CoreException e) {
      e.printStackTrace();
      logMessage(
          "possiblyRemoveLaunchConfiguration: Couldn't stop the Super Dev Mode Code Server.", e);
    }
  }

  private String setLauncherIdToWtpRunTimeLaunchConfig(ILaunchConfiguration launchConfig) {
    String launcherId = getLaunchId();

    logMessage("setLauncherIdToWtpRunTimeLaunchConfig: Adding server launcherId id="
        + getLaunchId());

    try {
      ILaunchConfigurationWorkingCopy launchConfigWorkingCopy = launchConfig.getWorkingCopy();
      launchConfigWorkingCopy.setAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, launcherId);
      launchConfigWorkingCopy.doSave();
    } catch (CoreException e) {
      logMessage(
          "posiblyLaunchGwtSuperDevModeCodeServer: Couldn't add server Launcher Id attribute.", e);
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
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Getting the WTP server error.", e);
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

    // Also used GaeServerBehaviour.setupLaunchConfig(...)
    String launcherDir = null;
    try {
      launcherDir =
          launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
              (String) null);
    } catch (CoreException e) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Couldn't get working directory for launcherDir.");
      e.printStackTrace();
    }
    if (launcherDir == null) {
      logMessage("posiblyLaunchGwtSuperDevModeCodeServer: No launcherDir is available, exiting.");
      return;
    }

    // LauncherId used to reference and terminate the the process
    String launcherId = setLauncherIdToWtpRunTimeLaunchConfig(launchConfig);

    logMessage("posiblyLaunchGwtSuperDevModeCodeServer: Launching GWT Super Dev Mode CodeServer.");

    // Creates ore launches an existing Super Dev Mode Code Server process
    GwtSuperDevModeCodeServerLaunchUtil.launch(project, launchMode, launcherDir, launcherId);
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
      logMessage("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }

  @Override
  public void stop(BundleContext v) throws Exception {
    DebugPlugin.getDefault().removeDebugEventListener(processListener);

    for (String[] command : commandsToExecuteAtExit) {
      try {
        logMessage(">>> " + command[0], null);
        BufferedReader input =
            new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(command)
                .getInputStream()));
        String line = null;
        while ((line = input.readLine()) != null) {
          logMessage(">>> " + line, null);
        }
        input.close();
      } catch (Throwable ex) {
        logMessage("Error executing process:\n" + ex);
      }
    }
    super.stop(v);
  }

}
