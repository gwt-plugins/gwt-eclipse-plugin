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
package com.google.gwt.eclipse.core.runtime.tools;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that is used to invoke GWT's WebAppCreator as a process.
 */
public class WebAppProjectCreatorRunner {

  public static final String GWT_VERSION_WITH_TEMPLATES = "2.3.0";

  /**
   * Calls GWT webAppCreator to produce a sample project.
   *
   * @param qualifiedModuleName module name to use for the project
   * @param outDir directory where the project will be created
   * @param gwtRuntime GWT environment
   * @param monitor progress monitor
   * @param extraClassPath other JARs and resources to use for project generation, possibly
   *        containing templates to use for project generation; only supported after GWT version
   *        2.3.0
   * @param templates templates to use for the project generation; "standard" or {@code null}
            produces the default template, other values are only supported after GWT version 2.3.0
   */
  public static void createProject(String qualifiedModuleName, String outDir,
      GWTRuntime gwtRuntime, IProgressMonitor monitor, String[] extraClassPath,
      String... templates)
      throws CoreException {
    try {
      int processStatus = ProcessUtilities.launchProcessAndWaitFor(
          computeWebAppCreatorCommandLine(qualifiedModuleName, outDir,
              gwtRuntime, templates, extraClassPath), null, System.out, null);

      if (processStatus != 0) {
        // FIXME: Capture WebAppCreator's output and make it available via the
        // error log.
        throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
            "Invocation of " + computeWebAppCreatorClassName()
                + " failed. See the error log for more details."));
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } catch (InterruptedException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }
  }

  private static String computeClasspath(GWTRuntime gwtRuntime, String[] extraClassPath)
      throws CoreException {
    List<String> cpPaths = new ArrayList<String>();
    for (IClasspathEntry c : gwtRuntime.getClasspathEntries()) {
      if (c.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
        IJavaProject javaProject = JavaProjectUtilities.findJavaProject(c.getPath().toOSString());
        IRuntimeClasspathEntry projectRuntimeEntry = JavaRuntime.newDefaultProjectClasspathEntry(javaProject);
        IRuntimeClasspathEntry[] resolvedEntries = JavaRuntime.resolveRuntimeClasspathEntry(
            projectRuntimeEntry, javaProject);
        for (IRuntimeClasspathEntry resolvedEntry : resolvedEntries) {
          cpPaths.add(resolvedEntry.getLocation());
        }
      } else {
        cpPaths.add(c.getPath().toFile().getAbsolutePath());
      }
    }
    if (extraClassPath != null) {
      cpPaths.addAll(Arrays.asList(extraClassPath));
    }
    return ProcessUtilities.buildClasspathString(cpPaths);
  }

  private static String computeWebAppCreatorClassName() {
    return "com.google.gwt.user.tools.WebAppCreator";
  }

  private static List<String> computeWebAppCreatorCommandLine(
      String qualifiedModuleName, String outDir, GWTRuntime gwtRuntime,
      String[] templates, String[] extraClassPath)
      throws CoreException {
    List<String> commandLine = new ArrayList<String>();
    // add the fully qualified path to java
    String javaExecutable = ProcessUtilities.findJavaExecutableForEclipse();
    commandLine.add(javaExecutable);

    try {
      commandLine.add("-Dgwt.devjar="
          + gwtRuntime.getDevJar().getAbsolutePath());
    } catch (SdkException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }

    // add the classpath
    commandLine.add("-cp");
    commandLine.add(computeClasspath(gwtRuntime, extraClassPath));

    // add the GWT WebAppCreator class name
    commandLine.add(computeWebAppCreatorClassName());

    // Add the arguments to WebAppCreator
    commandLine.addAll(computeWebAppCreatorOptions(qualifiedModuleName, outDir,
        templates, gwtRuntime));

    return commandLine;
  }

  private static List<String> computeWebAppCreatorOptions(
      String qualifiedModuleName, String outDir, String[] templates,
      GWTRuntime gwtRuntime) throws CoreException {
    List<String> options = new ArrayList<String>();
    options.add("-out");
    options.add(outDir);
    
    String version = gwtRuntime.getVersion();
    if (SdkUtils.isInternal(version)
        || SdkUtils.compareVersionStrings(version, GWT_VERSION_WITH_TEMPLATES) >= 0) {
      String templatesList;
      if (templates != null && templates.length != 0) {
        templatesList = templates[0];
        for (int i = 1; i < templates.length; i++) {
          templatesList += "," + templates[i];
        }
      } else {
        templatesList = "sample";
      }
      options.add("-templates");
      options.add(templatesList);
    } else if (templates != null && templates.length != 1
        && !templates[0].equals("sample")) {
      /* WebAppProjectCreatorRunner is being requested to use templates
       * (that is, templates contains something other than "standard"),
       * for a GWT version that does not support this feature.
       */
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Invocation of " + computeWebAppCreatorClassName()
              + " failed. GWT before " + GWT_VERSION_WITH_TEMPLATES
              + " does not support templates."));
    }

    options.add(qualifiedModuleName);
    return options;
  }
}
