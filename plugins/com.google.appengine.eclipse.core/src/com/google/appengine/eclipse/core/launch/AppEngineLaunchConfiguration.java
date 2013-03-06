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
package com.google.appengine.eclipse.core.launch;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Utility methods for App Engine launch configurations.
 */
public class AppEngineLaunchConfiguration {
  /**
   * Minimum version of App Engine SDK that requires a VM argument to specify
   * the KickStart Java agent.
   */
  private static final String MIN_SDK_VERSION_USING_KICKSTART_AGENT = "1.2.6";

  /**
   * Add any default VM arguments that are needed by App Engine. Note that these
   * are *not* the JVM arguments that are "magically" injected at launch time,
   * e.g. -XstartOnFirstThread.
   */
  public static void addDefaultVMArguments(
      ILaunchConfigurationWorkingCopy configuration, IProject project) {
    try {
      String javaAgentArg = computeExpectedJavaAgentVMArgument(project);
      if (javaAgentArg != null) {
        String vmArgs = configuration.getAttribute(
            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");
        if (vmArgs.length() > 0) {
          vmArgs += " ";
        }
        vmArgs += javaAgentArg;

        configuration.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
      }
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }
  }

  /**
   * Returns the VM flag to start the KickStart Java agent, or <code>null</code>
   * if the project does not require it.
   */
  public static String computeExpectedJavaAgentVMArgument(IProject project) {
    try {
      if (project == null || !GaeNature.isGaeProject(project)) {
        return null;
      }

      GaeSdk sdk = GaeSdk.findSdkFor(JavaCore.create(project));
      if (sdk == null || !sdk.validate().isOK()) {
        return null;
      }

      if (SdkUtils.compareVersionStrings(sdk.getVersion(),
          MIN_SDK_VERSION_USING_KICKSTART_AGENT) >= 0) {
        String agentPath = sdk.getInstallationPath().append(
            "lib/agent/appengine-agent.jar").toOSString();

        // Quote the path if there are spaces
        if (agentPath.indexOf(' ') > -1) {
          agentPath = "\"" + agentPath + "\"";
        }

        return "-javaagent:" + agentPath;
      }
    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    } catch (NumberFormatException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return null;
  }

  /*
   * Returns the unapplied job percentage HRD setting.
   */
  public static String getUnappliedJobPct(ILaunchConfiguration configuration)
      throws CoreException {

    return LaunchConfigurationAttributeUtilities.getString(configuration,
        AppEngineLaunchAttributes.HRD_UNAPPLIED_JOB_PERCENTAGE);
  }
}
