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

import com.google.appengine.eclipse.wtp.facet.GaeFacetInstallDataModelProvider;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.wizards.GaeFacetWizardPage;
import com.google.common.collect.Sets;
import com.google.gdt.eclipse.appengine.swarm.util.XmlUtil;
import com.google.gdt.eclipse.appengine.swarm_backend.impl.BackendGenerator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelWizard;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;

import java.util.Set;

/**
 * A Wizard for creating backends.
 */
@SuppressWarnings("restriction")
public final class BackendGeneratorWizard extends DataModelWizard {

  private IFacetedProjectWorkingCopy facetedProject;
  private String androidProjectPackageName;
  private GaeFacetWizardPage gaeFacetPage;

  public BackendGeneratorWizard() {
    setWindowTitle("Generate Backend Wizard");
    setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(AppEngineSwarmPlugin.getInstance().getBundle().getEntry(
        "/icons/app_engine_droid_64.png")));
    setNeedsProgressMonitor(true);
  }

  public void init(IProject androidProject, String newProjectName) {
    if (facetedProject != null) {
      return;
    }
    IProjectFacet f = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
    Set<IRuntime> runtimes = RuntimeManager.getRuntimes();
    Set<IRuntime> gaeRuntimes = Sets.newHashSet();
    // collect runtimes
    for (IRuntime runtime : runtimes) {
      if (runtime.supports(f)) {
        gaeRuntimes.add(runtime);
      }
    }
    facetedProject = FacetedProjectFramework.createNewProject();
    facetedProject.setTargetedRuntimes(gaeRuntimes);
    facetedProject.setProjectName(newProjectName);
    IDataModel dataModel = getDataModel();
    dataModel.setProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY, facetedProject);
    dataModel.setProperty(IFacetDataModelProperties.FACET_PROJECT_NAME, newProjectName);
    dataModel.setProperty(BackendGeneratorDataModelProvider.ANDROID_PROJECT_NAME,
        androidProject.getName());
    // android package
    try {
      androidProjectPackageName = new XmlUtil().findAndroidPackage(androidProject);
    } catch (Throwable e) {
      AppEngineSwarmPlugin.logMessage(e);
      throw new RuntimeException(e);
    }
    dataModel.setStringProperty(BackendGeneratorDataModelProvider.ANDROID_PACKAGE_NAME,
        androidProjectPackageName);

    dataModel.addListener(new IDataModelListener() {
      @Override
      public void propertyChanged(DataModelEvent event) {
        if (BackendGeneratorDataModelProvider.SELECTED_RUNTIME.equals(event.getPropertyName())) {
          IRuntime runtime = (IRuntime) event.getProperty();
          facetedProject.setPrimaryRuntime(runtime);
        }
      }
    });
  }

  @Override
  protected void doAddPages() {
    {
      BackendGeneratorWizardPage page = new BackendGeneratorWizardPage(getDataModel());
      addPage(page);
    }
    addGaeFacetPage();
  }

  @Override
  protected IDataModelProvider getDefaultProvider() {
    return new BackendGeneratorDataModelProvider();
  }

  /**
   * Prepares {@link IDataModel} for {@link GaeFacetWizardPage} and adds this page.
   */
  private void addGaeFacetPage() {
    gaeFacetPage = new GaeFacetWizardPage();
    // gaeFacetPage.setWizard(this);
    // create and fill datamodel
    IDataModel gaeFacetDataModel = DataModelFactory.createDataModel(new GaeFacetInstallDataModelProvider(
        false));
    gaeFacetDataModel.setProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY,
        facetedProject);
    gaeFacetDataModel.setBooleanProperty(IGaeFacetConstants.GAE_PROPERTY_CREATE_SAMPLE, false);
    gaeFacetDataModel.setStringProperty(
        IFacetProjectCreationDataModelProperties.FACET_PROJECT_NAME,
        facetedProject.getProjectName());
    // propose default app engine project package based on android project package
    String appEnginePackageName = BackendGenerator.getAndroidBackendProjectPackage(androidProjectPackageName);
    gaeFacetDataModel.setStringProperty(IGaeFacetConstants.GAE_PROPERTY_PACKAGE,
        appEnginePackageName);
    // done
    gaeFacetPage.setConfig(gaeFacetDataModel);
    getDataModel().setProperty(BackendGeneratorDataModelProvider.GAE_FACET_INSTALL_DM,
        gaeFacetDataModel);
    addPage(gaeFacetPage);
    // if (gaeFacetPage.getControl() == null) {
    // gaeFacetPage.createControl(pageContainer);
    // gaeFacetPage.getControl().setVisible(false);
    // }
    // final IWizardContainer wizardContainer = getContainer();
    // if (wizardContainer.getCurrentPage() != null) {
    // wizardContainer.updateButtons();
    // }
  }
}
