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
package com.google.gdt.eclipse.suite.launch;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfigurationWorkingCopy;
import com.google.gdt.eclipse.suite.launch.processors.WarArgumentProcessor;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.ILaunchShortcutStrategyProvider;
import com.google.gwt.eclipse.core.launch.LegacyGWTLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;
import com.google.gwt.eclipse.core.launch.WebAppLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
 */
public class WebAppLaunchUtil {

  /**
   * @throws CoreException
   * @throws OperationCanceledException
   */
  public static ILaunchConfigurationWorkingCopy createLaunchConfigWorkingCopy(
      String launchConfigName, final IProject project, String url,
      boolean isExternal) throws CoreException, OperationCanceledException {

    assert (url != null);

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(WebAppLaunchConfiguration.TYPE_ID);

    final ILaunchConfigurationWorkingCopy wc = type.newInstance(null,
        launchConfigName);

    setDefaults(wc, project);
    LaunchConfigurationUtilities.setProjectName(wc, project.getName());
    if (isExternal) {
      WebAppLaunchConfigurationWorkingCopy.setRunServer(wc, false);
    }
    GWTLaunchConfigurationWorkingCopy.setStartupUrl(wc, url);

    IPath warDir = null;
    if (WebAppUtilities.hasManagedWarOut(project)) {
      warDir = WebAppUtilities.getManagedWarOut(project).getLocation();
    }

    if (warDir != null) {
      // The processor will update to the proper argument style for the current
      // project nature(s)
      WarArgumentProcessor warArgProcessor = new WarArgumentProcessor();
      warArgProcessor.setWarDirFromLaunchConfigCreation(warDir.toOSString());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(warArgProcessor,
          wc);
    }
    // Link the launch configuration to the project. This will cause the
    // launch config to be deleted automatically if the project is deleted.
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

    if (strategy == null) {

      if (WebAppLaunchUtil.projectIsGaeOnly(project)) {
        return "";
      }

      if (WebAppUtilities.isWebApp(project)) {
        strategy = new WebAppLaunchShortcutStrategy();
      } else {
        assert (GWTNature.isGWTProject(project));
        strategy = new LegacyGWTLaunchShortcutStrategy();
      }
    }

    return strategy.generateUrl(resource, isExternalLaunch);
  }

  public static ILaunchConfiguration findConfigurationByName(String name) {
    try {
      String configTypeStr = WebAppLaunchConfiguration.TYPE_ID;
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

  public static boolean projectIsGaeOnly(IProject project) {
    return GaeNature.isGaeProject(project) && !GWTNature.isGWTProject(project);
  }

  public static void setDefaults(ILaunchConfigurationWorkingCopy configuration,
      IProject project) {
    configuration.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
        ModuleClasspathProvider.computeProviderId(project));
  }
}
