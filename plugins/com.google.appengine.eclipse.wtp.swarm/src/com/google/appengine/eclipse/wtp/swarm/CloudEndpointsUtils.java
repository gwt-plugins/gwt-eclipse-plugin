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
package com.google.appengine.eclipse.wtp.swarm;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IType;

import java.util.List;

/**
 * Utility methods related to Cloud Endpoints.
 */
public final class CloudEndpointsUtils {
  public static final QualifiedName PROP_DISABLE_ENDPOINTS_BUILDER = new QualifiedName(
      AppEngineSwarmPlugin.PLUGIN_ID, "endpointsBuilderDisabled");
  public static final String MARKER_ID = AppEngineSwarmPlugin.PLUGIN_ID
      + ".cloudEndpointsProblemMarker";
  /**
   * Instantiates and configures an {@link SwarmServiceCreator} instance. Project must be app engine
   * dynamic web project.
   */
  public static SwarmServiceCreator createServiceCreator(IProject project, List<IType> entityList)
      throws CoreException {
    String appId = ProjectUtils.getAppId(project);
    // TODO fix , does this use the facet?
    IPath gaeSdkPath = ProjectUtils.getGaeSdkLocation(project);
    if (gaeSdkPath == null) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "No Google App Engine SDK found for project: " + project.getName(),
          AppEngineSwarmPlugin.PLUGIN_ID));
    }
    SwarmServiceCreator serviceCreator = new SwarmServiceCreator();
    serviceCreator.setEntities(entityList);
    serviceCreator.setAppId(appId);
    serviceCreator.setGaeSdkPath(gaeSdkPath);
    return serviceCreator;
  }

  /**
   * Returns <code>true</code> if the project has the runtime which supports EAR, otherwise returns
   * <code>false</code>. If GAE SDK is not found, throws exception.
   */
  public static boolean isEndpointsSupported(IProject project) throws CoreException {
    IPath sdkLocation = ProjectUtils.getGaeSdkLocation(project);
    if (sdkLocation != null) {
      SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
      GaeSdk sdk = SdkUtils.findSdkForInstallationPath(sdks, sdkLocation);
      if (sdk != null) {
        return GaeSdkCapability.CLOUD_ENDPOINTS.check(sdk);
      }
    }
    throw new CoreException(StatusUtilities.newErrorStatus(
        "No Google App Engine SDK found for project: " + project.getName(),
        AppEngineSwarmPlugin.PLUGIN_ID));
  }

  /**
   * Not instantiable.
   */
  private CloudEndpointsUtils() {
  }
}
