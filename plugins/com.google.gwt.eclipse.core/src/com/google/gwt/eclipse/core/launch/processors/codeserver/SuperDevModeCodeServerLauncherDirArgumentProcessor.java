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
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFolder;
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

    // Skip if arg already exists, developer entered or this previously added it.
    if (argIndex > -1) {
      return;
    }

    // Get the war output path, such as target/.../webapp or ./war
    // TODO get launch configuration working directory
    IPath pathToWarOut = WebAppUtilities.getWarOutLocationOrPrompt(javaProject.getProject()); // This won't work, b/c GAE is in tmp dir



    // Path to the war, web content, context-root directory
    String pathToWarDir = null;

    // Is this a WTP facet Dynamic web module config or classic launcher config
    if (pathToWarOut != null) {
      pathToWarDir = pathToWarOut.toFile().getAbsolutePath();

    } else {
      // TODO is this even needed ***********************************
      // This is a classic launcher config managed war directory
      IFolder warDir = WebAppUtilities.getManagedWarOut(javaProject.getProject());
      if (warDir != null) {
        pathToWarDir = warDir.getLocation().toFile().getAbsolutePath();
      }
      // TODO ********************************************************
    }

    if (pathToWarDir == null) {
      // Not a WTP project or classic launcher config,
      return;
    }

    int insertionIndex =
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(programArgs,
            argIndex, true);

    // add the args to the program arguments list -launcherDir = /path/To/war
    programArgs.add(insertionIndex, LAUNCHERDIR_ARG);
    programArgs.add(insertionIndex + 1, pathToWarDir);
  }

  @Override
  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // TODO Possibly verify if actual directory exists.
    return null;
  }

}
