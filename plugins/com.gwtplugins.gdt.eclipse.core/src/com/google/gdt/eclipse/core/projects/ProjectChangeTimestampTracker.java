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
package com.google.gdt.eclipse.core.projects;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks timestamps of changes to projects determined by addition,
 * modification, or removal of resources in non-output folders that do not have
 * names (or ancestors with names) that start with '.'.
 * 
 * Note: This tracks non-output folders instead of just source folders since GWT
 * generators can depend on resources that aren't considered on Eclipse's
 * classpath (e.g., the developer could have excluded a resource from the
 * Eclipse classpath, but it is still accessible by generators.)
 * 
 * This only tracks changes to GPE projects (see
 * {@link ProjectUtilities#isGpeProject(org.eclipse.core.resources.IProject)}.)
 */
public enum ProjectChangeTimestampTracker implements IResourceChangeListener {
  INSTANCE;

  private static final QualifiedName KEY = new QualifiedName(
      ProjectChangeTimestampTracker.class.getName(), "projectChangeTimestamp");

  /**
   * Gets the project's changed timestamp, or 0 if a change hasn't happened yet.
   * 
   * @return the timestamp (from {@link System#currentTimeMillis()}) when the
   *         last change was recognized
   * @throws CoreException
   */
  public static long getProjectTimestamp(IProject project) throws CoreException {
    return getTimestampFromKey(project, KEY);
  }

  /**
   * Gets the timestamp from a project's properties.
   * 
   * @return the timestamp, or 0 if not found (or invalid)
   */
  public static long getTimestampFromKey(IProject project, QualifiedName key)
      throws CoreException {
    String timestampAsString = project.getPersistentProperty(key);
    try {
      return timestampAsString != null ? Long.parseLong(timestampAsString) : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static boolean doesResourceAncestorNameStartWithDot(IResource resource) {
    do {
      if (resource.getName().startsWith(".")) {
        return true;
      }
      resource = resource.getParent();
    } while (resource.getParent() != null);

    return false;
  }

  private static boolean isResourceInAnOutputPath(IResource resource)
      throws JavaModelException {
    IProject project = resource.getProject();
    IJavaProject javaProject = JavaCore.create(project);

    if (javaProject != null) {
      IPath resourcePath = resource.getFullPath();

      if (WebAppUtilities.isWebApp(project)) {
        if (WebAppUtilities.hasManagedWarOut(project)
            && WebAppUtilities.getManagedWarOut(project).getFullPath().isPrefixOf(
                resourcePath)) {
          return true;
        }

        IPath previousWarOutAbsPath = WebAppProjectProperties.getLastUsedWarOutLocation(project);
        if (previousWarOutAbsPath != null
            && previousWarOutAbsPath.isPrefixOf(resource.getLocation())) {
          return true;
        }
      }

      if (javaProject.getOutputLocation().isPrefixOf(resourcePath)) {
        return true;
      }

      IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(false);
      for (IClasspathEntry classpathEntry : resolvedClasspath) {
        IPath outputLocation = classpathEntry.getOutputLocation();
        if (outputLocation != null && outputLocation.isPrefixOf(resourcePath)) {
          return true;
        }
      }
    }

    return false;
  }

  private static void markProjectAsChanged(IProject project,
      Set<IProject> projectsMarkedAsChanged) throws CoreException {
    project.setPersistentProperty(KEY,
        String.valueOf(System.currentTimeMillis()));
    projectsMarkedAsChanged.add(project);
  }

  /**
   * @param projectsMarkedAsChanged cache of projects that have already been
   *          marked as changed so we do not check resources belonging to these
   *          projects
   */
  private static void visitDelta(IResourceDelta delta,
      Set<IProject> projectsMarkedAsChanged) throws CoreException {

    // Skip over non-GPE projects
    IResource resource = delta.getResource();
    IProject project = resource.getProject();
    if (project != null && !ProjectUtilities.isGpeProject(project)) {
      return;
    }

    // Exit early on projects we have already marked
    if (projectsMarkedAsChanged.contains(project)) {
      return;
    }

    if (ResourceUtils.isRelevantResourceChange(delta)) {
      if (!doesResourceAncestorNameStartWithDot(resource)
          && !isResourceInAnOutputPath(resource)) {
        markProjectAsChanged(project, projectsMarkedAsChanged);
        return;
      }
    }

    for (IResourceDelta childDelta : delta.getAffectedChildren()) {
      visitDelta(childDelta, projectsMarkedAsChanged);
    }
  }

  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    if (delta == null) {
      return;
    }

    try {
      Set<IProject> projectsMarkedAsChanged = new HashSet<IProject>();
      visitDelta(delta, projectsMarkedAsChanged);
    } catch (CoreException e) {
      CorePluginLog.logError(e,
          "Error while tracking project-level changes from a change in "
              + delta.getResource());
    }
  }

  public void startTracking() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  public void stopTracking() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }
}
