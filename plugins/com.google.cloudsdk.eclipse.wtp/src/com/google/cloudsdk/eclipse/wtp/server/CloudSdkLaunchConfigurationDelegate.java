/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp.server;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.cloudsdk.eclipse.wtp.GCloudCommandDelegate;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud SDK server's launch configuration delegate.
 */
public class CloudSdkLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {
  private IServerListener debugServerListener;
  private static final String DEBUGGER_HOST = "localhost";

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    final IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return;
    }

    final IModule[] modules = server.createWorkingCopy().getModules();
    if (modules == null || modules.length == 0){
      return;
    }

    String runnables = getRunnable(modules[0].getProject(), monitor);
    IRuntime runtime = server.getRuntime();
    if (runtime == null) {
      return;
    }
    IPath sdkLocation = runtime.getLocation();

    CloudSdkServerBehaviour sb = (CloudSdkServerBehaviour) server.loadAdapter(
        CloudSdkServerBehaviour.class, null);
    sb.setupLaunch(mode);

    final CloudSdkServer cloudSdkServer = CloudSdkServer.getCloudSdkServer(server);
    int debugPort = -1;
    if (mode.equals(ILaunchManager.DEBUG_MODE)) {
      final int port = getDebugPort();
      debugPort = port;

      // Create server listener so that when server has started we create a remote debugger
      // and attach it to the server
      debugServerListener = new IServerListener () {
        @Override
        public void serverChanged(ServerEvent event) {
          if (event != null) {
            if (event.getState() == IServer.STATE_STARTED) {
              try {
                Thread.sleep(500);
                runDebugTarget(cloudSdkServer, modules[0].getProject().getName(), port);
              } catch (InterruptedException | CoreException e) {
                CloudSdkPlugin.logError(e);
              }
            } else if (event.getState() == IServer.STATE_STOPPING) {
              removeRemoteDebuggerListener(server);
            }

          }
        }
      };
      server.addServerListener(debugServerListener);
    }

    // TODO: print out command in Console
    String commands = GCloudCommandDelegate.createAppRunCommand(sdkLocation.toOSString(), runnables,
        mode, cloudSdkServer.getApiHost(), debugPort);

    Process p = null;
    try {
      p = Runtime.getRuntime().exec(commands, null);
    } catch (IOException e1) {
      CloudSdkPlugin.logError(e1);
      sb.stop(true);
      return;
    }

    // The DebugPlugin handles the streaming of the output to the console and
    // sends notifications of debug events
    DebugPlugin.newProcess(launch, p, commands);
    sb.addProcessListener(launch.getProcesses()[0]);
  }

  @Override
  public boolean preLaunchCheck(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return false;
    }
    IModule[] modules = server.getModules();
    if (modules == null || modules.length == 0) {
      abort("No modules associated with this server instance.", null, 0);
      return false;
    }
    return super.preLaunchCheck(configuration, mode, monitor);
  }

  @Override
  protected void abort(String message, Throwable exception, int code) throws CoreException {
    throw new CoreException(new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID, code,
        message, exception));
  }

  private String getRunnable(IProject project, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade =
        MavenPlugin.getMavenProjectRegistry().getProject(project);
    MavenProject mavenProject = facade.getMavenProject(monitor);
    return mavenProject.getBasedir() + File.separator + "target" + File.separator
        + mavenProject.getArtifactId() + "-" + mavenProject.getVersion();
  }

  private int getDebugPort() throws CoreException {
    // TODO: add check to see if debug port was set, if not use a random free port.
    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort("Cannot find free port for remote debugger", null, IStatus.ERROR);
    }
    return port;
  }

  private void runDebugTarget(CloudSdkServer cloudSdkServer, String projectName, int port)
      throws CoreException, InterruptedException {
    ILaunchConfigurationWorkingCopy remoteDebugLaunchConfig =
        createRemoteDebugLaunchConfiguration(cloudSdkServer, projectName, Integer.toString(port));
    remoteDebugLaunchConfig.launch(ILaunchManager.DEBUG_MODE, null);
  }

  private ILaunchConfigurationWorkingCopy createRemoteDebugLaunchConfiguration(
      CloudSdkServer cloudSdkServer, final String projectName, final String port)
          throws CoreException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager
        .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

    final ILaunchConfigurationWorkingCopy remoteDebugConfig = type.newInstance(null,
        "Remote debugger for " + projectName + " (" + port + ")");

    // Set project
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);

    // Set JVM debugger connection parameters
    Map<String, String> connectionParameters = new HashMap<String, String>();
    connectionParameters.put("hostname", DEBUGGER_HOST);
    connectionParameters.put("port", port);
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP,
        connectionParameters);
    remoteDebugConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
            "org.eclipse.jdt.launching.socketAttachConnector");

    cloudSdkServer.setRemoteDebugLaunchConfig(remoteDebugConfig);

    return remoteDebugConfig;
  }

  private void removeRemoteDebuggerListener(IServer server) {
    server.removeServerListener(debugServerListener);
    debugServerListener = null;
  }
}
