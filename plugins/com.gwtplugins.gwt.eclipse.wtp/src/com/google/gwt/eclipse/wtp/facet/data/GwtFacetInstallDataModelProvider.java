/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.facet.data;

import java.util.Set;

import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GwtSdk;

/**
 * GWT facet install data model provider.
 * 
 * TODO wrap model in a specific type?
 */
public class GwtFacetInstallDataModelProvider extends FacetInstallDataModelProvider implements IGwtFacetConstants {

  public static boolean isMavenProject(IDataModel model) {
    return model.getBooleanProperty(IGwtFacetConstants.USE_MAVEN_DEPS_PROPERTY_NAME);
  }
  
  public static void setMavenProject(IDataModel model, boolean isMaven) {
    model.setBooleanProperty(IGwtFacetConstants.USE_MAVEN_DEPS_PROPERTY_NAME, isMaven);
  }

  public static void setGwtSdk(IDataModel model, GwtSdk selectedGwtSdk) {
    model.setProperty(IGwtFacetConstants.GWT_SDK, selectedGwtSdk);
  }

  public static GwtSdk getGwtSdk(IDataModel model) {
    return (GwtSdk) model.getProperty(IGwtFacetConstants.GWT_SDK);
  }
  
  @Override
  public Object getDefaultProperty(String propertyName) {
    if (propertyName.equals(FACET_ID)) {
      return GWT_PLUGINS_FACET_ID;
    }
    return super.getDefaultProperty(propertyName);
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Set getPropertyNames() {
    Set propertyNames = super.getPropertyNames();
    propertyNames.add(GWT_SDK);
    propertyNames.add(USE_MAVEN_DEPS_PROPERTY_NAME);

    return propertyNames;
  }

  @Override
  public Object create() {
    Object object = super.create();

    // default sdk
    GwtSdk defaultGwtSdk = GWTPreferences.getDefaultRuntime();
    if (defaultGwtSdk != null) {
      model.setProperty(GWT_SDK, defaultGwtSdk);
    }

    return object;
  }

  public void setGwtSdk(GwtSdk selectedGwtSdk) {
    setProperty(IGwtFacetConstants.GWT_SDK, selectedGwtSdk);
  }

  public GwtSdk getGwtSdk() {
    GwtSdk selectedGwtSdk = (GwtSdk) getProperty(IGwtFacetConstants.GWT_SDK);
    if (selectedGwtSdk == null) {
      selectedGwtSdk = GWTPreferences.getDefaultRuntime();
    }
    return selectedGwtSdk;
  }

  public boolean isMavenProject() {
    return getBooleanProperty(IGwtFacetConstants.USE_MAVEN_DEPS_PROPERTY_NAME);
  }

  public void setMavenProject(boolean isMaven) {
    setBooleanProperty(IGwtFacetConstants.USE_MAVEN_DEPS_PROPERTY_NAME, isMaven);
  }
  
}
