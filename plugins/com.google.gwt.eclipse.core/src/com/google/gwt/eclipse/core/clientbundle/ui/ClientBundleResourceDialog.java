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

import com.google.gdt.eclipse.core.ui.JavaTypeCompletionProcessorWithAutoActivation;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import java.util.List;

/**
 * Dialog for editing the properties of a resource to add to a ClientBundle.
 */
@SuppressWarnings("restriction")
public class ClientBundleResourceDialog extends StatusDialog {

  private class FieldAdapter implements IStringButtonAdapter,
      IDialogFieldListener {

    public void changeControlPressed(DialogField field) {
      if (field == fileField) {
        List<IFile> files = ClientBundleResourceSelectionDialog.getFiles(
            getShell(), false, javaProject, getFileFieldValue());
        if (files != null) {
          assert (files.size() == 1);
          fileField.setText(files.get(0).getFullPath().toString());
        }
      } else if (field == resourceTypeField) {
        IType resourceType = chooseResourceType();
        if (resourceType != null) {
          resourceTypeField.setText(resourceType.getFullyQualifiedName('.'));
        }
      }
    }

    public void dialogFieldChanged(DialogField field) {
      validateFields();

      if (field == fileField) {
        fileFieldChanged();
      }
    }
  }

  private final ClientBundleResource editedResource;

  private final String[] existingMethodNames;

  private final String[] extendedInterfaces;

  private final FieldAdapter fieldAdapter = new FieldAdapter();

  private StringButtonDialogField fileField;

  private final IJavaProject javaProject;

  private StringDialogField methodNameField;

  private final IPackageFragment pckgFragment;

  private ClientBundleResource resource;

  private StringButtonDialogField resourceTypeField;

  public ClientBundleResourceDialog(Shell parent, IJavaProject javaProject,
      IPackageFragment pckgFragment, String[] extendedInterfaces,
      String[] existingMethodNames) {
    this(parent, javaProject, pckgFragment, extendedInterfaces,
        existingMethodNames, null);
  }

  public ClientBundleResourceDialog(Shell parent, IJavaProject javaProject,
      IPackageFragment pckgFragment, String[] extendedInterfaces,
      String[] existingMethodNames, ClientBundleResource editedResource) {
    super(parent);
    this.javaProject = javaProject;
    this.pckgFragment = pckgFragment;
    this.extendedInterfaces = extendedInterfaces;
    this.existingMethodNames = existingMethodNames;
    this.editedResource = editedResource;

    setTitle("ClientBundle Resource");
    setHelpAvailable(false);
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  public void create() {
    super.create();

    if (editedResource != null) {
      // If we're editing a resource, it's ok to have the dialog appear with an
      // error. However, because StatusDialog prevents it, we have to override
      // create() and call validateFields() again.
      validateFields();
    }
  }

  public ClientBundleResource getClientBundleResource() {
    return resource;
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    initializeControls();
    addEventHandlers();
    validateFields();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    final GridLayout gridLayout = new GridLayout();
    int nColumns = 3;
    gridLayout.numColumns = nColumns;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    container.setLayout(gridLayout);

    createFileControls(container, nColumns);
    createMethodNameControls(container, nColumns);
    createResourceTypeControls(container, nColumns);

    return container;
  }

  private void addEventHandlers() {
    fileField.setDialogFieldListener(fieldAdapter);
    methodNameField.setDialogFieldListener(fieldAdapter);
    resourceTypeField.setDialogFieldListener(fieldAdapter);
  }

  private IType chooseResourceType() {
    IJavaSearchScope scope;
    try {
      scope = SearchEngine.createHierarchyScope(ClientBundleUtilities.findResourcePrototypeType(javaProject));
      FilteredTypesSelectionDialog dialog = new FilteredTypesSelectionDialog(
          getShell(), false, PlatformUI.getWorkbench().getProgressService(),
          scope, IJavaSearchConstants.INTERFACE);
      dialog.setTitle("Resource Type Selection");
      dialog.setMessage("Choose a resource type:");

      if (dialog.open() == Window.OK) {
        return (IType) dialog.getFirstResult();
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }

    return null;
  }

  private void createFileControls(Composite parent, int nColumns) {
    fileField = new StringButtonDialogField(fieldAdapter);
    fileField.setLabelText("File:");
    fileField.setButtonLabel("Browse...");
    fileField.doFillIntoGrid(parent, nColumns);
    Text text = fileField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());
    LayoutUtil.setHorizontalGrabbing(fileField.getTextControl(null));
    fileField.postSetFocusOnDialogField(parent.getDisplay());
  }

  private void createMethodNameControls(Composite parent, int nColumns) {
    methodNameField = new StringDialogField();
    methodNameField.setLabelText("Method name:");
    methodNameField.doFillIntoGrid(parent, nColumns - 1);
    Text text = methodNameField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());

    new Label(parent, SWT.NONE);
  }

  private void createResourceTypeControls(Composite parent, int nColumns) {
    resourceTypeField = new StringButtonDialogField(fieldAdapter);
    resourceTypeField.setLabelText("Resource type:");
    resourceTypeField.setButtonLabel("Browse...");
    resourceTypeField.doFillIntoGrid(parent, nColumns);
    Text text = resourceTypeField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());

    // Set up auto-completion
    JavaTypeCompletionProcessor resourceTypeCompletionProcessor = new JavaTypeCompletionProcessorWithAutoActivation();
    resourceTypeCompletionProcessor.setPackageFragment(pckgFragment);
    ControlContentAssistHelper.createTextContentAssistant(text,
        resourceTypeCompletionProcessor);
    TextFieldNavigationHandler.install(text);
  }

  /**
   * If the user just changed the File field to a valid file and the other
   * fields are blank, pre-populate them based on the filename.
   */
  private void fileFieldChanged() {
    IFile file = getFileFieldValue();
    if (file == null || !file.exists()) {
      return;
    }

    if (methodNameField.getText().length() == 0) {
      methodNameField.setText(ClientBundleUtilities.suggestMethodName(file));
    }

    if (resourceTypeField.getText().length() == 0) {
      resourceTypeField.setText(ClientBundleUtilities.suggestResourceTypeName(
          javaProject, file));
    }
  }

  private IFile getFileFieldValue() {
    IPath path = new Path(fileField.getText());
    if (path.segmentCount() < 2) {
      return null;
    }
    return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
  }

  private int getMaxFieldWidth() {
    return convertWidthInCharsToPixels(60);
  }

  private void initializeControls() {
    if (editedResource != null) {
      fileField.setText(editedResource.getFile().getFullPath().toString());
      methodNameField.setText(editedResource.getMethodName());
      resourceTypeField.setText(editedResource.getResourceTypeName());
    }
  }

  private void validateFields() {
    resource = null;

    // We can't delegate this one to ClientBundleResource, because it takes an
    // IFile, not the raw entry text.
    String fileFieldValue = fileField.getText().trim();
    if (fileFieldValue.length() == 0) {
      updateStatus(ClientBundleResource.errorStatus("Enter the resource file"));
      return;
    }

    IFile file = getFileFieldValue();
    String methodName = methodNameField.getText().trim();
    String resourceTypeName = resourceTypeField.getText().trim();

    // Populate a temporary ClientBundleResource with the field values and
    // validate the resource against the project and interface(s)
    ClientBundleResource curResource = ClientBundleResource.create(file,
        methodName, resourceTypeName);
    IStatus resourceStatus = curResource.validate(javaProject,
        extendedInterfaces, existingMethodNames);
    updateStatus(resourceStatus);

    if (resourceStatus.getSeverity() != IStatus.ERROR) {
      resource = curResource;
    }
  }

}
