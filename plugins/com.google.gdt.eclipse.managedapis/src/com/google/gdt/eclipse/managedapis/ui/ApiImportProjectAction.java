/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.AdapterUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

/**
 * Action to support the import api item in a project's context menu.
 */
public class ApiImportProjectAction implements IActionDelegate {
  private IProject appEngineProject;

  public void run(IAction action) {
    if (!AppEngineAndroidCheckDialog.isAppEngineAndroidProject(appEngineProject)) {
      return;
    }
    new ApiImportProjectHandler().execute(null);
  }

  public void selectionChanged(IAction action, ISelection selection) {
    if (selection == null || !(selection instanceof IStructuredSelection)
        || !((IStructuredSelection) selection).iterator().hasNext()) {
      return;
    }
    appEngineProject = AdapterUtilities.getAdapter(
        ((IStructuredSelection) selection).iterator().next(), IProject.class);
  }

}
