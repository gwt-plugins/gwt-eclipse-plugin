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
package com.google.gdt.eclipse.core.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * General utility methods for launch configurations.
 * 
 * For {@link ILaunchConfigurationProcessor} related methods, see
 * {@link LaunchConfigurationProcessorUtilities}.
 */
public class LaunchConfigurationUtilities {

  /**
   * Returns a configuration from the given collection of configurations that
   * should be launched, or <code>null</code> to cancel. Default implementation
   * opens a selection dialog that allows the user to choose one of the
   * specified launch configurations. Returns the chosen configuration, or
   * <code>null</code> if the user cancels.
   * 
   * @param configList list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  public static ILaunchConfiguration chooseConfiguration(
      List<ILaunchConfiguration> configList, Shell shell) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    try {
      ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell,
          labelProvider);
      dialog.setElements(configList.toArray());
      dialog.setTitle("Choose a launch configuration:");
      dialog.setMessage("More than one launch configuration is applicable; please choose one:");
      dialog.setMultipleSelection(false);
      int result = dialog.open();
      if (result == Window.OK) {
        return (ILaunchConfiguration) dialog.getFirstResult();
      }
      return null;
    } finally {
      labelProvider.dispose();
    }
  }

  /**
   * @return the Java project, or null
   */
  public static IJavaProject getJavaProject(
      ILaunchConfiguration launchConfiguration) {
    try {
      return JavaRuntime.getJavaProject(launchConfiguration);
    } catch (CoreException e) {
      // When the attribute holding the Java project had an issue, assume no
      // Java project
      return null;
    }
  }

  /**
   * @param typeIds launch configuration type ids that will be searched for, or
   *          empty to match all
   * @throws CoreException
   */
  public static List<ILaunchConfiguration> getLaunchConfigurations(
      IProject project, String... typeIds) throws CoreException {
    Set<String> setOfTypeIds = new HashSet<String>(Arrays.asList(typeIds));
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    List<ILaunchConfiguration> launchConfigs = new ArrayList<ILaunchConfiguration>();
    for (ILaunchConfiguration launchConfig : manager.getLaunchConfigurations()) {
      IJavaProject javaProject = getJavaProject(launchConfig);
      boolean typeIdIsOk = setOfTypeIds.isEmpty()
          || setOfTypeIds.contains(launchConfig.getType().getIdentifier());
      if (javaProject != null && project.equals(javaProject.getProject())
          && typeIdIsOk) {
        launchConfigs.add(launchConfig);
      }
    }

    return launchConfigs;
  }

  public static IProject getProject(ILaunchConfiguration launchConfiguration) {
    try {
      String projectName = getProjectName(launchConfiguration);
      return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    } catch (CoreException e) {
      return null;
    }
  }

  public static String getProjectName(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return launchConfiguration.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
  }

  public static void setProjectName(
      ILaunchConfigurationWorkingCopy workingCopy, String projectName) {
    workingCopy.setAttribute(
        IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
  }

}
