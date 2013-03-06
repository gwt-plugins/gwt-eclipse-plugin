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
 * Processes the "-server" argument. This is only present in GWT + GAE when
 * running a server.
 */
public class ServerArgumentProcessor implements ILaunchConfigurationProcessor {

  private static final String ARG_SERVER = "-server";
  private static final String SCL = "com.google.appengine.tools.development.gwt.AppEngineLauncher";

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    boolean runServer = WebAppLaunchConfiguration.getRunServer(launchConfig);
    int serverArgIndex = programArgs.indexOf(ARG_SERVER);

    IProject project = javaProject.getProject();
    if (runServer && GWTNature.isGWTProject(project)
        && GaeNature.isGaeProject(project)) {
      if (serverArgIndex == -1) {
        // Add if it is not present
        programArgs.add(0, ARG_SERVER);
        programArgs.add(1, SCL);
      }
    } else {
      if (serverArgIndex >= 0) {
        String server = LaunchConfigurationProcessorUtilities.getArgValue(
            programArgs, serverArgIndex + 1);
        if (server != null && server.equals(SCL)) {
          // Only if the parameter is what we set
          LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
              programArgs, serverArgIndex, true);
        }
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    return null;
  }
}
