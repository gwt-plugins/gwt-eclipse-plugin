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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
 * Utility methods for manipulating generic projects.
 * 
 * TODO: Refactor the different java project creation code used by the test code
 * and WebAppProjectCreator into a single place.
 */
@SuppressWarnings("restriction")
public final class ProjectTestUtilities {
  /**
   * Creates a project with the specified name in the workspace.
   * 
   * @param projectName
   * @return new project
   * @throws CoreException
   */
  public static IProject createProject(String projectName) throws CoreException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (project.exists()) {
      throw new IllegalStateException("Project " + projectName
          + " already exists in this workspace");
    }

    IProgressMonitor monitor = new NullProgressMonitor();
    BuildPathsBlock.createProject(project, project.getLocationURI(), monitor);
    return project;
  }

  /**
   * Deletes the project with the given name.
   * 
   * @throws CoreException
   */
  public static void deleteProject(String projectName) throws CoreException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (!project.exists()) {
      throw new IllegalStateException("Project " + projectName
          + " does not exist in this workspace");
    }

    project.delete(true, new NullProgressMonitor());
  }

  private ProjectTestUtilities() {
  }
}
