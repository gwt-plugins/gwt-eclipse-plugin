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

import org.eclipse.jpt.common.utility.model.event.CollectionAddEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionChangeEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionClearEvent;
import org.eclipse.jpt.common.utility.model.event.CollectionRemoveEvent;
import org.eclipse.jpt.common.utility.model.listener.CollectionChangeListener;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.jpt.jpa.core.JptJpaCorePlugin;

// We want to listen to creation of new JpaProjects
// Due to code-reorganization in the WTP plugins used for Eclipse 3.7 vs
// Eclipse 3.6 vs Eclipse 3.5, the App Engine WTP plugin needed to be split
// into 3.7, 3.6, and 3.5 versions.
// Whenever you modify this class, please make corresponding changes to the
// 3.5 and 3.6 classes.
public class JpaProjectsListener implements CollectionChangeListener {


  // The shared instance
  private static JpaProjectsListener listener;

  public static void start() {
    if (listener == null) {
      listener = new JpaProjectsListener();
      JptJpaCorePlugin.getJpaProjectManager().addCollectionChangeListener(
          JpaProjectManager.JPA_PROJECTS_COLLECTION, listener);
    }
  }

  public static void stop() {
    JptJpaCorePlugin.getJpaProjectManager().removeCollectionChangeListener(
        JpaProjectManager.JPA_PROJECTS_COLLECTION, listener);
    listener = null;
  }

  private JpaProjectsListener() {
  }

  public void collectionChanged(CollectionChangeEvent event) {
  }

  public void collectionCleared(CollectionClearEvent event) {
  }

  public void itemsAdded(CollectionAddEvent event) {
    for (Object o : event.getItems()) {
      final JpaProject jpaProject = (JpaProject) o;
      if (GaeNature.isGaeProject(jpaProject.getProject())) {
        JpaFacetHelper.jobDisableDataNucleus(jpaProject.getJavaProject());
        JpaFacetHelper.jobUpdatePersistenceAndWebInf(
            jpaProject.getJavaProject(), new JpaFacetHelper.Updater());
      }
    }
  }

  public void itemsRemoved(CollectionRemoveEvent event) {
    // TODO (raksit) delete jpa library from WEB-INF/lib
  }
}
