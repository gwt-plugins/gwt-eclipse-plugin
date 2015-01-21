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
package com.google.appengine.eclipse.wtp.swarm;

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent.Type;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;

/**
 * Listener for GAE facet install/uninstall to add/remove Cloud Endpoint builder.
 */
public class EndpointsFacetedProjectListener implements IFacetedProjectListener {

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    IProjectFacetActionEvent pfaEvent = (IProjectFacetActionEvent) event;
    IFacetedProject facetedProject = pfaEvent.getProject();
    IProjectFacet eventFacet = pfaEvent.getProjectFacet();
    IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
    if (eventFacet.equals(gaeFacet)) {
      try {
        IProject project = facetedProject.getProject();
        if (ProjectUtils.isGaeProject(project)) {
          Type type = event.getType();
          if (Type.POST_INSTALL.equals(type)) {
            if (CloudEndpointsUtils.isEndpointsSupported(project)) {
              BuilderUtilities.addBuilderToProject(project, CloudEndpointsBuilder.ID);
            }
          } else if (Type.PRE_UNINSTALL.equals(type)) {
            BuilderUtilities.removeBuilderFromProject(project, CloudEndpointsBuilder.ID);
          }
        }
      } catch (CoreException e) {
        // do nothing
      }
    }
  }
}
