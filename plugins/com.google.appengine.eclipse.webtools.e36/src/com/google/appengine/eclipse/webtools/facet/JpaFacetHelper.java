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
package com.google.appengine.eclipse.webtools.facet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jpt.core.JpaProject;
import org.eclipse.jpt.core.context.persistence.Persistence;
import org.eclipse.jpt.core.context.persistence.PersistenceUnit;
import org.eclipse.jpt.core.context.persistence.PersistenceUnitTransactionType;
import org.eclipse.jpt.core.resource.xml.JpaXmlResource;
import org.eclipse.jpt.db.ConnectionProfile;

import java.io.IOException;
import java.util.Collections;

// This helper is used by the JPA projects listener.
// Due to code-reorganization in the WTP plugins used for Eclipse 3.7 vs
// Eclipse 3.6 vs Eclipse 3.5, the App Engine WTP plugin needed to be split
// into 3.7, 3.6, and 3.5 versions.
// Whenever you modify this class, please make corresponding changes to the
// 3.5 and 3.7 classes.
public class JpaFacetHelper extends AbstractJpaFacetHelper {

  public static class Updater implements PersistenceXmlUpdater {
    public void updateConnection(IJavaProject javaProject) throws IOException {
      JpaProject jpaProject = (JpaProject) javaProject.getProject()
          .getAdapter(JpaProject.class);
      if (jpaProject == null) {
        return;
      }
      ConnectionProfile conn = jpaProject.getConnectionProfile();
      if (conn == null) {
        // Nothing to do if there is no associated connection
        return;
      }
      JpaXmlResource resource = jpaProject.getPersistenceXmlResource();
      Persistence persistence = jpaProject.getRootContextNode()
        .getPersistenceXml().getPersistence();
      PersistenceUnit pUnit;
      // Create a persistence unit if there isn't one
      if (persistence.persistenceUnitsSize() == 0) {
        pUnit = persistence.addPersistenceUnit();
        pUnit.setName(jpaProject.getName());
      } else {
        // Only one persistence unit
        pUnit = persistence.persistenceUnits().next();
      }

      // Use default persistence provider (This might have earlier been set
      // some DataNucleus value).
      if (pUnit.getProvider() != null) {
        pUnit.setProvider("");
      }
      pUnit.setSpecifiedTransactionType(
          PersistenceUnitTransactionType.RESOURCE_LOCAL);
      if (conn.getDriverClassName() != null) {
        pUnit.setProperty(JDBC_DRIVER, getFixedDriverClassName(
        conn.getDriverClassName()));
      }
      if (conn.getURL() != null) {
        pUnit.setProperty(JDBC_URL, getFixedUrl(conn.getURL()));
      }
      if (conn.getUserName() != null) {
        pUnit.setProperty(JDBC_USER, conn.getUserName());
      }
      if (conn.getUserPassword() != null) {
        pUnit.setProperty(JDBC_PASSWORD, conn.getUserPassword());
      }

      resource.save(Collections.EMPTY_MAP);
    }
  }

  // Check if all the optional dependencies required for the JPA
  // functionality are available.
  public static boolean areJpaDepsAvailable() {
    return (Platform.getBundle("org.eclipse.jpt.core") != null
        && Platform.getBundle("org.eclipse.jpt.db") != null
        && Platform.getBundle("org.eclipse.jpt.utility") != null
        && Platform.getBundle("org.eclipse.wst.common.emf") != null);
  }

  private JpaFacetHelper() {
    // Not instantiable
  }
}
