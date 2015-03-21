/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.launch.processors.codeserver;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Handles the -launcherDir which is the path to the war directory.<br/>
 * <br/>
 * Super Dev Mode Code Server only.
 */
public class SuperDevModeCodeServerLauncherDirArgumentProcessor implements
    ILaunchConfigurationProcessor {

  public static final String LAUNCHERDIR_ARG = "-launcherDir";

  /**
   * Only update this argument when in GWT SDM mode.
   */
  @Override
  public void update(ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // Only GWT projects
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return;
    }

    // Only GWT projects with SDM mode CodeServer main type
    if (!GwtLaunchConfigurationProcessorUtilities.isSuperDevModeCodeServer(launchConfig)) {
      return;
    }

    int argIndex = programArgs.indexOf(LAUNCHERDIR_ARG);

    // This is created in the GWT WTP facet and started by WTP server
    String launcherDir = GWTLaunchConfiguration.getSuperDevModeCodeServerLauncherDir(launchConfig);

    // Skip if arg already exists, developer entered or this previously added it.
    if (argIndex > -1 && (launcherDir == null || launcherDir.isEmpty())) {
      return;
    }

    // Path to the output war directory
    String pathToWarOutDir = null;

    // TODO possibly prompt for directory
    IPath path = WebAppUtilities.getWarOutLocationOrPrompt(javaProject.getProject());
    if (path != null) {
      pathToWarOutDir = path.toFile().getAbsolutePath();
    }

    // Override the path with the launcher configuration stored attribute.
    // This is activated in the GWT WTP plugin.
    if (launcherDir != null) {
      pathToWarOutDir = launcherDir;
    }

    if (pathToWarOutDir == null) {
      // Not a WTP project or classic launcher config
      // TODO, should it prompt for one?
      return;
    }

    int insertionIndex =
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(programArgs,
            argIndex, true);

    // Add the args to the list
    programArgs.add(insertionIndex, LAUNCHERDIR_ARG);
    programArgs.add(insertionIndex + 1, pathToWarOutDir);

    // TODO commit working addition?
  }

  @Override
  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // TODO Possibly verify if actual directory exists.
    return null;
  }

}
