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
package com.google.appengine.eclipse.wtp.wizards;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.properties.ui.DeployComponent;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;

/**
 * A {@link IFacetWizardPage} for configuring GAE Facet.
 */
@SuppressWarnings("restriction")
public final class GaeFacetWizardPage extends GaeFacetAbstractWizardPage {

  private static final String WIZARD_PAGE_NAME = IGaeFacetConstants.GAE_FACET_ID + ".install.page";
  private DeployComponent deployComponent = new DeployComponent();
  private Button shouldCreateSampleButton;
  private Button openImportApiWizardButton;
  private Text packageText;

  /**
   * Default ctor
   */
  public GaeFacetWizardPage() {
    super(WIZARD_PAGE_NAME);
  }

  @Override
  protected void addModificationListeners() {
    super.addModificationListeners();
    synchHelper.synchText(deployComponent.getAppIdTextControl(), GAE_PROPERTY_APP_ID, null);
    synchHelper.synchText(deployComponent.getVersionTextControl(), GAE_PROPERTY_APP_VERSION, null);
    synchHelper.synchText(deployComponent.getModuleIdTextControl(), GAE_PROPERTY_MODULE_ID, null);
    synchHelper.synchText(packageText, GAE_PROPERTY_PACKAGE, null);
    synchHelper.synchCheckbox(shouldCreateSampleButton, GAE_PROPERTY_CREATE_SAMPLE, null);
    synchHelper.synchCheckbox(openImportApiWizardButton, GAE_PROPERTY_OPEN_IMPORT_API_WIZARD, null);
  }

  @Override
  protected Composite createTopLevelComposite(Composite parent) {
    initializeDialogUnits(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    {
      Group projectGroup = new Group(composite, SWT.NONE);
      projectGroup.setText("Project");
      projectGroup.setLayout(new GridLayout(2, false));
      projectGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      Label packageLabel = new Label(projectGroup, SWT.NONE);
      packageLabel.setText("Package:");
      packageText = new Text(projectGroup, SWT.BORDER);
      packageText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
    }
    {
      deployComponent.createContents(composite);
      try {
        boolean earSupported = isEarSupported();
        deployComponent.setEarSupported(earSupported);
      } catch (CoreException e) {
        setErrorStatus(9999, e.getStatus().getMessage());
        setErrorMessage();
      }
    }
    {
      createDeployOptionsComponent(composite);
    }
    {
      Group sampleGroup = new Group(composite, SWT.NONE);
      sampleGroup.setText("Sample Code");
      sampleGroup.setLayout(new GridLayout());
      sampleGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      shouldCreateSampleButton = new Button(sampleGroup, SWT.CHECK | SWT.LEFT);
      shouldCreateSampleButton.setText("Generate project sample code");
    }
    {
      Group googleApiGroup = new Group(composite, SWT.NONE);
      googleApiGroup.setText("Google APIs");
      googleApiGroup.setLayout(new GridLayout());
      googleApiGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
      openImportApiWizardButton = new Button(googleApiGroup, SWT.CHECK | SWT.LEFT);
      openImportApiWizardButton.setText("Open Google API Import Wizard upon project creation");
    }
    addModificationListeners();
    return composite;
  }

  @Override
  protected String[] getValidationPropertyNames() {
    return new String[] {
        GAE_PROPERTY_APP_ID, GAE_PROPERTY_MODULE_ID, GAE_PROPERTY_APP_VERSION, GAE_PROPERTY_PACKAGE};
  }

  @Override
  protected void restoreDefaultSettings() {
    super.restoreDefaultSettings();
    IProject project = ProjectUtils.getProject(model);
    if (project != null) {
      try {
        model.setStringProperty(GAE_PROPERTY_APP_ID, ProjectUtils.getAppId(project));
        model.setStringProperty(GAE_PROPERTY_APP_VERSION, ProjectUtils.getAppVersion(project));
        model.setStringProperty(GAE_PROPERTY_MODULE_ID, ProjectUtils.getModuleId(project));
      } catch (CoreException e) {
        AppEnginePlugin.logMessage(e);
      }
    }
    if (model.getProperty(GAE_PROPERTY_CREATE_SAMPLE) == null) {
      model.setBooleanProperty(GAE_PROPERTY_CREATE_SAMPLE,
          (Boolean) model.getDefaultProperty(GAE_PROPERTY_CREATE_SAMPLE));
    }
    if (model.getProperty(GAE_PROPERTY_OPEN_IMPORT_API_WIZARD) == null) {
      model.setBooleanProperty(GAE_PROPERTY_OPEN_IMPORT_API_WIZARD,
          (Boolean) model.getDefaultProperty(GAE_PROPERTY_OPEN_IMPORT_API_WIZARD));
    }
    synchHelper.synchAllUIWithModel();
  }
}
