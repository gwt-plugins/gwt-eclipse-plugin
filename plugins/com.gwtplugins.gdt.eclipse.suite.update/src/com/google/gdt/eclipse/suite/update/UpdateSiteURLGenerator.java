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
package com.google.gdt.eclipse.suite.update;

import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gdt.eclipse.core.update.internal.core.UpdateQueryBuilder;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin.GwtMaxSdkVersionComputer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.update.internal.configurator.VersionedIdentifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates various URLs for update pings.
 */
@SuppressWarnings("restriction")
public class UpdateSiteURLGenerator {

  public static String getSHA1Hash(String appId) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      byte[] hash = md.digest(appId.getBytes());

      // assemble the array of bytes into a hex string
      StringBuffer sb = new StringBuffer(40);
      for (byte b : hash) {
        String s = Integer.toHexString(b & 0xFF).toUpperCase();
        if (s.length() == 1) {
          sb.append('0');
        }
        sb.append(s);
      }

      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  /**
   * For normal update pings.
   */
  public URL generateURL() throws MalformedURLException {
    return computeCompositeUpdateSiteURL(
        GdtExtPlugin.FEATURE_UPDATE_SITE_URL, null, null, null, null, null);
  }

  /**
   * For redirecting pings to a different update site.
   */
  public URL generateURL(String baseUrl) throws MalformedURLException {
    return computeCompositeUpdateSiteURL(baseUrl, null, null, null, null, null);
  }

  /**
   * Computes the update site URL for the composite update site.
   *
   * @param baseUrl the base url to use, without /compositeArtifacts.xml
   * @param action the action parameter to add to the url, eg
   *          {@link UpdateQueryBuilder}.GAE_DEPLOY_ACTION. May be null
   * @param project if not null and is a GAE project, then the project's app ID
   *          is hashed and added to the url.
   * @param apiName the name of the api added if this is an api add action. May
   *          be null
   * @param apiPlatform a platform name such as "Unknown" if this is an
   *          api add action. May be null
   * @param extra if not null, then is appended at the end of the URL
   */
  @SuppressWarnings("deprecation") // PluginVersionIdentifier.toString()
  private static URL computeCompositeUpdateSiteURL(String baseUrl, String action,
      IProject project, String apiName, String apiPlatform, String extra)
      throws MalformedURLException {
    String compositeArtifactsXMLUrlWithQuery = baseUrl;

    // Normalize URL
    if (!compositeArtifactsXMLUrlWithQuery.endsWith("/")) {
      compositeArtifactsXMLUrlWithQuery += '/';
    }

    String core = "core/";
    int coreIndex = compositeArtifactsXMLUrlWithQuery.indexOf(core);
    if (coreIndex >= 0) {
      compositeArtifactsXMLUrlWithQuery = compositeArtifactsXMLUrlWithQuery
          .substring(0, coreIndex)
          .concat(compositeArtifactsXMLUrlWithQuery.substring(coreIndex + core.length()));
    }

    // Append compositeArtifacts.xml to URL
    compositeArtifactsXMLUrlWithQuery += "compositeArtifacts.xml";

    // Build query string
    UpdateQueryBuilder updateQueryBuilder = new UpdateQueryBuilder();

    updateQueryBuilder.setInstallationId(GdtPlugin.getInstallationId());
    updateQueryBuilder.setEclipseVersion(GdtPlugin.getEclipseVersion());
    updateQueryBuilder.setFeatureVersion(
        new VersionedIdentifier(
            GdtExtPlugin.FEATURE_ID, GdtExtPlugin.FEATURE_VERSION.toString()).toString());

    SdkUtils.MaxSdkVersionComputer gwtMaxVersionComputer = new GwtMaxSdkVersionComputer();

    Map<String, String> maxSdkVersions = new HashMap<String, String>();
    IJavaProject[] projects = GdtExtPlugin.getJavaProjects();

    String maxGWTSdkVersion = gwtMaxVersionComputer.computeMaxSdkVersion(
        projects);
    if (maxGWTSdkVersion != null) {
      maxSdkVersions.put("gwtv", maxGWTSdkVersion);
    }

    updateQueryBuilder.setMaxSdkVersions(maxSdkVersions);

    updateQueryBuilder.setProductId(Platform.getProduct().getId());

    if (action != null) {
      updateQueryBuilder.setAction(action);
    }

    if (apiName != null) {
      updateQueryBuilder.setApiNamePlatform(apiName, apiPlatform);
    }

    if (project != null) {
      updateQueryBuilder.retrieveFacetsEnabled(project);
    }

    updateQueryBuilder.retrieveExtensionContributions(project);
    String queryString = updateQueryBuilder.toString();

    // Append query string to URL, if available
    if (queryString != null && queryString.length() > 0) {
      compositeArtifactsXMLUrlWithQuery += '?' + queryString;
    }

    if (extra != null && !extra.isEmpty()) {
      compositeArtifactsXMLUrlWithQuery += extra;
    }

    return new URL(compositeArtifactsXMLUrlWithQuery);
  }

}
