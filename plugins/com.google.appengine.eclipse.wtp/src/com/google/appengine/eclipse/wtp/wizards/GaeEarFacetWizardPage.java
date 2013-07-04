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

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;

/**
 * A {@link IFacetWizardPage} for configuring GAE EAR Facet.
 */
@SuppressWarnings("restriction")
public final class GaeEarFacetWizardPage extends GaeFacetAbstractWizardPage {

  private static final String WIZARD_PAGE_NAME = IGaeFacetConstants.GAE_EAR_FACET_ID
      + ".install.page";
  private Text applicationIdText;

  /**
   * Default ctor
   */
  public GaeEarFacetWizardPage() {
    super(WIZARD_PAGE_NAME);
  }

  @Override
  public boolean isPageComplete() {
    // a bit of HACK here: the goal is to validate SDK version against EAR support.
    IDataModel masterDataModel = getMasterDataModel();
    IRuntime runtime = (IRuntime) masterDataModel.getProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME);
    if (runtime != null) {
      IPath sdkLocation = ProjectUtils.getSdkPath(runtime);
      if (sdkLocation != null) {
        SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
        GaeSdk sdk = SdkUtils.findSdkForInstallationPath(sdks, sdkLocation);
        if (sdk != null) {
          boolean earSupported = sdk.getCapabilities().contains(GaeSdkCapability.EAR);
          if (!earSupported) {
            setErrorStatus(9999, "This App Engine SDK doesn't support EAR projects, use "
                + GaeSdkCapability.EAR.minVersion + " or later.");
            setErrorMessage();
          }
          return earSupported;
        }
      }
    }
    return super.isPageComplete();
  }

  @Override
  protected void addModificationListeners() {
    super.addModificationListeners();
    synchHelper.synchText(applicationIdText, GAE_PROPERTY_APP_ID, null);
  }

  @Override
  protected Composite createTopLevelComposite(Composite parent) {
    initializeDialogUnits(parent);
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
    }
    createDeployOptionsComponent(composite);
    addModificationListeners();
    return composite;
  }

  @Override
  protected String[] getValidationPropertyNames() {
    return new String[] {GAE_PROPERTY_APP_ID};
  }

  @Override
  protected void restoreDefaultSettings() {
    super.restoreDefaultSettings();
    synchHelper.synchAllUIWithModel();
  }

  private IDataModel getMasterDataModel() {
    IDataModel dataModel = getDataModel();
    return (IDataModel) dataModel.getProperty(FacetInstallDataModelProvider.MASTER_PROJECT_DM);
  }
}
