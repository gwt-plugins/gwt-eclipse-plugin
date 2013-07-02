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

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.properties.ui.DeployOptionsComponent;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizardPage;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IWizardContext;

/**
 * Base class for GAE facets wizard pages.
 */
@SuppressWarnings("restriction")
abstract class GaeFacetAbstractWizardPage extends DataModelWizardPage implements IFacetWizardPage,
    IGaeFacetConstants {
  private DeployOptionsComponent deployOptionsComponent = new DeployOptionsComponent();

  public GaeFacetAbstractWizardPage(String wizardPageName) {
    super(DataModelFactory.createDataModel(new AbstractDataModelProvider() {
      // fake one, to make super constructor happy, real model will be set in setConfig()
    }), wizardPageName);
    setTitle("Google App Engine");
    setDescription("Configure Google App Engine");
    setImageDescriptor(AppEngineCorePlugin.getDefault().getImageDescriptor(
        GaeImages.APP_ENGINE_DEPLOY_LARGE));
  }

  @Override
  public void setConfig(Object config) {
    model.removeListener(this);
    synchHelper.dispose();

    model = (IDataModel) config;
    model.addListener(this);
    synchHelper = initializeSynchHelper(model);
  }

  @Override
  public void setWizardContext(IWizardContext context) {
    // do nothing here
  }

  @Override
  public void transferStateToConfig() {
    // do nothing here
  }

  protected void addModificationListeners() {
    synchHelper.synchCheckbox(deployOptionsComponent.getEnableJarSplittingButton(),
        GAE_PROPERTY_ENABLE_JAR_SPLITTING, null);
    synchHelper.synchCheckbox(deployOptionsComponent.getDoJarClassesButton(),
        GAE_PROPERTY_DO_JAR_CLASSES, null);
    synchHelper.synchCheckbox(deployOptionsComponent.getRetainDirectoryButton(),
        GAE_PROPERTY_RETAIN_STAGING_DIR, null);
  }

  protected void createDeployOptionsComponent(Composite composite) {
    deployOptionsComponent.createContents(composite);
  }

  @Override
  protected void restoreDefaultSettings() {
    super.restoreDefaultSettings();
    IProject project = ProjectUtils.getProject(model);
    if (project != null) {
      model.setBooleanProperty(GAE_PROPERTY_DO_JAR_CLASSES,
          GaeProjectProperties.getGaeDoJarClasses(project));
      model.setBooleanProperty(GAE_PROPERTY_ENABLE_JAR_SPLITTING,
          GaeProjectProperties.getGaeEnableJarSplitting(project));
      model.setBooleanProperty(GAE_PROPERTY_RETAIN_STAGING_DIR,
          GaeProjectProperties.getGaeRetainStagingDir(project));
    }
  }

  @Override
  protected boolean showValidationErrorsOnEnter() {
    return true;
  }
}