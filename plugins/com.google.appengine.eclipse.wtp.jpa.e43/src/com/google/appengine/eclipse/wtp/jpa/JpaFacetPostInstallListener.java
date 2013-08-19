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

import org.eclipse.jpt.common.core.resource.xml.JptXmlResource;
import org.eclipse.jpt.common.utility.model.listener.CollectionChangeListener;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceXml;

/**
 * Listener for JPA facet post-install action. Install {@link CollectionChangeListener} hooking for
 * {@link JpaProject} creation/adding.
 */
public final class JpaFacetPostInstallListener extends JpaFacetAbstractPostInstallListener {
  @Override
  protected JpaProjectChangeListener createJpaListener() {
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
          JptXmlResource resource = jpaProject.getPersistenceXmlResource();
          PersistenceXml persistenceXml = jpaProject.getContextModelRoot().getPersistenceXml();
          Persistence persistence = persistenceXml.getRoot();
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
          resource.save();
        }
      }
    };
  }
}
