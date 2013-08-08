/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.appengine.swarm.util;

import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Utils managing connected projects.
 * 
 */
public class ConnectedProjectHandler extends RenameParticipant {

  private static final String CONNECTED_PROJECT = "connectedProject";

  public static IProject getConnectedProject(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IProject connectedProject = null;
    IEclipsePreferences prefs = projectScope.getNode(
        AppEngineSwarmPlugin.PLUGIN_ID);
    String connectedProjectName = prefs.get(CONNECTED_PROJECT, null);
    if (connectedProjectName != null) {
      connectedProject = ResourcesPlugin.getWorkspace()
          .getRoot().getProject(connectedProjectName);
      if (!connectedProject.isAccessible()) {
        connectedProject = null;
      }
    }
    return connectedProject;
  }

  public static void setConnectedProject(IProject project, IProject connectedProject)
      throws BackingStoreException {
    setConnectedProject(project, connectedProject.getName());

  }

  private static String getConnectedProjectName(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IProject connectedProject = null;
    IEclipsePreferences prefs = projectScope.getNode(
        AppEngineSwarmPlugin.PLUGIN_ID);
    return prefs.get(CONNECTED_PROJECT, null);
  }

  private static void setConnectedProject(IProject project, String connectedProjectName)
      throws BackingStoreException {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences prefs = projectScope.getNode(AppEngineSwarmPlugin.PLUGIN_ID);
    prefs.put(CONNECTED_PROJECT, connectedProjectName);
    prefs.flush();
  }

  private IProject project;
  private String connectedProjectName;

  @Override
  public RefactoringStatus checkConditions(
      IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    if (connectedProjectName != null) {
      IProject connectedProject = ResourcesPlugin.getWorkspace()
          .getRoot().getProject(connectedProjectName);
      if (!connectedProject.isAccessible()) {
        return RefactoringStatus.createErrorStatus("The connected project "
            + connectedProjectName + " is not accessible and so not updated!!!");
      }
    }
    return null;
  }

  @Override
  public Change createChange(IProgressMonitor pm)
      throws OperationCanceledException {
    IProject connectedProject = getConnectedProject(project);
    if (connectedProject != null) {
      try {
        setConnectedProject(connectedProject, getArguments().getNewName());
      } catch (BackingStoreException e) {
        AppEngineSwarmPlugin.log(e);
      }
    }
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  protected boolean initialize(Object element) {
    project = (IProject) element;
    connectedProjectName = getConnectedProjectName(project);
    if (connectedProjectName != null) {
      return true;
    }
    return false;
  }

}
