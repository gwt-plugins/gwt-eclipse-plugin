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

import java.util.Set;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.internal.datamodel.DataModelImpl;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action.Type;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.common.collect.ImmutableSet;

/**
 * Provides a method for determining whether an AppEngine facet (for either war or ear packaging)
 * should be added to a given project, and if so, adding it.
 */
@SuppressWarnings("restriction") // DataModelImpl
public class GaeFacetManager {
   
  /**
   * Adds the appropriate AppEngine facet (WAR or EAR) to a given {@code IFacetedProject} if the
   * project does not already have that facet. If the project has WAR packaging, and does not
   * already have the facet {@link Constants.GAE_WAR_FACET_ID}, that facet is added; otherwise, if
   * the project has EAR packaging, and does not already have the facet
   * {@link Constants.GAE_EAR_FACET_ID}, that facet is added.
   * 
   * @param pom the Maven model for the project
   * @param facetedProject the given project, expected to be an App Engine project
   * @param monitor a progress monitor for the operation
   */
  public void addGaeFacet(Model pom, IFacetedProject facetedProject, IProgressMonitor monitor) {
    GaeRuntime runtime;
    try {
      GaeSdk sdkFromRepository = new GaeSdkInstaller().installGaeSdkIfNeeded(pom);
      runtime = GaeRuntimeManager.ensureGaeRuntimeWithSdk(sdkFromRepository, monitor);
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError("Error ensuring that correct GAE SDK is installed", e);
      return;
    }
    try {
      IProjectFacet facetOfInterest = getFacetForPackaging(pom.getPackaging());
      if (!facetedProject.hasProjectFacet(facetOfInterest)) {
        addFacetToProject(facetOfInterest, facetedProject, runtime, monitor);
      }
    } catch (EarlyExit e) {
      return;
    }
  }

  private static IProjectFacet getFacetForPackaging(String packaging) throws EarlyExit {
    String facetIdOfInterest;
    switch (packaging) {
      case "war":
        facetIdOfInterest = Constants.GAE_WAR_FACET_ID;
        break;
      case "ear":
        facetIdOfInterest = Constants.GAE_EAR_FACET_ID;
        break;
      default:
        AppEngineMavenPlugin.logError(
            "Unexpected packaging \"" + packaging + "\" for a project using "
                + Constants.APPENGINE_MAVEN_PLUGIN_ARTIFACT_ID,
            null);
        throw new EarlyExit();
    }
    return ProjectFacetsManager.getProjectFacet(facetIdOfInterest);
  }

  // See https://code.google.com/p/appengine-maven-plugin/source/browse/src/main/java/com/google/appengine/SdkResolver.java
  private static void addFacetToProject(
      IProjectFacet facetOfInterest, IFacetedProject facetedProject, GaeRuntime gaeRuntime,
      IProgressMonitor monitor)
      throws EarlyExit {
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(facetOfInterest.getDefaultVersion());
    workingCopy.addTargetedRuntime(FacetUtil.getRuntime(gaeRuntime.getRuntime()));
    // GaeRuntime.getRuntime returns a value of type org.eclipse.wst.server.core.IRuntime.
    // IFacetedProjectWorkingCopy.addTargetedRuntime takes an argument of a different type,
    // org.eclipse.wst.common.project.facet.core.runtime.IRuntime. FacetUtil.getRuntime converts
    // between the two IRuntime types.
    markToUseMavenDependencies(facetOfInterest, workingCopy);
    suppressSampleAppGeneration(workingCopy);
    
    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError(
          "Error committing addition of " + facetOfInterest.getId() + " facet to project", e);
      throw new EarlyExit();
    }
  }
  
  // Sets a property that will be read by GaeFacetInstallDelegate to decide whether or not to
  // create a WTP classpath container with GAE SDK dependencies. A property value of true indicates
  // that we should not create the WTP classpath container, because we will be using the Maven
  // classpath container.
  private static void markToUseMavenDependencies(
      IProjectFacet facet, IFacetedProjectWorkingCopy workingCopy) {
    Object config = workingCopy.getProjectFacetAction(facet).getConfig();
    IDataModel model = (IDataModel) config;
    model.addNestedModel(
        AppEnginePlugin.USE_MAVEN_DEPS_PROPERTY_NAME + ".model",
        new DataModelImpl(
            new AbstractDataModelProvider(){
              @Override public Set<?> getPropertyNames() {
                return ImmutableSet.of(AppEnginePlugin.USE_MAVEN_DEPS_PROPERTY_NAME);
              }
            }));
    model.setBooleanProperty(AppEnginePlugin.USE_MAVEN_DEPS_PROPERTY_NAME, true);
    workingCopy.setProjectFacetActionConfig(facet, model);
  }
  
  private static void suppressSampleAppGeneration(IFacetedProjectWorkingCopy fpwc) {
    for (Action action : ((IFacetedProjectWorkingCopy) fpwc).getProjectFacetActions()) {
      if (action.getType().equals(Type.INSTALL)) {
        IDataModel dm = ((IDataModel) action.getConfig());
        dm.setBooleanProperty(IGaeFacetConstants.GAE_PROPERTY_CREATE_SAMPLE, false);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class EarlyExit extends Exception {
    public EarlyExit() { }
  }

}
