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
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Processes the "-dgwt.devjar" argument.
 */
public class DGwtDevJarArgumentProcessor implements
    ILaunchConfigurationProcessor {

  private static final String ARG_DGWT_DEVJAR = "-Dgwt.devjar=";

  /**
   * Returns the path to the gwt-dev-xxx.jar in the event that the launch
   * configuration depends on a GWT Contributor Runtime. Otherwise, returns the
   * empty string.
   */
  private static String maybeGetDevJarPath(IJavaProject project) {

    /*
     * In order to figure out whether or not to add the -Dgwt.devjar argument to
     * the list of VM args, we have to figure out the runtime that this launch
     * configuration depends on. If the project is one of the GWT Runtime
     * projects, then we'll definitely have to add the -Dgwt.devjar argument to
     * the launch configuration.
     */
    try {
      if (GWTProjectsRuntime.isGWTRuntimeProject(project)) {
        // Synthesize a temporary contributor SDK so that we can use it
        // to compute the devjar path
        GWTRuntime tempContribSDK = GWTProjectsRuntime.syntheziseContributorRuntime();

        if (tempContribSDK.validate().isOK()) {
          return tempContribSDK.getDevJar().getAbsolutePath();
        } else {
          return "";
        }
      }

      GWTRuntime sdk = GWTRuntime.findSdkFor(project);
      if (sdk == null) {
        MessageConsole messageConsole =
            MessageConsoleUtilities.getMessageConsole(
                project.getProject().getName() + "-GWT", null);
        messageConsole.activate();
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(
            new IConsole[] {messageConsole});
        final ConsoleColorProvider consoleColorProvider =
            new ConsoleColorProvider();
        final MessageConsoleStream console = messageConsole.newMessageStream();
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            console.setColor(consoleColorProvider.getColor(
                IDebugUIConstants.ID_STANDARD_ERROR_STREAM));
          }
        });
        console.println("GWT SDK not installed.");
        try {
          console.close();
        } catch (IOException e) {
          GWTPluginLog.logError(e, "Unable to close output stream to console");
        }
        return "";
      } else if (sdk.usesGwtDevProject()) {
        File gwtDevJarFile = sdk.getDevJar();
        return gwtDevJarFile.getAbsolutePath();
      }
    } catch (SdkException sdke) {
      GWTPluginLog.logError(sdke,
          "Unable to extract gwt dev jar argument from GWTProjectsRuntime");
    } catch (JavaModelException jme) {
      GWTPluginLog.logError(jme,
          "Unable to extract gwt dev jar argument from GWTProjectsRuntime");
    }
    return "";
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    int devJarIndex = StringUtilities.indexOfThatStartsWith(vmArgs,
        ARG_DGWT_DEVJAR, 0);

    int insertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        vmArgs, devJarIndex, false);

    if (GWTNature.isGWTProject(javaProject.getProject())) {
      String devJarPath = maybeGetDevJarPath(javaProject);
      if (!StringUtilities.isEmpty(devJarPath)) {
        vmArgs.add(insertionIndex, ARG_DGWT_DEVJAR + devJarPath);
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    return null;
  }
}
