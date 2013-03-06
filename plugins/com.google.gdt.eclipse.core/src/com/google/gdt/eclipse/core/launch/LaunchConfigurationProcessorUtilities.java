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

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.osgi.service.environment.Constants;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for launch configuration processors.
 * 
 * For more general methods relating to launch configurations, see
 * {@link LaunchConfigurationUtilities}.
 */
public final class LaunchConfigurationProcessorUtilities {

  /**
   * Creates an argument string from the given list of arguments. This takes
   * care of ensuring each argument is enclosed in quotes if required.
   */
  public static String createArgsString(List<String> args) {
    if (args == null) {
      return "";
    }

    StringBuilder argsSb = new StringBuilder();
    for (String arg : args) {
      if ((arg.contains(" ") || arg.contains("${"))
          && !StringUtilities.isQuoted(arg)) {
        argsSb.append(StringUtilities.quote(arg));
      } else {
        argsSb.append(arg);
      }

      argsSb.append(' ');
    }

    // Remove trailing space
    if (argsSb.length() > 0) {
      argsSb.setLength(argsSb.length() - 1);
    }

    return argsSb.toString();
  }

  /**
   * Returns the argument value that should be at the given index. If the index
   * is out of bounds or the string at the index is another argument (i.e.
   * starts with '-'), null is returned.
   */
  public static String getArgValue(List<String> args, int argValueIndex) {
    if (argValueIndex >= args.size() || argValueIndex < 0) {
      return null;
    }

    String argValue = args.get(argValueIndex);
    if (argValue.startsWith("-")) {
      return null;
    }

    return argValue;
  }

  /**
   * Finds the arg in the given args, gets its value, and ensures it is in the
   * possibleValues array.
   */
  public static String getArgValueFromUpperCaseChoices(List<String> args,
      String arg, String[] possibleValues, String defaultValue) {
    int argIndex = args.indexOf(arg);
    if (argIndex == -1) {
      return defaultValue;
    }

    String value = LaunchConfigurationProcessorUtilities.getArgValue(args,
        argIndex + 1);
    if (value == null) {
      return defaultValue;
    }

    value = value.toUpperCase();

    if (Arrays.asList(possibleValues).indexOf(value) == -1) {
      return defaultValue;
    } else {
      return value;
    }
  }

  /**
   * Returns a {@link ClassLoader} that contains all of the entries returned by
   * {@link #getClasspath(ILaunchConfiguration)}.
   */
  public static ClassLoader getClassLoaderFor(ILaunchConfiguration configuration)
      throws CoreException {
    String[] classpath = getClasspath(configuration);
    List<URL> urls;
    try {
      urls = toURLs(classpath);
    } catch (MalformedURLException e) {
      throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
          "Could not get a class loader for the launch config", e));
    }
    return new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
  }

  /**
   * @see JavaLaunchDelegate#getClasspath(ILaunchConfiguration)
   */
  public static String[] getClasspath(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    JavaLaunchDelegate javaLaunchDelegate = new JavaLaunchDelegate();
    return javaLaunchDelegate.getClasspath(launchConfiguration);
  }

  /**
   * @return the main type, or null
   */
  public static String getMainTypeName(ILaunchConfiguration config)
      throws CoreException {
    return config.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
  }

  /**
   * @return the WAR output directory, or null
   */
  public static String getWarDirectory(IJavaProject javaProject) {
    if (!WebAppUtilities.isWebApp(javaProject.getProject())) {
      return null;
    }

    IFolder warFolder = WebAppUtilities.getManagedWarOut(javaProject.getProject());
    return warFolder != null ? warFolder.getRawLocation().toOSString() : null;
  }

  /**
   * Parses the arguments string into a list of arguments. This takes care of
   * removing quoted arguments (for example, if an argument has a space.)
   * <p>
   * Any double-quotes that need to be preserved should be escaped (for example,
   * "arg\\\"1 arg2", where a list consisting of "arg\"1" and "arg2" will be
   * returned.) Any double-quotes that are not escaped will be treated as
   * grouping e.g. white-space separated words into a single arg. Note that
   * {@link #createArgsString(List)} will properly escape when assembling a
   * single arg string from a list of args (the opposite of this method.)
   */
  public static List<String> parseArgs(String args) {
    List<String> argsList = new ArrayList<String>();
    String[] argsArray = DebugPlugin.parseArguments(args);

    if (Platform.getOS().equals(Constants.OS_WIN32)) {
      /*
       * On Windows, DebugPlugin.parseArguments escapes embedded quotes, but for
       * other platforms, it does not. Undo this Windows-specific behavior since
       * we will re-escape the quotes when creating a single args string from a
       * list of args.
       */
      for (int i = 0; i < argsArray.length; i++) {
        argsArray[i] = argsArray[i].replaceAll("\\\\\\\"", "\"");
      }
    }

    argsList.addAll(Arrays.asList(argsArray));
    return argsList;
  }

  /**
   * Parses the program arguments from the launch configuration into a list of
   * arguments.
   * 
   * @throws CoreException
   */
  public static List<String> parseProgramArgs(ILaunchConfiguration launchConfig)
      throws CoreException {
    return parseArgs(launchConfig.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""));
  }

  /**
   * @see #parseProgramArgs(ILaunchConfiguration)
   */
  public static List<String> parseVmArgs(ILaunchConfiguration launchConfig)
      throws CoreException {
    return parseArgs(launchConfig.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""));
  }

  /**
   * Removes the argument at the given index.
   * 
   * @param args the list of arguments
   * @param argIndex the argument index to remove (out of bounds is allowed to
   *          simplify client logic)
   * @param tryRemovingArgValue (whether the given argument has a value which
   *          should be removed as well)
   * @return the index of the removed arg, or 0 (so this can be used as an
   *         insertion index)
   */
  public static int removeArgsAndReturnInsertionIndex(List<String> args,
      int argIndex, boolean tryRemovingArgValue) {
    if (argIndex < 0 || argIndex >= args.size()) {
      return 0;
    }

    if (tryRemovingArgValue) {
      String argValue = getArgValue(args, argIndex + 1);
      if (argValue != null) {
        args.remove(argIndex + 1);
      }
    }

    args.remove(argIndex);

    return argIndex;
  }

  /**
   * Uses the processor to update the given configuration.
   */
  public static void updateViaProcessor(
      ILaunchConfigurationProcessor processor,
      ILaunchConfigurationWorkingCopy configuration) {
    IJavaProject javaProject = LaunchConfigurationUtilities.getJavaProject(configuration);
    if (javaProject != null && javaProject.exists()) {
      try {
        List<String> programArgs = parseProgramArgs(configuration);
        List<String> vmArgs = parseVmArgs(configuration);
        processor.update(configuration, javaProject, programArgs, vmArgs);
        configuration.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
            LaunchConfigurationProcessorUtilities.createArgsString(programArgs));
        configuration.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
            LaunchConfigurationProcessorUtilities.createArgsString(vmArgs));
      } catch (Throwable e) {
        CorePluginLog.logError(e, "Could not update the launch configuration");
      }
    }
  }

  private static List<URL> toURLs(String... paths) throws MalformedURLException {
    List<URL> urls = new ArrayList<URL>(paths.length);
    for (int i = 0; i < paths.length; ++i) {
      File file = new File(paths[i]);
      if (file.exists()) {
        urls.add(file.toURI().toURL());
      }
    }
    return urls;
  }

  private LaunchConfigurationProcessorUtilities() {
  }
}
