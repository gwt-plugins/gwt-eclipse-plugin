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
package com.google.gdt.eclipse.suite.preferences;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.PropertiesUtilities;
import com.google.gdt.eclipse.core.sdk.SdkRegistrant;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gwt.eclipse.core.sdk.GWTSdkRegistrant;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Contains static methods for retrieving and setting GDT Plugin Preferences.
 */
@SuppressWarnings("deprecation")
public final class GdtPreferences {

  /*
   * This preference key was changed for 1.2 since we now store a string array (of wizard IDs) instead of a boolean
   * flag. If a user upgrades to 1.2 from 1.0 or 1.1, the old preference will remain in their workspace settings store,
   * but will be ignored.
   */
  private static final String ADDED_NEW_WIZARD_ACTIONS = "addedNewWizardActions1.2_";

  /**
   * Key used to store the installation id.
   */
  private static final String INSTALLATION_ID = "id";

  /**
   * The prefix of the key which stores the version associated with the last update notification that the user
   * acknowledged for a given feature. The suffix of the key is the feature id.
   */
  private static final String LAST_ACKNOWLEDGED_UPDATE_NOTIFICATION_PREFIX = "lastAckUpdateNotification_";

  /**
   * Last update time of this feature.
   */
  private static final String LAST_UPDATE_TIME_MILLIS = "lastUpdate";

  /**
   * Severities of our custom problem types. We only store severities that differ from the default, which allows us to
   * add new problem types or modify the default severity of existing problem types without changing any code.
   */
  private static final String PROBLEM_SEVERITIES = "problemSeverities";

  /**
   * Records the migrator version of the project for use by {@link com.google.gdt.eclipse.suite.ProjectMigrator}.
   */
  private static final String PROJECT_MIGRATOR_VERSION = "projectMigratorVersion_";

  /*
   * SDK bundles are discovered using their ID pattern: "com.gwtplugins.*.eclipse.sdkbundle"
   */
  private static final String SDK_BUNDLE_PREFIX = "com.gwtplugins.";
  private static final String SDK_BUNDLE_SUFFIX = ".eclipse.sdkbundle";

  /**
   * The prefix for the key to use for storing the SdkRegistrants. Version 1 key was just "SdkRegistrants". Version 2
   * key was "SdkRegistrants_installationpath" (see computeSdkRegistrantsKeyV2). Version 3 key is
   * "SdkRegistrants_installationID" (see computeSdkRegistrantsKeyV3).
   */
  private static final String SDK_REGISTRANTS_KEY_PREFIX = "SdkRegistrants";

  /**
   * key in the marker property that identifies the kind of SDK is present
   */
  private static final String SDK_BUNDLE_MARKER_PROPERTY = "sdkType";

  /**
   * identifies the path local prefix in a bundle to the SDK (default value <SDKTYPE>_HOME is replaced at build time
   * with the actual path)
   */
  private static final String SDK_PATH_PREFIX_PROPERTY = "sdkBundlePath";

  /**
   * The property filename expected at the root of any plugin/bundle that should be probed for SDK registration.
   */
  private static final String SDK_REGISTRANT_PROPERTY_FILE = "SdkBundleRegistrant.properties";

  /**
   * Controls whether the updates should be automatically downloaded.
   */
  private static final String UPDATE_NOTIFICATIONS = "updateNotifications";

  /**
   * Capture analytics of the use of this plugin.
   */
  private static final String CAPTURE_ANALYTICS = "captureAnalytics";

  /**
   * Records the GEP plugin version that most recently forced a rebuild on a project after install (to clean up and
   * regenerate stale markers, etc.).
   */
  private static final String VERSION_FOR_LAST_FORCED_REBUILD_PREFIX = "versionForLastForcedRebuild_";

  public static boolean areUpdateNotificationsEnabled() {
    return getConfigurationPreferences().getBoolean(UPDATE_NOTIFICATIONS, false);
  }

  public static boolean getCaptureAnalytics() {
    return getConfigurationPreferences().getBoolean(CAPTURE_ANALYTICS, false);
  }

  public static List<String> getAddedNewWizardActionsForPerspective(String perspectiveId) {
    IEclipsePreferences instancePrefs = getInstancePreferences();
    return PropertiesUtilities.deserializeStrings(instancePrefs.get(ADDED_NEW_WIZARD_ACTIONS + perspectiveId, ""));
  }

  /**
   * Gets the problem severities as an encoded string. See
   * {@link com.google.gdt.eclipse.core.markers.GdtProblemSeverities} for details on how this string is decoded.
   */
  public static String getEncodedProblemSeverities() {
    IEclipsePreferences instancePrefs = getConfigurationPreferences();
    return instancePrefs.get(PROBLEM_SEVERITIES, "");
  }

  /**
   * Returns the installation id for this plugin, or <code>null</code> if the installation id has never been set.
   *
   * @return installation id for this plugin.
   */
  public static String getInstallationId() {
    return getConfigurationPreferences().get(INSTALLATION_ID, null);
  }

  /**
   * Returns the last update time in milliseconds for this installation or <code>0</code> if the last update time has
   * never been set.
   *
   * @return the time in millis
   */
  public static long getLastUpdateTimeMillis() {
    return getConfigurationPreferences().getLong(LAST_UPDATE_TIME_MILLIS, 0);
  }

  public static int getProjectMigratorVersion(IProject project) {
    IEclipsePreferences instancePrefs = getInstancePreferences();
    return instancePrefs.getInt(PROJECT_MIGRATOR_VERSION + project.getName(), 0);
  }

  @SuppressWarnings("deprecation")
  public static PluginVersionIdentifier getVersionForLastAcknowledgedUpdateNotification(String featureId) {
    return new PluginVersionIdentifier(
        getConfigurationPreferences().get(getLastAckFeatureUpdateVersionKey(featureId), "0.0.0.0"));
  }

  public static Version getVersionForLastForcedRebuild(IProject project) {
    IEclipsePreferences instancePrefs = getInstancePreferences();
    String versionString = instancePrefs.get(VERSION_FOR_LAST_FORCED_REBUILD_PREFIX + project.getName(), "0.0.0.0");
    return new Version(versionString);
  }

  /**
   * Registers all of the {@link SdkRegistrant}s, and records as a workspace preference which ones those were.
   * Registrants will only be called once per workspace.
   *
   * Prior versions of the SDK registration mechanism required adding implementations of SdkBundleRegistratant that were
   * aware of GPE internals, and would handle registrations themselves. To decouple the build/release process of
   * sdkbundles from GPE, this approach has been abandoned.
   *
   * This implementation inspects bundles with a com.google.*.eclipse.sdkbundle bundle id and looks for a marker
   * property file for details about the SDK. If the information resolves as a known SDK type and a valid SDK path, the
   * SDK path is then registered against the proper registrant.
   *
   */
  public static synchronized void registerSdks() {
    IEclipsePreferences instancePrefs = getInstancePreferences();

    final String sdkRegistrantsKeyV3 = computeSdkRegistrantsKeyV3();
    ensureUsingNewSdkRegistrantsKey(instancePrefs, sdkRegistrantsKeyV3);

    String sdkRegistrantsAsString = instancePrefs.get(sdkRegistrantsKeyV3, "");
    Set<String> sdkRegistrants = new LinkedHashSet<String>(decodeRegistrants(sdkRegistrantsAsString));

    BundleContext context = GdtPlugin.getDefault().getBundle().getBundleContext();

    for (Bundle bundle : context.getBundles()) {
      String bundleName = bundle.getSymbolicName();
      String bundleVersion = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
      String sdkId = bundleName + '_' + bundleVersion;

      // The bundle name must match com.google.*.eclipse.sdkbundle
      if (bundleName.startsWith(SDK_BUNDLE_PREFIX) && bundleName.contains(SDK_BUNDLE_SUFFIX)
          && !sdkRegistrants.contains(sdkId)) {

        GdtPlugin.getLogger().logInfo("Registering: " + sdkId);
        try {
          registerBundleSdk(bundle);
        } catch (CoreException e) {
          // Log and continue.
          GdtPlugin.getLogger().logError(e);
        }

        // Add the sdk even if we get an exception while registering to prevent
        // logging an error on each restart.
        sdkRegistrants.add(sdkId);
      }
    }

    instancePrefs.put(sdkRegistrantsKeyV3, encodeRegistrants(sdkRegistrants));
    flushPreferences(instancePrefs);
  }

  public static void setAddedNewWizardActionsForPerspective(String perspectiveId, List<String> wizardIds) {
    IEclipsePreferences instancePrefs = getInstancePreferences();
    instancePrefs.put(ADDED_NEW_WIZARD_ACTIONS + perspectiveId, PropertiesUtilities.serializeStrings(wizardIds));
    flushPreferences(instancePrefs);
  }

  /**
   * Sets the problem severities using an encoded string generated by
   * {@link com.google.gdt.eclipse.core.markers.GdtProblemSeverities#toPreferenceString()
   * GdtProblemSeverities#toPreferenceString()}.
   */
  public static void setEncodedProblemSeverities(String encodedSeverities) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.put(PROBLEM_SEVERITIES, encodedSeverities);
    flushPreferences(configurationPreferences);
  }

  public static void setInstallationId(String id) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.put(INSTALLATION_ID, id);
    flushPreferences(configurationPreferences);
  }

  /**
   * Sets the last update time in milliseconds.
   *
   * @param lastUpdateTimeMillis
   *          date of the last update time in milliseconds
   */
  public static void setLastUpdateTimeMillis(long lastUpdateTimeMillis) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.putLong(LAST_UPDATE_TIME_MILLIS, lastUpdateTimeMillis);
    flushPreferences(configurationPreferences);
  }

  public static void setProjectMigratorVersion(IProject project, int version) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.putInt(PROJECT_MIGRATOR_VERSION + project.getName(), version);
    flushPreferences(configurationPreferences);
  }

  public static void setUpdateNotificationsEnabled(boolean enabled) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.putBoolean(UPDATE_NOTIFICATIONS, enabled);
    flushPreferences(configurationPreferences);
  }

  public static void setAnalytics(boolean capture) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.putBoolean(CAPTURE_ANALYTICS, capture);
    flushPreferences(configurationPreferences);
  }

  @SuppressWarnings("deprecation")
  public static void setVersionForLastAcknowlegedUpdateNotification(String featureId, PluginVersionIdentifier version) {
    IEclipsePreferences configurationPreferences = getConfigurationPreferences();
    configurationPreferences.put(getLastAckFeatureUpdateVersionKey(featureId), version.toString());
    flushPreferences(configurationPreferences);
  }

  public static void setVersionForLastForcedRebuild(IProject project, Version version) {
    IEclipsePreferences instancePrefs = getInstancePreferences();
    instancePrefs.put(VERSION_FOR_LAST_FORCED_REBUILD_PREFIX + project.getName(), version.toString());
    flushPreferences(instancePrefs);
  }

  private static String computeSdkRegistrantsKeyV2() {
    StringBuilder key = new StringBuilder(SDK_REGISTRANTS_KEY_PREFIX);
    Location location = Platform.getInstallLocation();
    if (location != null) {
      URL locationUrl = location.getURL();
      if (locationUrl != null) {
        key.append('_').append(locationUrl.toString());
      }
    }

    return key.toString();
  }

  /**
   * Version 1 key was just "SdkRegistrants". Version 2 key was "SdkRegistrants_installationpath" (see
   * computeSdkRegistrantsKeyV2 above). Version 3 key is "SdkRegistrants_installationID".
   */
  private static String computeSdkRegistrantsKeyV3() {
    return SDK_REGISTRANTS_KEY_PREFIX + "_" + getInstallationId();
  }

  private static List<String> decodeRegistrants(String encodedRegistrants) {
    if (encodedRegistrants.length() == 0) {
      return Collections.emptyList();
    }

    String[] registrantsAsArray = encodedRegistrants.split(",");
    return Arrays.asList(registrantsAsArray);
  }

  private static String encodeRegistrants(Collection<String> registrants) {
    StringBuilder sb = new StringBuilder();
    boolean addComma = false;
    for (String registrant : registrants) {
      if (!addComma) {
        addComma = true;
      } else {
        sb.append(",");
      }

      sb.append(registrant);
    }
    return sb.toString();
  }

  private static void ensureUsingNewSdkRegistrantsKey(IEclipsePreferences instancePrefs, String newKey) {
    try {
      List<String> keys = Arrays.asList(instancePrefs.keys());
      if (!keys.contains(newKey)) {
        String oldKey2;
        // are we using the original "SdkRegistrants" key?
        if (keys.contains(SDK_REGISTRANTS_KEY_PREFIX)) {
          updateSdkRegistrantsKey(instancePrefs, SDK_REGISTRANTS_KEY_PREFIX, newKey);
        } else if (keys.contains((oldKey2 = computeSdkRegistrantsKeyV2()))) {
          // or are we using the 2nd version "SdkRegistrants_installationpath"
          // key?
          updateSdkRegistrantsKey(instancePrefs, oldKey2, newKey);
        }
      }
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e, "Could not check if migration to new SdkRegistrants key format is needed.");
    }
  }

  private static void flushPreferences(IEclipsePreferences preferences) {
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
    }
  }

  private static IEclipsePreferences getConfigurationPreferences() {
    ConfigurationScope scope = new ConfigurationScope();
    IEclipsePreferences configurationPrefs = scope.getNode(GdtPlugin.PLUGIN_ID);
    return configurationPrefs;
  }

  /**
   * Returns the instance scope context for this plugin.
   */
  private static IEclipsePreferences getInstancePreferences() {
    InstanceScope scope = new InstanceScope();
    IEclipsePreferences instancePrefs = scope.getNode(GdtPlugin.PLUGIN_ID);
    return instancePrefs;
  }

  private static String getLastAckFeatureUpdateVersionKey(String featureId) {
    return LAST_ACKNOWLEDGED_UPDATE_NOTIFICATION_PREFIX + featureId;
  }

  /**
   * Attempts to register the SDK from a bundle.
   */
  private static void registerBundleSdk(Bundle bundle) throws CoreException {
    try {
      IPath propPath = new Path(SDK_REGISTRANT_PROPERTY_FILE);
      URL propUrl = FileLocator.find(bundle, propPath, (Map<String, String>) null);
      if (propUrl != null) {
        InputStream instream = propUrl.openStream();
        Properties props = new Properties();
        props.load(instream);
        String sdkType = props.getProperty(SDK_BUNDLE_MARKER_PROPERTY);
        String sdkPrefix = props.getProperty(SDK_PATH_PREFIX_PROPERTY);
        if (sdkType != null && sdkPrefix != null) {
          IPath sdkPrefixPath = new Path(sdkPrefix);
          URL sdkPathUrl = FileLocator.find(bundle, sdkPrefixPath, (Map<String, String>) null);
          if (sdkPathUrl == null) {
            // Automatic SDK registration failed. This is expected in dev mode.
            CorePluginLog.logWarning("Failed to register SDK: " + sdkPrefix);
            return;
          }
          // resolve needed to switch from bundleentry to file url
          sdkPathUrl = FileLocator.resolve(sdkPathUrl);
          if (sdkPathUrl != null) {
            if ("file".equals(sdkPathUrl.getProtocol())) {
              GWTSdkRegistrant.registerSdk(sdkPathUrl, sdkType);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.WARNING, GdtPlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
    }
  }

  private static void updateSdkRegistrantsKey(IEclipsePreferences instancePrefs, String oldKey, String newKey) {

    // Copy the oldKey's value into the newKey
    instancePrefs.put(newKey, instancePrefs.get(oldKey, ""));

    // Remove the oldKey so we do not copy it for future Eclipse
    // installations
    instancePrefs.remove(oldKey);
  }

  private GdtPreferences() {
    // Not instantiable.
  }
}
