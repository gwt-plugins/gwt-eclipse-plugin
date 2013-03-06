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
package com.google.gdt.eclipse.appengine.rpc.nature;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.util.ProjectCreationConstants;
import com.google.gdt.eclipse.core.natures.NatureUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a Cloud connected Nature to the project, indicating that it has a
 * corresponding server project
 */

@SuppressWarnings("restriction")
public class AppEngineConnectedNature implements IProjectNature {

  public static final String NATURE_ID = AppEngineRPCPlugin.PLUGIN_ID
      + ".appengineConnectedNature"; //$NON-NLS-1$

  public static void addNatureToProject(IProject project) throws CoreException {
    NatureUtils.addNature(project, AppEngineConnectedNature.NATURE_ID);
  }

  /**
   * List of Android projects corresponding to the App Engine project
   */
  public static List<IProject> getAndroidProjects(String projectName) {
    List<IProject> list = new ArrayList<IProject>();
    // search for references
    IWorkspace root = ResourcesPlugin.getWorkspace();
    IProject[] projects = root.getRoot().getProjects();
    for (IProject aproject : projects) {
      if (AppEngineConnectedNature.isCloudConnectedProject(aproject)
          && hasConnection(aproject, projectName)) {
        list.add(aproject);
      }
    }
    return list;
  }

  public static IProject getAppEngineProject(IProject project)
      throws CoreException {

    if (isAppEngineProject(project)) {
      if (getConnectedProjectName(project) != null) {
        return project;
      }
    }

    if (isCloudConnectedProject(project)) {
      String projectName = getConnectedProjectName(project);
      IProject aeProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
          projectName);
      if (aeProject.exists()) {
        return aeProject;
      }
    }
    return null;
  }

  public static boolean isAppEngineProject(IProject project) {
    try {
      return project.hasNature(GaeNature.NATURE_ID);
    } catch (CoreException e) {
      return false;
    }
  }

  public static boolean isCloudConnectedProject(IProject project) {
    try {
      return project.isAccessible()
          && project.hasNature(AppEngineConnectedNature.NATURE_ID);
    } catch (CoreException e) {
      AppEngineRPCPlugin.getLogger().logError(e);
    }
    return false;
  }

  public static void removeNatureFromProject(IProject project)
      throws CoreException {
    if (!project.hasNature(AppEngineConnectedNature.NATURE_ID)) {
      return;
    }

    NatureUtils.removeNature(project, AppEngineConnectedNature.NATURE_ID);
  }

  private static String getConnectedProjectName(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences prefs = projectScope.getNode(AppEngineRPCPlugin.PLUGIN_ID);
    return prefs.get(ProjectCreationConstants.APP_ENGINE_PROJECT, ""); //$NON-NLS-1$
  }

  /**
   * Match the Android and App Engine project through the preferences
   */
  private static boolean hasConnection(IProject aproject, String projectName) {
    String name = getConnectedProjectName(aproject);
    if (projectName.equals(name)) {
      return true;
    }
    return false;
  }

  private IProject project;

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#configure()
   */
  public void configure() throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#deconfigure()
   */
  public void deconfigure() throws CoreException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IProjectNature#getProject()
   */
  public IProject getProject() {
    return project;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources
   * .IProject)
   */
  public void setProject(IProject project) {
    this.project = project;
  }

}
