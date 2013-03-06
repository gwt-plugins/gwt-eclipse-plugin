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

import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;

public abstract class AbstractFacetInstallListener
    implements IFacetedProjectListener {

  private static final String FACET_JPT_JPA = "jpt.jpa";

  public void handleEvent(IFacetedProjectEvent event) {
    IProjectFacetActionEvent installEvent = (IProjectFacetActionEvent) event;

    // On installation of JPA facet, we want to auto-populate persistence.xml,
    // etc.. But we can do that only after a JpaProject associated with this
    // project has been created, and that hasn't happened yet. We will use this
    // event to start a listener that listens to creation of a JpaProject, and
    // the actual work of persistence.xml population, copying over jpa library
    // to WEB-INF/lib, etc will be done there.
    if (installEvent.getProjectFacet().getId().equals(FACET_JPT_JPA)
        && GaeNature.isGaeProject(event.getProject().getProject())) {
      startJpaListener();
    }
  }

  protected abstract void startJpaListener();
}
