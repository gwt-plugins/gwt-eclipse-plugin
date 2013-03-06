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
package com.google.gdt.eclipse.appsmarketplace.properties;

import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.appsmarketplace.data.Category;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Gets and sets apps marketplace project properties.
 */
public final class AppsMarketplaceProjectProperties {

  private static final String APPLICATION_LISTING_NAME =
      "applicationListingName";

  private static final String APPLICATION_LISTING_URL = "applicationListingUrl";

  private static final String APPLICATION_LISTING_ID = "applicationListingId";

  private static final String APPLICATION_LISTING_CATEGORY =
      "applicationListingCategory";

  private static final String APPLICATION_LISTING_ALREADY_LISTED =
      "applicationListingAlreadyListed";

  private static final String APPLICATION_LISTING_CONSUMER_KEY =
      "applicationListingConsumerKey";

  private static final String APPLICATION_LISTING_CONSUMER_SECRET =
      "applicationListingConsumerSecret";

  private static final String APPLICATION_LISTING_WAR_DIRECTORY =
      "applicationListingWarDirectory";

  private static final String AUTO_POPULATE_STRING =
      "Automatically populated once application is successfully listed";

  public static boolean getApplicationListingAlreadyListed(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(APPLICATION_LISTING_ALREADY_LISTED, false);
  }

  public static Integer getAppListingCategory(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getInt(APPLICATION_LISTING_CATEGORY, Category.getDefault());
  }

  public static String getAppListingConsumerKey(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(APPLICATION_LISTING_CONSUMER_KEY, AUTO_POPULATE_STRING);
  }

  public static String getAppListingConsumerSecret(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(APPLICATION_LISTING_CONSUMER_SECRET, AUTO_POPULATE_STRING);
  }

  public static String getAppListingId(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(APPLICATION_LISTING_ID, "");
  }

  public static String getAppListingName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(APPLICATION_LISTING_NAME, project.getName());
  }

  public static String getAppListingUrl(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    String url = prefs.get(APPLICATION_LISTING_URL, "");
    if (StringUtilities.isEmpty(url)) {
      url = getAppEngineUrl(project);
    }
    return url;
  }

  public static String getAppListingWarDirectory(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    if (WebAppUtilities.isWebApp(project)) {
      return WebAppUtilities.DEFAULT_WAR_DIR_NAME;
    } else {
      return prefs.get(APPLICATION_LISTING_WAR_DIRECTORY, "");
    }
  }

  public static void setAppListingAlreadyListed(
      IProject project, boolean settings) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putBoolean(APPLICATION_LISTING_ALREADY_LISTED, settings);
    prefs.flush();
  }

  public static void setAppListingCategory(IProject project, Integer settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putInt(APPLICATION_LISTING_CATEGORY, settings);
    prefs.flush();
  }

  public static void setAppListingConsumerKey(IProject project, String settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_CONSUMER_KEY, settings);
    prefs.flush();
  }

  public static void setAppListingConsumerSecret(
      IProject project, String settings) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_CONSUMER_SECRET, settings);
    prefs.flush();
  }

  public static void setAppListingId(IProject project, String settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_ID, settings);
    prefs.flush();
  }

  public static void setAppListingName(IProject project, String settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_NAME, settings);
    prefs.flush();
  }

  public static void setAppListingUrl(IProject project, String settings)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_URL, settings);
    prefs.flush();
  }

  public static void setAppListingWarDirectory(
      IProject project, String settings) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(APPLICATION_LISTING_WAR_DIRECTORY, settings);
    prefs.flush();
  }

  private static String getAppEngineUrl(IProject project) {
    String url = new String("");
    GaeProject gaeProject = GaeProject.create(project);
    if (gaeProject != null && !StringUtilities.isEmpty(gaeProject.getAppId())) {
      url = "http://" + gaeProject.getAppId() + ".appspot.com";
    }
    return url;
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(AppsMarketplacePlugin.PLUGIN_ID);
  }

  private AppsMarketplaceProjectProperties() {
    // Not instantiable
  }
}