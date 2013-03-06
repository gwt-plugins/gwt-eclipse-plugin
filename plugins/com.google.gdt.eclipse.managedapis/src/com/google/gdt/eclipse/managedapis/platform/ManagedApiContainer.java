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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

import java.text.MessageFormat;

/**
 * TODO: doc me.
 */
@SuppressWarnings("restriction")
public class ManagedApiContainer implements IClasspathContainer {

  /**
   * A convenience method for
   * getManagedApiForClassPathContainer(ManagedApiProject, ClassPathContainer).
   * This version determines the project from the classpath container and then
   * calls the proxied method.
   * 
   * @param classPathContainer
   * @return
   */
  public static ManagedApi getManagedApiForClassPathContainer(
      ClassPathContainer classPathContainer) {
    ManagedApiProject project = getManagedApiProjectForClassPathContainer(classPathContainer);
    return getManagedApiForClassPathContainer(project, classPathContainer);
  }

  /**
   * For a project and a ClassPathContainer, this method identifies and returns
   * the ManagedApi instance associated with the project.
   */
  public static ManagedApi getManagedApiForClassPathContainer(
      ManagedApiProject managedApiProject, ClassPathContainer classPathContainer) {
    IClasspathEntry icpe = classPathContainer.getClasspathEntry();
    String localPath = icpe.getPath().removeFirstSegments(1).toString();
    return managedApiProject != null
        ? managedApiProject.findManagedApi(localPath) : null;
  }

  /**
   * This method determines the ManagedApiProject that contains a given
   * ClassPathContainer.
   */
  public static ManagedApiProject getManagedApiProjectForClassPathContainer(
      ClassPathContainer classPathContainer) {
    ManagedApiProject managedApiProject = null;
    IJavaProject javaProject = null;
    try {
      javaProject = classPathContainer.getJavaProject();
      managedApiProject = ManagedApiProjectImpl.getManagedApiProject(javaProject);
    } catch (CoreException e) {
      ManagedApiLogger.warn(
          e,
          MessageFormat.format(
              "Caught core exception while trying to access ManagedApi for project {0}",
              javaProject != null ? javaProject.getProject().getName()
                  : "unknown"));
    }
    return managedApiProject;
  }

  /**
   * Determines whether the provided element is in fact the ClassPathContainer
   * correlated to a ManagedApiConstainer.
   */
  public static boolean isManagedApiContainer(Object element) {
    boolean isMatch = false;
    if (element instanceof ClassPathContainer) {
      ClassPathContainer cpc = (ClassPathContainer) element;
      IClasspathEntry icpe = cpc.getClasspathEntry();
      isMatch = (ManagedApiPlugin.API_CONTAINER_PATH_ID.equals(icpe.getPath().segment(
          0)));
    }
    return isMatch;
  }

  private ManagedApi managedApi;

  public ManagedApiContainer(ManagedApiProject project, IPath path) {
    this.managedApi = project.createManagedApi(path.removeFirstSegments(1).toString());
  }

  public boolean contains(IResource file) {
    if (managedApi != null && file != null) {
      IFolder rootDir = managedApi.getRootDirectory();
      if (rootDir != null) {
        return rootDir.getFullPath().isPrefixOf(file.getFullPath());
      }
    }
    return false;
  }

  public IClasspathEntry[] getClasspathEntries() {
    try {
      return managedApi.getClasspathEntries();
    } catch (CoreException e) {
      ManagedApiLogger.log(
          ManagedApiLogger.ERROR,
          e,
          MessageFormat.format(
              "Core Exception caught while accessing classpath entries for {0} in {1}",
              managedApi.getName(), managedApi.getRootDirectory().toString()));
      return new IClasspathEntry[0];
    }
  }

  public String getDescription() {
    return managedApi.getDisplayName();
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public ManagedApi getManagedApi() {
    return managedApi;
  }

  public IPath getPath() {
    return null;
  }
}
