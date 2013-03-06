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
package com.google.gwt.eclipse.core.wizards;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Base class for wizards that create new (non-Java) files.
 */
@SuppressWarnings("restriction")
public abstract class AbstractNewFileWizard extends Wizard implements
    INewWizard {

  public static IResource getSelectedResource(IStructuredSelection selection) {
    Object selectedElement = null;

    if (selection != null) {
      selectedElement = selection.getFirstElement();
    }

    if (selectedElement instanceof IResource) {
      return (IResource) selectedElement;
    } else if (selectedElement instanceof IJavaElement) {
      return ((IJavaElement) selectedElement).getResource();
    } else if (selectedElement instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) selectedElement;
      return (IResource) adaptable.getAdapter(IResource.class);
    }

    // If we don't have a resource yet, try to get one from the active editor
    IEditorPart editor = GWTPlugin.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if (editor != null) {
      IFile file = ResourceUtils.getEditorInput(editor);
      if (file != null) {
        return file;
      }
    }

    // Fall back to the workspace root if we don't have a selection yet
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private IResource initialSelectedResource;

  private IStructuredSelection selection;

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    this.selection = selection;
  }

  @Override
  public boolean performFinish() {
    IFile file = createNewFile();

    if (file != null) {
      ResourceUtils.openInDefaultEditor(getShell(), file, true);
      return true;
    }
    return false;
  }

  protected String getFileExtension() {
    return null;
  }

  protected abstract IPath getFilePath();

  protected InputStream getInitialContents() {
    return null;
  }

  protected IResource getInitialSelectedResource() {
    // Initial selection is cached
    if (initialSelectedResource == null) {
      initialSelectedResource = getSelectedResource(selection);
    }
    return initialSelectedResource;
  }

  protected IStructuredSelection getSelection() {
    return selection;
  }

  private IFile createNewFile() {
    // Get the file creation parameters
    final IPath newFilePath = getNormalizedFilePath();
    final IFile newFileHandle = IDEWorkbenchPlugin.getPluginWorkspace().getRoot().getFile(
        newFilePath);
    final InputStream initialContents = getInitialContents();

    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) {
        CreateFileOperation op = new CreateFileOperation(newFileHandle, null,
            initialContents,
            IDEWorkbenchMessages.WizardNewFileCreationPage_title);
        try {
          PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(
              op, monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
        } catch (final ExecutionException e) {
          getContainer().getShell().getDisplay().syncExec(new Runnable() {
            public void run() {
              if (e.getCause() instanceof CoreException) {
                ErrorDialog.openError(getContainer().getShell(),
                    IDEWorkbenchMessages.WizardNewFileCreationPage_errorTitle,
                    null, ((CoreException) e.getCause()).getStatus());
              } else {
                IDEWorkbenchPlugin.log(getClass(), "createNewFile()",
                    e.getCause());
                MessageDialog.openError(
                    getContainer().getShell(),
                    IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorTitle,
                    NLS.bind(
                        IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorMessage,
                        e.getCause().getMessage()));
              }
            }
          });
        }
      }
    };
    try {
      getContainer().run(true, true, op);
    } catch (InterruptedException e) {
      return null;
    } catch (InvocationTargetException e) {
      // Execution Exceptions are handled above but we may still get
      // unexpected runtime errors.
      IDEWorkbenchPlugin.log(getClass(), "createNewFile()",
          e.getTargetException());
      MessageDialog.openError(
          getContainer().getShell(),
          IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorTitle,
          NLS.bind(
              IDEWorkbenchMessages.WizardNewFileCreationPage_internalErrorMessage,
              e.getTargetException().getMessage()));

      return null;
    }

    return newFileHandle;
  }

  /**
   * Ensure the new file's name ends with the default file extension.
   * 
   * @return the filename ending with the file extension
   */
  private IPath getNormalizedFilePath() {
    IPath newFilePath = getFilePath();
    String newFilename = newFilePath.lastSegment();
    String ext = getFileExtension();
    if (ext != null && ext.length() != 0) {
      if (!(newFilename.endsWith("." + ext))) {
        return newFilePath.addFileExtension(ext);
      }
    }

    return newFilePath;
  }

}
