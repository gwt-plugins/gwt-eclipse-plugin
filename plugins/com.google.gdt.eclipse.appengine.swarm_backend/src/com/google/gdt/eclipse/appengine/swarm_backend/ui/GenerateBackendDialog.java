/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.appengine.swarm_backend.ui;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.gdt.eclipse.appengine.swarm_backend.AppEngineSwarmBackendImages;
import com.google.gdt.eclipse.appengine.swarm_backend.AppEngineSwarmBackendPlugin;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock;
import com.google.gdt.eclipse.core.ui.SdkSelectionBlock.SdkSelection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for generating an App Engine backend for an existing Android project.
 */
public class GenerateBackendDialog extends TitleAreaDialog {

  /**
   * Select a {@link GaeSdk} from the set of {@link Sdk} known to the workspace.
   */
  private final class GaeWorkspaceSdkSelectionBlock extends SdkSelectionBlock<GaeSdk> {
    private GaeWorkspaceSdkSelectionBlock(Composite parent, int style) {
      super(parent, style);

      updateSdkBlockControls();
      initializeSdkComboBox();

      setSelection(-1);
    }

    @Override
    protected void doConfigure() {
      if (Window.OK == PreferencesUtil.createPreferenceDialogOn(getShell(), GaePreferencePage.ID,
          new String[] {GaePreferencePage.ID}, null).open()) {
        GenerateBackendDialog.this.validatePage();
      }
    }

    @Override
    protected GaeSdk doGetDefaultSdk() {
      return GaePreferences.getDefaultSdk();
    }

    @Override
    protected List<GaeSdk> doGetSpecificSdks() {
      return new ArrayList<GaeSdk>(GaePreferences.getSdks());
    }
  }

  private static final String SAMPLE_CODE_DESCRIPTION = "When you generate an App Engine backend, "
      + "sample code will be generated in both your Android project and your new App Engine project."
      + " You'll be able to register devices with your backend and send notifications to them from a sample web app.\n\n"
      + "The Android App has functionality that allows it to register and receive messages via Google Cloud Messaging.\n\n"
      + "The App Engine backend provides an Endpoints API to register Android clients and a Web UI to broadcast messages to clients.\n";

  private static final String DIALOG_TITLE = "Create App Engine Backend for Android";
  private static final String DIALOG_MESSAGE = "Connect your mobile app to the Cloud with App Engine";

  private final IProject androidProject;

  private Button createButton;

  private Text gcmProjectNumberText;
  private Text gcmApiKeyText;

  private GaeWorkspaceSdkSelectionBlock sdkSelectionBlock;

  public GenerateBackendDialog(Shell parentShell, IProject androidProject, String backendProjectName) {
    super(parentShell);
    this.androidProject = androidProject;
  }

  public void validatePage() {
    GaeSdk selectedGaeSdk = sdkSelectionBlock.getSdkSelection().getSelectedSdk();
    if (selectedGaeSdk == null) {
      setErrorStatus("Please configure an App Engine SDK.");
      return;
    }

    IStatus gaeSdkValidationStatus = selectedGaeSdk.validate();
    if (!gaeSdkValidationStatus.isOK()) {
      setErrorStatus("The selected App Engine SDK is not valid: "
          + gaeSdkValidationStatus.getMessage());
      return;
    }

    if (!GaeSdkCapability.CLOUD_ENDPOINTS.check(selectedGaeSdk)) {
      setErrorStatus("App Engine backends for Android projects require an App Engine SDK with version "
          + GaeSdkCapability.CLOUD_ENDPOINTS.minVersion + " or higher.");
      return;
    }

    setOkStatus();
  }

  private void setErrorStatus(String message) {
    setErrorMessage(message);
    createButton.setEnabled(false);
  }

  private void setOkStatus() {
    setErrorMessage(null);
    createButton.setEnabled(true);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(DIALOG_TITLE);
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    createButton = getButton(IDialogConstants.OK_ID);

    // Re-label the OK button and set it as default
    createButton.setText("Create");
    getShell().setDefaultButton(createButton);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    // "Adding a backend allows your Android App to request and
    // store data from an App Engine server."
    setTitle(DIALOG_TITLE);

    setTitleImage(AppEngineSwarmBackendPlugin.getDefault().getImageDescriptor(
        AppEngineSwarmBackendImages.GENERATE_BACKEND_DIALOG_IMAGE).createImage());

    setMessage(DIALOG_MESSAGE);

    validatePage();
    
    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(parent, SWT.NONE);
    GridData containerGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    container.setLayout(gridLayout);

    GridData textGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    textGridData.widthHint = convertWidthInCharsToPixels(80);
    Label cloudMessagingText = new Label(container, SWT.WRAP);
    cloudMessagingText.setLayoutData(textGridData);
    cloudMessagingText.setText(SAMPLE_CODE_DESCRIPTION);

    createBackendConfigSection(container);

    createAppEngineSdkSection(container);
        
    return container;
  }

  /**
   * Creates a GCM configuration section in the dialog for a user to enter a
   * project number and an api key
   * 
   * @param parent
   */
  private void createBackendConfigSection(Composite parent) {
    Group configGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
    configGroup.setText("Configuration Parameters");
    configGroup.setLayout(new GridLayout(1, false));
    configGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridData textGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    textGridData.widthHint = convertWidthInCharsToPixels(80);
    Label configText = new Label(configGroup, SWT.WRAP);
    configText.setLayoutData(textGridData);
    configText.setText("These parameters are required for a working example.  "
        + "They can be obtained from the Google API console for your project.\n\n"
        + "If entered now they will be injected into the project. If "
        + "left blank, they can be manually entered into code after the " + "project is generated.\n");

    Link docsLink = new Link(configGroup, SWT.NONE);
    docsLink.setText("For more information, click <a href=\"#\">here...</a>");
    docsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions("https://developers.google.com/eclipse/docs/endpoints-create-fromandroid");
      }
    });

    Composite formGroup = new Composite(configGroup, SWT.NONE);
    formGroup.setLayout(new GridLayout(2, false));
    formGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    // api key label
    final String gcmApiTip = "API Key for Google Cloud Messaging. "
        + "This will be injected into the App Engine project. "
        + "Can be left blank and manually entered later.";
    Label apiLabel = new Label(formGroup, SWT.NONE);
    apiLabel.setText("API Key:");
    apiLabel.setFont(parent.getFont());
    apiLabel.setToolTipText(gcmApiTip);
    // api key entry field
    gcmApiKeyText = new Text(formGroup, SWT.BORDER);
    gcmApiKeyText.setToolTipText(gcmApiTip);
    gcmApiKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmApiKeyText.setFont(parent.getFont());

    // project number
    final String gcmProjectNumberTip = "Project Number for Google Cloud Messaging. "
        + "This will be injected into the Android project. "
        + "Can be left blank and manually entered later.";
    Label pnLabel = new Label(formGroup, SWT.NONE);
    pnLabel.setText("Project Number:");
    pnLabel.setFont(parent.getFont());
    pnLabel.setToolTipText(gcmProjectNumberTip);
    // project number entry field
    gcmProjectNumberText = new Text(formGroup, SWT.BORDER);
    gcmProjectNumberText.setToolTipText(gcmProjectNumberTip);
    gcmProjectNumberText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmProjectNumberText.setFont(parent.getFont());
  }

  /**
   * Creates the group for App Engine SDK Selection
   */
  private void createAppEngineSdkSection(Composite container) {
    Label filler = new Label(container, SWT.NONE);
    Group sdkGroup = new Group(container, SWT.NONE);
    sdkGroup.setText("App Engine SDK");
    sdkGroup.setLayout(new GridLayout(3, false));
    sdkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    sdkSelectionBlock = new GaeWorkspaceSdkSelectionBlock(sdkGroup, SWT.NONE);
    sdkSelectionBlock.addSdkSelectionListener(new SdkSelectionBlock.SdkSelectionListener() {
      public void onSdkSelection(SdkSelectionEvent e) {
        validatePage();
      }
    });
  }

  @Override
  protected void okPressed() {
    // extract the data before closing the dialog
    String gcmProjectNumber = gcmProjectNumberText.getText();
    String gcmApiKey = gcmApiKeyText.getText();
    SdkSelection<GaeSdk> sdkSelection = sdkSelectionBlock.getSdkSelection();

    // Close the dialog
    super.okPressed();

    try {
      BackendGenerator generator = new BackendGenerator(androidProject, false, gcmProjectNumber,
          gcmApiKey, sdkSelection);
      generator.generateBackendProject();

    } catch (CoreException e) {
      AppEngineSwarmBackendPlugin.log(e);
    } catch (InterruptedException e) {
      AppEngineSwarmBackendPlugin.log(e);
    }
  }
}
