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
package com.google.appengine.eclipse.wtp.maven;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

public class JpaFacetManager {
  
  public void addJpaFacet(IFacetedProject facetedProject, IProgressMonitor monitor) {
    IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet(Constants.JPA_FACET_ID);
    if (!facetedProject.hasProjectFacet(jpaFacet)) {
      try {
        IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
        workingCopy.addProjectFacet(jpaFacet.getDefaultVersion());
        workingCopy.commitChanges(monitor);
      } catch (CoreException e) {
        AppEngineMavenPlugin.logError("Error committing addition of JPA facet to project", e);
      }
    }
  }

}
