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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IActionConfig;

/**
 * {@link IActionConfig} instance for GAE EAR Facet install operation with {@link IDataModel}.
 */
public final class GaeEarFacetInstallDataModelProvider extends
    GaeFacetInstallAbstractDataModelProvider {
  @Override
  public Object getDefaultProperty(String propertyName) {
    if (propertyName.equals(FACET_ID)) {
      return GAE_EAR_FACET_ID;
    } else if (propertyName.equals(GAE_PROPERTY_APP_ID)) {
      return "";
    }
    return super.getDefaultProperty(propertyName);
  }

  @Override
  public IStatus validate(String propertyName) {
    IStatus status = validateAppId(propertyName);
    if (!status.isOK()) {
      return status;
    }
    return super.validate(propertyName);
  }
}
