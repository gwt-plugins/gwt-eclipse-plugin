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
package com.google.gdt.eclipse.core.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkFactory;
import com.google.gdt.eclipse.core.sdk.SdkSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * Allows the user to add an {@link Sdk}.
 * 
 * @param <T>
 */
public class AddSdkDialog<T extends Sdk> extends StatusDialog {

  protected final SdkSet<T> sdkSet;

  protected Button browseButton;

  protected Text directoryText;

  protected Text nameText;

  protected final String pluginId;

  protected T sdk;

  protected final SdkFactory<T> sdkFactory;

  private String shellTitle;

  public AddSdkDialog(Shell parentShell, SdkSet<T> sdkSet, String pluginId,
      String shellTitle, SdkFactory<T> sdkFactory) {
    super(parentShell);

    this.pluginId = pluginId;
    this.sdkSet = sdkSet;
    this.shellTitle = shellTitle;
    this.sdkFactory = sdkFactory;

    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  public T getSdk() {
    return sdk;
  }

  protected String browseForDirectory() {
    DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.OPEN);
    dlg.setFilterPath(directoryText.getText().trim());
    dlg.setMessage("Please select the root directory of your SDK installation.");

    return dlg.open();
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(shellTitle);
    setHelpAvailable(false);
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    Control buttonBar = super.createButtonBar(parent);

    // Initially, OK button needs to be disabled
    super.updateStatus(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, null));
    return buttonBar;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    final GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    container.setLayout(gridLayout);

    createDirectoryArea(container);
    createNameArea(container);

    return container;
  }
  
  protected void createDirectoryArea(Composite container) {
    Label jarsLabel = new Label(container, SWT.NONE);
    jarsLabel.setText("Installation directory:");

    directoryText = new Text(container, SWT.BORDER);
    GridData directoryTextGridData = new GridData(SWT.FILL, SWT.CENTER, true,
        false);
    // TODO: Derive the pixel width from font size
    directoryTextGridData.widthHint = 300;
    directoryText.setLayoutData(directoryTextGridData);
    directoryText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });

    browseButton = new Button(container, SWT.NONE);
    browseButton.setLayoutData(new GridData());
    browseButton.setText("Browse...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String directory = browseForDirectory();
        if (directory != null) {
          directoryText.setText(directory);

          // Suggest the installation directory name as the runtime name, if
          // the user has not already entered one
          if (nameText.getText().length() == 0) {
            nameText.setText(new Path(directory).lastSegment());
          }
        }
      }
    });    
  }

  protected void createNameArea(Composite container) {
    final Label nameLabel = new Label(container, SWT.NONE);
    nameLabel.setText("Display name:");

    nameText = new Text(container, SWT.BORDER);
    nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    nameText.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
      }

      public void focusLost(FocusEvent e) {
        validate();
      }
    });
    nameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });
  }
  
  protected String getDirectory() {
    return directoryText.getText().trim();
  }

  protected String getName() {
    return nameText.getText().trim();
  }
  
  protected IPath getSdkHome(String directory) {
    return new Path(directory);
  }

  @Override
  protected void updateStatus(IStatus status) {
    // Only update the status if it has changed
    if (status.getSeverity() == getStatus().getSeverity()
        && status.getMessage().equals(getStatus().getMessage())) {
      return;
    }

    super.updateStatus(status);
  }

  protected void validate() {
    if (!validateDirectory()) {
      return;
    }

    if (!validateName()) {
      return;
    }

    if (!validateSdk()) {
      return;
    }
  }

  protected boolean validateDirectory() {
    String directory = directoryText.getText().trim();
    if (directory.length() == 0) {
      // No directory specified
      updateStatus(new Status(IStatus.ERROR, pluginId,
          "Enter the installation directory for the SDK."));
      return false;
    }

    IPath sdkHome = getSdkHome(directory);
    File dir = sdkHome.toFile();
    if (!dir.isDirectory()) {
      updateStatus(new Status(IStatus.ERROR, pluginId,
          "The installation directory does not exist"));
      return false;
    }
    return true;
  }

  // returns false if there's a validation error.
  protected boolean validateName() {
    // Can't allow path separator in name, since we use it as a delimiter when
    // we serialize runtimes to the Eclipse preferences store
    String name = nameText.getText().trim();
    if (name.length() == 0) {
      updateStatus(new Status(IStatus.ERROR, pluginId, "The SDK needs a name"));
      return false;
    }

    if (name.indexOf(File.pathSeparator) != -1) {
      updateStatus(new Status(IStatus.ERROR, pluginId,
          "The SDK name contains invalid characters"));
      return false;
    }

    if (sdkSet.contains(name)) {
      updateStatus(new Status(IStatus.ERROR, pluginId,
          "Another SDK already has this name"));
      return false;
    }

    if (name.matches("v[0-9]{1,2}")) {
      updateStatus(new Status(IStatus.ERROR, pluginId,
          "The SDK name is not allowed."));
      return false;
    }
    return true;
  }

  protected boolean validateSdk() {
    String name = nameText.getText().trim();
    String directory = directoryText.getText().trim();
    IPath sdkHome = getSdkHome(directory);

    sdk = sdkFactory.newInstance(name, sdkHome);
    IStatus sdkValidationStatus = sdk.validate();

    // If the status is not OK (either an error, warning, or info), then there
    // needs to be a user-visible message displayed.
    assert (sdkValidationStatus.isOK() || (sdkValidationStatus.getMessage() != null && sdkValidationStatus.getMessage().length() > 0));

    updateStatus(sdkValidationStatus);

    return sdkValidationStatus.isOK();
  }
}
