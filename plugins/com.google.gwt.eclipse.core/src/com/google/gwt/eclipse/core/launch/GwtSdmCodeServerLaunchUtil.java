/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.launch;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.codeserver.SdmCodeServerMainTypeProcessor;
import com.google.gwt.eclipse.core.launch.processors.codeserver.SdmCodeServerPortArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.codeserver.SdmSrcArgumentProcessor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Launch utilities.
 *
 * This is similar to WebAppLaunchUtil, but pulled out not to be cyclic,
 * and unique to code server.
 */
public class GwtSdmCodeServerLaunchUtil {

  /**
   * Create a new GWT SDM Code Server Configuration.
   * This will occur when running the debug configuration from shortcut.
   *
   * @throws CoreException
   * @throws OperationCanceledException
   */
  public static ILaunchConfigurationWorkingCopy createLaunchConfigWorkingCopy(
      String launchConfigName, final IProject project,
      boolean isExternal) throws CoreException, OperationCanceledException {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(GwtSdmLaunchConfiguration.TYPE_ID);

    final ILaunchConfigurationWorkingCopy wc = type.newInstance(null,
        launchConfigName);

    // TODO move the below into this possibly?
    setDefaults(wc, project);

    // project name
    LaunchConfigurationUtilities.setProjectName(wc, project.getName());

    // main type processor
    ClasspathUtilities.ClassFinder classFinder = new ClasspathUtilities.ClassFinder();
    SdmCodeServerMainTypeProcessor maintTypeProcessor = new SdmCodeServerMainTypeProcessor(
        classFinder);

    // src processor
    SdmSrcArgumentProcessor srcProcessor = new SdmSrcArgumentProcessor();

    // module processor
    ModuleArgumentProcessor moduleProcessor = new ModuleArgumentProcessor();

    // sdm code server port
    SdmCodeServerPortArgumentProcessor codeServerPort = new SdmCodeServerPortArgumentProcessor();

    // add main type, -src, module & -port
    LaunchConfigurationProcessorUtilities.updateViaProcessor(maintTypeProcessor, wc);
    LaunchConfigurationProcessorUtilities.updateViaProcessor(srcProcessor, wc);
    LaunchConfigurationProcessorUtilities.updateViaProcessor(moduleProcessor, wc);
    LaunchConfigurationProcessorUtilities.updateViaProcessor(codeServerPort, wc);

    wc.setMappedResources(new IResource[] {project});

    return wc;
  }

  /**
   * Given a resource, figure out what the target URL should be for a launch
   * configuration corresponding to this resource. If the resource has several
   * possible URLs that correspond to it, this method will cause a dialog to pop
   * up, asking the user to choose one. If the resource's project uses GAE but
   * not GWT, return empty string; GAE-only projects have no startup url. If the
   * resource's project uses GWT 2.0+, return empty string since no URL is
   * started in dev mode.
   *
   * @param resource
   * @throws CoreException
   */
  public static String determineStartupURL(IResource resource,
      boolean isExternalLaunch) throws CoreException {
    ILaunchShortcutStrategy strategy = null;
    IProject project = resource.getProject();

    ExtensionQuery<ILaunchShortcutStrategyProvider> extQuery = new ExtensionQuery<ILaunchShortcutStrategyProvider>(
        GWTPlugin.PLUGIN_ID, "launchShortcutStrategy", "class");
    List<ExtensionQuery.Data<ILaunchShortcutStrategyProvider>> strategyProviderInfos = extQuery.getData();

    for (ExtensionQuery.Data<ILaunchShortcutStrategyProvider> data : strategyProviderInfos) {
      strategy = data.getExtensionPointData().getStrategy(project);
      break;
    }

    if (strategy == null && WebAppUtilities.isWebApp(project)) {
      strategy = new WebAppLaunchShortcutStrategy();
    }

    return strategy.generateUrl(resource, isExternalLaunch);
  }

  public static ILaunchConfiguration findConfigurationByName(String name) {
    try {
      String configTypeStr = "";
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType typeid = launchManager.getLaunchConfigurationType(configTypeStr);
      ILaunchConfiguration[] configs = launchManager.getLaunchConfigurations(typeid);

      for (ILaunchConfiguration config : configs) {
        if (config.getName().equals(name)) {
          return config;
        }
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
    return null;
  }

  /**
   * Returns the project associated with this launch configuration, or <code>
   * null</code>
   * if there is no project assigned or if the project does not exist.
   */
  public static IProject getProject(ILaunchConfiguration configuration)
      throws CoreException {
    String projectName = LaunchConfigurationUtilities.getProjectName(configuration);
    if (projectName.length() == 0) {
      return null;
    }

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    return project.exists() ? project : null;
  }

  public static void setDefaults(ILaunchConfigurationWorkingCopy configuration,
      IProject project) {
    configuration.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        ModuleClasspathProvider.computeProviderId(project));
  }

}
