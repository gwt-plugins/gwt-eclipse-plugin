/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.gdt.eclipse.drive.wizards;

import com.google.gdt.eclipse.drive.DriveEclipseProjectMediator;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.model.FolderTree.FolderTreeLeaf;
import com.google.gdt.eclipse.drive.notifications.Broadcaster;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nullable;

/**
 * Wizard for importing a new Eclipse Apps Script project from a Drive Apps Script project.
 */
public class ScriptProjectImportWizard extends Wizard implements IImportWizard {

  private static final String TITLE = "Apps Script Project Import Wizard";
  
  /**
   * A container that can be referenced by a final variable, so that its contents can be set in an
   * inner class (used in {@link #createEclipseProjectWithProgress()}).
   */
  private static class StringHolder {
    public String value;
  }

  private ScriptProjectImportWizardPage mainPage;
  
  // When currentMonitor is nonnull, it references the IProgressMonitor that should be affected by
  // a call on performCancel(). This variable is written by setCurrentMonitor and read by
  // performCancel(). When the user selects File > Import > Google > Apps Script Project and presses
  // the Next button, the WizardDialog invokes ScriptProjectImportWizardPage.createControl(), which
  // causes an IRunnableWithProgress to be started to retrieve Apps Script projects from the user's
  // Drive file system. When the user selects one of the retrieved projects and presses the Finish
  // button, the framework invokes ScriptProjectImportWizard.performFinish(), which causes an
  // IRunnableWithProgress to be started to retrieve the selected project.
  //
  // Each IRunnableWithProgress begins with a call on setCurrentMonitor passing its own
  // IProgressMonitor as a parameter, and ends (in a finally block) with a call on setCurrentMonitor
  // passing null as a parameter. Since the two IRunnableWithProgress executions are separated by
  // user actions, the various writes cannot possibly interfere with each other.
  private volatile IProgressMonitor currentMonitor;

  /**
   * Initializes this {@code ScriptProjectImportWizard}. A call on this method results in a login
   * prompt if the user is not already logged in. This method is called after the no argument
   * constructor and before other methods are called.
   * 
   * @param workbench ignored
   * @param selection ignored
   */
  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setWindowTitle(TITLE);
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    super.addPages();
    mainPage = new ScriptProjectImportWizardPage(TITLE);
    addPage(mainPage);
  }
  
  // We override setContainer so that, in addition to performing its usual duties, it adds an
  // IPageChanging listener to this wizard's WizardDialog. The listener cannot be added in the init
  // method because init() is called before this wizard has been associated with the WizardDialog
  // that is to contain it. Overriding setContainer allows this wizard to install the listener as
  // soon as the association is made.
  @Override
  public void setContainer(IWizardContainer wizardContainer) {
     super.setContainer(wizardContainer);
     if (wizardContainer instanceof WizardDialog) {
       WizardDialog wizardDialog = (WizardDialog) wizardContainer;
       wizardDialog.addPageChangingListener(
           new IPageChangingListener() {        
             @Override public void handlePageChanging(PageChangingEvent event) {
               mainPage.setErrorMessage(null);
             }
           });
     }
  }

  @Override
  public boolean performFinish() {
    mainPage.setErrorMessage(null);
    String projectFileId;
    try {
      projectFileId = createEclipseProjectWithProgress();
    } catch (InterruptedException e) {
      mainPage.setErrorMessage("The reading of the project from Drive was interrupted.");
      return false;
    }
    if (projectFileId == null) {
      return false;
    } else {
      new Broadcaster().onImportFromDrive(projectFileId);      
      return true;
    }
  }
  
  @Override
  public boolean performCancel() {
    IProgressMonitor monitorToCancel = currentMonitor;
    // Since currentMonitor is volatile, the assignment above is guaranteed to capture the most
    // recent value assigned to currentMonitor by any thread, and not just a stale value cached by
    // the current thread (as would otherwise be allowed by the Java memory model).
    if (monitorToCancel == null) {
      return true;
    } else {
      monitorToCancel.setCanceled(true);
      return false; // Keep the main wizard dialog open.
    }
  }
  
  public void setCurrentMonitor(IProgressMonitor monitor) {
    currentMonitor = monitor;
  }
  
  @Nullable
  private String createEclipseProjectWithProgress() throws InterruptedException {
    final FolderTreeLeaf selectedProject = mainPage.getSelectedDriveProject();
    final String projectName = mainPage.getProjectName();
    final IPath projectLocation = mainPage.useDefaults() ? null : mainPage.getLocationPath();
    final StringHolder fileIdHolder = new StringHolder();
    try {
      // TODO(nhcohen): Prevent the cancel button from being disabled when run(...) is called below.
      getContainer().run(
          true, false,
          new IRunnableWithProgress(){
            @Override public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
              fileIdHolder.value = selectedProject.getFileId();
              setCurrentMonitor(monitor);
              monitor.beginTask(
                  "Creating Eclipse project " + projectName + " from Drive project "
                      + selectedProject.getTitle(),
                  IProgressMonitor.UNKNOWN);
              // The following call on createEclipseProject results in a login prompt if the user is
              // not already logged in.
              try {
                DriveEclipseProjectMediator.getInstance().importDriveProjectIntoEclipse(
                    fileIdHolder.value, projectLocation, projectName, monitor);
              } catch (IOException e) {
                throw new InvocationTargetException(
                    e, "Error reading contents of project from Drive; see Error Log");
              } catch (CoreException e) {
                throw new InvocationTargetException(
                    e, "Error trying to create new project \"" + projectName + "\"");
              } finally {
                monitor.done();
                setCurrentMonitor(null);
              }
            }
          });
      return fileIdHolder.value;
    } catch (InvocationTargetException e) {
      String message = "Error reading contents of project from Drive; see Error Log.";
      mainPage.setErrorMessage(message);
      DrivePlugin.logError(message, e.getCause());
      return null;
    }
  }

}
