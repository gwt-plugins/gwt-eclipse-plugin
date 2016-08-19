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

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gwt.eclipse.core.dialogs.ModuleSelectionDialogButtonField;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page for specifying options for a new entry point class.
 */
@SuppressWarnings("restriction")
public class NewEntryPointWizardPage extends NewTypeWizardPage {

  private class ModuleFieldAdapter implements IDialogFieldListener {

    public void dialogFieldChanged(DialogField field) {
      entryPointFieldChanged(field);
    }
  }

  private static final String MODULE = "NewEntryPointWizardPage.module";

  private SelectionButtonDialogFieldGroup methodStubsButtons;

  private ModuleFile module;

  private ModuleSelectionDialogButtonField moduleField;

  private IStatus moduleStatus;

  private JavaElementLabelProvider javaLabelProvider = new JavaElementLabelProvider();

  public NewEntryPointWizardPage() {
    super(true, "Entry Point Class");
    setTitle("Entry Point Class");
    setDescription("Create a new entry point class.");

    moduleField = new ModuleSelectionDialogButtonField(this,
        new ModuleFieldAdapter());

    String[] buttonNames = new String[] {
        NewWizardMessages.NewClassWizardPage_methods_constructors,
        NewWizardMessages.NewClassWizardPage_methods_inherited};
    methodStubsButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK,
        buttonNames, 1);
    methodStubsButtons.setLabelText(NewWizardMessages.NewClassWizardPage_methods_label);

    moduleStatus = new StatusInfo();
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    int nColumns = 4;
    GridLayout layout = new GridLayout();
    layout.numColumns = nColumns;
    composite.setLayout(layout);

    // Create the standard input fields
    createContainerControls(composite, nColumns);
    createModuleControls(composite, nColumns);
    createPackageControls(composite, nColumns);

    createSeparator(composite, nColumns);

    createTypeNameControls(composite, nColumns);

    createSuperClassControls(composite, nColumns);
    createSuperInterfacesControls(composite, nColumns);

    createMethodStubSelectionControls(composite, nColumns);
    createCommentControls(composite, nColumns);
    enableCommentControl(true);

    setControl(composite);

    List<String> superInterfaces = new ArrayList<String>();
    superInterfaces.add("com.google.gwt.core.client.EntryPoint");
    setSuperInterfaces(superInterfaces, true);
  }

  public ModuleFile getModule() {
    return this.module;
  }

  public void init(IStructuredSelection selection) {
    IJavaElement initialJavaElement = getInitialJavaElement(selection);
    initContainerPage(initialJavaElement);
    initTypePage(initialJavaElement);

    // Set the initial resource, based either on the selection or on the
    // initial Java element returned by NewContainerWizardPage
    IResource initialResource = AbstractNewFileWizard.getSelectedResource(selection);
    if (initialResource.getType() == IResource.ROOT
        && initialJavaElement != null) {
      initialResource = initialJavaElement.getResource();
    }

    initEntryPointPage(initialResource);
    doStatusUpdate();

    setMethodStubSelection(false, true);
  }

  public boolean isCreateConstructors() {
    return methodStubsButtons.isSelected(0);
  }

  public boolean isCreateInherited() {
    return methodStubsButtons.isSelected(1);
  }

  public void setMethodStubSelection(boolean createCtors,
      boolean createInherited) {
    methodStubsButtons.setSelection(0, createCtors);
    methodStubsButtons.setSelection(1, createInherited);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setFocus();
    }
  }

  @Override
  protected IStatus containerChanged() {
    IStatus status = super.containerChanged();

    if (!status.isOK()) {
      moduleField.setGWTProject(null);

      if (status.getSeverity() == IStatus.ERROR) {
        return status;
      }

      /*
       * TODO: Right now, we assume that all WARNING statuses that are set by
       * NewContainerWizardPage are actually error conditions for us. This is
       * true, but to verify this, one has to look at the code. Ideally, we
       * would have ultimate control over the status levels that are set by the
       * NewContainerWizardPage, but there is no way to do that without
       * overriding the containerChanged() method and doing a lot of
       * copy-pasting. Really, it is not so bad to upgrade WARNINGS to ERRORS;
       * we are just being more conservative.
       */
      if (status.getSeverity() == IStatus.WARNING) {
        return Util.newErrorStatus(status.getMessage());
      }
    }

    IJavaProject javaProject = getJavaProject();
    if (JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)
        && GWTNature.isGWTProject(javaProject.getProject())) {
      moduleField.setGWTProject(javaProject);
    } else {
      moduleField.setGWTProject(null);
      status = Util.newErrorStatus("The source folder does not belong to a GWT Project.");
    }

    return status;
  }

  @Override
  protected void createTypeMembers(IType newType, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {

    boolean doConstr = isCreateConstructors();
    boolean doInherited = isCreateInherited();
    createInheritedMethods(newType, doConstr, doInherited, imports,
        new SubProgressMonitor(monitor, 1));

    if (monitor != null) {
      monitor.done();
    }
  }

  @Override
  protected void handleFieldChanged(String fieldName) {
    super.handleFieldChanged(fieldName);

    if (fieldName == CONTAINER || fieldName == PACKAGE) {
      moduleStatus = moduleChanged();
      fPackageStatus = packageChanged();
    }

    doStatusUpdate();
  }

  @Override
  protected IStatus packageChanged() {
    IStatus status = super.packageChanged();
    if (status.getSeverity() != IStatus.ERROR) {
      IPackageFragment pckg = getPackageFragment();
      if (pckg != null && module != null) {
        // TODO: replace this with a check of the module's client src paths
        if (!pckg.getElementName().startsWith(module.getPackageName())) {
          status = Util.newErrorStatus(
              "''{0}'' is not in a client source path of module ''{1}''",
              javaLabelProvider.getText(pckg), module.getQualifiedName());
        }
      }
    }
    return status;
  }

  private void createMethodStubSelectionControls(Composite composite,
      int nColumns) {
    Control labelControl = methodStubsButtons.getLabelControl(composite);
    LayoutUtil.setHorizontalSpan(labelControl, nColumns);

    DialogField.createEmptySpace(composite);

    Control buttonGroup = methodStubsButtons.getSelectionButtonsGroup(composite);
    LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
  }

  private void createModuleControls(Composite composite, int columns) {
    moduleField.doFillIntoGrid(composite, columns);
    Text text = moduleField.getTextControl(null);
    LayoutUtil.setWidthHint(text, convertWidthInCharsToPixels(40));
  }

  private void doStatusUpdate() {
    IStatus[] status = new IStatus[] {
        fContainerStatus, moduleStatus, fPackageStatus, fTypeNameStatus,
        fSuperClassStatus, fSuperInterfacesStatus};

    updateStatus(status);
  }

  private void entryPointFieldChanged(DialogField field) {
    if (field == moduleField) {
      moduleStatus = moduleChanged();
      fPackageStatus = packageChanged();
    }
    handleFieldChanged(MODULE);
  }

  private void initEntryPointPage(IResource resource) {
    if (resource.getType() == IResource.ROOT || !resource.isAccessible()) {
      return;
    }

    if (moduleField.init(resource)) {
      // Initialize the package based on the module's client source path
      if (moduleStatus.isOK()) {
        initPackageFromModule();
      } else {
        // If initializing the module caused an error, reset it
        moduleField.setText("");
      }
    }
  }

  private boolean initPackageFromModule() {
    List<IPath> moduleSrcPaths = module.getSourcePaths();
    if (moduleSrcPaths.size() == 1) {
      IFolder folder = module.getFolder(moduleSrcPaths.get(0));
      if (folder != null) {
        IJavaElement javaElement = JavaCore.create(folder);
        if (javaElement != null
            && javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
          setPackageFragment((IPackageFragment) javaElement, true);
          return true;
        }
      }
    }

    return false;
  }

  private IStatus moduleChanged() {
    StatusInfo status = new StatusInfo();
    module = null;

    moduleField.enableButton(getPackageFragmentRoot() != null
        && getPackageFragmentRoot().exists()
        && JavaProjectUtilities.isJavaProjectNonNullAndExists(getJavaProject())
        && GWTNature.isGWTProject(getJavaProject().getProject()));

    IStatus fieldStatus = moduleField.getStatus();
    if (!fieldStatus.isOK()) {
      status.setError(fieldStatus.getMessage());
      return status;
    }

    // TODO: verify that package is in client source path of module

    module = moduleField.getModule();
    return status;
  }

}
