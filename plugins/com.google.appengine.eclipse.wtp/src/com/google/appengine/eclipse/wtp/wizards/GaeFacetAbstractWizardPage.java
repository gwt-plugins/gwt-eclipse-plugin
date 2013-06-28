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
import com.google.appengine.eclipse.core.resources.GaeImages;

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
abstract class GaeFacetAbstractWizardPage extends DataModelWizardPage implements
    IFacetWizardPage {

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

  @Override
  protected boolean showValidationErrorsOnEnter() {
    return true;
  }
}