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
import org.eclipse.jst.j2ee.project.EarUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * GAE WTP Web project deployment property page.
 */
public final class DeployWebPropertiesPage extends DeployPropertiesPage {
  public static final String ID = AppEnginePlugin.PLUGIN_ID + ".web.deployProperties";

  private DeployComponent deployComponent = new DeployComponent();
  private String currentAppId;
  private String currentModuleId;

  private String currentVersion;

  private boolean supportsEar;

  @Override
  protected Control createContents(Composite parent) {
    Dialog.applyDialogFont(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());

    deployComponent.createContents(composite);
    deployComponent.setModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });
    createDeploymentOptionsComponent(composite);

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
      currentModuleId = ProjectUtils.getModuleId(project);
      currentVersion = ProjectUtils.getAppVersion(project);
      deployComponent.setAppIdText(currentAppId);
      deployComponent.setVersionText(currentVersion);
      deployComponent.setModuleIdText(currentModuleId);
      // EAR
      supportsEar = ProjectUtils.isEarSupported(project);
      deployComponent.setEarSupported(supportsEar);
      if (supportsEar) {
        // disable app id & deployment options if the project is part of EAR project.
        boolean standaloneProject = EarUtilities.isStandaloneProject(getProject());
        deployComponent.setUsingEar(!standaloneProject);
        deployOptionsComponent.setEnabled(standaloneProject);
      }
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
  }

  @Override
  protected void saveProjectProperties() throws Exception {
    // save App ID, Version & Module into appengine-web.xml
    // TODO(amitin): if this project is a part of EAR GAE project consider saving project
    // properties into EAR project instead, since app id & deployment options are taken from EAR
    // project and this project properties are ignored
    // The problem is that this project can be part of more than one EAR projects.
    IProject project = getProject();
    String appId = deployComponent.getAppId();
    if (!appId.equals(currentAppId)) {
      ProjectUtils.setAppId(project, appId, true);
    }
    String version = deployComponent.getVersion();
    if (!version.equals(currentVersion)) {
      ProjectUtils.setAppVersion(project, version, true);
    }
    String moduleId = deployComponent.getModuleId();
    if (!moduleId.equals(currentModuleId) && supportsEar) {
      ProjectUtils.setModuleId(project, moduleId, true);
    }
    super.saveProjectProperties();
  }

  /**
   * Do validate app ID.
   */
  private IStatus validateAppId() throws CoreException {
    if (EarUtilities.isStandaloneProject(getProject())) {
      // validate only if it is not a part of EAR project.
      String enteredAppId = deployComponent.getAppId();
      if (enteredAppId.length() > 0) {
        IFile appEngineWebXml = ProjectUtils.getAppEngineWebXml(getProject());
        if (!appEngineWebXml.exists()) {
          return StatusUtilities.newErrorStatus(
              "Cannot set application ID (appengine-web.xml is missing)", AppEnginePlugin.PLUGIN_ID);
        }
      } else {
        return StatusUtilities.newWarningStatus(
            "You won't be able to deploy to Google without valid Application ID.",
            AppEnginePlugin.PLUGIN_ID);
      }
    }
    return StatusUtilities.OK_STATUS;
  }

  /**
   * Do field values validation.
   */
  private void validateInput() {
    try {
      IStatus appIdStatus = validateAppId();
      IStatus versionStatus = validateVersion();
      IStatus moduleIdStatus = validateModuleId();
      updateStatus(appIdStatus, versionStatus, moduleIdStatus);
    } catch (CoreException e) {
      updateStatus(StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
    }
  }

  /**
   * Do validate module ID.
   */
  private IStatus validateModuleId() throws CoreException {
    if (!supportsEar) {
      return StatusUtilities.OK_STATUS;
    }
    String enteredModuleId = deployComponent.getModuleId();
    if (enteredModuleId.length() > 0) {
      IFile appEngineWebXml = ProjectUtils.getAppEngineWebXml(getProject());
      if (!appEngineWebXml.exists()) {
        return StatusUtilities.newErrorStatus(
            "Cannot set module ID (appengine-web.xml is missing)", AppEnginePlugin.PLUGIN_ID);
      }
    }
    return StatusUtilities.OK_STATUS;
  }

  /**
   * Do validate app version.
   */
  private IStatus validateVersion() throws CoreException {
    String enteredVersion = deployComponent.getVersion();
    if (enteredVersion.length() == 0) {
      return StatusUtilities.newErrorStatus("Please enter version number.",
          AppEnginePlugin.PLUGIN_ID);
    }
    if (!enteredVersion.matches("[a-zA-Z0-9-]*")) {
      return StatusUtilities.newErrorStatus(
          "Invalid version number. Only letters, digits and hyphen allowed.",
          AppEnginePlugin.PLUGIN_ID);
    }
    IFile appEngineWebXml = ProjectUtils.getAppEngineWebXml(getProject());
    if (enteredVersion.length() > 0 && !appEngineWebXml.exists()) {
      return StatusUtilities.newErrorStatus("Cannot set version (appengine-web.xml is missing)",
          AppEnginePlugin.PLUGIN_ID);
    }
    return StatusUtilities.OK_STATUS;
  }
}
