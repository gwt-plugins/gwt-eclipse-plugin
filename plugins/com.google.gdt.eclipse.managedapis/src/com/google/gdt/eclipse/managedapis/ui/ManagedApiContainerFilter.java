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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiContainer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * This element filter filters files from the Java Package View if they are
 * included in a SimpleDirContainer that is on the parent Java project's
 * classpath. This will prevent the user from right-clicking the file and adding
 * it to the build path as a CPE_LIBRARY classpath entry and thus prevent
 * duplication on the classpath.
 */
public class ManagedApiContainerFilter extends ViewerFilter {

  /**
   * @return false if the Java element is a file that is contained in a
   *         SimpleDirContainer that is in the classpath of the owning Java
   *         project (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
   *      java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (element instanceof IResource) {
      IResource resource = (IResource) element;
      if (resource.getType() == IResource.FILE
          || resource.getType() == IResource.FOLDER) {
        IProject project = resource.getProject();
        try {
          if (project != null && project.exists()
              && NatureUtils.hasNature(project, JavaCore.NATURE_ID)) {
            IJavaProject jp = JavaCore.create(resource.getProject());
            // lets see if this file is included within a ManagedApiRoot
            IClasspathEntry[] entries = jp.getRawClasspath();
            for (IClasspathEntry entry : entries) {
              if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                if (ManagedApiPlugin.API_CONTAINER_PATH.isPrefixOf(entry.getPath())) {
                  // this is likely a ManagedApiContainer, but the container
                  // could be a ghost, and thus unmapped to a
                  // ManagedApiContainer - check below
                  IClasspathContainer container = JavaCore.getClasspathContainer(
                      entry.getPath(), jp);
                  if (container instanceof ManagedApiContainer) {
                    ManagedApiContainer managedApiContainer = (ManagedApiContainer) container;
                    if (managedApiContainer.contains(resource)) {
                      // this file will is included in the container, so hide it
                      return false;
                    }
                  }
                }
              }
            }
          }
        } catch (JavaModelException e) {
          ManagedApiLogger.warn(e, "Error reading the classpath");
        } catch (CoreException e) {
          ManagedApiLogger.warn(e, "Error accessing Java Project");
        }
      }
    }
    return true;
  }
}
