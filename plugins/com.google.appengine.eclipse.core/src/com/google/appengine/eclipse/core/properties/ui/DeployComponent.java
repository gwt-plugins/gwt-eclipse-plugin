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
package com.google.appengine.eclipse.core.properties.ui;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

/**
 * An UI Component displaying UI elements for deployment operation.
 */
@SuppressWarnings("restriction")
public final class DeployComponent {
  // TODO: read from .properties file
  private static final String APPENGINE_APP_VERSIONS_URL = "http://appengine.google.com/deployment?&app_id=";

  // TODO: read from .properties file
  private static final String APPENGINE_CREATE_APP_URL = "http://appengine.google.com/";

  private Group deployGroup;
  private Text appIdText;
  private Link myApplicationsLink;
  private Text versionText;
  private Link existingVersionsLink;

  private String appId = "";
  private String version = "";

  private ModifyListener modifyListener;

  /**
   * @return the {@link Control} to embed into enclosing UI.
   */
  public Control createContents(Composite parent) {
    createUI(parent);
    addListeners();
    return deployGroup;
  }

  /**
   * @return the appId
   */
  public String getAppId() {
    return appId;
  }

  /**
   * @return the appIdText
   */
  public Text getAppIdTextControl() {
    return appIdText;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return the versionText
   */
  public Text getVersionTextControl() {
    return versionText;
  }

  /**
   * Sets App ID text field value to given text.
   *
   * @param appId the appId to set
   */
  public void setAppIdText(String appId) {
    appIdText.setText(appId);
  }

  /**
   * Enables/Disables the entire component.
   */
  public void setEnabled(boolean enabled) {
    SWTUtilities.setEnabledRecursive(deployGroup, enabled);
    existingVersionsLink.setEnabled(enabled && appId != null && appId.length() > 0);
  }

  /**
   * Sets listener to be fired as any of fields are modified.
   */
  public void setModifyListener(ModifyListener modifyListener) {
    this.modifyListener = modifyListener;
  }

  /**
   * Sets Version text field value to given text.
   *
   * @param version the version to set
   */
  public void setVersionText(String version) {
    versionText.setText(version);
  }

  /**
   * Adds listeners to controls.
   */
  private void addListeners() {
    myApplicationsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions(APPENGINE_CREATE_APP_URL);
      }
    });
    existingVersionsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        assert appId != null;
        BrowserUtilities.launchBrowserAndHandleExceptions(APPENGINE_APP_VERSIONS_URL + appId);
      }
    });
    appIdText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        appId = appIdText.getText().trim();
        if (modifyListener != null) {
          modifyListener.modifyText(e);
        }
      }
    });
    getVersionTextControl().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        version = getVersionTextControl().getText().trim();
        if (modifyListener != null) {
          modifyListener.modifyText(e);
        }
      }
    });
  }

  /**
   * Creates UI components.
   */
  private void createUI(Composite parent) {
    deployGroup = SWTFactory.createGroup(parent, "Deployment", 3, 1, GridData.FILL_HORIZONTAL);
    // Application ID field
    Label appIdLabel = new Label(deployGroup, SWT.NONE);
    appIdLabel.setText("Application ID:");
    appIdText = new Text(deployGroup, SWT.BORDER);
    appIdText.setLayoutData(new GridData(180, SWT.DEFAULT));

    // Link to applications
    myApplicationsLink = new Link(deployGroup, SWT.NONE);
    myApplicationsLink.setText("<a href=\"#\">My applications...</a>");
    GridData createAppLinkGridData = new GridData(SWT.END, SWT.TOP, true, false);
    myApplicationsLink.setLayoutData(createAppLinkGridData);

    // Version field
    Label versionLabel = new Label(deployGroup, SWT.NONE);
    versionLabel.setText("Version:");
    versionText = new Text(deployGroup, SWT.BORDER);
    GridData versionTextGridData = new GridData(60, SWT.DEFAULT);
    getVersionTextControl().setLayoutData(versionTextGridData);

    // Link to existing versions
    existingVersionsLink = new Link(deployGroup, SWT.NONE);
    GridData seeVersionsLinkGridData = new GridData(SWT.END, SWT.TOP, true, false);
    existingVersionsLink.setLayoutData(seeVersionsLinkGridData);
    existingVersionsLink.setText("<a href=\"#\">Existing versions...</a>");

    // Set tab order to skip links
    deployGroup.setTabList(new Control[] {appIdText, getVersionTextControl()});
  }
}
