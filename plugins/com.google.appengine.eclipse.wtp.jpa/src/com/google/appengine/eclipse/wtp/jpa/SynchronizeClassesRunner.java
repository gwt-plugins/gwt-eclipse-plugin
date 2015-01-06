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

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jpt.common.utility.command.Command;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;

/**
 * Invokes the JPA class synchronization (with persistence.xml) action.
 */
public class SynchronizeClassesRunner {
  public void synchronizeClasses(JpaProject jpaProject, final IProgressMonitor monitor)
      throws CoreException {
    Persistence persistence = JpaFacetHelper.getPersistence(jpaProject);
    if (persistence.getPersistenceUnitsSize() != 0) {
      final PersistenceUnit unit = persistence.getPersistenceUnits().iterator().next();
      JpaFacetHelper.executeProjectManagerCommand(new Command() {
        @Override
        public void execute() {
          unit.synchronizeClasses(monitor);
        }
      });
      jpaProject.getPersistenceXmlResource().save();
    }
  }
}
