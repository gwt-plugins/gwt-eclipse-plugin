/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.webtools.facet.listeners;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jpt.common.utility.model.event.CollectionAddEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionChangeEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionClearEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionRemoveEvent;
import org.eclipse.jpt.common.utility.model.listener.CollectionChangeListener;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;

/**
 * A listener that responds to the addition of JPA (Java Persistence API) project facet to a
 * Google AppEngine projects by reconfiguring it to use the JPA.
 */
public class JpaProjectsListener implements CollectionChangeListener {
  // The shared instance
  private static JpaProjectsListener listener;

  public static void start() {
    if (listener == null) {
      listener = new JpaProjectsListener();
      JpaProjectManager manager =
          (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(JpaProjectManager.class);
      manager.addCollectionChangeListener(JpaProjectManager.JPA_PROJECTS_COLLECTION, listener);
    }
  }

  public static void stop() {
    JpaProjectManager manager =
        (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(JpaProjectManager.class);
    manager.removeCollectionChangeListener(JpaProjectManager.JPA_PROJECTS_COLLECTION, listener);
    listener = null;
  }

  private JpaProjectsListener() {}

  @Override
  public void collectionChanged(CollectionChangeEvent event) {}

  @Override
  public void collectionCleared(CollectionClearEvent event) {}

  // TODO(tparker): Check if this should be enhanced/removed: b/17625300
  @Override
  public void itemsAdded(CollectionAddEvent event) {
    for (Object o : event.getItems()) {
      JpaProject jpaProject = (JpaProject) o;
      if (GaeNature.isGaeProject(jpaProject.getProject())) {
        JpaFacetHelper.jobDisableDataNucleus(jpaProject.getJavaProject());
        JpaFacetHelper.jobUpdatePersistenceAndWebInf(jpaProject.getJavaProject());
      }
    }
  }

  @Override
  public void itemsRemoved(CollectionRemoveEvent event) {
    // TODO (raksit) delete jpa library from WEB-INF/lib
  }
}
