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
package com.google.appengine.eclipse.wtp.facet;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IActionConfig;

import java.util.Set;

/**
 * {@link IActionConfig} instance for GAE Facet install operation with {@link IDataModel}.
 */
public final class GaeFacetInstallDataModelProvider extends
    GaeFacetInstallAbstractDataModelProvider implements IGaeFacetConstants {

  private boolean hasEnabledSample;

  public GaeFacetInstallDataModelProvider() {
    this(true);
  }

  /**
   * In some cases we have to disable generating App Engine facet sample.
   */
  public GaeFacetInstallDataModelProvider(boolean hasEnabledSample) {
    super();
    this.hasEnabledSample = hasEnabledSample;
  }

  @Override
  public Object getDefaultProperty(String propertyName) {
    if (FACET_ID.equals(propertyName)) {
      return GAE_FACET_ID;
    } else if (GAE_PROPERTY_APP_ID.equals(propertyName)) {
      return "";
    } else if (GAE_PROPERTY_MODULE_ID.equals(propertyName)) {
      return "";
    } else if (GAE_PROPERTY_APP_VERSION.equals(propertyName)) {
      return "1";
    } else if (GAE_PROPERTY_CREATE_SAMPLE.equals(propertyName)) {
      return hasEnabledSample;
    } else if (GAE_PROPERTY_OPEN_IMPORT_API_WIZARD.equals(propertyName)) {
      return false;
    } else if (GAE_PROPERTY_PACKAGE.equals(propertyName)) {
      return DEFAULT_PACKAGE_NAME;
    }
    return super.getDefaultProperty(propertyName);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Set getPropertyNames() {
    Set propertyNames = super.getPropertyNames();
    propertyNames.add(GAE_PROPERTY_APP_VERSION);
    propertyNames.add(GAE_PROPERTY_MODULE_ID);
    propertyNames.add(GAE_PROPERTY_CREATE_SAMPLE);
    propertyNames.add(GAE_PROPERTY_PACKAGE);
    propertyNames.add(GAE_PROPERTY_OPEN_IMPORT_API_WIZARD);
    return propertyNames;
  }

  @Override
  public boolean isPropertyEnabled(String propertyName) {
    if (GAE_PROPERTY_CREATE_SAMPLE.equals(propertyName)) {
      return hasEnabledSample;
    }
    return super.isPropertyEnabled(propertyName);
  }

  @Override
  public IStatus validate(String propertyName) {
    // app id should go first, so use special method in superclass.
    IStatus status = validateAppId(propertyName);
    if (!status.isOK()) {
      return status;
    }
    if (GAE_PROPERTY_APP_VERSION.equals(propertyName)) {
      String value = (String) getProperty(GAE_PROPERTY_APP_VERSION);
      if (value == null || value.trim().length() == 0) {
        return StatusUtilities.newErrorStatus("Please enter Application version",
            AppEnginePlugin.PLUGIN_ID);
      }
      if (!value.matches("[a-zA-Z0-9-]*")) {
        return StatusUtilities.newErrorStatus(
            "Invalid version number. Only letters, digits and hyphen allowed.",
            AppEnginePlugin.PLUGIN_ID);
      }
    }
    if (GAE_PROPERTY_PACKAGE.equals(propertyName)) {
      String value = (String) getProperty(GAE_PROPERTY_PACKAGE);
      if (DEFAULT_PACKAGE_NAME.equals(value) || value == null || value.trim().length() == 0) {
        return StatusUtilities.newWarningStatus("The use of the default package is discouraged.",
            AppEnginePlugin.PLUGIN_ID);
      }
    }
    return super.validate(propertyName);
  }
}
