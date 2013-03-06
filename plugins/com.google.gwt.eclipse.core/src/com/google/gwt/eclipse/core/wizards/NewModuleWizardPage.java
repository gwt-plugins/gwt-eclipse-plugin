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
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.dialogs.ModuleSelectionDialog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPackageCompletionProcessor;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonStatusDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard page for selecting the options for a new GWT module.
 */
@SuppressWarnings("restriction")
public class NewModuleWizardPage extends NewContainerWizardPage {

  /**
   * Provides labels and images for modules displayed in a module selection
   * dialog.
   */
  public static class ModuleSelectionLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
      assert (element instanceof IModule);
      return GWTPlugin.getDefault().getImage(GWTImages.MODULE_ICON);
    }

    @Override
    public String getText(Object element) {
      assert (element instanceof IModule);
      return ((IModule) element).getQualifiedName();
    }
  }

  private class ModuleDialogFieldAdapter implements IStringButtonAdapter,
      IDialogFieldListener, IListAdapter {

    public void changeControlPressed(DialogField field) {
      // Right now, we do not need to do anything in addition
      // to the default behavior if the 'Browse' button of a dialog
      // field is pressed.
      if (field == modulePackageField) {
        modulePackageBrowseButtonPressed();
      }
    }

    public void customButtonPressed(ListDialogField field, int index) {
      if (index == ADD_INHERITS_BUTTON_GROUP_INDEX) {
        moduleAddInheritsButtonPressed();
      } else {
        moduleRemoveInheritsButtonPressed();
      }
    }

    public void dialogFieldChanged(DialogField field) {
      moduleDialogFieldChanged(field);
    }

    public void doubleClicked(ListDialogField field) {
      // Right now, we do not need to do anything special
      // if an item in the inherits list is double-clicked
    }

    public void selectionChanged(ListDialogField field) {
      // Right now we do not need to do anything special
      // if a different item in the list is selected
    }
  }

  private static int ADD_INHERITS_BUTTON_GROUP_INDEX = 0;

  private static int CLIENT_PACKAGE_CHECKBOX_GROUP_INDEX = 1;

  private static int PUBLIC_PATH_CHECKBOX_GROUP_INDEX = 0;

  private static int REMOVE_INHERITS_BUTTON_GROUP_INDEX = 2;

  /*
   * While the superclass has its own field for the status result of a container
   * change, we need to translate that status into the appropriate context for
   * this wizard, and store the result in this field. We end up using
   * moduleContainerStatus when reporting the status of this wizard, as opposed
   * to the superclass' status field.
   */
  protected IStatus moduleContainerStatus;
  protected IStatus moduleNameStatus;
  protected IStatus modulePackageStatus;

  private SelectionButtonDialogFieldGroup moduleCreateElementsCheckboxes;
  private ListDialogField moduleInheritsDialogField;
  private StringDialogField moduleNameField;
  private JavaPackageCompletionProcessor modulePackageCompletionProcessor;
  private StringButtonStatusDialogField modulePackageField;

  public NewModuleWizardPage() {
    super("newModuleWizardPage");
    setTitle("New GWT Module");
    setDescription("Create a new GWT Module.");

    ModuleDialogFieldAdapter adapter = new ModuleDialogFieldAdapter();

    modulePackageField = new StringButtonStatusDialogField(adapter);
    modulePackageField.setDialogFieldListener(adapter);
    modulePackageField.setLabelText("Package:");
    modulePackageField.setButtonLabel(NewWizardMessages.NewTypeWizardPage_package_button);
    modulePackageField.setStatusWidthHint(NewWizardMessages.NewTypeWizardPage_default);

    modulePackageCompletionProcessor = new JavaPackageCompletionProcessor();

    moduleNameField = new StringDialogField();
    moduleNameField.setDialogFieldListener(adapter);
    moduleNameField.setLabelText("Module name:");

    String[] addButtons = new String[] {
        NewWizardMessages.NewTypeWizardPage_interfaces_add,
        /* 1 */null, NewWizardMessages.NewTypeWizardPage_interfaces_remove};
    moduleInheritsDialogField = new ListDialogField(adapter, addButtons,
        new ModuleSelectionLabelProvider());
    moduleInheritsDialogField.setDialogFieldListener(adapter);
    moduleInheritsDialogField.setTableColumns(new ListDialogField.ColumnsDescription(
        1, false));
    moduleInheritsDialogField.setLabelText("Inherited modules:");
    moduleInheritsDialogField.setRemoveButtonIndex(REMOVE_INHERITS_BUTTON_GROUP_INDEX);

    String[] buttonNames = new String[] {
        "Create public resource path", "Create package for client source"};
    moduleCreateElementsCheckboxes = new SelectionButtonDialogFieldGroup(
        SWT.CHECK, buttonNames, 1);

    moduleContainerStatus = new StatusInfo();
    modulePackageStatus = new StatusInfo();
    moduleNameStatus = new StatusInfo();
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());
    int nColumns = 4;
    GridLayout layout = new GridLayout();
    layout.numColumns = nColumns;
    composite.setLayout(layout);

    createContainerControls(composite, nColumns);
    createPackageControls(composite, nColumns);
    createModuleNameControls(composite, nColumns);
    createModuleInheritsControls(composite, nColumns);

    // TODO: Comment this out until we decide if we want to provide the
    // checkboxes as part of the wizard.
    // DialogField.createEmptySpace(composite);
    // createModuleOptionControls(composite, nColumns);

    setControl(composite);
  }

  @SuppressWarnings("unchecked")
  public List<IModule> getModuleInherits() {
    return new ArrayList<IModule>(moduleInheritsDialogField.getElements());
  }

  public String getModuleName() {
    return moduleNameField.getText();
  }

  public String getModulePackageName() {
    return modulePackageField.getText();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setFocus();
    }
  }

  public boolean shouldCreateClientPackage() {
    // TODO: Comment this out until we decide if we want to provide the
    // checkboxes as part of the wizard.
    // return
    // moduleCreateElementsCheckboxes.isSelected(CLIENT_PACKAGE_CHECKBOX_GROUP_INDEX);
    return true;
  }

  public boolean shouldCreatePublicPath() {
    // TODO: Comment this out until we decide if we want to provide the
    // checkboxes as part of the wizard.
    // return
    // moduleCreateElementsCheckboxes.isSelected(PUBLIC_PATH_CHECKBOX_GROUP_INDEX);
    return false;
  }

  // TODO: Comment this out until we decide if we want to provide the checkboxes
  // as part of the wizard.
  /*
   * protected void createModuleOptionControls(Composite composite, int
   * nColumns) { moduleCreateElementsCheckboxes.setLabelText("Which module
   * subdirectories would you like to create?");
   * 
   * Control labelControl =
   * moduleCreateElementsCheckboxes.getLabelControl(composite);
   * LayoutUtil.setHorizontalSpan(labelControl, nColumns);
   * 
   * DialogField.createEmptySpace(composite);
   * 
   * Control buttonGroup =
   * moduleCreateElementsCheckboxes.getSelectionButtonsGroup(composite);
   * LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1); }
   */

  protected void createModuleInheritsControls(Composite composite, int nColumns) {
    moduleInheritsDialogField.doFillIntoGrid(composite, nColumns);
  }

  protected void createModuleNameControls(Composite composite, int nColumns) {
    moduleNameField.doFillIntoGrid(composite, nColumns - 1);
    DialogField.createEmptySpace(composite);

    Text moduleNameText = moduleNameField.getTextControl(null);
    LayoutUtil.setWidthHint(moduleNameText, getMaxFieldWidth());
    LayoutUtil.setHorizontalGrabbing(moduleNameText);
  }

  protected void createPackageControls(Composite composite, int nColumns) {
    modulePackageField.doFillIntoGrid(composite, nColumns);
    Text modulePackageText = modulePackageField.getTextControl(null);
    LayoutUtil.setWidthHint(modulePackageText, getMaxFieldWidth());
    ControlContentAssistHelper.createTextContentAssistant(modulePackageText,
        modulePackageCompletionProcessor);
    TextFieldNavigationHandler.install(modulePackageText);
  }

  protected void doFieldChange(String fieldName, DialogField field) {

    if (CONTAINER.equals(fieldName)) {
      moduleContainerStatus = moduleContainerChanged();
      modulePackageStatus = packageChanged();
      moduleNameStatus = nameChanged();
    } else if (field == modulePackageField) {
      modulePackageStatus = packageChanged();
      moduleNameStatus = nameChanged();
    } else if (field == moduleNameField) {
      moduleNameStatus = nameChanged();
    }

    doStatusUpdate();
  }

  protected IPath getModulePath() {
    IPackageFragmentRoot root = getPackageFragmentRoot();
    IPath rootPath = root.getPath();

    String packageName = getModulePackageName();
    if (packageName != null && packageName.length() > 0) {
      rootPath = rootPath.append(packageName.replace('.', '/'));
    }

    IPath moduleFilePath = rootPath.append(getModuleName());

    moduleFilePath = moduleFilePath.addFileExtension("gwt.xml");

    return moduleFilePath;
  }

  @Override
  protected void handleFieldChanged(String fieldName) {
    doFieldChange(fieldName, null);
  }

  protected void initModulePage(IStructuredSelection selection) {
    IJavaElement jelem = getInitialJavaElement(selection);

    initContainerPage(jelem);

    String pName = "";
    if (jelem != null) {
      IPackageFragment pf = (IPackageFragment) jelem.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
      if (pf != null) {
        pName = pf.getElementName();
      }
    }

    modulePackageField.setText(pName);

    modulePackageCompletionProcessor.setPackageFragmentRoot(getPackageFragmentRoot());

    moduleCreateElementsCheckboxes.setSelection(
        CLIENT_PACKAGE_CHECKBOX_GROUP_INDEX, true);
    moduleCreateElementsCheckboxes.setSelection(
        PUBLIC_PATH_CHECKBOX_GROUP_INDEX, true);

    doFieldChange(CONTAINER, null);
  }

  private void addCommonGWTModulesAsDefaultInherits() {

    IJavaProject javaProject = getJavaProject();

    assert (JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject) && GWTNature.isGWTProject(javaProject.getProject()));

    // Look for com.google.gwt.user.User module, and add it as a
    // default in the inherits list if available.

    /*
     * TODO: If the project has many jars, this lookup could be slow, thus
     * causing a delay when the dialog is rendered the first time. Look into the
     * possibility of being able to ask a project's GWT Runtime about its
     * available modules, as opposed to calling ModuleUtils.findModule, which
     * iterates through all of the project's package fragment roots.
     */
    if (javaProject.getResource().isAccessible()) {
      IModule gwtUserModule = ModuleUtils.findModule(javaProject,
          "com.google.gwt.user.User", true);
      if (gwtUserModule != null) {
        addModuleIfNotAlreadyInList(gwtUserModule);
      }
    }
  }

  private void addModuleIfNotAlreadyInList(IModule module) {
    for (Object elem : moduleInheritsDialogField.getElements()) {
      IModule curModule = (IModule) elem;
      if (curModule.getQualifiedName().equals(module.getQualifiedName())) {
        return;
      }
    }

    moduleInheritsDialogField.addElement(module);
  }

  private IPackageFragment choosePackage() {
    IPackageFragmentRoot root = getPackageFragmentRoot();
    IJavaElement[] packages = null;
    try {
      if (root != null && root.exists()) {
        packages = root.getChildren();
      }
    } catch (JavaModelException e) {
      JavaPlugin.log(e);
    }
    if (packages == null) {
      packages = new IJavaElement[0];
    }

    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), new JavaElementLabelProvider(
            JavaElementLabelProvider.SHOW_DEFAULT));
    dialog.setIgnoreCase(false);
    dialog.setTitle(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_title);
    dialog.setMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_description);
    dialog.setEmptyListMessage(NewWizardMessages.NewTypeWizardPage_ChoosePackageDialog_empty);
    dialog.setElements(packages);
    dialog.setHelpAvailable(false);

    if (dialog.open() == Window.OK) {
      return (IPackageFragment) dialog.getFirstResult();
    }
    return null;
  }

  private void doStatusUpdate() {
    IStatus[] status = new IStatus[] {
        moduleContainerStatus, modulePackageStatus, moduleNameStatus};
    // the most severe status will be displayed and the OK button, will be
    // disabled if the most severe status is an ERROR
    updateStatus(status);
  }

  private void moduleAddInheritsButtonPressed() {
    IModule module = ModuleSelectionDialog.show(getShell(),
        getPackageFragmentRoot().getJavaProject(), true);

    if (module == null) {
      return;
    }

    addModuleIfNotAlreadyInList(module);
  }

  /**
   * This method is invoked after the parent wizard's containerChanged() method
   * is invoked. We only do state changes related to our wizard in this method.
   * Also, we translate the status returned from containerChanged(), so that it
   * makes sense in the context of this wizard.
   * 
   * TODO: Consider overriding containerChanged(); it may simplify this method.
   */
  private IStatus moduleContainerChanged() {

    if (getPackageFragmentRoot() == null
        || !getPackageFragmentRoot().exists()
        || !JavaProjectUtilities.isJavaProjectNonNullAndExists(getJavaProject())
        || !GWTNature.isGWTProject(getJavaProject().getProject())) {
      modulePackageField.enableButton(false);
      // disable add button
      moduleInheritsDialogField.enableButton(ADD_INHERITS_BUTTON_GROUP_INDEX,
          false);

    } else {
      modulePackageField.enableButton(true);
      // enable add button
      moduleInheritsDialogField.enableButton(ADD_INHERITS_BUTTON_GROUP_INDEX,
          true);
    }

    modulePackageCompletionProcessor.setPackageFragmentRoot(getPackageFragmentRoot());

    if (fContainerStatus.getSeverity() == IStatus.ERROR) {
      return fContainerStatus;
    }

    /*
     * TODO: Right now, we assume that all WARNING statuses that are set by
     * NewContainerWizardPage are actually error conditions for us. This is
     * true, but to verify this, one has to look at the code. Ideally, we would
     * have ultimate control over the status levels that are set by the
     * NewContainerWizardPage, but there is no way to do that without overriding
     * the containerChanged() method and doing a lot of copy-pasting. Really, it
     * is not so bad to upgrade WARNINGS to ERRORS; we are just being more
     * conservative.
     */
    if (fContainerStatus.getSeverity() == IStatus.WARNING) {
      return Util.newErrorStatus(fContainerStatus.getMessage());
    }

    if (JavaProjectUtilities.isJavaProjectNonNullAndExists(getJavaProject())
        && !GWTNature.isGWTProject(getJavaProject().getProject())) {
      /*
       * Only set this error message when the project IS a Java project, but not
       * a GWT Project. The other case, where the project is not a Java project,
       * would have already been flagged by the superclass' validation.
       */
      return Util.newErrorStatus("The source folder is not part of a GWT Project.");
    }

    // If we've reached this point, we know that the user has selected a valid
    // source folder within a GWT Project.
    if (moduleInheritsDialogField.getElements().isEmpty()) {
      // If there are no modules listed in the inherits list, add the User
      // module.
      addCommonGWTModulesAsDefaultInherits();
    }

    return fContainerStatus;
  }

  private void moduleDialogFieldChanged(DialogField field) {
    doFieldChange(null, field);
  }

  private void modulePackageBrowseButtonPressed() {
    IPackageFragment result = choosePackage();
    if (result != null) {
      modulePackageField.setText(result.getElementName());
    }
    doFieldChange(null, modulePackageField);
  }

  private void moduleRemoveInheritsButtonPressed() {
    moduleInheritsDialogField.removeElements(moduleInheritsDialogField.getSelectedElements());
  }

  private IStatus nameChanged() {
    return ModuleUtils.validateSimpleModuleName(moduleNameField.getText());
  }

  private IStatus packageChanged() {

    String packName = modulePackageField.getText();
    IStatus validatePackageStatus = Util.validatePackageName(packName);

    if (validatePackageStatus.getSeverity() == IStatus.ERROR) {
      return validatePackageStatus;
    }

    if (packName.length() == 0) {
      modulePackageField.setStatus(NewWizardMessages.NewTypeWizardPage_default);
    } else {
      modulePackageField.setStatus("");
    }

    IJavaProject project = getJavaProject();
    IPackageFragmentRoot root = getPackageFragmentRoot();

    if (project != null && root != null) {
      if (project.exists() && packName.length() > 0) {
        try {
          IPath rootPath = root.getPath();
          IPath outputPath = project.getOutputLocation();
          if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
            // if the bin folder is inside of our root, don't allow to name a
            // package like the bin folder
            IPath packagePath = rootPath.append(packName.replace('.', '/'));
            if (outputPath.isPrefixOf(packagePath)) {
              return Util.newErrorStatus(NewWizardMessages.NewTypeWizardPage_error_ClashOutputLocation);
            }
          }
        } catch (JavaModelException e) {
          // Not a critical exception at this juncture; we'll just log it
          // and move on.
          GWTPluginLog.logError(e);
        }
      }
    }

    return validatePackageStatus;
  }

  private void setFocus() {
    if (moduleNameField.isEnabled()) {
      moduleNameField.setFocus();
    } else {
      setFocusOnContainer();
    }
  }
}
