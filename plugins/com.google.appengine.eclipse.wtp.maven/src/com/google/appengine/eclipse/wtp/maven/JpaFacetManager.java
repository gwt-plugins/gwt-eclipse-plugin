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

import com.google.appengine.eclipse.webtools.facet.JpaFacetHelper;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Provides a method to configure a faceted project's JPA facet for App Engine.
 */
public class JpaFacetManager {

  private static final String APP_ENGINE_JPA_PLATFORM_ID =
      "com.google.appengine.eclipse.wtp.jpa.GaePlatform";
      // Defined in the org.eclipse.jpt.jpa.core.jpaPlatforms extension point in
      // com.google.appengine.eclipse.wtp.jpa.e43/plugin.xml

  /**
   * Configures the JPA facet of a specified faceted project to use the implementation of JPA
   * bundled with App Engine, by ensuring that the latest JPA project facet is installed, setting
   * the JPA platform in project preferences to App Engine, and adding the builder for the
   * App Engine Datanucleus enhancer to the project.
   *
   * @param facetedProject the specified faceted project
   * @param monitor a progress monitor for the operation
   * @throws CoreException if there is an error adding the builder
   */
  public void configureJpaFacet(
      IFacetedProject facetedProject, IProgressMonitor monitor) throws CoreException {
    IProjectFacetVersion installedVersion =
        ensureLatestJpaFacetVersionInstalled(facetedProject, monitor);
    if (installedVersion != null) {
      JpaFacetHelper.setJpaPlatformId(facetedProject.getProject(), APP_ENGINE_JPA_PLATFORM_ID);
      BuilderUtilities.addBuilderToProject(
          facetedProject.getProject(), AppEnginePlugin.PLUGIN_ID + ".enhancerbuilder");
    }
  }

  /**
   * Ensure that the latest available JPA facet version is installed in a specified faceted project.
   * If no JPA facet is installed, the latest JPA facet version is installed. If an earlier JPA
   * facet version is installed, it is upgraded to the latest version.
   *
   * @param facetedProject the specified faceted project
   * @param monitor a progress monitor for the operation
   * @return
   *     the installed version, or {@code null} in the event (which is supposed to be impossible)
   *     that no JPA facet version can be found
   */
  private IProjectFacetVersion ensureLatestJpaFacetVersionInstalled(
      IFacetedProject facetedProject, IProgressMonitor monitor) {
    IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet(Constants.JPA_FACET_ID);
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    IProjectFacetVersion latestVersion = workingCopy.getHighestAvailableVersion(jpaFacet);
    if (latestVersion == null) {
      // This shouldn't happen.
      AppEngineMavenPlugin.logError(
          "Unable to find an available version of the JPA facet", null);
      return null;
    }
    // Although it's not documented, the source code of the class implementing IFacetedProject
    // (org.eclipse.wst.common.project.facet.core.internal.FacetedProject) indicates that when
    // facetedProject.hasProjectFacet(jpaFacet) returns true, a nonnull result is returned by
    // facetedProject.getInstalledVersion(jpaFacet). Thus, whenever the else-if test below is
    // reached, the first argument to sameVersion will be nonnull.
    if (!facetedProject.hasProjectFacet(jpaFacet)) {
      workingCopy.addProjectFacet(latestVersion);
    } else if (!sameVersion(facetedProject.getInstalledVersion(jpaFacet), latestVersion)) {
      workingCopy.changeProjectFacetVersion(latestVersion);
    }
    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      AppEngineMavenPlugin.logError("Error committing changes to faceted project", e);
    }
    return latestVersion;
  }

  // This method is needed because IProjectFacetVersion.equals uses default (reference) equality.
  private static final boolean sameVersion(IProjectFacetVersion v1, IProjectFacetVersion v2) {
    return v1.getVersionString().equals(v2.getVersionString());
  }
}
