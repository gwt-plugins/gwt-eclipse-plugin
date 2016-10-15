/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.maven;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.wtp.facet.data.GwtFacetInstallDataModelProvider;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

/**
 * Provides a method for determining whether an GWT facet (for either war or ear packaging) should be added to a given
 * project, and if so, adding it.
 */
public class GwtMavenFacetManager {

  /**
   * Adds the GWT facet to a given {@code IFacetedProject}.
   *
   * @param pom
   *          the Maven model for the project
   * @param facetedProject
   *          the given project, expected to be an GWT project
   * @param monitor
   *          a progress monitor for the operation
   */
  public void addGwtFacet(Model pom, IFacetedProject facetedProject, IProgressMonitor monitor) {
    IProjectFacet gwtProjectFacet = null;
    try { // Look for something odd going on here. If it does exit with out an error. Like finding some
          // com.google.gwt.facet.
      gwtProjectFacet = ProjectFacetsManager.getProjectFacet(IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
    } catch (Exception e) {
      GwtMavenPlugin.logInfo(
          "GwtMavenFacetManager.addGwtFacet(): 1. Couldn't find facet: IGwtFacetConstants.GWT_PLUGINS_FACET_ID="
              + IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
      e.printStackTrace();
    }
    if (gwtProjectFacet == null) {
      GwtMavenPlugin.logInfo(
          "GwtMavenFacetManager.addGwtFacet(): 2. Couldn't find facet: IGwtFacetConstants.GWT_PLUGINS_FACET_ID="
              + IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
      return;
    }

    try {
      // If the facet is already installed, skip it.
      if (facetedProject != null && !facetedProject.hasProjectFacet(gwtProjectFacet)) {
        addFacetToProject(gwtProjectFacet, facetedProject, monitor);
      } else {
        GwtMavenPlugin
            .logInfo("GwtMavenFacetManager.addGwtFacet(): The GWT facet has already been installed. Exiting.");
      }
    } catch (EarlyExit e) {
      GwtMavenPlugin.logError("GwtMavenFacetManager.addGwtFacet(): Error adding gwt facet. Exiting.", e);
      return;
    }
  }

  /**
   * Add GWT facet to project.
   * 
   * Note: Default facet version is 1.0 (facet version does not reflect sdk version)
   * 
   * @throws EarlyExit
   */
  private void addFacetToProject(IProjectFacet facetOfInterest, IFacetedProject facetedProject,
      IProgressMonitor monitor) throws EarlyExit {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(facetOfInterest.getDefaultVersion());

    markToUseMavenDependencies(facetOfInterest, workingCopy);

    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      String facetId = "";
      if (facetOfInterest != null) {
        facetId = facetOfInterest.getId();
      }
      String message = "GwtMavenFacetManager.addFacetToProject() Error committing addition of (facetId=" + facetId
          + ") facet to project. Exiting.";
      GwtMavenPlugin.logError(message, e);
      throw new EarlyExit();
    }

    // Support the legacy GWT operations, actions...
    // TODO in the future depend on GWT facet entirely
    try {
      GWTNature.addNatureToProject(facetedProject.getProject());
    } catch (CoreException e) {
      GwtMavenPlugin.logError("GwtMavenFacetManager.addFacetToProject() Error setting GWT Nature.", e);
    }
  }

  /**
   * Sets a property that will be read by GwtFacetInstallDelegate to decide whether or not to create a WTP classpath
   * container with GAE SDK dependencies. A property value of true indicates that we should not create the WTP classpath
   * container, because we will be using the Maven classpath container.
   *
   * @param facet
   * @param workingCopy
   */
  private void markToUseMavenDependencies(IProjectFacet facet, IFacetedProjectWorkingCopy workingCopy) {
    IDataModel model = (IDataModel) workingCopy.getProjectFacetAction(facet).getConfig();
    GwtFacetInstallDataModelProvider.setMavenProject(model, true);
    workingCopy.setProjectFacetActionConfig(facet, model);
  }

  @SuppressWarnings("serial")
  private static class EarlyExit extends Exception {
    public EarlyExit() {
      GwtMavenPlugin.logError("GwtMavenFacetManager.EarlyExit(): Exittied gwt facet addition.", null);
    }
  }

}
