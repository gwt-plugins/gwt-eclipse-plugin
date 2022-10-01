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
package com.google.gwt.eclipse.core.clientbundle.ui;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adds resources to an existing ClientBundle subtype.
 */
@SuppressWarnings("restriction")
public class AddResourcesToClientBundleAction extends Action implements
    IActionDelegate {

  private static class AddResourcesJob implements IWorkspaceRunnable {

    private final IType clientBundle;

    private final List<ClientBundleResource> resources;

    public AddResourcesJob(IType clientBundle,
        List<ClientBundleResource> resources) {
      this.clientBundle = clientBundle;
      this.resources = resources;
    }

    @Override
    public void run(IProgressMonitor monitor) throws CoreException {
      ICompilationUnit icu = clientBundle.getCompilationUnit();
      CompilationUnit cu = JavaASTUtils.parseCompilationUnit(icu);
      ImportRewrite importRewrite = StubUtility.createImportRewrite(cu, true);

      // Find the target type declaration
      TypeDeclaration typeDecl = JavaASTUtils.findTypeDeclaration(cu,
          clientBundle.getFullyQualifiedName());
      if (typeDecl == null) {
        throw new CoreException(
            StatusUtilities.newErrorStatus("Missing ClientBundle type "
                + clientBundle.getFullyQualifiedName(), GWTPlugin.PLUGIN_ID));
      }

      // We need to rewrite the AST of the ClientBundle type declaration
      ASTRewrite astRewrite = ASTRewrite.create(cu.getAST());
      ChildListPropertyDescriptor property = ASTNodes.getBodyDeclarationsProperty(typeDecl);
      ListRewrite listRewriter = astRewrite.getListRewrite(typeDecl, property);

      // Add the new resource methods
      boolean addComments = StubUtility.doAddComments(icu.getJavaProject());
      for (ClientBundleResource resource : resources) {
        listRewriter.insertLast(resource.createMethodDeclaration(clientBundle,
            astRewrite, importRewrite, addComments), null);
      }

      // Create the edit to add the methods and update the imports
      TextEdit rootEdit = new MultiTextEdit();
      rootEdit.addChild(astRewrite.rewriteAST());
      rootEdit.addChild(importRewrite.rewriteImports(null));

      // Apply the change to the compilation unit
      CompilationUnitChange cuChange = new CompilationUnitChange(
          "Update ClientBundle", icu);
      cuChange.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
      cuChange.setEdit(rootEdit);
      cuChange.perform(new NullProgressMonitor());
    }
  }

  private static IType findFirstTopLevelClientBundleType(IFile file) {
    try {
      IJavaElement element = JavaCore.create(file);
      if (element instanceof ICompilationUnit) {
        ICompilationUnit cu = (ICompilationUnit) element;
        if (cu.exists()) {
          for (IType type : cu.getTypes()) {
            if (ClientBundleUtilities.isClientBundle(cu.getJavaProject(), type)) {
              return type;
            }
          }
        }
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }
    return null;
  }

  private IType clientBundleType;

  private IFile[] files;

  private IProject project;

  @Override
  public void run(IAction action) {
    AddResourcesToClientBundleDialog dlg = new AddResourcesToClientBundleDialog(
        getShell(), project, clientBundleType, files);
    if (dlg.open() != Window.OK) {
      return;
    }

    IType clientBundle = dlg.getClientBundleType();
    try {
      // Update the type and open it
      addResourcesToClientBundle(clientBundle, dlg.getResources());
      ResourceUtils.openInDefaultEditor(getShell(),
          (IFile) clientBundle.getResource(), true);
    } catch (CoreException e) {
      ErrorDialog.openError(
          getShell(),
          "Error Adding Resources",
          "One or more selected resources could not be added to the ClientBundle.",
          e.getStatus());
    }
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    // Reset our old initial selections
    project = null;
    clientBundleType = null;
    List<IFile> selectedFiles = new ArrayList<IFile>();

    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Iterator<?> iter = structuredSelection.iterator();
      while (iter.hasNext()) {
        Object element = iter.next();

        IFile file = AdapterUtilities.getAdapter(element, IFile.class);
        if (file != null) {
          // Set initial project if it's not already set
          if (project == null) {
            project = file.getProject();
          }

          // Try to set the initial ClientBundle type if it's not already set
          if (clientBundleType == null) {
            clientBundleType = findFirstTopLevelClientBundleType(file);
            if (clientBundleType != null) {
              // Ensure that initial project is the one containing the initial
              // ClientBundle type
              project = clientBundleType.getJavaProject().getProject();
            }
          }

          // If the file looks like a ClientBundle resource, add it
          if (ClientBundleResource.isProbableClientBundleResource(file)) {
            selectedFiles.add(file);
          }
        }
      }
    }
    files = selectedFiles.toArray(new IFile[0]);
  }

  private void addResourcesToClientBundle(IType clientBundle,
      List<ClientBundleResource> resources) throws CoreException {
    IWorkspaceRunnable op = new AddResourcesJob(clientBundle, resources);
    // Need to lock on the package fragment since we might have to create new
    // CssResource subtypes in addition to the ClientBundle interface.
    ISchedulingRule lock = clientBundle.getPackageFragment().getResource();
    ResourcesPlugin.getWorkspace().run(op, lock, IWorkspace.AVOID_UPDATE, null);
  }

  private Shell getShell() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
  }

}
