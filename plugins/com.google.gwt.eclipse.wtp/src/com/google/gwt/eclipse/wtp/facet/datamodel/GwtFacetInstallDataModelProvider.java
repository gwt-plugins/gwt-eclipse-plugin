/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.facet.datamodel;

import com.google.gwt.eclipse.wtp.facet.IGwtFacetConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;

import java.util.Set;

/**
 * GWT facet install data model provider.
 */
public class GwtFacetInstallDataModelProvider extends FacetInstallDataModelProvider implements
    IGwtFacetConstants {

  @Override
  public Object getDefaultProperty(String propertyName) {
    if (propertyName.equals(FACET_ID)) {
      return GWT_FACET_ID;
    }
    return super.getDefaultProperty(propertyName);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Set getPropertyNames() {
    Set propertyNames = super.getPropertyNames();
    // propertyNames.add(GAE_PROPERTY_APP_ID);
    // propertyNames.add(GAE_PROPERTY_ENABLE_JAR_SPLITTING);
    // propertyNames.add(GAE_PROPERTY_DO_JAR_CLASSES);
    // propertyNames.add(GAE_PROPERTY_RETAIN_STAGING_DIR);
    return propertyNames;
  }

  protected IStatus validateAppId(String propertyName) {
    return Status.OK_STATUS;
  }

}
