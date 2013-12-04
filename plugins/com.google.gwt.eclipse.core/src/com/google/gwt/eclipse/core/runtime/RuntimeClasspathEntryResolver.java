/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Special RuntimeClasspathEntryResolver for GWT projects that mangles the classpath for OOPHM to
 * work if the appropriate launch settings are set.
 * 
 * TODO(rdayal): Investigate the removal of this class. Now that we've removed the old
 * "transitional OOPHM" code, it's not clear that this is needed anymore. It seems to be used by the
 * ModuleClasspathProvider, but I'm not sure if this needs to be a separate class.
 */
public class RuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver {

  private static final IRuntimeClasspathEntry[] NO_ENTRIES = new IRuntimeClasspathEntry[0];

  public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
      IJavaProject project) throws CoreException {
    GWTRuntime gwtSdk = findGWTSdk(entry);
    if (gwtSdk == null) {
      return NO_ENTRIES;
    }

    IClasspathEntry[] classpathEntries = gwtSdk.getClasspathEntries();

    return resolveClasspathEntries(Arrays.asList(classpathEntries));
  }

  public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
      ILaunchConfiguration configuration) throws CoreException {
    GWTRuntime gwtSdk = findGWTSdk(entry);
    if (gwtSdk == null) {
      return NO_ENTRIES;
    }

    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>(
        Arrays.asList(gwtSdk.getClasspathEntries()));

    return resolveClasspathEntries(classpathEntries);
  }

  public IVMInstall resolveVMInstall(IClasspathEntry entry) {
    return null;
  }

  /**
   * Expand out the list of dependencies for a given IJavaProject.
   */
  private List<IRuntimeClasspathEntry> dependenciesForProject(IJavaProject project)
      throws CoreException {
    ArrayList<IRuntimeClasspathEntry> out = new ArrayList<IRuntimeClasspathEntry>();
    String[] deps = JavaRuntime.computeDefaultRuntimeClassPath(project);

    for (String dep : deps) {
      IRuntimeClasspathEntry cpEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(new Path(dep));
      out.add(cpEntry);
    }

    return out;
  }

  private GWTRuntime findGWTSdk(IRuntimeClasspathEntry entry) {
    GWTRuntime gwtSdk = GWTPreferences.getSdkManager().findSdkForPath(entry.getPath());

    if (gwtSdk != null) {
      return gwtSdk;
    }

    /*
     * If the project for which we're trying to resolve these entries is a GWT runtime project, and
     * the entry that we're trying to resolve is actually the the GWT Runtime project itself, then
     * we'll synthesize a contributor SDK and return that.
     */
    if (entry.getType() != IRuntimeClasspathEntry.PROJECT) {
      return null;
    }

    String entryProjectName = entry.getPath().lastSegment();
    IJavaProject entryJavaProject = JavaProjectUtilities.findJavaProject(entryProjectName);

    if (entryJavaProject != null && GWTProjectsRuntime.isGWTRuntimeProject(entryJavaProject)) {
      gwtSdk = GWTProjectsRuntime.syntheziseContributorRuntime();

      if (gwtSdk.validate().isOK()) {
        return gwtSdk;
      }
    }

    return null;
  }

  /**
   * Given a list of IClasspathEntry, produce an array of IRuntimeClasspathEntry based on that list.
   */
  private IRuntimeClasspathEntry[] resolveClasspathEntries(List<IClasspathEntry> classpathEntries)
      throws CoreException {
    LinkedHashSet<IRuntimeClasspathEntry> runtimeClasspathEntries = new LinkedHashSet<IRuntimeClasspathEntry>();

    for (IClasspathEntry classpathEntry : classpathEntries) {
      if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
        String projectName = classpathEntry.getPath().lastSegment();
        IJavaProject theproject = JavaProjectUtilities.findJavaProject(projectName);

        IRuntimeClasspathEntry projectEntry = JavaRuntime.newProjectRuntimeClasspathEntry(theproject);
        runtimeClasspathEntries.add(projectEntry);
        runtimeClasspathEntries.addAll(dependenciesForProject(theproject));
      } else {
        runtimeClasspathEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(classpathEntry.getPath()));
      }
    }

    return runtimeClasspathEntries.toArray(NO_ENTRIES);
  }
}
