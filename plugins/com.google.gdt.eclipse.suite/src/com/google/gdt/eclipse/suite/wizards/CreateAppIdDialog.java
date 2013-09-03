/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.appengine.api.AppengineApiWrapper;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;

/**
 * This is the Create App Id dialog. It allows the user to create a new Cloud project app id using
 * the App Engine API. User should be logged in before opening this dialog. If user is not logged in
 * before opening this dialog, user will be prompted to log in when they attempt to create a new app
 * id.
 */
public class CreateAppIdDialog extends TitleAreaDialog {

  private Text newAppIdText;
  private Button createBtn;
  private String newAppIdCopy = "";
  private static final String ENTER_APP_ID_MESSAGE = "Enter the app id you would like to create";
  private static final String CREATE_APP_ID_MESSAGE = "Create a new app id";

  /**
   * Constructor
   */
  public CreateAppIdDialog(Shell parentShell) {
    super(parentShell);
  }

  /**
   * Returns the last text entered in the newAppIdText text box.
   */
  public String getAppId() {
    return newAppIdCopy;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Create App Id");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // create Create and Cancel buttons
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    createButton(parent, IDialogConstants.CLIENT_ID, "Create", false);
    createBtn = getButton(IDialogConstants.CLIENT_ID);
    createBtn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (createAppID()) {
          okPressed();
        }
      }
    });
    createBtn.setEnabled(false);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(parent, SWT.NONE);
    GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    container.setLayout(gridLayout);

    // New App Id label
    final Label projectNameLabel = new Label(container, SWT.NONE);
    projectNameLabel.setText("New App Id:");

    // New App Id text box
    newAppIdText = new Text(container, SWT.BORDER);
    final GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
    gd1.horizontalSpan = 2;
    newAppIdText.setLayoutData(gd1);
    newAppIdText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        newAppIdCopy = newAppIdText.getText();
        updateControls();
      }
    });
    setMessage(ENTER_APP_ID_MESSAGE);
    return container;
  }

  /**
   * Creates a new Cloud project with the App ID in the "New App Id" text box. If it is successful
   * the function returns true else an error message is displayed and the function returns false.
   */
  private boolean createAppID() {
    AppengineApiWrapper appEngineApi = new AppengineApiWrapper();
    try {
      appEngineApi.insertNewApplication(newAppIdText.getText(), true);
    } catch (IOException e) {
      MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating App Id",
          e.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Disables the "Create" button if the "New App Id" text box is empty and enables it otherwise.
   */
  private void updateControls() {
    if (newAppIdText.getText().equals("")) {
      setMessage(ENTER_APP_ID_MESSAGE);
      createBtn.setEnabled(false);
   } else {
      setMessage(CREATE_APP_ID_MESSAGE);
      createBtn.setEnabled(true);
   }
  }
}
