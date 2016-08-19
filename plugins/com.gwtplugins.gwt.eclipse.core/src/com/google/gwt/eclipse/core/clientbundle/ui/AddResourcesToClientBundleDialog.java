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

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.ui.JavaProjectSelectionDialog;
import com.google.gdt.eclipse.core.ui.JavaTypeCompletionProcessorWithAutoActivation;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleResource;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog for adding resources to an existing ClientBundle.
 */
@SuppressWarnings("restriction")
public class AddResourcesToClientBundleDialog extends StatusDialog {

  /**
   * General-purpose event handler for dialog fields.
   */
  private class FieldAdapter implements IStringButtonAdapter,
      IDialogFieldListener,
      BundledResourcesSelectionBlock.IResourcesChangeListener {
    public void changeControlPressed(DialogField field) {
      if (field == projectField) {
        IJavaProject selectedProject = JavaProjectSelectionDialog.chooseProject(
            getShell(), getJavaProject(),
            Collections.singleton(GWTNature.NATURE_ID));
        if (selectedProject != null) {
          projectField.setText(selectedProject.getElementName());
        }
      } else if (field == clientBundleTypeField) {
        IType type = chooseClientBundleType();
        if (type != null) {
          clientBundleTypeField.setText(type.getFullyQualifiedName('.'));
        }
      }
    }

    public void dialogFieldChanged(DialogField field) {
      fieldChanged();
    }

    public void onResourcesChanged() {
      fieldChanged();
    }
  }

  private final BundledResourcesSelectionBlock bundledResourcesBlock;

  private IType clientBundleType;

  private StringButtonDialogField clientBundleTypeField;

  private final FieldAdapter fieldAdapter = new FieldAdapter();

  private IFile[] files;

  private IProject project;

  private StringButtonDialogField projectField;

  private JavaTypeCompletionProcessor resourceTypeCompletionProcessor;

  public AddResourcesToClientBundleDialog(Shell parent, IProject project,
      IType clientBundleType, IFile[] files) {
    super(parent);
    this.project = project;
    this.clientBundleType = clientBundleType;
    this.files = files;
    this.bundledResourcesBlock = new BundledResourcesSelectionBlock(
        "Bundled resources:", fieldAdapter);

    setTitle("Add Resources to ClientBundle");
    setHelpAvailable(false);
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  public void create() {
    super.create();

    /*
     * If we had errors on any of the initially-populated fields, we want the
     * dialog to appear with an error. However, because StatusDialog explicitly
     * suppress errors from showing when the dialog is first loaded, we have to
     * override create() and call validateFields() again.
     */
    validateFields();
  }

  public IType getClientBundleType() {
    return clientBundleType;
  }

  public List<ClientBundleResource> getResources() {
    return bundledResourcesBlock.getResources();
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    initializeControls();
    addEventHandlers();
    fieldChanged();

    setInitialFocus(parent);

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

    createProjectControls(container, nColumns);
    createClientBundleTypeControls(container, nColumns);
    createBundledResourcesControls(container, nColumns);

    return container;
  }

  private void addEventHandlers() {
    projectField.setDialogFieldListener(fieldAdapter);
    clientBundleTypeField.setDialogFieldListener(fieldAdapter);
  }

  private IType chooseClientBundleType() {
    try {
      // Create a search scope for finding ClientBundle subtypes
      IJavaSearchScope scope = SearchEngine.createHierarchyScope(ClientBundleUtilities.findClientBundleType(getJavaProject()));

      // Configure the type selection dialog
      FilteredTypesSelectionDialog dialog = new FilteredTypesSelectionDialog(
          getShell(), false, PlatformUI.getWorkbench().getProgressService(),
          scope, IJavaSearchConstants.INTERFACE);
      dialog.setTitle("ClientBundle Type Selection");
      dialog.setMessage("Choose a type:");

      if (dialog.open() == Window.OK) {
        return (IType) dialog.getFirstResult();
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }

    return null;
  }

  private void clientBundleTypeChanged() {
    // Ensure the controls' enablement is correct
    updateControls();

    // Notify the bundled resources block about the new ClientBundle type
    if (clientBundleType != null) {
      IPackageFragment pckgFragment = clientBundleType.getPackageFragment();
      bundledResourcesBlock.setPackage(pckgFragment);
      // Since we already have an existing ClientBundle type, it is the only
      // type we need to search for conflicting method names (unlike in the New
      // ClientBundle wizard, where the user can choose to extend additional
      // arbitrary interfaces in addition to ClientBundle).
      bundledResourcesBlock.setExtendedInterfaces(new String[] {clientBundleType.getFullyQualifiedName('.')});
    } else {
      bundledResourcesBlock.setPackage(null);
      bundledResourcesBlock.setExtendedInterfaces(new String[0]);
    }
  }

  private void createBundledResourcesControls(Composite parent, int columns) {
    bundledResourcesBlock.doFillIntoGrid(parent, columns);
  }

  private void createClientBundleTypeControls(Composite parent, int nColumns) {
    clientBundleTypeField = new StringButtonDialogField(fieldAdapter);
    clientBundleTypeField.setLabelText("ClientBundle:");
    clientBundleTypeField.setButtonLabel("Browse...");
    clientBundleTypeField.doFillIntoGrid(parent, nColumns);
    Text text = clientBundleTypeField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());

    // Set up auto-completion
    resourceTypeCompletionProcessor = new JavaTypeCompletionProcessorWithAutoActivation();
    ControlContentAssistHelper.createTextContentAssistant(text,
        resourceTypeCompletionProcessor);
    TextFieldNavigationHandler.install(text);
  }

  private void createProjectControls(Composite parent, int nColumns) {
    projectField = new StringButtonDialogField(fieldAdapter);
    projectField.setLabelText("Project:");
    projectField.setButtonLabel("Browse...");
    projectField.doFillIntoGrid(parent, nColumns);
    Text text = projectField.getTextControl(null);
    LayoutUtil.setWidthHint(text, getMaxFieldWidth());
    LayoutUtil.setHorizontalGrabbing(projectField.getTextControl(null));
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private IJavaProject getJavaProject() {
    return JavaCore.create(project);
  }

  private int getMaxFieldWidth() {
    return convertWidthInCharsToPixels(60);
  }

  private void initBundledResources() {
    assert (getJavaProject() != null);

    List<ClientBundleResource> resources = new ArrayList<ClientBundleResource>();

    // Try to wrap each file from initial selection as a ClientBundle resource
    for (IFile file : files) {
      ClientBundleResource resource = ClientBundleResource.createFromFile(
          getJavaProject(), file);
      if (resource != null) {
        resources.add(resource);
      }
    }
    bundledResourcesBlock.setResources(resources);
  }

  private void initializeControls() {
    if (project != null) {
      projectField.setText(project.getName());
      projectChanged();

      IJavaProject javaProject = getJavaProject();
      if (javaProject != null) {
        if (clientBundleType != null) {
          clientBundleTypeField.setText(clientBundleType.getFullyQualifiedName('.'));
          clientBundleTypeChanged();
        }

        if (files != null) {
          initBundledResources();
        }
      }
    }
  }

  private void projectChanged() {
    // Ensure the controls' enablement is correct
    updateControls();

    // Notify the bundled resources block about the new project
    bundledResourcesBlock.setJavaProject(getJavaProject());

    // Notify the ClientBundle type field auto-completion processor
    setPackageFragmentForTypeCompletion();
  }

  private void setInitialFocus(Composite parent) {
    if (project == null) {
      projectField.postSetFocusOnDialogField(parent.getDisplay());
    } else if (clientBundleType == null) {
      clientBundleTypeField.postSetFocusOnDialogField(parent.getDisplay());
    }
  }

  private void setPackageFragmentForTypeCompletion() {
    IPackageFragment pckgFragment = null;
    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      try {
        IPackageFragmentRoot[] srcRoots = javaProject.getPackageFragmentRoots();
        if (srcRoots.length > 0) {
          // Just use the default package of the first source root we find
          pckgFragment = srcRoots[0].getPackageFragment("");
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }
    }
    resourceTypeCompletionProcessor.setPackageFragment(pckgFragment);
  }

  private void updateControls() {
    boolean enableClientBundleTypeField = (project != null);
    boolean enableBundledResourcesBlock = (enableClientBundleTypeField && clientBundleType != null);

    if (enableClientBundleTypeField != clientBundleTypeField.isEnabled()) {
      clientBundleTypeField.setEnabled(enableClientBundleTypeField);
    }

    if (enableBundledResourcesBlock != bundledResourcesBlock.isEnabled()) {
      bundledResourcesBlock.setEnabled(enableBundledResourcesBlock);
    }
  }

  private IStatus validateBundledResources() {
    IStatus blockStatus = bundledResourcesBlock.getStatus();

    // If the block reports an error, return that
    if (!blockStatus.isOK()) {
      return blockStatus;
    }

    // Must add at least one resource to the ClientBundle
    if (bundledResourcesBlock.getResources().size() == 0) {
      // Empty message will disable the OK button, but not display an error
      return StatusUtilities.newErrorStatus("", GWTPlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validateClientBundleType() {
    IType oldClientBundleType = clientBundleType;
    clientBundleType = null;

    if (getJavaProject() == null) {
      // Bail out if we don't have a valid Java project; we can return OK since
      // the error on the project will take precedence.
      return StatusUtilities.OK_STATUS;
    }

    String clientBundleTypeName = clientBundleTypeField.getText().trim();
    if (clientBundleTypeName.length() == 0) {
      // Empty message will disable the OK button, but not display an error
      return StatusUtilities.newErrorStatus("", GWTPlugin.PLUGIN_ID);
    }

    IType enteredClientBundleType = JavaModelSearch.findType(getJavaProject(),
        clientBundleTypeName);
    if (enteredClientBundleType == null) {
      return StatusUtilities.newErrorStatus(
          "ClientBundle type does not exist", GWTPlugin.PLUGIN_ID);
    }

    try {
      if (!ClientBundleUtilities.isClientBundle(getJavaProject(),
          enteredClientBundleType)) {
        return StatusUtilities.newErrorStatus("ClientBundle type must extend "
            + ClientBundleUtilities.CLIENT_BUNDLE_TYPE_NAME,
            GWTPlugin.PLUGIN_ID);
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      return StatusUtilities.newErrorStatus(
          "Error while calculating super types.  See Eclipse log for details.",
          GWTPlugin.PLUGIN_ID);
    }

    // ClientBundle type is valid, so set it
    this.clientBundleType = enteredClientBundleType;

    // Notify others if the ClientBundle type has changed
    if (!JavaUtilities.equalsWithNullCheck(oldClientBundleType,
        enteredClientBundleType)) {
      clientBundleTypeChanged();
    }

    return StatusUtilities.OK_STATUS;
  }

  private void validateFields() {
    IStatus projectStatus = validateProject();
    IStatus clientBundleTypeStatus = validateClientBundleType();
    IStatus bundledResourcesStatus = validateBundledResources();

    updateStatus(StatusUtil.getMostSevere(new IStatus[] {
        projectStatus, clientBundleTypeStatus, bundledResourcesStatus}));
  }

  private IStatus validateProject() {
    IProject oldProject = project;
    project = null;

    String projectName = projectField.getText().trim();
    if (projectName.length() == 0) {
      // Empty message will disable the OK button, but not display an error
      return StatusUtilities.newErrorStatus("", GWTPlugin.PLUGIN_ID);
    }

    IProject enteredProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    if (!enteredProject.exists()) {
      return StatusUtilities.newErrorStatus("Project does not exist",
          GWTPlugin.PLUGIN_ID);
    }

    if (!enteredProject.isOpen()) {
      return StatusUtilities.newErrorStatus("Project is not open",
          GWTPlugin.PLUGIN_ID);
    }

    IJavaProject javaProject = JavaCore.create(enteredProject);
    if (javaProject == null
        || ClientBundleUtilities.findClientBundleType(javaProject) == null) {
      return StatusUtilities.newErrorStatus(
          "Project must be using GWT 2.0 or later to use ClientBundle.",
          GWTPlugin.PLUGIN_ID);
    }

    // Project is valid, so set it
    this.project = enteredProject;

    // Notify others if the project has changed
    if (!JavaUtilities.equalsWithNullCheck(oldProject, enteredProject)) {
      projectChanged();
    }

    return StatusUtilities.OK_STATUS;
  }

}
