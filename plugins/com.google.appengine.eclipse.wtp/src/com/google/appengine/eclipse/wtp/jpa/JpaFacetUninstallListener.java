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

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;

/**
 * Listener for removing JPA facet.
 */
public final class JpaFacetUninstallListener implements IFacetedProjectListener {

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    IProjectFacetActionEvent actionEvent = (IProjectFacetActionEvent) event;
    // handle JPA facet only
    if ("jpt.jpa".equals(actionEvent.getProjectFacet().getId())) {
      try {
        // remove enhancer
        BuilderUtilities.removeBuilderFromProject(actionEvent.getProject().getProject(),
            AppEnginePlugin.PLUGIN_ID + ".enhancerbuilder");
      } catch (CoreException e) {
        // TODO: actually don't need to log this.
        AppEnginePlugin.logMessage(e);
      }
    }
  }

}
