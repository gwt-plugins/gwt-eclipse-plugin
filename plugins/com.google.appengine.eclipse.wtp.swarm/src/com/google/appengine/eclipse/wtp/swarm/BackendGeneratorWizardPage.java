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
package com.google.appengine.eclipse.wtp.swarm;

import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizardPage;
import org.eclipse.wst.server.ui.ServerUIUtil;

/**
 * A WizardPage for generating backend for Android projects.
 */
@SuppressWarnings("restriction")
public final class BackendGeneratorWizardPage extends DataModelWizardPage {

  private static final String SAMPLE_CODE_DESCRIPTION = "When you generate an App Engine backend, "
      + "sample code will be generated in both your Android project and your new App Engine project."
      + " You'll be able to register devices with your backend and send notifications to them from a sample web app.\n\n"
      + "The Android App has functionality that allows it to register and receive messages via Google Cloud Messaging.\n\n"
      + "The App Engine backend provides an Endpoints API to register Android clients and a Web UI to broadcast messages to clients.\n";

  private static final String PAGE_TITLE = "Create App Engine Backend for Android";
  private static final String PAGE_MESSAGE = "Connect your mobile app to the Cloud with App Engine";

  public BackendGeneratorWizardPage(IDataModel model) {
    super(model, "Backend Generator Page");
    setTitle(PAGE_TITLE);
    setDescription(PAGE_MESSAGE);
  }

  @Override
  protected Composite createTopLevelComposite(Composite parent) {
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

    setControl(container);
    Dialog.applyDialogFont(container);

    synchHelper.synchAllUIWithModel();
    return container;
  }

  @Override
  protected String[] getValidationPropertyNames() {
    return new String[] {BackendGeneratorDataModelProvider.GAE_BACKEND_SELECTED_RUNTIME};
  }

  @Override
  protected boolean showValidationErrorsOnEnter() {
    return true;
  }

  /**
   * Creates the group for App Engine Runtime selection.
   */
  private void createAppEngineSdkSection(Composite container) {
    Group sdkGroup = new Group(container, SWT.NONE);
    sdkGroup.setText("App Engine Runtime");
    sdkGroup.setLayout(new GridLayout(2, false));
    sdkGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    {
      Combo serverTargetCombo = new Combo(sdkGroup, SWT.READ_ONLY);
      serverTargetCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      synchHelper.synchCombo(serverTargetCombo,
          BackendGeneratorDataModelProvider.GAE_BACKEND_SELECTED_RUNTIME, null);
      if (serverTargetCombo.getSelectionIndex() == -1
          && serverTargetCombo.getVisibleItemCount() != 0) {
        serverTargetCombo.select(0);
      }
    }
    {
      Link link = new Link(sdkGroup, SWT.NONE);
      link.setText("<a>New Runtime...</a>");
      link.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          // TODO(amitin): do not close wizard, but update runtime list upon completing.
          final Display display = getShell().getDisplay();
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              WizardDialog wizardDialog = (WizardDialog) getWizard().getContainer();
              wizardDialog.close();
              ServerUIUtil.showNewRuntimeWizard(display.getActiveShell(), null, null);
            }
          });
        }
      });
    }
  }

  /**
   * Creates a GCM configuration section in the dialog for a user to enter a project number and an
   * api key.
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
        + "left blank, they can be manually entered into code after the "
        + "project is generated.\n");

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
    Text gcmApiKeyText = new Text(formGroup, SWT.BORDER);
    gcmApiKeyText.setToolTipText(gcmApiTip);
    gcmApiKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmApiKeyText.setFont(parent.getFont());
    synchHelper.synchText(gcmApiKeyText, BackendGeneratorDataModelProvider.SCM_API_KEY, null);
    // project number
    final String gcmProjectNumberTip = "Project Number for Google Cloud Messaging. "
        + "This will be injected into the Android project. "
        + "Can be left blank and manually entered later.";
    Label pnLabel = new Label(formGroup, SWT.NONE);
    pnLabel.setText("Project Number:");
    pnLabel.setFont(parent.getFont());
    pnLabel.setToolTipText(gcmProjectNumberTip);
    // project number entry field
    Text gcmProjectNumberText = new Text(formGroup, SWT.BORDER);
    gcmProjectNumberText.setToolTipText(gcmProjectNumberTip);
    gcmProjectNumberText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    gcmProjectNumberText.setFont(parent.getFont());
    synchHelper.synchText(gcmProjectNumberText,
        BackendGeneratorDataModelProvider.SCM_PROJECT_NUMBER, null);
  }
}
