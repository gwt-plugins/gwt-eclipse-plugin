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
package com.google.gwt.eclipse.core;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility methods for project-related tasks.
 */
public class GWTProjectUtilities {

  /**
   * Returns the GWT-applicable source folder paths from a project (note: this
   * will not traverse into the project's dependencies, for this behavior, see
   * {@link #getGWTSourceFolderPathsFromProjectAndDependencies(IJavaProject, boolean)}
   * ).
   *
   * @param javaProject Reference to the project
   * @param sourceEntries The list to be filled with the entries corresponding
   *          to the source folder paths
   * @param includeTestSourceEntries Whether to include the entries for test
   *          source
   * @throws SdkException
   */
  private static void fillGWTSourceFolderPathsFromProject(
      IJavaProject javaProject, Collection<? super IRuntimeClasspathEntry>
      sourceEntries, boolean includeTestSourceEntries) throws SdkException {

    assert (javaProject != null);

    if (GWTProjectsRuntime.isGWTRuntimeProject(javaProject)) {
      // TODO: Do we still need to handle this here since Sdk's report their
      // own runtime classpath entries?
      sourceEntries.addAll(GWTProjectsRuntime.getGWTRuntimeProjectSourceEntries(
          javaProject, includeTestSourceEntries));
    } else {
      try {
        for (IClasspathEntry curClasspathEntry : javaProject.getRawClasspath()) {
          if (curClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
            IPath sourcePath = curClasspathEntry.getPath();
            // If including tests, include all source, or if not including tests, ensure
            // it is not a test path
            if (includeTestSourceEntries || !GWTProjectUtilities.isTestPath(sourcePath)) {
              if (!isOptional(curClasspathEntry) || exists(sourcePath)) {
                sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(sourcePath));
              }
            }
          }
        }
        IFolder folder = javaProject.getProject().getFolder("super");
        if (folder.exists()) {
          sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(folder.getFullPath()));
        }
      } catch (JavaModelException jme) {
        GWTPluginLog.logError(jme,
            "Unable to retrieve raw classpath for project "
                + javaProject.getProject().getName());
      }
    }
  }

  /**
   * Returns the GWT-applicable source folder paths from a project and all of
   * its transitively required projects.
   * @throws SdkException
   * @throws JavaModelException
   *
   * @see #fillGWTSourceFolderPathsFromProject(IJavaProject, Collection, boolean)
   */
  public static List<IRuntimeClasspathEntry> getGWTSourceFolderPathsFromProjectAndDependencies(
      IJavaProject javaProject, boolean includeTestSourceEntries)
      throws JavaModelException, SdkException {
    List<IRuntimeClasspathEntry> sourceEntries =
        new ArrayList<IRuntimeClasspathEntry>();
    for (IJavaProject curJavaProject :
        JavaProjectUtilities.getTransitivelyRequiredProjects(javaProject)) {
      fillGWTSourceFolderPathsFromProject(curJavaProject, sourceEntries,
          includeTestSourceEntries);
    }
    return sourceEntries;
  }

  public static boolean isTestPath(IPath path) {
    String[] segments = path.segments();
    int length = segments.length;
    String last = length < 1 ? null : segments[length - 1];
    String lastButOne = length < 2 ? null : segments[length - 2];
    String lastButTwo = length < 3 ? null : segments[length - 3];
    return "test".equals(last) || "javatests".equals(last) || "src".equals(lastButTwo) && "test".equals(lastButOne);
  }

  private static boolean exists(IPath sourcePath) {
    return ResourcesPlugin.getWorkspace().getRoot().exists(sourcePath);
  }

  private static boolean isOptional(IClasspathEntry entry) {
    IClasspathAttribute[] attributes = entry.getExtraAttributes();
    for (IClasspathAttribute attribute : attributes) {
      if (IClasspathAttribute.OPTIONAL.equals(attribute.getName()) && "true".equals(attribute.getValue())) //$NON-NLS-1$
        return true;
    }
    return false;
  }

}
