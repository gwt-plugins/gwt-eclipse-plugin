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
package com.google.gdt.eclipse.core.actions;

import com.google.gdt.eclipse.platform.shared.ui.IPixelConverter;
import com.google.gdt.eclipse.platform.ui.PixelConverterFactory;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewProjectAction;

/**
 * NOTE: This class is a local copy of
 * org.eclipse.jdt.ui.actions.AbstractOpenWizardAction. It was copied here
 * because the javadoc states that it is not intended to be subclassed.
 * 
 * <p>
 * Abstract base classed used for the open wizard actions.
 * </p>
 * 
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 */
@SuppressWarnings("restriction")
public abstract class AbstractOpenWizardAction extends Action {

  private Shell shell;
  private IStructuredSelection selection;
  private IJavaElement createdElement;

  /**
   * Creates the action.
   */
  protected AbstractOpenWizardAction() {
    shell = null;
    selection = null;
    createdElement = null;
  }

  /**
   * Returns the created element or <code>null</code> if the wizard has not run
   * or was canceled.
   * 
   * @return the created element or <code>null</code>
   */
  public IJavaElement getCreatedElement() {
    return createdElement;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    Shell localShell = getShell();
    if (!doCreateProjectFirstOnEmptyWorkspace(localShell)) {
      return;
    }
    try {
      INewWizard wizard = createWizard();
      wizard.init(PlatformUI.getWorkbench(), getSelection());

      WizardDialog dialog = new WizardDialog(localShell, wizard);
      IPixelConverter converter = PixelConverterFactory.createPixelConverter(JFaceResources.getDialogFont());
      dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70),
          converter.convertHeightInCharsToPixels(20));
      dialog.create();
      int res = dialog.open();
      if (res == Window.OK && wizard instanceof NewElementWizard) {
        createdElement = ((NewElementWizard) wizard).getCreatedElement();
      }

      notifyResult(res == Window.OK);
    } catch (CoreException e) {
      String title = NewWizardMessages.AbstractOpenWizardAction_createerror_title;
      String message = NewWizardMessages.AbstractOpenWizardAction_createerror_message;
      ExceptionHandler.handle(e, localShell, title, message);
    }
  }

  /**
   * Configures the selection to be used as initial selection of the wizard.
   * 
   * @param selection the selection to be set or <code>null</code> to use the
   *          selection of the active workbench window
   */
  public void setSelection(IStructuredSelection selection) {
    this.selection = selection;
  }

  /**
   * Configures the shell to be used as parent shell by the wizard.
   * 
   * @param shell the shell to be set or <code>null</code> to use the shell of
   *          the active workbench window
   */
  public void setShell(Shell shell) {
    this.shell = shell;
  }

  /**
   * Creates and configures the wizard. This method should only be called once.
   * 
   * @return returns the created wizard.
   * @throws CoreException exception is thrown when the creation was not
   *           successful.
   */
  protected abstract INewWizard createWizard() throws CoreException;

  /**
   * Opens the new project dialog if the workspace is empty. This method is
   * called on {@link #run()}.
   * 
   * @param shell the shell to use
   * @return returns <code>true</code> when a project has been created, or
   *         <code>false</code> when the new project has been canceled.
   */
  protected boolean doCreateProjectFirstOnEmptyWorkspace(Shell shell) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    if (workspaceRoot.getProjects().length == 0) {
      String title = NewWizardMessages.AbstractOpenWizardAction_noproject_title;
      String message = NewWizardMessages.AbstractOpenWizardAction_noproject_message;
      if (MessageDialog.openQuestion(shell, title, message)) {
        new NewProjectAction().run();
        return workspaceRoot.getProjects().length != 0;
      }
      return false;
    }
    return true;
  }

  /**
   * Returns the configured selection. If no selection has been configured using
   * {@link #setSelection(IStructuredSelection)}, the currently selected element
   * of the active workbench is returned.
   * 
   * @return the configured selection
   */
  protected IStructuredSelection getSelection() {
    if (selection == null) {
      return evaluateCurrentSelection();
    }
    return selection;
  }

  /**
   * Returns the configured shell. If no shell has been configured using
   * {@link #setShell(Shell)}, the shell of the currently active workbench is
   * returned.
   * 
   * @return the configured shell
   */
  protected Shell getShell() {
    if (shell == null) {
      return JavaPlugin.getActiveWorkbenchShell();
    }
    return shell;
  }

  private IStructuredSelection evaluateCurrentSelection() {
    IWorkbenchWindow window = JavaPlugin.getActiveWorkbenchWindow();
    if (window != null) {
      ISelection localSelection = window.getSelectionService().getSelection();
      if (localSelection instanceof IStructuredSelection) {
        return (IStructuredSelection) localSelection;
      }
    }
    return StructuredSelection.EMPTY;
  }

}
