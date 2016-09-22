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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * A class for use with toolbar button handlers to get the active project. This
 * is an action delegate so that it can get selection changed events to keep
 * track of the current project. It's added to the plugin's actions set, but not
 * added to a menu or toolbar.
 */
public class ActiveProjectFinder implements IWorkbenchWindowActionDelegate {

  private static ActiveProjectFinder instance;

  public static ActiveProjectFinder getInstance() {
    return instance;
  }

  private IProject selectedProject;

  private IWorkbenchWindow window;

  public ActiveProjectFinder() {
    ActiveProjectFinder.instance = this;
  }

  public void dispose() {
  }

  public IProject getProject() {
    // If we already have a project selected, return it
    if (selectedProject != null) {
      return selectedProject;
    }

    // Otherwise, if there's only one project, return it
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    if (projects.length == 1) {
      return projects[0];
    }

    // Finally, if nothing is selected, try to figure out the selected project
    // based on the active editor
    IWorkbenchPage activePage = window.getActivePage();
    if (activePage != null) {
      IEditorPart editor = window.getActivePage().getActiveEditor();
      if (editor != null) {
        IFile input = ResourceUtils.getEditorInput(editor);
        // can return null if the open editor isn't backed by an IFile
        if (input != null) {
          return input.getProject();
        }
      }
    }

    // Couldn't find any project
    return null;
  }

  public void init(IWorkbenchWindow window) {
    this.window = window;
  }

  public void run(IAction action) {
    // do nothing
  }

  public void selectionChanged(IAction action, ISelection selection) {
    IResource selectionResource = ResourceUtils.getSelectionResource(selection);
    if (selectionResource != null) {
      selectedProject = selectionResource.getProject();
    } else {
      selectedProject = null;
    }
  }

}
