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
package com.google.appengine.eclipse.wtp.jpa;

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.jpa.libprov.IGaeLibraryProvider;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jpt.common.utility.model.event.CollectionAddEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionChangeEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionClearEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionRemoveEvent;
import org.eclipse.jpt.common.utility.model.listener.CollectionChangeListener;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetDataModelProperties;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

/**
 * Listener for JPA facet post-install action. Install {@link CollectionChangeListener} hooking for
 * {@link JpaProject} creation/adding.
 */
@SuppressWarnings("restriction")
public abstract class JpaFacetAbstractPostInstallListener implements IFacetedProjectListener {

  protected static abstract class JpaProjectChangeListener implements CollectionChangeListener,
      IDatanucleusConstants {
    @Override
    public void collectionChanged(CollectionChangeEvent event) {
    }

    @Override
    public void collectionCleared(CollectionClearEvent event) {
    }

    @Override
    public void itemsAdded(CollectionAddEvent event) {
      Iterable<?> items = event.getItems();
      initializePersistenceUnit(items);
      // done, remove this listener
      JpaProjectManager manager = (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(
          JpaProjectManager.class);
      manager.removeCollectionChangeListener(JpaProjectManager.JPA_PROJECTS_COLLECTION, this);
    }

    @Override
    public void itemsRemoved(CollectionRemoveEvent event) {
    }

    protected abstract void initializePersistenceUnit(Iterable<?> items);
  }

  /**
   * @return <code>true</code> if the project is configured for using GAE library provider.
   */
  public static boolean isUsingGaeLibProv(IProject project) {
    if (project == null) {
      return false;
    }
    IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet("jpt.jpa");
    ILibraryProvider provider = LibraryProviderFramework.getCurrentProvider(project, jpaFacet);
    return provider != null && IGaeLibraryProvider.PROVIDER_ID.equals(provider.getId());
  }

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    IProjectFacetActionEvent installEvent = (IProjectFacetActionEvent) event;
    // handle JPA facet only
    if ("jpt.jpa".equals(installEvent.getProjectFacet().getId())) {
      IDataModel dataModel = (IDataModel) installEvent.getActionConfig();
      IRuntime runtime = (IRuntime) dataModel.getProperty(JpaFacetDataModelProperties.RUNTIME);
      IFacetedProjectWorkingCopy facetedProject = (IFacetedProjectWorkingCopy) dataModel.getProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
      IProject project = facetedProject.getProject();
      if (runtime != null) {
        IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
        // is this GAE runtime?
        if (runtime.supports(gaeFacet)) {
          // additionally check that GAE libprov is used
          if (isUsingGaeLibProv(project)) {
            JpaProjectManager manager = (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(
                JpaProjectManager.class);
            // since JpaProject is not available yet, add listener which will later setup
            // Datanucleus stuff.
            manager.addCollectionChangeListener(JpaProjectManager.JPA_PROJECTS_COLLECTION,
                createJpaListener());
            try {
              // setup enhancer builder
              BuilderUtilities.addBuilderToProject(installEvent.getProject().getProject(),
                  AppEngineJpaPlugin.PLUGIN_ID + ".enhancerbuilder");
            } catch (CoreException e) {
              AppEngineJpaPlugin.logMessage(e);
            }
          }
        }
      }
    }
  }

  protected abstract JpaProjectChangeListener createJpaListener();
}
