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
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;

import java.util.Set;

/**
 * A base class for facet install data model provider.
 */
abstract class GaeFacetInstallAbstractDataModelProvider extends FacetInstallDataModelProvider
    implements IGaeFacetConstants {

  @Override
  public Object getDefaultProperty(String propertyName) {
    if (GAE_PROPERTY_RETAIN_STAGING_DIR.equals(propertyName)) {
      return false;
    } else if (GAE_PROPERTY_DO_JAR_CLASSES.equals(propertyName)) {
      return false;
    } else if (GAE_PROPERTY_ENABLE_JAR_SPLITTING.equals(propertyName)) {
      return true;
    }
    return super.getDefaultProperty(propertyName);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Set getPropertyNames() {
    Set propertyNames = super.getPropertyNames();
    propertyNames.add(GAE_PROPERTY_APP_ID);
    propertyNames.add(GAE_PROPERTY_ENABLE_JAR_SPLITTING);
    propertyNames.add(GAE_PROPERTY_DO_JAR_CLASSES);
    propertyNames.add(GAE_PROPERTY_RETAIN_STAGING_DIR);
    return propertyNames;
  }

  protected IStatus validateAppId(String propertyName) {
    if (GAE_PROPERTY_APP_ID.equals(propertyName)) {
      String value = (String) getProperty(GAE_PROPERTY_APP_ID);
      if (value == null || value.trim().length() == 0) {
        return StatusUtilities.newWarningStatus(
            "You won't be able to deploy to Google without valid Application ID.",
            AppEnginePlugin.PLUGIN_ID);
      }
    }
    return Status.OK_STATUS;
  }
}