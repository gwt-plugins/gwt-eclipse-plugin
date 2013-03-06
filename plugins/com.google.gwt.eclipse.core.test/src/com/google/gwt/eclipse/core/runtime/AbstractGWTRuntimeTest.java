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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for GWT runtime tests.
 */
public abstract class AbstractGWTRuntimeTest extends AbstractGWTPluginTestCase {

  protected IPath defaultRuntimePath;

  protected GWTRuntime specificRuntime;

  protected IPath specificRuntimePath;

  protected void addClasspathEntryToTestProject(IClasspathEntry entry)
      throws JavaModelException {
    IJavaProject project = getTestProject();

    List<IClasspathEntry> cpEntries = new ArrayList<IClasspathEntry>();
    cpEntries.addAll(Arrays.asList(project.getRawClasspath()));
    cpEntries.add(entry);
    ClasspathUtilities.setRawClasspath(project, cpEntries);
    JobsUtilities.waitForIdle();
  }

  protected boolean assertGWTRuntimeEntry(IPath runtimePath,
      IClasspathEntry[] entries) {
    boolean hasGWTRuntime = false;

    for (IClasspathEntry entry : entries) {
      IPath entryPath = entry.getPath();

      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
        if (GWTRuntimeContainer.isPathForGWTRuntimeContainer(entryPath)) {
          // Make sure we have only one GWT runtime
          if (hasGWTRuntime) {
            return false;
          }

          // We found at a GWT runtime
          hasGWTRuntime = true;

          // Make sure it's the one we're looking for
          if (!entryPath.equals(runtimePath)) {
            return false;
          }
        }
      }

      // Make sure we don't have any gwt-user.jar dependencies
      if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
        String jarName = entryPath.lastSegment();
        if (jarName.equals(GWTRuntime.GWT_USER_JAR)) {
          return false;
        }
      }
    }

    return hasGWTRuntime;
  }

  protected void removeGWTRuntimeFromTestProject() throws Exception {
    IJavaProject project = getTestProject();

    // Replace GWT runtime classpath entry with gwt-user.jar and
    // gwt-dev-PLAT.jar
    List<IClasspathEntry> newEntries = new ArrayList<IClasspathEntry>();
    for (IClasspathEntry entry : project.getRawClasspath()) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
        if (GWTRuntimeContainer.isPathForGWTRuntimeContainer(entry.getPath())) {
          GWTJarsRuntime defaultSdk = GwtRuntimeTestUtilities.getDefaultRuntime();
          IPath gwtUserJar = defaultSdk.getInstallationPath().append(
              GWTRuntime.GWT_USER_JAR);
          newEntries.add(JavaCore.newLibraryEntry(gwtUserJar, null, null));

          IPath gwtDevJar = defaultSdk.getInstallationPath().append(
              Util.getDevJarName(defaultSdk.getInstallationPath()));
          newEntries.add(JavaCore.newLibraryEntry(gwtDevJar, null, null));
          continue;
        }
      }

      // Leave non-GWT runtime entries on the classpath as is
      newEntries.add(entry);
    }
    ClasspathUtilities.setRawClasspath(project, newEntries);
    JobsUtilities.waitForIdle();
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    specificRuntime = new GWTJarsRuntime("specific",
      GwtRuntimeTestUtilities.getDefaultRuntime().getInstallationPath());
    sdkSet.add(specificRuntime);
    GWTPreferences.setSdks(sdkSet);

    // Create container pointing to default runtime
    defaultRuntimePath = GWTRuntimeContainer.CONTAINER_PATH;

    // Create container pointing to newly-added runtime
    specificRuntimePath = GWTRuntimeContainer.CONTAINER_PATH.append(specificRuntime.getName());
  }

}
