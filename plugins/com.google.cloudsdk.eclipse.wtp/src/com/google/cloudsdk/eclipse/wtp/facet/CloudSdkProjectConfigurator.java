/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp.facet;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.cloudsdk.eclipse.wtp.CloudSdkUtils;

import org.apache.maven.model.Model;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.WTPProjectConfigurator;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.facets.FacetUtil;

/**
 * A {@link WTPProjectConfigurator} that adds the Cloud SDK facet to projects.
 */
@SuppressWarnings("restriction") // For WTPProjectConfigurator and FacetUtil
public class CloudSdkProjectConfigurator extends WTPProjectConfigurator {
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    Model pom = request.getMavenProject().getModel();
    if (CloudSdkUtils.hasGcloudMavenPlugin(pom)) {
      IFacetedProject facetedProject = ProjectFacetsManager.create(request.getProject());
      addCloudSdkFacet(facetedProject, monitor);
    }
  }

  /**
   * Adds the Cloud SDK facet to {@code facetedProject} and adds the available Cloud SDK runtimes
   * to the list of targeted runtimes for {@code facetedProject}.
   *
   * @param facetedProject the project
   * @param monitor to monitor the progress of the {@code facetedProject} update
   */
  public void addCloudSdkFacet(IFacetedProject facetedProject, IProgressMonitor monitor) {
    IProjectFacet facetOfInterest =
        ProjectFacetsManager.getProjectFacet(CloudSdkUtils.CLOUD_SDK_FACET_ID);
    if (facetedProject.hasProjectFacet(facetOfInterest)) {
      return;
    }

    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(facetOfInterest.getDefaultVersion());

    // Get all the available Cloud SDK runtime instances
    IRuntimeType runtimeType = ServerCore.findRuntimeType(CloudSdkUtils.CLOUD_SDK_RUNTIME_ID);
    if (runtimeType != null) {
      for (IRuntime aRuntime : ServerCore.getRuntimes()) {
        if (aRuntime.getRuntimeType().equals(runtimeType)) {
          workingCopy.addTargetedRuntime(FacetUtil.getRuntime(aRuntime));
        }
      }
    }

    try {
      workingCopy.commitChanges(monitor);
    } catch (CoreException e) {
      CloudSdkPlugin.logError("Error installing the Cloud SDK Facet in "
          + facetedProject.getProject().getName(), e);
    }
  }
}