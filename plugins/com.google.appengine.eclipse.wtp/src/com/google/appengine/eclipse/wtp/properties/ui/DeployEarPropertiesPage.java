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
package com.google.appengine.eclipse.wtp.properties.ui;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * GAE WTP EAR project deployment property page.
 */
public final class DeployEarPropertiesPage extends DeployPropertiesPage {
  public static final String ID = AppEnginePlugin.PLUGIN_ID + ".ear.deployProperties";

  private String currentAppId;

  private Text applicationIdText;

  @Override
  protected Control createContents(Composite parent) {
    Dialog.applyDialogFont(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    {
      Group deployGroup = new Group(composite, SWT.NONE);
      deployGroup.setText("Deployment");
      deployGroup.setLayout(new GridLayout(2, false));
      deployGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      Label applicationLabel = new Label(deployGroup, SWT.NONE);
      applicationLabel.setText("Application ID:");
      applicationIdText = new Text(deployGroup, SWT.BORDER);
      applicationIdText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      applicationIdText.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          validateInput();
        }
      });
    }
    {
      createDeploymentOptionsComponent(composite);
    }

    initializeValues();

    return composite;
  }

  /**
   * Setup current values.
   */
  @Override
  protected void initializeValues() {
    super.initializeValues();
    try {
      IProject project = getProject();
      currentAppId = ProjectUtils.getAppId(project);
      applicationIdText.setText(currentAppId);
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
  }

  @Override
  protected void saveProjectProperties() throws Exception {
    // save App ID into appengine-application.xml
    IProject project = getProject();
    String appId = applicationIdText.getText().trim();
    if (!appId.equals(currentAppId)) {
      ProjectUtils.setAppId(project, appId, true);
    }
    super.saveProjectProperties();
  }

  /**
   * Do validate app ID.
   */
  private IStatus validateAppId() throws CoreException {
    String enteredAppId = applicationIdText.getText().trim();
    if (enteredAppId.length() > 0) {
      IFile appEngineApplicationXml = ProjectUtils.getAppEngineApplicationXml(getProject());
      if (!appEngineApplicationXml.exists()) {
        return StatusUtilities.newErrorStatus(
            "Cannot set application ID (appengine-application.xml is missing)",
            AppEnginePlugin.PLUGIN_ID);
      }
    } else {
      return StatusUtilities.newWarningStatus(
          "You won't be able to deploy to Google without valid Application ID.",
          AppEnginePlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  /**
   * Do field values validation.
   */
  private void validateInput() {
    try {
      updateStatus(validateAppId());
    } catch (CoreException e) {
      updateStatus(StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
    }
  }
}
