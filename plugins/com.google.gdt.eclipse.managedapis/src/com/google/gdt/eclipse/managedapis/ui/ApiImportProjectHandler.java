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

import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

/**
 * The ApiImportProject Action launches the API import process: primarily the
 * API Import Wizard.
 */
public class ApiImportProjectHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) {
    IProject project = null;

    try {
      project = ActiveProjectFinder.getInstance().getProject();

      if (project != null && !project.exists()) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Google Plugin for Eclipse", "Project " + project.getName()
                + " does not exist. Please select a different project and try to add a Google API again.");
      } else if (project != null && !project.isOpen()) {
        MessageDialog.openInformation(Display.getDefault().getActiveShell(),
            "Google Plugin for Eclipse", "Project " + project.getName()
                + " is not open. Please open the project and try to add a Google API again.");
      } else if (project == null
          || AppEngineAndroidCheckDialog.isAppEngineAndroidProject(project)) {
        ApiImportWizard wizard = new ApiImportWizard();
        wizard.setProject(project);
        wizard.setResources(ManagedApiPlugin.getDefault().getResources());
        WizardDialog dialog = new WizardDialog(
            Display.getDefault().getActiveShell(), wizard);
        dialog.setMinimumPageSize(wizard.getPreferredPageSize());
        dialog.open();
      }
    } catch (CoreException e) {
      ManagedApiLogger.log(IStatus.ERROR, e);

      MessageDialog.openError(Display.getDefault().getActiveShell(),
          "Google Plugin for Eclipse",
              "There was a problem retrieving information for Project "
              + project != null ? project.getName() : "(unknown)"
              + " . See the Error Log for details.");
    }

    return null;
  }
}
