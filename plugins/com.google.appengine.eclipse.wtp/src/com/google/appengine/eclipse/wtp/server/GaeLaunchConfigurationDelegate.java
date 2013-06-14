/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.server;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jst.server.core.ServerProfilerDelegate;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import java.io.File;
import java.util.Map;

/**
 * Google App Engine launch configuration delegate.
 */
public final class GaeLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    IServer server = ServerUtil.getServer(configuration);
    if (server == null) {
      return;
    }

    GaeServerBehaviour sb = (GaeServerBehaviour) server.loadAdapter(
        GaeServerBehaviour.class, null);
    String mainTypeName = sb.getStartClassName();

    IVMInstall vm = verifyVMInstall(configuration);
    IVMRunner runner = vm.getVMRunner(mode);
    if (runner == null) {
      runner = vm.getVMRunner(ILaunchManager.RUN_MODE);
    }

    File workingDir = verifyWorkingDirectory(configuration);
    String workingDirName = null;
    if (workingDir != null) {
      workingDirName = workingDir.getAbsolutePath();
    }

    // Program & VM args
    String pgmArgs = getProgramArguments(configuration);
    String vmArgs = getVMArguments(configuration);
    String[] envp = getEnvironment(configuration);

    ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

    // VM-specific attributes
    Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

    // Classpath
    String[] classpath = getClasspath(configuration);

    // Create VM config
    VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
    runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
    runConfig.setVMArguments(execArgs.getVMArgumentsArray());
    runConfig.setWorkingDirectory(workingDirName);
    runConfig.setEnvironment(envp);
    runConfig.setVMSpecificAttributesMap(vmAttributesMap);

    // Bootpath
    String[] bootpath = getBootpath(configuration);
    if (bootpath != null && bootpath.length > 0) {
      runConfig.setBootClassPath(bootpath);
    }

    setDefaultSourceLocator(launch, configuration);

    if (ILaunchManager.PROFILE_MODE.equals(mode)) {
      try {
        ServerProfilerDelegate.configureProfiling(launch, vm, runConfig, monitor);
      } catch (CoreException ce) {
        sb.stopImpl();
        throw ce;
      }
    }

    // Launch the configuration
    sb.setupLaunch(launch, mode, monitor);
    try {
      runner.run(runConfig, launch, monitor);
      sb.addProcessListener(launch.getProcesses()[0]);
    } catch (Exception e) {
      // Ensure we don't continue to think the server is starting
      sb.stopImpl();
    }
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
    throw new CoreException(new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID, code,
        message, exception));
  }

}
