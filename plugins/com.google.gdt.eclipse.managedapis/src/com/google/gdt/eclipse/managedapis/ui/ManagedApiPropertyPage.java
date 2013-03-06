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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;
import com.google.gdt.eclipse.core.ui.ResourceTreeSelectionDialog;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;
import com.google.gdt.eclipse.managedapis.platform.ManagedApiProjectProperties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.List;

/**
 * Sets project properties related to the ManagedAPIs.
 */
public class ManagedApiPropertyPage extends AbstractProjectPropertyPage {

  private class CopyToDirEventHandler extends SelectionAdapter implements
      ModifyListener {

    public void modifyText(ModifyEvent e) {
      fieldChanged();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == hasCopyToDirButton) {
        maybePrepopulateDynamicWebValues(false);
        if (!hasCopyToDirButton.getSelection()) {
          useCopyToDirDefaultButton.setSelection(false);
          copyToDirText.setText("");
        }
        fieldChanged();
      } else if (e.widget == useCopyToDirDefaultButton) {
        maybePrepopulateDynamicWebValues(useCopyToDirDefaultButton.getSelection());
        fieldChanged();
      } else if (e.widget == copyToDirBrowseButton) {
        chooseCopyToDir();
      }
    }
  }

  public static final String ID = "com.google.gdt.eclipse.managedapis.ui.managedApiPropertyPage";
  private static final String COPY_TO_DIR_SELECTION_DIALOG_MESSAGE = "Choose the directory to copy to";
  private static final String COPY_TO_DIR_SELECTION_DIALOG_TITLE = "Directory Selection";
  private IFolder copyToDir;

  private Button copyToDirBrowseButton;

  private Composite copyToDirComponent;

  private Text copyToDirText;

  private Button hasCopyToDirButton;
  private Button useCopyToDirDefaultButton;
  private ManagedApiProject managedApiProject;

  public ManagedApiPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(3, false);
    layout.verticalSpacing = layout.horizontalSpacing = 0;
    panel.setLayout(layout);

    createCopyToTargetDirComponent(panel);

    boolean initializeAndEnable = false;
    try {
      managedApiProject = ManagedApiProjectImpl.getManagedApiProject(JavaCore.create(getProject()));
      initializeAndEnable = managedApiProject.hasManagedApis();
    } catch (CoreException e) {
      // not a ManagedAPI project
    }

    if (!initializeAndEnable) {
      SWTUtilities.setEnabledRecursive(panel, false);
    } else {
      initializeControls();
      addEventHandlers();
      fieldChanged();
    }
    return panel;
  }

  @Override
  protected void saveProjectProperties() throws Exception {
    // Save project properties
    if (useCopyToDirDefaultButton.getSelection()) {
      managedApiProject.setDefaultCopyToTargetDir();
    } else {
      managedApiProject.setCopyToTargetDir(copyToDir);
    }
  }

  private void addEventHandlers() {
    CopyToDirEventHandler handler = new CopyToDirEventHandler();
    hasCopyToDirButton.addSelectionListener(handler);
    useCopyToDirDefaultButton.addSelectionListener(handler);
    copyToDirBrowseButton.addSelectionListener(handler);
    copyToDirText.addModifyListener(handler);
  }

  private void chooseCopyToDir() {
    IProject project = getProject();

    ResourceTreeSelectionDialog dialog = new ResourceTreeSelectionDialog(
        getShell(), COPY_TO_DIR_SELECTION_DIALOG_TITLE,
        COPY_TO_DIR_SELECTION_DIALOG_MESSAGE, project, copyToDir,
        IResource.FOLDER, IResource.FOLDER, false);
    List<IPath> paths = dialog.chooseResourcePaths();
    if (paths != null) {
      assert (paths.size() == 1);
      copyToDirText.setText(paths.get(0).toString());
    }
  }

  private void createCopyToTargetDirComponent(Composite parent) {
    int numColumns = ((GridLayout) parent.getLayout()).numColumns;

    hasCopyToDirButton = new Button(parent, SWT.CHECK);
    hasCopyToDirButton.setText("Copy Google API JARs to directory:");
    GridData hasCopyToDirButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
    hasCopyToDirButtonGridData.horizontalSpan = numColumns;
    hasCopyToDirButton.setLayoutData(hasCopyToDirButtonGridData);

    useCopyToDirDefaultButton = new Button(parent, SWT.CHECK);
    useCopyToDirDefaultButton.setText("Use default directory");
    GridData useCopyToDirDefaultButtonGridData = new GridData(
        GridData.FILL_HORIZONTAL);
    useCopyToDirDefaultButtonGridData.horizontalSpan = numColumns;
    useCopyToDirDefaultButton.setLayoutData(useCopyToDirDefaultButtonGridData);

    copyToDirComponent = new Composite(parent, SWT.NONE);
    GridData copyToDirComponentGridData = new GridData(GridData.FILL_HORIZONTAL);
    copyToDirComponentGridData.horizontalSpan = numColumns;
    copyToDirComponent.setLayoutData(copyToDirComponentGridData);
    GridLayout warDirComponentLayout = new GridLayout(3, false);
    warDirComponentLayout.marginLeft = 16;
    copyToDirComponent.setLayout(warDirComponentLayout);

    Label warDirLabel = new Label(copyToDirComponent, SWT.NONE);
    warDirLabel.setText("Directory:");

    copyToDirText = new Text(copyToDirComponent, SWT.BORDER);
    GridData warDirTextGridData = new GridData(GridData.FILL_HORIZONTAL);
    copyToDirText.setLayoutData(warDirTextGridData);

    copyToDirBrowseButton = new Button(copyToDirComponent, SWT.NONE);
    copyToDirBrowseButton.setText("&Browse...");
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private void initializeControls() {
    String copyToDirProp = ManagedApiProjectProperties.getCopyToTargetDir(getProject());
    try {
      if (managedApiProject.isUseDefaultCopyToTargetDir()) {
        hasCopyToDirButton.setSelection(true);
        useCopyToDirDefaultButton.setSelection(true);
        copyToDirText.setText(managedApiProject.getDefaultCopyToTargetDir().getProjectRelativePath().toPortableString());
      } else if (managedApiProject.getCopyToTargetDir() != null) {
        hasCopyToDirButton.setSelection(true);
        useCopyToDirDefaultButton.setSelection(false);
        copyToDirText.setText(copyToDirProp);
      } else {
        hasCopyToDirButton.setSelection(false);
        useCopyToDirDefaultButton.setSelection(false);
        copyToDirText.setText("");
      }
    } catch (CoreException e) {
      ManagedApiLogger.log(ManagedApiLogger.ERROR, e,
          "Failure reading Managed API project properties");
    }
  }

  private void maybePrepopulateDynamicWebValues(boolean overwrite) {
    if (hasCopyToDirButton.getSelection()) {
      if (overwrite
          || StringUtilities.isEmptyOrWhitespace(copyToDirText.getText())) {
        IFolder defaultCopyToDir;
        try {
          defaultCopyToDir = managedApiProject.getDefaultCopyToTargetDir();
          if (defaultCopyToDir != null) {
            copyToDirText.setText(defaultCopyToDir.getProjectRelativePath().toPortableString());
          }
        } catch (CoreException e) {
          ManagedApiLogger.log(ManagedApiLogger.ERROR, e,
              "Failure reading Managed API project properties");
        }
      }
    }
  }

  private void updateControls() {
    try {
      boolean enableUseCopyToDirDefaultButton = hasCopyToDirButton.getSelection()
          && (managedApiProject.getDefaultCopyToTargetDir() != null);
      boolean enableCopyToDirComponent = hasCopyToDirButton.getSelection()
          && !useCopyToDirDefaultButton.getSelection();
      SWTUtilities.setEnabledRecursive(useCopyToDirDefaultButton,
          enableUseCopyToDirDefaultButton);
      SWTUtilities.setEnabledRecursive(copyToDirComponent,
          enableCopyToDirComponent);
    } catch (CoreException e) {
      ManagedApiLogger.log(ManagedApiLogger.ERROR, e,
          "Failure reading Managed API project properties");
    }
  }

  private IStatus validateCopyToDir() {
    copyToDir = null;

    if (!hasCopyToDirButton.getSelection()) {
      // If main checkbox is not set, we have no CopyTo directory to validate
      return StatusUtilities.OK_STATUS;
    } else if (hasCopyToDirButton.getSelection()
        && useCopyToDirDefaultButton.getSelection()) {
      // If main checkbox is set and the default is selected, you are good by
      // default
      return StatusUtilities.OK_STATUS;
    }

    String copyToDirString = copyToDirText.getText().trim();

    if (copyToDirString.length() == 0) {
      return StatusUtilities.newErrorStatus(
          "Enter the directory that the Google API JARs should be copied to",
          CorePlugin.PLUGIN_ID);
    }

    IPath path = new Path(copyToDirString);
    IProject project = getProject();

    IResource copyToDirRes = project.findMember(path);
    if (!(copyToDirRes instanceof IFolder)) {
      return StatusUtilities.newErrorStatus(
          "The folder ''{0}/{1}'' does not exist", CorePlugin.PLUGIN_ID,
          project.getName(), path);
    }
    copyToDir = (IFolder) copyToDirRes;
    return StatusUtilities.OK_STATUS;
  }

  private void validateFields() {
    IStatus warDirStatus = validateCopyToDir();
    updateStatus(warDirStatus);
  }

}
