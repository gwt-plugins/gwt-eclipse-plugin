/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.maven;

import com.google.common.collect.ImmutableSet;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.DataModelImpl;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import java.util.Set;

/**
 * Provides a method for determining whether an GWT facet (for either war or ear
 * packaging) should be added to a given project, and if so, adding it.
 */
@SuppressWarnings("restriction")
public class GwtFacetManager {

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
    try {
      IProjectFacet facetOfInterest = getFacetForPackaging(pom.getPackaging());
      if (!facetedProject.hasProjectFacet(facetOfInterest)) {
        addFacetToProject(facetOfInterest, facetedProject, monitor);
      }
    } catch (EarlyExit e) {
      GwtMavenPlugin.logError("GwtFacetManager.addGwtFacet(): Error adding gwt facet. Exiting.", e);
      return;
    }
  }

  private static IProjectFacet getFacetForPackaging(String packaging) throws EarlyExit {
    String facetIdOfInterest;
    switch (packaging) {
    case "war":
      facetIdOfInterest = Constants.GAE_WAR_FACET_ID;
      break;
    default:
      GwtMavenPlugin.logError("GwtFacetManager.getFacetForPackaging(): Unexpected packaging \"" + packaging
          + "\" for a project using " + Constants.GWT_MAVEN_PLUGIN_ARTIFACT_ID + ". Not a War. Exiting.", null);
      throw new EarlyExit();
    }
    return ProjectFacetsManager.getProjectFacet(facetIdOfInterest);
  }

  private static void addFacetToProject(IProjectFacet facetOfInterest, IFacetedProject facetedProject,
      IProgressMonitor monitor) throws EarlyExit {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    // default facet version is 1.0 (facet version does not reflect sdk version)
    workingCopy.addProjectFacet(facetOfInterest.getDefaultVersion());

    markToUseMavenDependencies(facetOfInterest, workingCopy);

    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      String facetId = "";
      if (facetOfInterest != null) {
        facetId = facetOfInterest.getId();
      }
      GwtMavenPlugin.logError("GwtFacetManager.addFacetToProject() Error committing addition of (facetId=" + facetId
          + ") facet to project. Exiting.", e);
      throw new EarlyExit();
    }
  }

  /**
   * Sets a property that will be read by GwtFacetInstallDelegate to decide
   * whether or not to create a WTP classpath container with GAE SDK
   * dependencies. A property value of true indicates that we should not create
   * the WTP classpath container, because we will be using the Maven classpath
   * container.
   *
   * @param facet
   * @param workingCopy
   */
  private static void markToUseMavenDependencies(IProjectFacet facet, IFacetedProjectWorkingCopy workingCopy) {
    Object config = workingCopy.getProjectFacetAction(facet).getConfig();
    IDataModel model = (IDataModel) config;
    model.addNestedModel(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME + ".model", new DataModelImpl(
        new AbstractDataModelProvider() {
          @Override
          public Set<?> getPropertyNames() {
            return ImmutableSet.of(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME);
          }
        }));
    model.setBooleanProperty(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME, true);
    workingCopy.setProjectFacetActionConfig(facet, model);
  }

  @SuppressWarnings("serial")
  private static class EarlyExit extends Exception {
    public EarlyExit() {
      GwtMavenPlugin.logError("GwtFacetManager.EarlyExit(): Exittied gwt facet addition.", null);
    }
  }

}
