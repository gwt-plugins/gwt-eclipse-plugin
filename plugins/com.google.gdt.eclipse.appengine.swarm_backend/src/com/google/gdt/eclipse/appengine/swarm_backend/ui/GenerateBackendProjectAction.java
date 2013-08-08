/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.appengine.swarm_backend.ui;

import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;
import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;

/**
 * Generates the backend project for a given project.
 */
public class GenerateBackendProjectAction extends Action implements IActionDelegate {
  private IProject androidProject;

  public static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";

  public IProject getAndroidProject() {
    return androidProject;
  }

  public void run(IAction action) {
    final String backendProjectName = BackendGenerator.getAndroidBackendProjectName(androidProject.getName());
    if (!androidProject.getFile(ANDROID_MANIFEST_FILENAME).exists()) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating App Engine Backend",
          "The selected project \"" + androidProject.getName() + "\" is not an Android Project.");
      return;
    }
    if (ResourcesPlugin.getWorkspace().getRoot().getProject(backendProjectName).exists()) {
      MessageDialog.openInformation(Display.getDefault().getActiveShell(),
          "Error in Generating App Engine Backend", "Connected App Engine Backend project \""
              + backendProjectName + "\" already exists.");
      return;
    } else {
      new GenerateBackendDialog(Display.getDefault().getActiveShell(), androidProject,
          backendProjectName).open();
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      androidProject = AdapterUtilities.getAdapter(
          ((IStructuredSelection) selection).iterator().next(), IProject.class);
    }
  }

}
