/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.actions;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.drive.DriveEclipseProjectMediator;
import com.google.gdt.eclipse.drive.DrivePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.util.Collection;

/**
 * Handles the selection of the project context-menu action to update an Eclipse Apps Script
 * project from the associated Drive project.
 */
public class UpdateFromDriveProjectAction implements IActionDelegate {
  
  private enum YesOrNo { YES, NO }

  private IProject currentlySelectedProject;

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // The user has right-clicked on a project, exposing its context menu. The action for updating
    // a project from Drive is enabled on this menu only if the project has Apps Script nature.
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      currentlySelectedProject =
          AdapterUtilities.getAdapter(
              ((IStructuredSelection) selection).iterator().next(), IProject.class);
    }
  }
  
  @Override
  public void run(IAction action) {
    Collection<String> unsavedFiles =
        DrivePlugin.getDefault().getPendingSaveManager().allUnsavedFiles(currentlySelectedProject);
    boolean clearedToUpdate;
    if (unsavedFiles.isEmpty()) {
      clearedToUpdate = true;
    } else {
      clearedToUpdate = askUserWhetherToOverwrite(unsavedFiles);
    }
    if (clearedToUpdate) {
      try {
        DriveEclipseProjectMediator.getInstance().updateEclipseProjectFromDrive(
            currentlySelectedProject, new NullProgressMonitor());
        // TODO(nhcohen): Find the context in which we could run the above call in an
        // IRunnableWithContext, then pass a real progress monitor to the call.
      } catch (CoreException | IOException | InterruptedException | BackingStoreException e) {
        DrivePlugin.logError("Exception updating an Eclipse project from Drive", e);
        DrivePlugin.displayLoggedErrorDialog(
            "The Eclipse project could not be updated because of an internal error");
      }
    }
  }
  
  private static boolean askUserWhetherToOverwrite(Collection<String> unsavedFiles) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Changes made on Eclipse to ");
    if (unsavedFiles.size() == 1) {
      messageBuilder.append("the file ");
      String onlyFile = unsavedFiles.iterator().next();
      messageBuilder.append(onlyFile);
      messageBuilder.append("have not been saved on Drive. ");
    } else {
      messageBuilder.append("the following files have not been saved on Drive:\n");
      for (String fileName : unsavedFiles) {
        messageBuilder.append("\n\t");
        messageBuilder.append(fileName);
      }
      messageBuilder.append("\n\n");
    }
    messageBuilder.append(
        "If you update the Eclipse project from Drive, these changes will be lost.");
    messageBuilder.append("\n\nUpdate from Drive anyway?");
    MessageDialog dialog =
        new MessageDialog(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            "Write-write conflict!",
            null,
            messageBuilder.toString(),
            MessageDialog.WARNING,
            new String[]{"Yes", "No"},
            YesOrNo.NO.ordinal());
    int selection = dialog.open();
    return selection == YesOrNo.YES.ordinal();
  }

}
