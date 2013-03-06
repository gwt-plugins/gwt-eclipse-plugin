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
package com.google.gdt.eclipse.maven.launch;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.MavenUtils;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates the runtime classpath based on the Maven build path.
 */
public class MavenClasspathProvider extends ModuleClasspathProvider {

  /**
   * Provides the id of a maven-based classpath provider (for the
   * org.eclipse.jdt.launching.classpathProvider extension point).
   */
  public static class MavenClasspathProviderIdProvider implements
      ModuleClasspathProvider.IModuleClasspathProviderIdProvider {

    public String getProviderId(IProject project) {
      if (!MavenUtils.hasMavenNature(project)) {
        return null;
      }

      return "com.google.gdt.eclipse.maven.mavenClasspathProvider";
    }
  }

  private static void addAppengineToolsJarIfPossible(IJavaProject proj,
      Set<IRuntimeClasspathEntry> classpath) throws CoreException {
    GaeSdk sdk = GaeSdk.findSdkFor(proj);
    if (sdk != null) {
      IStatus validationStatus = sdk.validate();
      if (!validationStatus.isOK()) {
        throw new CoreException(validationStatus);
      }

      IPath toolsJarPath = sdk.getInstallationPath().append(
          GaeSdk.APPENGINE_TOOLS_API_JAR_PATH);
      IRuntimeClasspathEntry toolsJar = JavaRuntime.newArchiveRuntimeClasspathEntry(toolsJarPath);
      classpath.add(toolsJar);
    } else {
      Activator.getDefault().getLog().log(
          new Status(
              IStatus.WARNING,
              Activator.PLUGIN_ID,
              "Unable to find a GAE SDK for project "
                  + proj.getElementName()
                  + ". Cannot add appengine-tools-api to the runtime classpath."));
    }
  }

  private static void addGwtDevjarIfPossible(IJavaProject proj,
      Set<IRuntimeClasspathEntry> classpath) throws CoreException {
    GWTRuntime runtime = GWTRuntime.findSdkFor(proj);
    IStatus validationStatus = runtime.validate();
    if (!validationStatus.isOK()) {
      throw new CoreException(validationStatus);
    }

    try {
      if (runtime != null) {
        IPath devJarPath = Path.fromOSString(runtime.getDevJar().getAbsolutePath());
        IRuntimeClasspathEntry devJarCpEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(devJarPath);
        classpath.add(devJarCpEntry);
      } else {
        Activator.getDefault().getLog().log(
            new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                "Unable to find a GWT Runtime for project "
                    + proj.getElementName()
                    + ". Cannot add gwt-dev to the runtime classpath."));
      }
    } catch (SdkException sdke) {
      throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
          "Unable to add gwt-dev.jar to the runtime classpath.", sdke));
    }
  }

  /**
   * Given a list of resolved runtime classpath entries, search for any jars
   * that are blacklisted from being present on the system classpath. Right now,
   * the <code>asm-*.jar</code> are the only such jars.
   */
  private static void removeBlacklistedAppEngineJars(IJavaProject proj,
      List<IRuntimeClasspathEntry> classpath) {
    List<IRuntimeClasspathEntry> entriesToRemove = new ArrayList<IRuntimeClasspathEntry>();

    for (IRuntimeClasspathEntry cpEntry : classpath) {

      if (cpEntry.getType() != IRuntimeClasspathEntry.ARCHIVE) {
        continue;
      }

      IPath cpEntryPath = cpEntry.getPath();
      if (cpEntryPath.lastSegment().startsWith("asm-")) {
        entriesToRemove.add(cpEntry);
        continue;
      }
    }

    classpath.removeAll(entriesToRemove);
  }

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
      ILaunchConfiguration config) throws CoreException {
    IRuntimeClasspathEntry[] unresolvedClasspathEntries = super.computeUnresolvedClasspath(config);
    IJavaProject proj = JavaRuntime.getJavaProject(config);
    if (proj == null) {
      return unresolvedClasspathEntries;
    }

    /*
     * Figure out if we are supposed to be relying on the default classpath or
     * not. The default classpath is the one that is generated for a launch
     * configuration based on the launch configuration's project's build
     * classpath.
     * 
     * To determine whether or not to rely on the default classpath, we look at
     * the ATTR_DEFAULT_CLASSPATH attribute of the launch configuration. This
     * attribute is set whenever the user makes a change to the launch
     * configuration classpath using the add/remove buttons. From this point on,
     * Eclipse will respect the user's changes and will not replace their
     * entries with the classpath that it computes.
     * 
     * However, users can specify that they want to restore the behavior of
     * having Eclipse compute the classpath by clicking on the "Restore Default
     * Entries" button. This causes the ATTR_DEFAULT_ATTRIBUTE to be unset for a
     * launch configuration.
     */
    boolean useDefault = config.getAttribute(
        IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);

    if (!useDefault) {
      return unresolvedClasspathEntries;
    }

    if (!MavenUtils.hasMavenNature(proj.getProject())) {
      return unresolvedClasspathEntries;
    }

    // Use a LinkedHashSet to prevent dupes
    Set<IRuntimeClasspathEntry> classpath = new LinkedHashSet<IRuntimeClasspathEntry>(
        unresolvedClasspathEntries.length);
    classpath.addAll(Arrays.asList(unresolvedClasspathEntries));

    // add GAE tools api jar
    if (GaeNature.isGaeProject(proj.getProject())) {
      addAppengineToolsJarIfPossible(proj, classpath);
    }

    // Add GWT dev jar
    if (GWTNature.isGWTProject(proj.getProject())) {
      addGwtDevjarIfPossible(proj, classpath);
    }

    return classpath.toArray(new IRuntimeClasspathEntry[classpath.size()]);
  }

  @Override
  public IRuntimeClasspathEntry[] resolveClasspath(
      IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
      throws CoreException {

    IRuntimeClasspathEntry[] resolvedEntries = super.resolveClasspath(entries,
        configuration);

    IJavaProject proj = JavaRuntime.getJavaProject(configuration);
    if (proj == null) {
      return resolvedEntries;
    }

    List<IRuntimeClasspathEntry> resolvedEntriesList = new ArrayList<IRuntimeClasspathEntry>(
        Arrays.asList(resolvedEntries));
    if (GaeNature.isGaeProject(proj.getProject())) {
      removeBlacklistedAppEngineJars(proj, resolvedEntriesList);
    }

    return resolvedEntriesList.toArray(new IRuntimeClasspathEntry[resolvedEntriesList.size()]);
  }
}
