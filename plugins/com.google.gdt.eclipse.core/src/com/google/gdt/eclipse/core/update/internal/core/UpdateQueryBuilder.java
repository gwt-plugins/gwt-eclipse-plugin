/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.core.update.internal.core;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Builds the query string to include with a feature update check request.
 */
public class UpdateQueryBuilder {

  public static final String API_ADD_ACTION = "api_add";

  public static final String GAE_BACKEND_DEPLOY_ACTION = "gae_backend_deploy";

  public static final String GAE_DEPLOY_ACTION = "gae_deploy";

  public static final String GPH_PROJECT_IMPORT = "gph_import";

  private static final String ACTION_PARAM = "&action=";

  private static final String APP_ENGINE_CORE_PLUGIN_ID = "com.google.appengine.eclipse.core";

  private static final String FACET_JST_JAVA = "jst.java";

  private static final String FACET_JAVA = "java";

  public static final String ENDPOINTS_GEN_APP_ENGINE_BACKEND = "endpoints_genbackend";

  public static final String ENDPOINTS_GEN_APP_ENGINE_CONNECTED_ANDROID =
      "endpoints_genconnectedandroid";

  public static final String ENDPOINTS_GEN_ENDPOINT_CLASS = "endpoints_genclass";
  
  public static final String APPS_SCRIPT_IMPORT = "apps_script_import";

  /**
   * Stores count of RPC layers used if Gae Project is App Engine Connected
   * Android Project.
   */
  public static synchronized void incrementRPCLayerCount(IProject project, Boolean initialize) {
    try {
      String rpcLayerCountString = project.getPersistentProperty(new QualifiedName(
          CorePlugin.PLUGIN_ID, "gaeConnectedAndroidProject"));
      int rpcLayerCountInt = 0;
      if (!initialize) {
        if (rpcLayerCountString == null) {
          return;
        } else {
          rpcLayerCountInt = Integer.parseInt(rpcLayerCountString) + 1;
        }
      }
      rpcLayerCountString = Integer.toString(rpcLayerCountInt);
      project.setPersistentProperty(new QualifiedName(CorePlugin.PLUGIN_ID,
          "gaeConnectedAndroidProject"), rpcLayerCountString);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
  }

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
   * The hash of the GAE app's ID, if applicable.
   */
  private String gaeAppIdHash;

  /**
   * The installation id.
   */
  private String installationId;

  private Map<String, String> maxSdkVersions;

  /**
   * Stores count of RPC layers used if Gae Project is App Engine Connected
   * Android Project.
   */
  private String rpcLayerCount;
  /**
   * Stores info about Google cloud sql usage.
   */
  private boolean isGoogleCloudSqlUsed;

  private boolean isGoogleCloudSqlUsedWithMysql;

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

  /**
   * Stores info about whether the project has Web API.
   */
  private boolean isCloudEndpointProject;

  public UpdateQueryBuilder() {
    isGoogleCloudSqlUsed = false;
    isGoogleCloudSqlUsedWithMysql = false;
    isCloudEndpointProject = false;
  }

  /**
   * Retrieves info about whether the project is Cloud Endpoint project.
   */
  public void retrieveCloudEndpointEnabled(IProject project) {
    IFolder f = ResourcesPlugin.getWorkspace().getRoot().getFolder(
        project.getFullPath().append("/war/WEB-INF/"));
    try {
      if (f.exists() && f.members().length != 0) {
        for (IResource r : f.members()) {
          if (r.getName().endsWith(".api")) {
            isCloudEndpointProject = true;
            break;
          }
        }
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
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
    IFacetedProject facetedProject;
    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      return;
    }
    if (facetedProject == null) {
      return;
    }
    facetsEnabled = "";
    for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
      // Skip JAVA facet, since that is always on by default.
      String facetId = facet.getProjectFacet().getId();
      if (!facetId.equals(FACET_JST_JAVA) && !facetId.equals(FACET_JAVA)) {
        facetsEnabled += "," + facetId;
      }
    }
    // Set string to null if no non-JAVA facets
    if (facetsEnabled.isEmpty()) {
      facetsEnabled = null;
    } else {
      // Remove the first comma
      facetsEnabled = facetsEnabled.substring(1);
      try {
        facetsEnabled = URLEncoder.encode(facetsEnabled, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // If we fail, just use un-encoded string
        CorePluginLog.logError(e);
      }
    }
  }

  /**
   * Retrieves info about Google cloud sql usage.
   */
  public void retrieveGoogleCloudSqlUsage(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences prefs = projectScope.getNode(APP_ENGINE_CORE_PLUGIN_ID);
    if (prefs == null) {
      CorePluginLog.logError("retrieveGoogleCloudSqlUsage: No appEngineCorePluginID preferences");
      return;
    }

    if (prefs.getBoolean("googleCloudSqlEnabled", false)) {
      isGoogleCloudSqlUsed = true;
      isGoogleCloudSqlUsedWithMysql = prefs.getBoolean("localDevMySqlEnabled", false);
    }
  }

  /**
   * Retrieves count of RPC layers used if Gae Project is App Engine Connected
   * Android Project.
   */
  public synchronized void retrieveRPCLayerCount(IProject project) {
    try {
      rpcLayerCount = project.getPersistentProperty(new QualifiedName(CorePlugin.PLUGIN_ID,
          "gaeConnectedAndroidProject"));
    } catch (CoreException e) {
      CorePluginLog.logError(e);
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
   * Calculates and stores the hash of the app id of the project if the given
   * project is a GAE project.
   */
  public void setGaeAppIdHash(String hash) {
    this.gaeAppIdHash = hash;
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

    if (gaeAppIdHash != null) {
      sb.append("&appIdHash=");
      sb.append(gaeAppIdHash);
    }

    if (apiName != null) {
      sb.append("&apiName=");
      sb.append(apiName);
      sb.append("&apiPlatform=");
      sb.append(apiPlatform);
    }

    if (rpcLayerCount != null) {
      sb.append("&rpcLayerCount=");
      sb.append(rpcLayerCount);
    }

    if (action != null) {
      sb.append("&isGoogleCloudSqlUsed=");
      sb.append(isGoogleCloudSqlUsed);
      if (isGoogleCloudSqlUsed) {
        sb.append("&isGoogleCloudSqlUsedWithMysql=");
        sb.append(isGoogleCloudSqlUsedWithMysql);
      }
    }

    if (facetsEnabled != null) {
      sb.append("&facets=");
      sb.append(facetsEnabled);
    }

    if (isCloudEndpointProject) {
      sb.append("&isCloudEndpointProject=true");
    }

    if (extensionContribs != null) {
      sb.append(extensionContribs);
    }

    return sb.toString();
  }
}
