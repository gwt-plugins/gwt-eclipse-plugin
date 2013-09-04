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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jpt.common.ui.internal.utility.SynchronousUiCommandExecutor;
import org.eclipse.jpt.common.utility.command.Command;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.jpt.jpa.core.context.persistence.Persistence;
import org.eclipse.jpt.jpa.core.context.persistence.PersistenceUnit;
import org.eclipse.jpt.jpa.core.resource.xml.JpaXmlResource;

/**
 * Does the JPA class synchronization on the Eclipse 4.2 platform.
 *
 * The name and package of this class must match that declared in the Eclipse 4.3 version of the JPA
 * plugin.
 */
@SuppressWarnings("restriction")
public class SynchronizeClassesRunner implements ISynchronizeClassesRunner {
  @Override
  public void synchronizeClasses(final JpaProject jpaProject, final IProgressMonitor monitor) {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    JpaXmlResource resource = jpaProject.getPersistenceXmlResource();
    Persistence persistence = jpaProject.getRootContextNode().getPersistenceXml().getPersistence();
    if (persistence.getPersistenceUnitsSize() != 0) {
      final PersistenceUnit unit = persistence.getPersistenceUnits().iterator().next();
      try {
        JpaProjectManager jpaProjectManager = (JpaProjectManager) workspace.getAdapter(JpaProjectManager.class);
        jpaProjectManager.execute(new Command() {
          @Override
          public void execute() {
            unit.synchronizeClasses(monitor);
          }
        }, SynchronousUiCommandExecutor.instance());
      } catch (InterruptedException e) {
        // ignore
      }
      resource.save();
    }
  }
}
