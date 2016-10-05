/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.core.update.internal.core;

import com.google.common.base.Joiner;
import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;

import org.eclipse.core.resources.IProject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builds the query string to include with a feature update check request.
 */
public class UpdateQueryBuilder {

  private static final String ACTION_PARAM = "&action=";

  /**
   * The argument to add to the &action= param. May be null.
   */
  private String action;

  /**
   * If this is for an api add ping, then the name of the api added.
   */
  private String apiName;

  /**
   * If this is for an api add ping, then the platform of the api added.
   */
  private String apiPlatform;

  /**
   * The current eclipse version that we are using.
   */
  private String eclipseVersion;

  /**
   * The version of the feature that we are looking to update.
   */
  private String featureVersion;

  /**
   * The installation id.
   */
  private String installationId;

  private Map<String, String> maxSdkVersions;

  /**
   * The product information for eclipse, to distinguish between different
   * "brands" of Eclipse, eg "normal" eclipse and STS.
   */
  private String productId;

  /**
   * Stores information about the facets enabled on this project.
   */
  private String facetsEnabled;

  /**
   * Stores update query arguments contributed by extensions
   */
  private String extensionContribs;

  public UpdateQueryBuilder() {
  }

  public void retrieveExtensionContributions(IProject project) {
    extensionContribs = "";
    ExtensionQuery<UpdateQueryArgContributor> extQuery = new ExtensionQuery<UpdateQueryArgContributor>(
        CorePlugin.PLUGIN_ID, "updateQueryArgContributor", "class");
    List<ExtensionQuery.Data<UpdateQueryArgContributor>> contributors = extQuery.getData();
    for (ExtensionQuery.Data<UpdateQueryArgContributor> c : contributors) {
      UpdateQueryArgContributor uqac = c.getExtensionPointData();
      extensionContribs += uqac.getContribution(project);
    }
    // Set string to null if empty
    if (extensionContribs.isEmpty()) {
      extensionContribs = null;
    }
  }

  /**
   * Retrieves information about the facets enabled on this project.
   */
  public void retrieveFacetsEnabled(IProject project) {
    List<String> facetIdList = FacetFinder.getEnabledNonJavaFacetIds(project);
    // Set string to null if no non-JAVA facets
    if (facetIdList.isEmpty()) {
      facetsEnabled = null;
    } else {
      facetsEnabled = Joiner.on(',').join(facetIdList);
      try {
        facetsEnabled = URLEncoder.encode(facetsEnabled, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // If we fail, just use un-encoded string
        CorePluginLog.logError(e);
      }
    }
  }

  /**
   * Sets if this query is a gae deploy ping.
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * @param apiName
   * @param apiPlatform
   */
  public void setApiNamePlatform(String apiName, String apiPlatform) {
    this.apiName = apiName;
    this.apiPlatform = apiPlatform;
  }

  /**
   * Sets the eclipse version string to use in the query.
   *
   * @param eclipseVersion eclipse version string to use in the query
   */
  public void setEclipseVersion(String eclipseVersion) {
    this.eclipseVersion = eclipseVersion;
  }

  /**
   * Sets the feature version component.
   *
   * @param featureVersion feature version component
   */
  public void setFeatureVersion(String featureVersion) {
    this.featureVersion = featureVersion;
  }

  /**
   * Sets the installation id.
   *
   * @param installationId installation id
   */
  public void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  public void setMaxSdkVersions(Map<String, String> maxSdkVersions) {
    this.maxSdkVersions = maxSdkVersions;
  }

  /**
   * Sets the product ID to differentiate different branded versions of eclipse,
   * eg, STS is com.springsource.sts.ide while regular eclipse is
   * org.eclipse.sdk.ide.
   */
  public void setProductId(String productId) {
    this.productId = productId;
  }

  /**
   * Converts this query into a string that can be used as a URL query
   * parameter.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("id=");
    sb.append(installationId);
    sb.append("&ev=");
    sb.append(eclipseVersion);
    sb.append("&v=");
    sb.append(featureVersion);
    if (maxSdkVersions != null && !maxSdkVersions.isEmpty()) {
      for (Entry<String, String> entry : maxSdkVersions.entrySet()) {
        sb.append("&");
        String key = entry.getKey();
        assert (key != null);
        sb.append(key);
        sb.append("=");
        String value = entry.getValue();
        assert (value != null);
        sb.append(value);
      }
    }

    sb.append("&p=");
    sb.append(productId);

    if (action != null) {
      sb.append(ACTION_PARAM);
      sb.append(action);
    }

    if (apiName != null) {
      sb.append("&apiName=");
      sb.append(apiName);
      sb.append("&apiPlatform=");
      sb.append(apiPlatform);
    }

    if (facetsEnabled != null) {
      sb.append("&facets=");
      sb.append(facetsEnabled);
    }

    if (extensionContribs != null) {
      sb.append(extensionContribs);
    }

    return sb.toString();
  }
}
