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
package com.google.gwt.eclipse.core.launch.processors.codeserver;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Main type for GWT SDM code server.
 *
 * This is similar to the web app main types, although this is pulled out so its not cyclic.
 */
public class SuperDevModeCodeServerMainTypeProcessor implements ILaunchConfigurationProcessor {

  /**
   * Enum for possible main types for a launch.
   */
  public enum MainType {
    GWT_SDM_MODE("com.google.gwt.dev.codeserver.CodeServer");
    public final String mainTypeName;

    private MainType(String mainTypeName) {
      this.mainTypeName = mainTypeName;
    }
  }

  /**
   * Interface used by extensions that supply their own MainTypeFinder implementation.
   */
  public interface MainTypeFinder {
    String findMainType(IJavaProject javaProject);
  }


  private static final String ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME = GWTPlugin.PLUGIN_ID
      + "MainTypeProcessor.PREVIOUSLY_SET_MAIN_TYPE_NAME";

  /**
   * @return whether the main type is one of the SDK-provided main types
   * @throws CoreException
   */
  public static boolean isMainTypeFromSdk(ILaunchConfiguration config) throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    for (MainType sdkMainType : MainType.values()) {
      if (sdkMainType.mainTypeName.equals(mainTypeName)) {
        return true;
      }
    }

    return false;
  }

  private static String getPreviouslySetMainTypeName(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME, (String) null);
    } catch (CoreException e) {
      CorePluginLog.logError(e, "Could not restore previously set main type, forcing overwrite.");
      return null;
    }
  }

  private static void setMainTypeName(ILaunchConfigurationWorkingCopy config, String mainType) {
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainType);
    config.setAttribute(ATTR_PREVIOUSLY_SET_MAIN_TYPE_NAME, mainType);
  }

  public SuperDevModeCodeServerMainTypeProcessor() {
  }

  @Override
  public void update(ILaunchConfigurationWorkingCopy config, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    String currentMainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    String previouslySetMainTypeName = getPreviouslySetMainTypeName(config);

    if (!StringUtilities.isEmpty(currentMainTypeName)
        && !currentMainTypeName.equals(previouslySetMainTypeName)) {
      // Our previously set main type must have been changed by the user, do not
      // adjust
      return;
    }

    String newMainTypeName = null;
    IProject project = javaProject.getProject();
    if (GWTNature.isGWTProject(project)) {
      newMainTypeName = MainType.GWT_SDM_MODE.mainTypeName;
    }

    if (StringUtilities.isEmpty(newMainTypeName) || newMainTypeName.equals(currentMainTypeName)) {
      return;
    }

    setMainTypeName(config, newMainTypeName);
  }

  @Override
  public String validate(ILaunchConfiguration config, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) {
    return null;
  }

}
