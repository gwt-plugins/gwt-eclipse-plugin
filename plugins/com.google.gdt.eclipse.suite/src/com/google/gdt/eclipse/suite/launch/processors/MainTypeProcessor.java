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
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.ClasspathUtilities.ClassFinder;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Sets the main type for a launch configuration. The policy is to set a
 * relevant main type, unless the user has manually changed the main type since
 * we last set it. We detect this by storing our previously set main type and
 * comparing it to the current value. If equal, we try to update to the main
 * type applicable to the current project configuration.
 */
public class MainTypeProcessor implements ILaunchConfigurationProcessor {

  /**
   * Enum for possible main types for a launch.
   */
  public enum MainType {
    GWT_DEV_MODE("com.google.gwt.dev.DevMode"), GWT_HOSTED_MODE(
        "com.google.gwt.dev.HostedMode"), GWT_SHELL(
        "com.google.gwt.dev.GWTShell"), GAE_APP_SERVER(
        "com.google.appengine.tools.development.DevAppServerMain");
    public final String mainTypeName;

    private MainType(String mainTypeName) {
      this.mainTypeName = mainTypeName;
    }
  }

  /**
   * Interface used by extensions that supply their own MainTypeFinder
   * implementation.
   */
  public interface MainTypeFinder {
    String findMainType(IJavaProject javaProject);
  }

  private static final String ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME = GdtPlugin.PLUGIN_ID
      + "MainTypeProcessor.PREVIOUSLY_SET_MAIN_TYPE_NAME";

  /**
   * @return whether the main type is one of the SDK-provided main types
   * @throws CoreException
   */
  public static boolean isMainTypeFromSdk(ILaunchConfiguration config)
      throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    for (MainType sdkMainType : MainType.values()) {
      if (sdkMainType.mainTypeName.equals(mainTypeName)) {
        return true;
      }
    }

    return false;
  }

  private static String computeMainTypeName(
      ILaunchConfigurationWorkingCopy config, IJavaProject javaProject,
      ClassFinder classFinder) throws CoreException {

    IProject project = javaProject.getProject();

    ExtensionQuery<MainTypeProcessor.MainTypeFinder> extQuery = new ExtensionQuery<MainTypeProcessor.MainTypeFinder>(
        GdtPlugin.PLUGIN_ID, "mainTypeFinder", "class");
    List<ExtensionQuery.Data<MainTypeProcessor.MainTypeFinder>> mainTypeFinders = extQuery.getData();
    for (ExtensionQuery.Data<MainTypeProcessor.MainTypeFinder> mainTypeFinder : mainTypeFinders) {
      String mainTypeFromExtension = mainTypeFinder.getExtensionPointData().findMainType(
          javaProject);
      if (mainTypeFromExtension != null) {
        return mainTypeFromExtension;
      }
    }

    if (GWTNature.isGWTProject(project)) {

      if (!WebAppUtilities.isWebApp(project)) {
        // We can use GWT shell for non-WAR projects (typically these are GWT
        // 1.5 and older projects)

        return MainType.GWT_SHELL.mainTypeName;
      }

      ClassLoader classLoader = LaunchConfigurationProcessorUtilities.getClassLoaderFor(config);
      if (classFinder.exists(classLoader, MainType.GWT_DEV_MODE.mainTypeName)) {
        return MainType.GWT_DEV_MODE.mainTypeName;
      }

      if (classFinder.exists(classLoader, MainType.GWT_HOSTED_MODE.mainTypeName)) {
        return MainType.GWT_HOSTED_MODE.mainTypeName;
      }

      // Fallback
      return MainType.GWT_SHELL.mainTypeName;

    } else if (GaeNature.isGaeProject(project)) {
      return MainType.GAE_APP_SERVER.mainTypeName;
    } else {
      return null;
    }
  }

  private static String getPreviouslySetMainTypeName(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME,
          (String) null);
    } catch (CoreException e) {
      CorePluginLog.logError(e,
          "Could not restore previously set main type, forcing overwrite.");
      return null;
    }
  }

  private static void setMainTypeName(ILaunchConfigurationWorkingCopy config,
      String mainType) {
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
        mainType);
    config.setAttribute(ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME, mainType);
  }

  private final ClassFinder classFinder;

  public MainTypeProcessor(ClasspathUtilities.ClassFinder classFinder) {
    this.classFinder = classFinder;
  }

  public void update(ILaunchConfigurationWorkingCopy config,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    String currentMainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    String previouslySetMainTypeName = getPreviouslySetMainTypeName(config);

    if (!StringUtilities.isEmpty(currentMainTypeName)
        && !currentMainTypeName.equals(previouslySetMainTypeName)) {
      // Our previously set main type must have been changed by the user, do not
      // adjust
      return;
    }

    String newMainTypeName = computeMainTypeName(config, javaProject,
        classFinder);
    if (StringUtilities.isEmpty(newMainTypeName)
        || newMainTypeName.equals(currentMainTypeName)) {
      return;
    }

    setMainTypeName(config, newMainTypeName);
  }

  public String validate(ILaunchConfiguration config, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) {
    return null;
  }
}
