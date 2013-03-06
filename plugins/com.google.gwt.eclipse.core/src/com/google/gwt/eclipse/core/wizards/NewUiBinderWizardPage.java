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

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.resources.HtmlBasedUiBinderResourceCreator;
import com.google.gwt.eclipse.core.uibinder.resources.UiBinderResourceCreator;
import com.google.gwt.eclipse.core.uibinder.resources.WidgetBasedUiBinderResourceCreator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.text.MessageFormat;
import java.util.List;

/**
 * Wizard page for creating new UiBinder template + owner class pairs.
 */
@SuppressWarnings("restriction")
public class NewUiBinderWizardPage extends AbstractNewTypeWizardPage {

  private static final int HTML_UI_INDEX = 1;

  private static final int WIDGETS_UI_INDEX = 0;

  private SelectionButtonDialogField addSampleContentButton;

  private SelectionButtonDialogFieldGroup uiTypeButtons;

  private IFile uiXmlFile;

  public NewUiBinderWizardPage() {
    super("UiBinder", "Create a new UiBinder template and owner class.", true,
        "NewUiBinderWizardPage");

    uiTypeButtons = new SelectionButtonDialogFieldGroup(SWT.RADIO,
        new String[] {"GWT widge&ts", "&HTML"}, 4);
    uiTypeButtons.setLabelText("&Create UI based on:");
    uiTypeButtons.setSelection(0, true);

    addSampleContentButton = new SelectionButtonDialogField(SWT.CHECK);
    addSampleContentButton.setLabelText("Generate &sample content");
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

    createSeparator(composite, nColumns);

    createTypeNameControls(composite, nColumns);
    createUiTypeControls(composite, nColumns);

    createSampleContentControls(composite, nColumns);

    createCommentControls(composite, nColumns);
    enableCommentControl(true);

    setControl(composite);

    Dialog.applyDialogFont(composite);
  }

  @Override
  public void createType(IProgressMonitor monitor) throws CoreException,
      InterruptedException {
    // Create the owner class
    super.createType(monitor);

    // Now create the ui.xml file
    IPath uiXmlPath = getUiXmlFilePath();
    if (uiXmlPath == null) {
      // Should not happen; path is only null when package fragment input cannot
      // be resolved, which should have disabled the Finish button.
      GWTPluginLog.logError("Unable to compute path of new UiBinder template file");
      return;
    }

    // Save the reference to the created ui.xml file so we can open it on finish
    uiXmlFile = createResourceCreator().createUiXmlFile(uiXmlPath,
        isAddComments(), isAddSampleContent());

    // Remember the "Generate sample content" setting
    GWTPreferences.setUiBinderWizardGenerateContentDefault(isAddSampleContent());
  }

  @Override
  public String getSuperClass() {
    // Instead of displaying the superclass field, we just infer it based on
    // which type of UI the user wants to create (HTML or Widget-based).
    return createResourceCreator().getOwnerSuperclass();
  }

  @Override
  public List<String> getSuperInterfaces() {
    return createResourceCreator().getOwnerSuperinterfaces(isAddSampleContent());
  }

  public IFile getUiXmlFile() {
    return uiXmlFile;
  }

  public boolean isAddSampleContent() {
    return addSampleContentButton.isSelected();
  }

  public void setAddSampleContent(boolean addContent) {
    addSampleContentButton.setSelection(addContent);
  }

  @Override
  protected IStatus containerChanged() {
    IStatus superStatus = super.containerChanged();

    // Errors from our super get precedence
    if (superStatus.getSeverity() == IStatus.ERROR) {
      return superStatus;
    }

    if (JavaModelSearch.findType(getJavaProject(),
        UiBinderConstants.UI_BINDER_TYPE_NAME) == null) {
      return StatusUtilities.newErrorStatus(
          "Project must be using GWT 2.0 or later to use UiBinder.",
          GWTPlugin.PLUGIN_ID);
    }

    return superStatus;
  }

  @Override
  protected void createTypeMembers(IType newType, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {
    createResourceCreator().createOwnerClassMembers(newType, imports,
        isAddComments(), isAddSampleContent(), monitor);
  }

  @Override
  protected void doStatusUpdate() {
    IStatus[] status = new IStatus[] {
        fContainerStatus, fPackageStatus, fTypeNameStatus};

    updateStatus(status);
  }

  @Override
  protected void initTypePage(IJavaElement elem) {
    super.initTypePage(elem);

    setAddSampleContent(GWTPreferences.getUiBinderWizardGenerateContentDefault());
  }

  @Override
  protected IStatus typeNameChanged() {
    IStatus ownerClassNameStatus = super.typeNameChanged();
    if (ownerClassNameStatus.getSeverity() == IStatus.ERROR) {
      return ownerClassNameStatus;
    }

    StatusInfo uiXmlNameStatus = new StatusInfo();
    IPath uiXmlFilePath = getUiXmlFilePath();
    if (uiXmlFilePath != null) {
      // Make sure there's not already a ui.xml file with the same name
      if (ResourcesPlugin.getWorkspace().getRoot().exists(uiXmlFilePath)) {
        uiXmlNameStatus.setError(MessageFormat.format("{0} already exists.",
            uiXmlFilePath.lastSegment()));
      }
    } else {
      // Don't need to worry about this case since the ui.xml path should only
      // be null if the package fragment is invalid, in which case that error
      // will supersede ours.
    }

    return StatusUtil.getMostSevere(new IStatus[] {
        ownerClassNameStatus, uiXmlNameStatus});
  }

  private UiBinderResourceCreator createResourceCreator() {
    if (isWidgetBasedUi()) {
      return new WidgetBasedUiBinderResourceCreator();
    } else if (isHtmlBasedUi()) {
      return new HtmlBasedUiBinderResourceCreator();
    }

    // Should never happen, since we only have 2 radio buttons
    GWTPluginLog.logWarning("UiBinder wizard: No UI type selection");
    return new WidgetBasedUiBinderResourceCreator();
  }

  private void createSampleContentControls(Composite composite, int columns) {
    Label label = new Label(composite, SWT.NONE);
    label.setText("Do you want to add sample content?");
    label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false,
        false, columns, 1));
    DialogField.createEmptySpace(composite);
    addSampleContentButton.doFillIntoGrid(composite, columns - 1);
  }

  private void createUiTypeControls(Composite composite, int columns) {
    uiTypeButtons.doFillIntoGrid(composite, columns);
  }

  private IPath getUiXmlFilePath() {
    IPackageFragment pckgFragment = getPackageFragment();
    if (pckgFragment == null) {
      return null;
    }
    return pckgFragment.getResource().getFullPath().append(
        getTypeName() + UiBinderConstants.UI_BINDER_XML_EXTENSION);
  }

  private boolean isHtmlBasedUi() {
    return uiTypeButtons.isSelected(HTML_UI_INDEX);
  }

  private boolean isWidgetBasedUi() {
    return uiTypeButtons.isSelected(WIDGETS_UI_INDEX);
  }

}
