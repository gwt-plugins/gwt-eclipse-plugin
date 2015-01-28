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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProject;

/**
 * Google Cloud SDK facet delegate for facet install action.
 */
@SuppressWarnings("restriction") // For the FacetedProject in the javadoc
public class CloudSdkFacetInstallDelegate implements IDelegate {
  /**
   * When the user clicks the "Apply" or "OK" button in the Project Facet page,
   * this function is called through {@link FacetedProject#mergeChanges} and if this function
   * exits without an exception, the facet will be added to the project via {@link FacetedProject}.
   */
  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config,
      IProgressMonitor monitor) throws CoreException {
    if (!CloudSdkUtils.hasGcloudMavenPlugin(project)) {
      // This exception will prevent the facet from being added to the project
      throw new CoreException(new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID,
          "Cannot install Cloud SDK facet.\n"
          + "Cloud SDK facets can only be installed in maven projects with GCloud Maven plugin"));
    }
  }
}
