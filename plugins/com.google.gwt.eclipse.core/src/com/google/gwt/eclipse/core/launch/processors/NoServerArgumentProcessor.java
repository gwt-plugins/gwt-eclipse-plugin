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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the GWT "-noserver" argument.
 */
public class NoServerArgumentProcessor implements ILaunchConfigurationProcessor {

  private static final String ARG_NO_SERVER = "-noserver";

  public static boolean hasNoServerArg(List<String> args) {
    return args.indexOf(ARG_NO_SERVER) >= 0;
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    IProject project = javaProject.getProject();
    int noServerArgIndex = programArgs.indexOf(ARG_NO_SERVER);

    int insertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        programArgs, noServerArgIndex, false);
    if (!WebAppLaunchConfiguration.getRunServer(launchConfig)
        && GWTNature.isGWTProject(project)) {
      programArgs.add(insertionIndex, ARG_NO_SERVER);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    // The server tab will show the error
    return null;
  }
}
