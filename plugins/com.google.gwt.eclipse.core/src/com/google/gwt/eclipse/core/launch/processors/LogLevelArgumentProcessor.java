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
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the GWT "-logLevel" argument.
 */
public class LogLevelArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String[] LOG_LEVELS = {
      "ERROR", "WARN", "INFO", "TRACE", "DEBUG", "SPAM", "ALL"};

  public static final String DEFAULT_LOG_LEVEL = "INFO";

  private static final String ARG_LOG_LEVEL = "-logLevel";

  public static String getValidLogLevel(List<String> args) {
    return LaunchConfigurationProcessorUtilities.getArgValueFromUpperCaseChoices(
        args, ARG_LOG_LEVEL, LOG_LEVELS, DEFAULT_LOG_LEVEL);
  }

  private static int getLogLevelArgIndex(List<String> args) {
    return args.indexOf(ARG_LOG_LEVEL);
  }

  /*
   * TODO: remove this and have different sets of processors based on the launch
   * config type
   */
  private static boolean isApplicable(ILaunchConfiguration config)
      throws CoreException {
    return WebAppLaunchConfiguration.TYPE_ID.equals(config.getType().getIdentifier());
  }

  public void update(ILaunchConfigurationWorkingCopy config,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    if (!isApplicable(config)) {
      return;
    }

    int insertionIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        programArgs, getLogLevelArgIndex(programArgs), true);
    String persistedLogLevel = GWTLaunchConfiguration.getLogLevel(config);
    if (GWTNature.isGWTProject(javaProject.getProject())
        && !StringUtilities.isEmpty(persistedLogLevel)) {
      programArgs.add(insertionIndex, ARG_LOG_LEVEL);
      programArgs.add(insertionIndex + 1, persistedLogLevel);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    if (!isApplicable(launchConfig)) {
      return null;
    }

    int logLevelArgIndex = getLogLevelArgIndex(programArgs);
    if (logLevelArgIndex == -1) {
      return null;
    }

    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return "Log level argument is only valid for GWT projects";
    }

    String logLevel = LaunchConfigurationProcessorUtilities.getArgValue(
        programArgs, logLevelArgIndex + 1);
    if (StringUtilities.isEmpty(logLevel)) {
      return "Argument for specifying a log level is missing or invalid";
    }

    return null;
  }
}
