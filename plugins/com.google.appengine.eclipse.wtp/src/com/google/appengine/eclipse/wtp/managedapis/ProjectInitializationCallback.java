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
package com.google.appengine.eclipse.wtp.managedapis;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.extensiontypes.IManagedApiProjectInitializationCallback;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

/**
 * Adds an observer to get notified about changes made to project with managed APIs.
 */
public final class ProjectInitializationCallback implements
    IManagedApiProjectInitializationCallback {
  @Override
  public void onInitialization(ManagedApiProject apiProject) {
    IProject project = apiProject.getProject();
    try {
      boolean isFaceted = FacetedProjectFramework.isFacetedProject(project);
      if (isFaceted
          && FacetedProjectFramework.hasProjectFacet(project, IGaeFacetConstants.GAE_FACET_ID)) {
        apiProject.registerManagedApiProjectObserver(new ManagedApiProjectObserverImpl(apiProject));
      }
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
  }
}
