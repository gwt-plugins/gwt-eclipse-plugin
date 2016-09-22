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
package com.google.gwt.eclipse.core.dialogs;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Field for selecting a fully-qualified module name.
 */
@SuppressWarnings("restriction")
public class ModuleSelectionDialogButtonField extends StringDialogField {

  private static GridData gridDataForButton(Button button, int span) {
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = false;
    gd.horizontalSpan = span;
    gd.widthHint = SWTUtil.getButtonWidthHint(button);
    return gd;
  }

  private Button browseButton;

  private boolean browseButtonEnabled;

  private IJavaProject javaProjectWithGWTNature;

  private IModule module;

  private final WizardPage page;

  private IResource selection;

  private final StatusInfo status;

  public ModuleSelectionDialogButtonField(WizardPage page,
      IDialogFieldListener changeListener) {
    super();
    this.page = page;
    this.browseButtonEnabled = true;
    setDialogFieldListener(changeListener);
    setContentAssistProcessor(new ModuleCompletionProcessor());

    status = new StatusInfo();
  }

  @Override
  public void dialogFieldChanged() {
    validate();
    super.dialogFieldChanged();
  }

  @Override
  public Control[] doFillIntoGrid(Composite parent, int nColumns) {
    assertEnoughColumns(nColumns);

    setLabelText("Module:");
    Label label = getLabelControl(parent);
    label.setLayoutData(gridDataForLabel(1));
    Text text = getTextControl(parent);
    text.setLayoutData(gridDataForText(nColumns - 2));
    Button button = getChangeControl(parent);
    button.setLayoutData(gridDataForButton(button, 1));

    return new Control[] {label, text, button};
  }

  public void enableButton(boolean enable) {
    if (isOkToUse(browseButton)) {
      browseButton.setEnabled(isEnabled() && enable);
    }
    browseButtonEnabled = enable;
  }

  public ModuleFile getModule() {
    // TODO: remove this cast if this field needs to support selecting modules
    // in JAR files
    return (ModuleFile) module;
  }

  @Override
  public int getNumberOfControls() {
    return 3;
  }

  public IStatus getStatus() {
    return status;
  }

  public boolean init(IResource selection) {
    assert (selection != null);

    // Can only set selection once
    assert (this.selection == null);

    // Selection can't be workspace root
    assert (selection.getType() != IResource.ROOT);

    this.selection = selection;
    setGWTProject(JavaCore.create(selection.getProject()));

    if (javaProjectWithGWTNature == null) {
      // If we're not dealing with a GWT project; there's nothing to be done
      // in terms of initializing the module field's text.
      return false;
    }

    List<IModule> modules = new ArrayList<IModule>();

    // First see if the selection itself is a module
    if (ModuleUtils.isModuleXml(selection)) {
      modules.add(ModuleUtils.create((IFile) selection));
    } else if (selection.getType() == IResource.FOLDER) {
      // If a folder is selected, see if it has a child module
      modules.addAll(Arrays.asList(ModuleUtils.findChildModules((IContainer) selection)));
    }

    // If we still haven't found a module, try searching all of the project's
    // Java source paths
    if (modules.isEmpty()) {
      modules.addAll(Arrays.asList(ModuleUtils.findAllModules(
          javaProjectWithGWTNature, false)));
    }

    // If we have just 1 module, go ahead and pre-populate the field with it
    if (modules.size() == 1) {
      String moduleName = modules.get(0).getQualifiedName();
      setText(moduleName);
      return true;
    }

    return false;
  }

  public void setGWTProject(IJavaProject javaProject) {

    if (JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)
        && GWTNature.isGWTProject(javaProject.getProject())) {
      this.javaProjectWithGWTNature = javaProject;
    } else {
      this.javaProjectWithGWTNature = null;
    }

    validate();

    ((ModuleCompletionProcessor) getContentAssistProcessor()).setProject(javaProjectWithGWTNature);
  }

  @Override
  protected void updateEnableState() {
    super.updateEnableState();
    if (isOkToUse(browseButton)) {
      browseButton.setEnabled(isEnabled() && browseButtonEnabled);
    }
  }

  private void changeControlPressed() {
    IModule selectedModule = ModuleSelectionDialog.show(page.getShell(),
        javaProjectWithGWTNature, false);
    if (selectedModule != null) {
      setText(selectedModule.getQualifiedName());
    }
  }

  private Button getChangeControl(Composite parent) {
    if (browseButton == null) {
      assertCompositeNotNull(parent);

      browseButton = new Button(parent, SWT.PUSH);
      browseButton.setFont(parent.getFont());
      browseButton.setText("Browse...");
      browseButton.setEnabled(isEnabled() && browseButtonEnabled);
      browseButton.addSelectionListener(new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
          changeControlPressed();
        }

        public void widgetSelected(SelectionEvent e) {
          changeControlPressed();
        }
      });
    }
    return browseButton;
  }

  private void validate() {
    module = null;

    if (javaProjectWithGWTNature == null) {
      return;
    }

    String str = getText().trim();
    if (str.length() == 0) {
      status.setError("Enter the fully-qualified module name");
      return;
    }

    IStatus nameStatus = ModuleUtils.validateQualifiedModuleName(str);
    if (!nameStatus.isOK()) {
      status.setError(nameStatus.getMessage());
      return;
    }

    // Verify that the module exists
    IModule newModule = ModuleUtils.findModule(javaProjectWithGWTNature, str,
        false);
    if (newModule == null) {
      status.setError(MessageFormat.format("The module ''{0}'' does not exist",
          str));
      return;
    }

    // We're all good
    module = newModule;
    status.setOK();
  }

}
