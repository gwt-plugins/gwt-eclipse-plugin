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
package com.google.appengine.eclipse.wtp.maven;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.WTPProjectConfigurator;

/**
 *
 */
@SuppressWarnings("restriction") // WTPProjectConfigurator
public class GaeProjectConfigurator extends WTPProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    AppEngineMavenPlugin.logInfo(
        "GaeProjectConfigurator.configure invoked with the following request: " + request);
    GaeFacetManager gaeFacetManager = new GaeFacetManager();
    gaeFacetManager.addGaeFacetIfNeeded(request.getMavenProject().getModel(), request.getProject());
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(
      IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    AppEngineMavenPlugin.logInfo("GaeProjectConfigurator.getBuildParticipant invoked");
    return super.getBuildParticipant(projectFacade, execution, executionMetadata);
  }

}
