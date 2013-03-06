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
package com.google.gwt.eclipse.core.compile;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.ProcessUtilities.IProcessReceiver;
import com.google.gdt.eclipse.core.jobs.ProgressMonitorCanceledWatcher;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.GWTProjectUtilities;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerArtifactsRemover;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a GWT compilation on a project.
 */
public class GWTCompileRunner {

  private static final Pattern EXTRACT_QUOTED_ARGS_PATTERN = Pattern.compile("^([\"'])(.*)([\"'])$");

  private static final Pattern SPLIT_QUOTED_ARGS_PATTERN = Pattern.compile("[^\\s\"]+|\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");

  /**
   * @param processReceiver optional, receives the process after it is started
   */
  public static void compile(IJavaProject javaProject, IPath warLocation,
      GWTCompileSettings settings, OutputStream consoleOutputStream,
      IProcessReceiver processReceiver) throws IOException,
      InterruptedException, CoreException, OperationCanceledException {
    IProject project = javaProject.getProject();

    if (warLocation != null) {
      // Remove any existing Speed Tracer launch artifacts
      new SpeedTracerArtifactsRemover(warLocation.toFile()).removeAll();
    }

    if (settings.getEntryPointModules().isEmpty()) {
      // Nothing to compile, so just return.
      return;
    }

    int processStatus = ProcessUtilities.launchProcessAndWaitFor(
        computeCompilerCommandLine(javaProject, warLocation, settings),
        project.getLocation().toFile(), consoleOutputStream, processReceiver);

    /*
     * Do a refresh on the war folder if it's in the workspace. This ensures
     * that Eclipse sees the generated artifacts from the GWT compile, and
     * doesn't complain about stale resources during subsequent file searches.
     */
    if (warLocation != null) {
      for (IContainer warFolder : ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(
          warLocation)) {
        warFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
      }
    }

    if (processStatus != 0) {
      if (processReceiver != null && processReceiver.hasDestroyedProcess()) {
        PrintWriter printWriter = new PrintWriter(consoleOutputStream);
        printWriter.println("GWT compilation terminated by the user.");
        printWriter.flush();
        throw new OperationCanceledException();
      } else {
        throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
            "GWT compilation failed"));
      }
    }
  }

  /**
   * @see #compile(IJavaProject, IPath, GWTCompileSettings, OutputStream,
   *      IProcessReceiver)
   */
  public static void compileWithCancellationSupport(IJavaProject javaProject,
      IPath warLocation, GWTCompileSettings settings,
      OutputStream consoleOutputStream, IProcessReceiver processReceiver,
      IProgressMonitor monitor,
      ProgressMonitorCanceledWatcher.Listener cancellationListener)
      throws IOException, InterruptedException, CoreException,
      OperationCanceledException {

    monitor.setTaskName("Performing GWT compile");
    ProgressMonitorCanceledWatcher monitorWatcher = new ProgressMonitorCanceledWatcher(
        monitor, cancellationListener);
    monitorWatcher.start();
    try {
      GWTCompileRunner.compile(javaProject, warLocation, settings,
          consoleOutputStream, processReceiver);
    } finally {
      monitorWatcher.stop();
    }
  }

  public static String computeTaskName(IProject project) {
    return project.getName() + " - GWT Compile";
  }

  /**
   * Computes a GWT compiler-tailored list of classpath entries for the given
   * Java project.
   */
  /*
   * This is package-scoped so it can be JUnit tested.
   */
  static List<IRuntimeClasspathEntry> computeClasspath(IJavaProject javaProject)
      throws CoreException {
    // Get the unresolved runtime classpath
    IRuntimeClasspathEntry[] unresolvedRuntimeClasspath = JavaRuntime.computeUnresolvedRuntimeClasspath(javaProject);
    List<IRuntimeClasspathEntry> resolvedRuntimeClasspath = new ArrayList<IRuntimeClasspathEntry>();

    for (IRuntimeClasspathEntry unresolvedClasspathEntry : unresolvedRuntimeClasspath) {
      if (JavaRuntime.isVMInstallReference(unresolvedClasspathEntry)) {
        continue;
      }

      // Add resolved entries for this unresolved entry
      resolvedRuntimeClasspath.addAll(Arrays.asList(JavaRuntime.resolveRuntimeClasspathEntry(
          unresolvedClasspathEntry, javaProject)));
    }

    /*
     * Prepend the resolved classpath with the source entries (parallels the
     * launch config's ordering of entries)
     */
    try {
      resolvedRuntimeClasspath.addAll(
          0,
          GWTProjectUtilities.getGWTSourceFolderPathsFromProjectAndDependencies(
              javaProject, false));
    } catch (SdkException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }

    /*
     * Try and add gwt-dev jar to the compiler classpath. It may already be on
     * the build classpath, but in some cases (i.e. when using Maven), it may
     * not be. Adding it to the end of the classpath (even if is duplicated)
     * does no harm.
     * 
     * TODO: Consider invoking the appropriate ModuleClasspathProvider.
     */
    GWTRuntime gwtRuntime = GWTRuntime.findSdkFor(javaProject);
    if (gwtRuntime == null) {
      GWTPluginLog.logWarning("Unable to find GWT runtime for project "
          + javaProject.getElementName()
          + ", will try to continue GWT compilation..");
      return resolvedRuntimeClasspath;
    }

    IStatus validationStatus = gwtRuntime.validate();
    if (!validationStatus.isOK()) {
      GWTPluginLog.logWarning("GWT runtime for project "
          + javaProject.getElementName() + " is not valid: "
          + validationStatus.getMessage()
          + ". Will attempt to proceed with GWT compilation anyway.");
      return resolvedRuntimeClasspath;
    }

    try {
      File gwtDevJar = gwtRuntime.getDevJar();
      resolvedRuntimeClasspath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(Path.fromOSString(gwtDevJar.getAbsolutePath())));
    } catch (SdkException e) {
      GWTPluginLog.logWarning(
          e,
          "Unable to add gwt-dev.jar to the compiler's classpath; will attempt GWT compilation anyway.");
    }

    return resolvedRuntimeClasspath;
  }

  private static String computeCompilerClassName(IJavaProject javaProject)
      throws JavaModelException {
    IClasspathEntry classpathContainer = ClasspathUtilities.findClasspathEntryContainer(
        javaProject.getRawClasspath(), GWTRuntimeContainer.CONTAINER_ID);
    if (classpathContainer != null) {
      GWTRuntime sdk = GWTPreferences.getSdkManager().findSdkForPath(
          classpathContainer.getPath());
      if (sdk != null) {
        if (!sdk.containsSCL()) {
          return "com.google.gwt.dev.GWTCompiler";
        }
      }
    }

    // Default to using the post-GWT 1.6 compiler class
    return "com.google.gwt.dev.Compiler";
  }

  /**
   * Computes the command line arguments required to invoke the GWT compiler for
   * this project.
   */
  private static List<String> computeCompilerCommandLine(
      IJavaProject javaProject, IPath warLocation, GWTCompileSettings settings)
      throws CoreException {
    List<String> commandLine = new ArrayList<String>();
    // add the fully qualified path to java
    String javaExecutable = ProcessUtilities.computeJavaExecutableFullyQualifiedPath(javaProject);
    commandLine.add(javaExecutable);

    commandLine.addAll(GWTLaunchConfiguration.computeCompileDynamicVMArgsAsList(
        javaProject, false));

    commandLine.addAll(splitArgs(VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(
        settings.getVmArgs())));

    // add the classpath
    commandLine.add("-cp");
    commandLine.add(ClasspathUtilities.flattenToClasspathString(computeClasspath(javaProject)));

    // add the GWT compiler class name
    commandLine.add(computeCompilerClassName(javaProject));

    // add the GWT compiler options
    commandLine.addAll(computeCompilerOptions(warLocation, settings));

    // add the startup modules
    commandLine.addAll(settings.getEntryPointModules());

    return commandLine;
  }

  private static List<String> computeCompilerOptions(IPath warLocation,
      GWTCompileSettings settings) {
    List<String> options = new ArrayList<String>();

    String logLevel = settings.getLogLevel();
    if (logLevel != null && logLevel.trim().length() > 0) {
      options.add("-logLevel");
      options.add(logLevel);
    }

    String outputStyle = settings.getOutputStyle();
    if (outputStyle != null && outputStyle.trim().length() > 0) {
      options.add("-style");
      options.add(outputStyle);
    }

    if (warLocation != null) {
      options.add("-war");
      options.add(warLocation.toOSString());
    }

    options.addAll(splitArgs(settings.getExtraArgs()));

    return options;
  }

  private static List<String> splitArgs(String args) {
    List<String> options = new ArrayList<String>();

    if (args != null && args.trim().length() > 0) {
      Matcher matcher = SPLIT_QUOTED_ARGS_PATTERN.matcher(args);
      while (matcher.find()) {
        // Strip leading and trailing quotes from the arg
        String arg = matcher.group();
        Matcher qmatcher = EXTRACT_QUOTED_ARGS_PATTERN.matcher(arg);
        if (qmatcher.matches()) {
          options.add(qmatcher.group(2));
        } else {
          options.add(arg);
        }
      }
    }

    return options;
  }

}
