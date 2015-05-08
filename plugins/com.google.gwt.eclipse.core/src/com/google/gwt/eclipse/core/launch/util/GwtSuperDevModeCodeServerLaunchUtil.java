/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.launch.util;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.GwtSuperDevModeLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.codeserver.SuperDevModeCodeServerLauncherDirArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.codeserver.SuperDevModeCodeServerMainTypeProcessor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Launch config utilities. <br/>
 * <br/>
 * This is similar to WebAppLaunchUtil, but pulled out not to be cyclic, and unique to code server.
 */
public class GwtSuperDevModeCodeServerLaunchUtil {

  /**
   * Create a new GWT SDM Code Server Configuration. This will occur when running the debug
   * configuration from shortcut.
   */
  public static ILaunchConfiguration createLaunchConfig(String launchConfigName, final IProject project)
      throws CoreException, OperationCanceledException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);
    ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(null, launchConfigName);

    // Project name
    LaunchConfigurationUtilities.setProjectName(launchConfig, project.getName());

    launchConfig.setMappedResources(new IResource[] {project});

    setDefaults(launchConfig, project);

    // Save the new launch configuration
    ILaunchConfiguration ilaunchConfig = launchConfig.doSave();

    return ilaunchConfig;
  }

  /**
   * Creating a new launch config, setup the default arguments.<br/>
   * <br/>
   * If the project doesn't have focus, the updateViaProcessors will not run.
   */
  public static void setDefaults(ILaunchConfigurationWorkingCopy launchConfig, IProject project) {
    launchConfig.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        ModuleClasspathProvider.computeProviderId(project));

    // Turn on Super Dev Mode
    GWTLaunchConfigurationWorkingCopy.setSuperDevModeEnabled(launchConfig, true);

    // Update program arg for - main type
    LaunchConfigurationProcessorUtilities.updateViaProcessor(new SuperDevModeCodeServerMainTypeProcessor(),
        launchConfig);

    // Update program arg for - GWT module
    LaunchConfigurationProcessorUtilities.updateViaProcessor(new ModuleArgumentProcessor(), launchConfig);
  }

  /**
   * Finds and returns an <b>existing</b> configuration to re-launch for the given URL, or
   * <code>null</code> if there is no existing configuration.
   *
   * @return a configuration to use for launching the given type or <code>null
   *         </code> if none
   * @throws CoreException
   */
  private static ILaunchConfiguration findLaunchConfiguration(IProject project) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType typeid =
        launchManager.getLaunchConfigurationType(GwtSuperDevModeLaunchConfiguration.TYPE_ID);
    ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

    return searchMatchingConfigWithProject(project, configs);
  }

  /**
   * Given a resource, infer the startup URL that the resource points at, then look for an existing
   * launch configuration that points at this URL. If none exists, we'll create a new one.
   *
   * @return the found or newly created launch configuration
   */
  private static ILaunchConfiguration findOrCreateLaunchConfiguration(IProject project, String launcherDir,
      String launcherId) throws CoreException, OperationCanceledException {
    ILaunchConfiguration config = findLaunchConfiguration(project);
    if (config == null) {
      config = createNewLaunchConfiguration(project);
    }

    // If a launcherDir value was provided from the WTP launcher, create or update the argument
    config = addOrModifyLauncherArgs(config, launcherDir, launcherId);

    return config;
  }

  /**
   * Add or modify the launcherDir program argument in the launch config.
   */
  private static ILaunchConfiguration addOrModifyLauncherArgs(ILaunchConfiguration config, String launcherDir,
      String launcherId) throws CoreException {
    ILaunchConfigurationWorkingCopy launchConfigWc = config.getWorkingCopy();

    if (launcherDir != null) {
      // Update the launcherDir argument
      GWTLaunchConfigurationWorkingCopy.setCodeServerLauncherDir(launchConfigWc, launcherDir);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new SuperDevModeCodeServerLauncherDirArgumentProcessor(), launchConfigWc);
    }

    // Update the launcherId
    if (launcherId != null) {
      launchConfigWc.setAttribute(GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID, launcherId);
    }

    config = launchConfigWc.doSave();

    return config;
  }

  /**
   * Given a specific resource, launch for that resource. This will involve either finding an
   * existing launch configuration, or making a new one.
   */
  public static void launch(IProject project, String mode) {
    launch(project, mode, null, null);
  }

  /**
   * Given a specific resource, launch for that resource. This will involve either finding an
   * existing launch configuration, or making a new one.
   *
   * @param launchMode - launch mode, like run or debug.
   * @param launcherDir - war directory path.
   * @param launcherId - provide an launch id to reference launch,
   *        GWTLaunchConstants.SUPERDEVMODE_LAUNCH_ID
   */
  public static void launch(IProject project, String launchMode, String launcherDir, String launcherId) {
    try {
      ILaunchConfiguration launchConfig = findOrCreateLaunchConfiguration(project, launcherDir, launcherId);

      // TODO check for running or terminated launcher and restart it

      DebugUITools.launch(launchConfig, launchMode);
    } catch (CoreException e) {
      CorePluginLog.logError(e, "CoreException: Aborting GWT Super Dev Mode Code Server launcher.");
    } catch (OperationCanceledException e) {
      CorePluginLog.logError(e, "OperationCancelException: Aborting GWT Super Dev Mode Code Server launcher.");
    }
  }

  private static ILaunchConfiguration searchMatchingConfigWithProject(IProject project, ILaunchConfiguration[] configs)
      throws CoreException {
    List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();
    for (ILaunchConfiguration config : configs) {
      if (LaunchConfigurationUtilities.getProjectName(config).equals(project.getName())) {
        candidates.add(config);
      }
    }

    if (candidates.isEmpty()) {
      return null;
    } else if (candidates.size() == 1) {
      return candidates.get(0);
    } else {
      return LaunchConfigurationUtilities.chooseConfiguration(candidates, getShell());
    }
  }

  /**
   * Create a new launch configuration.
   */
  private static ILaunchConfiguration createNewLaunchConfiguration(IProject project) throws CoreException,
      OperationCanceledException {
    String initialName = calculateLaunchConfigName(project);

    // Create a new launch config
    ILaunchConfiguration launchConfig = GwtSuperDevModeCodeServerLaunchUtil.createLaunchConfig(initialName, project);

    return launchConfig;
  }

  private static String calculateLaunchConfigName(IProject project) {
    return project.getName();
  }

  private static Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

}
