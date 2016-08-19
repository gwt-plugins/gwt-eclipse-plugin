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

import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

/**
 * Utility methods for manipulating generic projects.
 */
@SuppressWarnings("restriction")
public final class ProjectUtilities {

  // Access to this variable needs to be synchronized, as it is initialized
  // in plugin initialization code, and may be accessed by different threads
  // later on.
  private static IWebAppProjectCreator.Factory webAppProjectCreatorFactory;

  /**
   * Creates a populated project via the {@link IWebAppProjectCreator}.
   *
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   * @throws MalformedURLException
   * @throws BackingStoreException
   * @throws ClassNotFoundException
   * @throws SdkException
   * @throws CoreException
   * @throws IOException
   */
  public static IProject createPopulatedProject(String projectName,
      IWebAppProjectCreator.Participant... creationParticipants) throws MalformedURLException,
      ClassNotFoundException, UnsupportedEncodingException, FileNotFoundException, CoreException,
      SdkException, BackingStoreException, IOException {

    IWebAppProjectCreator projectCreator = ProjectUtilities.createWebAppProjectCreator();
    projectCreator.setProjectName(projectName);
    projectCreator.setPackageName(projectName.toLowerCase());

    for (IWebAppProjectCreator.Participant participant : creationParticipants) {
      participant.updateWebAppProjectCreator(projectCreator);
    }

    projectCreator.create(new NullProgressMonitor());

    return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
  }

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
   * Do not call this from startup code, as it may not have been initialized yet
   * (if you do, you must catch the {@link IllegalStateException}).
   *
   * @return an instance of the {@link IWebAppProjectCreator}
   * @throws IllegalStateException when trying to use this before it has been
   *           initialized (should only happen if your startup code uses this)
   */
  public static synchronized IWebAppProjectCreator createWebAppProjectCreator() {
    if (webAppProjectCreatorFactory == null) {
      throw new IllegalStateException(
          "Trying to get the web app project creator before it has been initialized.");
    }

    return webAppProjectCreatorFactory.create();
  }

  /**
   * Deletes the project with the given name.
   *
   * @throws CoreException
   */
  public static void deleteProject(String projectName) throws CoreException {
    deleteProject(projectName, true);
  }

  /**
   * Deletes the project with the given name and optionally deletes the project
   * content.
   *
   * @throws CoreException
   */
  public static void deleteProject(String projectName, boolean deleteContent)
      throws CoreException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (!project.exists()) {
      throw new IllegalStateException("Project " + projectName
          + " does not exist in this workspace");
    }

    project.delete(deleteContent, true, new NullProgressMonitor());
  }

  /**
   * Returns an IProject by the given name, or null if no such project
   * can be found.
   * @param projectName The project's name
   * @return the IProject, or null if not found
   */
  public static IProject findProject(String projectName) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (project == null || !project.exists()) {
      return null;
    }
    return project;
  }

  /**
   * This should not be called by typical clients.
   */
  public static synchronized IWebAppProjectCreator.Factory getWebAppProjectCreatorFactory() {
    return webAppProjectCreatorFactory;
  }

  public static void importProject(String projectName, IPath directory)
      throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    if (!project.exists()) {
      IPath path = directory.append(IProjectDescription.DESCRIPTION_FILE_NAME);
      IProjectDescription projectFile = ResourcesPlugin.getWorkspace().loadProjectDescription(
          path);
      IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(
          projectName);
      projectDescription.setLocation(path);

      project.create(projectFile, null);
    }

    project.open(null);
    JobsUtilities.waitForIdle();
  }

  /*
   * TODO: Figure out a way to achieve this without hardcoding the plugin IDs
   */
  /**
   * @return true if the project has any GPE natures on it
   */
  public static boolean isGpeProject(IProject project) throws CoreException {
    return NatureUtils.hasNature(project, "com.google.gwt.eclipse.core.gwtNature")
        || NatureUtils.hasNature(project,
            "com.google.appengine.eclipse.core.gaeNature");
  }

  /**
   * This should not be called by typical clients.
   */
  public static synchronized void setWebAppProjectCreatorFactory(
      IWebAppProjectCreator.Factory factory) {
    webAppProjectCreatorFactory = factory;
  }

  private ProjectUtilities() {
  }
}
