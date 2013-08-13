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

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetDataModelProperties;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import java.util.Collection;

/**
 * A {@link IDataModelOperation} for backend generation.
 */
@SuppressWarnings("restriction")
public final class BackendGeneratorDataModelOperation extends AbstractDataModelOperation {
  private static final String GAE_LIBRARY_PROVIDER_ID = "com.google.appengine.eclipse.wtp.jpa.GaeLibraryProvider";
  private static final String FACET_JPT_JPA_ID = "jpt.jpa";
  private static final String FACET_JST_JAVA_ID = "jst.java";
  private static final String FACET_JST_WEB_ID = "jst.web";

  public BackendGeneratorDataModelOperation(IDataModel model) {
    super(model);
  }

  @Override
  public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
    try {
      IDataModel dataModel = getDataModel();
      IFacetedProjectWorkingCopy facetedProject = (IFacetedProjectWorkingCopy) dataModel.getProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
      IRuntime runtime = (IRuntime) dataModel.getProperty(BackendGeneratorDataModelProvider.GAE_BACKEND_SELECTED_RUNTIME);
      facetedProject.setPrimaryRuntime(runtime);
      // add Java facet
      IProjectFacet javaFacet = ProjectFacetsManager.getProjectFacet(FACET_JST_JAVA_ID);
      facetedProject.addProjectFacet(javaFacet.getDefaultVersion());
      // add Web facet
      IProjectFacet webFacet = ProjectFacetsManager.getProjectFacet(FACET_JST_WEB_ID);
      IProjectFacetVersion version = webFacet.getLatestSupportedVersion(runtime);
      facetedProject.addProjectFacet(version);
      // add JPA facet
      IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet(FACET_JPT_JPA_ID);
      if (jpaFacet != null) {
        IProjectFacetVersion jpaFacetVersion = jpaFacet.getVersion("2.0");
        if (jpaFacetVersion != null) {
          facetedProject.addProjectFacet(jpaFacetVersion);
          Action jpaFacetAction = facetedProject.getProjectFacetAction(jpaFacet);
          IDataModel jpaDataModel = (IDataModel) jpaFacetAction.getConfig();
          configureJpaFacet(jpaDataModel);
        } else {
          return StatusUtilities.newErrorStatus("Required JPA facet 2.0 is not found",
              AppEngineSwarmPlugin.PLUGIN_ID);
        }
      } else {
        return StatusUtilities.newErrorStatus("Required JPA facet is missing",
            AppEngineSwarmPlugin.PLUGIN_ID);
      }
      // add App Engine facet
      IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
      facetedProject.addProjectFacet(gaeFacet.getDefaultVersion());
      Action gaeFacetAction = facetedProject.getProjectFacetAction(gaeFacet);
      IDataModel gaeCurrentDataModel = (IDataModel) gaeFacetAction.getConfig();
      IDataModel gaeConfiguredDataModel = (IDataModel) dataModel.getProperty(BackendGeneratorDataModelProvider.GAE_FACET_INSTALL_DM);
      configureGaeFacet(gaeCurrentDataModel, gaeConfiguredDataModel);
      // commit
      facetedProject.commitChanges(monitor);
      // XXX
      return Status.OK_STATUS;
    } catch (CoreException e) {
      return e.getStatus();
    }
  }

  /**
   *
   */
  private void configureGaeFacet(IDataModel targetDataModel, IDataModel sourceDataModel) {
    @SuppressWarnings("unchecked")
    Collection<String> properties = sourceDataModel.getBaseProperties();
    for (String property : properties) {
      if (sourceDataModel.isPropertySet(property)) {
        Object value = sourceDataModel.getProperty(property);
        targetDataModel.setProperty(property, value);
      }
    }
  }

  /**
   *
   */
  private void configureJpaFacet(IDataModel jpaDataModel) {
    Object runtime = jpaDataModel.getProperty(JpaFacetDataModelProperties.RUNTIME);
    if (runtime == null) {
      runtime = getDataModel().getProperty(
          BackendGeneratorDataModelProvider.GAE_BACKEND_SELECTED_RUNTIME);
      jpaDataModel.setProperty(JpaFacetDataModelProperties.RUNTIME, runtime);
    }

    ILibraryProvider provider = LibraryProviderFramework.getProvider(GAE_LIBRARY_PROVIDER_ID);
    LibraryInstallDelegate delegate = (LibraryInstallDelegate) jpaDataModel.getProperty(JpaFacetDataModelProperties.LIBRARY_PROVIDER_DELEGATE);
    delegate.setLibraryProvider(provider);
  }
}