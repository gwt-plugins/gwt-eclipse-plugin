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

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.Logger;
import com.google.gdt.eclipse.core.PluginProperties;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gdt.eclipse.core.update.internal.core.FeatureUpdateChecker;
import com.google.gdt.eclipse.core.update.internal.core.FeatureUpdateChecker.UpdateComputer;
import com.google.gdt.eclipse.core.update.internal.core.FeatureUpdateChecker.UpdateInfo;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gdt.eclipse.suite.update.FeatureUpdateCheckersMap.UpdateSiteToken;
import com.google.gdt.eclipse.suite.update.usage.AnalyticsPingManager;
import com.google.gdt.eclipse.suite.update.usage.PingManager;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Element;

/**
 * Activator class for the plugin.
 */
@SuppressWarnings("deprecation")
public class GdtExtPlugin extends AbstractGooglePlugin {

  /**
   * Computes max gwt sdk version.
   */
  public static final class GwtMaxSdkVersionComputer extends SdkUtils.MaxSdkVersionComputer {
    @Override
    public Sdk doFindSdk(IJavaProject project) throws JavaModelException {
      return GWTRuntime.findSdkFor(project);
    }
  }

  // TODO: Expose these via accessors.
  public static final String FEATURE_ID;

  public static final String FEATURE_UPDATE_SITE_URL;
  public static final PluginVersionIdentifier FEATURE_VERSION;
  public static final String PLUGIN_ID = GdtExtPlugin.class.getPackage().getName();

  public static final String GWT_SDK_BUNDLE_FEATURE_ID = "com.google.gwt.eclipse.sdkbundle.feature";
  public static final String APP_ENGINE_SDK_BUNDLE_FEATURE_ID = "com.google.appengine.eclipse.sdkbundle.feature";

  private static BundleContext context;
  private static FeatureUpdateManager featureUpdateManager;
  private static Logger logger;
  private static AnalyticsPingManager analyticsPingManager;
  private static GdtExtPlugin plugin;

  static {
    PluginProperties props = new PluginProperties(GdtExtPlugin.class);
    FEATURE_ID = props.getProperty("featureId", "com.google.gdt.eclipse.suite.e3x.feature");
    FEATURE_VERSION = new PluginVersionIdentifier(props.getProperty("featureVersion", "0.0.0.0"));
    FEATURE_UPDATE_SITE_URL = props.getProperty("featureUpdateSiteURL", "http://localhost/update");
  }

  public static BundleContext getContext() {
    return context;
  }

  public static GdtExtPlugin getDefault() {
    return plugin;
  }

  public static FeatureUpdateManager getFeatureUpdateManager() {
    return featureUpdateManager;
  }

  public static String getFeatureUpdateSiteUrl() {
    return FEATURE_UPDATE_SITE_URL;
  }

  public static IJavaProject[] getJavaProjects() {
    try {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IJavaProject[] projects = JavaCore.create(workspaceRoot).getJavaProjects();
      return projects;
    } catch (JavaModelException e) {
      getLogger().logError(e);
      return new IJavaProject[0];
    }
  }

  public static Logger getLogger() {
    return logger;
  }

  public static PingManager getAnalyticsPingManager() {
    return analyticsPingManager;
  }

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    context = bundleContext;
    super.start(context);
    plugin = this;
    logger = new Logger(this);

    initializeFeatureUpdateManager();
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {

    if (featureUpdateManager != null) {
      featureUpdateManager.cancelPendingUpdates();
      featureUpdateManager = null;
    }

    if (analyticsPingManager != null) {
      analyticsPingManager = null;
    }

    plugin = null;
    logger = null;

    super.stop(context);
    context = null;
  }

  private void initializeFeatureUpdateManager() {
    FeatureUpdateChecker gwtSdkChecker = new FeatureUpdateChecker(new UpdateComputer() {
      @Override
      public UpdateInfo checkSiteXMLForUpdates(Element siteXMLRootElem) {
        GwtMaxSdkVersionComputer maxVersionComputer = new GwtMaxSdkVersionComputer();
        String maxGwtSdkVersion = maxVersionComputer.computeMaxSdkVersion(getJavaProjects());
        if (maxGwtSdkVersion == null) {
          /*
           * Doesn't look like the GWT SDK is being used in the workspace;
           * indicate that there is no update available
           */
          return new UpdateInfo(GWT_SDK_BUNDLE_FEATURE_ID);
        }

        return doCheckSiteXMLForUpdates(
            GWT_SDK_BUNDLE_FEATURE_ID,
            new PluginVersionIdentifier(maxGwtSdkVersion),
            GdtPreferences.getVersionForLastAcknowledgedUpdateNotification(GWT_SDK_BUNDLE_FEATURE_ID),
            siteXMLRootElem, false);
      }
    });

    FeatureUpdateChecker checker = new FeatureUpdateChecker(new UpdateComputer() {
      @Override
      public UpdateInfo checkSiteXMLForUpdates(Element siteXMLRootElem) {
        return doCheckSiteXMLForUpdates(FEATURE_ID, FEATURE_VERSION,
            GdtPreferences.getVersionForLastAcknowledgedUpdateNotification(FEATURE_ID),
            siteXMLRootElem, true);
      }
    });

    FeatureUpdateCheckersMap updateCheckersMap = new FeatureUpdateCheckersMap(UpdateSiteToken.class);

    updateCheckersMap.put(UpdateSiteToken.GPE_CORE, checker);
    updateCheckersMap.put(UpdateSiteToken.GWT_SDK, gwtSdkChecker);

    UpdateSiteURLGenerator generator = new UpdateSiteURLGenerator();

    featureUpdateManager = new FeatureUpdateManager(generator, updateCheckersMap);
    analyticsPingManager = new AnalyticsPingManager(generator);
  }

}
