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
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.io.IOException;

/**
 * Cloud SDK server's launch configuration delegate.
 */
public class CloudSdkLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {
  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return;
    }

    IModule[] modules = server.createWorkingCopy().getModules();
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

    String[] commands = {sdkLocation + "/bin/gcloud", "preview", "app", "run", runnables,
        "--api-host", CloudSdkServer.getCloudSdkServer(server).getApiHost()};
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
    DebugPlugin.newProcess(launch, p, configuration.getName());
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
}
