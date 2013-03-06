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
package com.google.gwt.eclipse.core.sdk.ui;

import com.google.gdt.eclipse.core.sdk.SdkFactory;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.ui.AddSdkDialog;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog to add a GWT SDK ("GWT Runtime") to a project. This is special,
 * because with GWT you have the option of running from the GWT source.
 */
public class AddGwtSdkDialog extends AddSdkDialog<GWTRuntime> {
  private final boolean contribRuntimeAvailable;
  private Button contribButton;

  public AddGwtSdkDialog(Shell parentShell, SdkSet<GWTRuntime> sdkSet,
      String pluginId, String shellTitle, SdkFactory<GWTRuntime> sdkFactory) {
    super(parentShell, sdkSet, pluginId, shellTitle, sdkFactory);

    IPath workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    GWTRuntime runtime = GWTRuntime.getFactory().newInstance("", workspace);
    IStatus status = runtime.validate();
    contribRuntimeAvailable = status.isOK();
  }

  @Override
  protected void createDirectoryArea(Composite container) {
    if (contribRuntimeAvailable) {
      Button jarsButton = new Button(container, SWT.RADIO);
      jarsButton.setText("Installation directory:");
      jarsButton.setSelection(true);
    } else {
      Label jarsLabel = new Label(container, SWT.NONE);
      jarsLabel.setText("Installation directory:");
    }

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

    if (contribRuntimeAvailable) {
      contribButton = new Button(container, SWT.RADIO);
      final GridData contribButtonGridData = new GridData(SWT.LEFT, SWT.CENTER,
          false, false, 3, 1);
      contribButton.setLayoutData(contribButtonGridData);
      contribButton.setText("Use GWT source projects in my workspace");
      contribButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateControls();
          validate();
        }
      });

      Label spacer = new Label(container, SWT.NONE);
      spacer.setText(" ");
      spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    }
  }

  /*
   * The only differences with validating for adding a GWT SDK are that if a
   * contributor runtime is selected, there's no need for the directory to be
   * put in, and the sdkHome is the workspace in that case.
   */
  @Override
  protected IPath getSdkHome(String directory) {
    if (isContribRuntimeSelected()) {
      // return the workspace if it's a contributor runtime.
      return ResourcesPlugin.getWorkspace().getRoot().getLocation();
    } else {
      return super.getSdkHome(directory);
    }
  }

  @Override
  protected boolean validateDirectory() {
    if (isContribRuntimeSelected()) {
      return true;
    } else {
      return super.validateDirectory();
    }
  }

  private boolean isContribRuntimeSelected() {
    return (contribButton != null && contribButton.getSelection());
  }

  private void updateControls() {
    boolean jarRuntime = (!isContribRuntimeSelected());
    directoryText.setEnabled(jarRuntime);
    browseButton.setEnabled(jarRuntime);
  }
}
