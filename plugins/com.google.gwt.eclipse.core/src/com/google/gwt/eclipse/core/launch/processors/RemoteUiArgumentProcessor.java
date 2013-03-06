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

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GwtCapabilityChecker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the GWT "-remoteUI" argument.
 */
public class RemoteUiArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String ARG_REMOTE_UI = "-remoteUI";

  static boolean isUseRemoteUiEnvVarFalse(ILaunchConfiguration configuration)
      throws CoreException {
    String[] environmentVariables = DebugPlugin.getDefault().getLaunchManager().getEnvironment(
        configuration);
    if (environmentVariables != null) {
      for (String environmentVariable : environmentVariables) {
        if (environmentVariable.matches("\\s*USE_REMOTE_UI\\s*=\\s*false\\s*")) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean shouldUseRemoteUI(ILaunchConfiguration configuration)
      throws CoreException {
    return !isUseRemoteUiEnvVarFalse(configuration);
  }

  // Package-scoped for testing.
  GwtCapabilityChecker.Factory gwtCapabilityCheckerFactory = new GwtCapabilityChecker.Factory();

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    int remoteUiArgIndex = programArgs.indexOf(ARG_REMOTE_UI);
    // Prefer the existing value, fallback on the precanned one
    String remoteUiValue = null;
    if (remoteUiArgIndex >= 0) {
      remoteUiValue = LaunchConfigurationProcessorUtilities.getArgValue(
          programArgs, remoteUiArgIndex + 1);
    }
    if (StringUtilities.isEmpty(remoteUiValue)) {
      remoteUiValue = "${gwt_remote_ui_server_port}:${unique_id}";
    }

    int insertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        programArgs, remoteUiArgIndex, true);

    IProject project = javaProject.getProject();
    if (GWTNature.isGWTProject(project) && shouldUseRemoteUI(launchConfig)) {
      programArgs.add(insertionIndex, ARG_REMOTE_UI);
      programArgs.add(insertionIndex + 1, remoteUiValue);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    return null;
  }
}
