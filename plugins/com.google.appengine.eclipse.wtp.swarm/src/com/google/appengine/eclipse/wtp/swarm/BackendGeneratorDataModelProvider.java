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
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelPropertyDescriptor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link IDataModelProvider} for backend project generation.
 */
final class BackendGeneratorDataModelProvider extends AbstractDataModelProvider {
  static final String SELECTED_RUNTIME = IGaeFacetConstants.GAE_FACET_ID
      + ".property.backend.selectedRuntime";
  static final String GAE_FACET_INSTALL_DM = IGaeFacetConstants.GAE_FACET_ID
      + ".property.backend.gaeFacetInstallDataModel";
  static final String SCM_PROJECT_NUMBER = IGaeFacetConstants.GAE_FACET_ID
      + ".property.backend.scmProjectNumber";
  static final String SCM_API_KEY = IGaeFacetConstants.GAE_FACET_ID + ".property.backend.scmApiKey";
  static final String ANDROID_PROJECT_NAME = IGaeFacetConstants.GAE_FACET_ID
      + ".property.backend.androidProjectName";
  static final String ANDROID_PACKAGE_NAME = IGaeFacetConstants.GAE_FACET_ID
      + ".property.backend.androidPackageName";

  private DataModelPropertyDescriptor[] runtimeDescriptors;

  @Override
  public IDataModelOperation getDefaultOperation() {
    return new BackendGeneratorDataModelOperation(getDataModel());
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Set getPropertyNames() {
    Set propertyNames = super.getPropertyNames();
    propertyNames.add(IFacetDataModelProperties.FACET_PROJECT_NAME);
    propertyNames.add(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
    propertyNames.add(SELECTED_RUNTIME);
    propertyNames.add(GAE_FACET_INSTALL_DM);
    propertyNames.add(SCM_PROJECT_NUMBER);
    propertyNames.add(SCM_API_KEY);
    propertyNames.add(ANDROID_PROJECT_NAME);
    propertyNames.add(ANDROID_PACKAGE_NAME);
    return propertyNames;
  }

  @Override
  public DataModelPropertyDescriptor[] getValidPropertyDescriptors(String propertyName) {
    if (SELECTED_RUNTIME.equals(propertyName)) {
      if (runtimeDescriptors != null) {
        // return cached
        return runtimeDescriptors;
      }
      IProjectFacet f = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
      Set<IRuntime> runtimes = RuntimeManager.getRuntimes();
      // sort runtimes by version string
      Set<IRuntime> gaeRuntimes = new TreeSet<IRuntime>(new Comparator<IRuntime>() {
        @Override
        public int compare(IRuntime o1, IRuntime o2) {
          return getSdkVersion(o2).compareTo(getSdkVersion(o1));
        }
      });
      // collect runtimes
      for (IRuntime runtime : runtimes) {
        if (runtime.supports(f)) {
          IStatus status = runtime.validate(new NullProgressMonitor());
          if (status == null || status.isOK()) {
            gaeRuntimes.add(runtime);
          }
        }
      }
      runtimeDescriptors = new DataModelPropertyDescriptor[gaeRuntimes.size()];
      int i = 0;
      for (IRuntime runtime : gaeRuntimes) {
        runtimeDescriptors[i++] = new DataModelPropertyDescriptor(runtime, runtime.getName());
      }
      return runtimeDescriptors;
    }
    return super.getValidPropertyDescriptors(propertyName);
  }

  @Override
  public IStatus validate(String propertyName) {
    if (SELECTED_RUNTIME.equals(propertyName)) {
      DataModelPropertyDescriptor[] descriptors = getValidPropertyDescriptors(propertyName);
      if (descriptors.length == 0) {
        return StatusUtilities.newErrorStatus("No valid Google App Engine Runtime found.",
            AppEngineSwarmPlugin.PLUGIN_ID);
      }
      IRuntime runtime = (IRuntime) getProperty(propertyName);
      if (runtime == null) {
        return StatusUtilities.newErrorStatus("No valid Google App Engine Runtime selected.",
            AppEngineSwarmPlugin.PLUGIN_ID);
      }
      String sdkVersion = getSdkVersion(runtime);
      if (sdkVersion.length() == 0) {
        // empty string means non-valid runtime
        return StatusUtilities.newErrorStatus(
            "Selected Google App Engine Runtime is not valid. Check the runtime in preferences.",
            AppEngineSwarmPlugin.PLUGIN_ID);
      }
      return null;
    }
    return super.validate(propertyName);
  }

  /**
   * @returns GAE SDK version, if found. Returns empty string otherwise.
   */
  private String getSdkVersion(IRuntime runtime) {
    IPath sdkLocation = ProjectUtils.getGaeSdkLocation(runtime);
    if (sdkLocation != null) {
      SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
      GaeSdk sdk = SdkUtils.findSdkForInstallationPath(sdks, sdkLocation);
      if (sdk != null) {
        return sdk.getVersion();
      }
    }
    return "";
  }
}