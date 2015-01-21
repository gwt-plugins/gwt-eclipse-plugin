/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.jpa;

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
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
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;
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
public class JpaFacetPostInstallListener implements IFacetedProjectListener {

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
                  AppEnginePlugin.PLUGIN_ID + ".enhancerbuilder");
            } catch (CoreException e) {
              AppEngineJpaPlugin.logMessage(e);
            }
          }
        }
      }
    }
  }

  private JpaProjectChangeListener createJpaListener() {
    return new JpaProjectChangeListener() {
      @Override
      protected void initializePersistenceUnit(Iterable<?> items) {
        // since it is 'itemsAdded' then 'items' should always has next element.
        for (Object object : items) {
          if (!(object instanceof JpaProject)) {
            continue;
          }
          final JpaProject jpaProject = (JpaProject) object;
          if (!isUsingGaeLibProv(jpaProject.getProject())) {
            continue;
          }
          Persistence persistence = null;
          try {
            persistence = JpaFacetHelper.getPersistence(jpaProject);
          } catch (CoreException e) {
            AppEngineJpaPlugin.logMessage("Failed to update JPA persistence for Appengine.", e);
          }
          PersistenceUnit unit;
          if (persistence.getPersistenceUnitsSize() != 0) {
            unit = persistence.getPersistenceUnits().iterator().next();
          } else {
            // create a persistence unit if there isn't one
            unit = persistence.addPersistenceUnit();
          }
          unit.setName("transactions-optional");
          // initial setup for properties
          if (unit.getProvider() == null) {
            unit.setProvider(PERSISTENCE_PROVIDER);
          }
          if (unit.getProperty(PROP_NONTRANSACTIONAL_READ) == null) {
            unit.setProperty(PROP_NONTRANSACTIONAL_READ, "true");
          }
          if (unit.getProperty(PROP_NONTRANSACTIONAL_WRITE) == null) {
            unit.setProperty(PROP_NONTRANSACTIONAL_WRITE, "true");
          }
          if (unit.getProperty(PROP_CONNECTION_URL) == null) {
            unit.setProperty(PROP_CONNECTION_URL, "appengine");
          }
          // save
          jpaProject.getPersistenceXmlResource().save();
        }
      }
    };
  }
}
