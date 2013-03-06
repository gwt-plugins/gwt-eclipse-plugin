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
package com.google.gdt.eclipse.core.properties;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;
import com.google.gdt.eclipse.core.PropertiesUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets and sets Web app project properties.
 */
public final class WebAppProjectProperties {

  /**
   * Interface definition to be called when a project's output WAR directory
   * changes.
   */
  public interface IWarOutLocationChangedListener {
    void warOutLocationChanged(IProject project);
  }

  private static final String JARS_EXCLUDED_FROM_WEB_INF_LIB = "jarsExcludedFromWebInfLib";

  // File system location of the unmanaged WAR output directory last used by a
  // launch, GWT compile, or App Engine deploy.
  private static final String LAST_USED_WAR_OUT_LOCATION = "lastWarOutDir";

  // Project-relative path to WAR source directory
  private static final String WAR_SRC_DIR = "warSrcDir";

  // Indicates whether the WAR source directory is also to be used as an output
  // war directory for launches, deploys, etc. If this option is set, we'll
  // manage the WEB-INF subdirectory to keep the WAR dependencies in sync.
  private static final String WAR_SRC_DIR_IS_OUTPUT = "warSrcDirIsOutput";

  private static final boolean WAR_SRC_DIR_IS_OUTPUT_DEFAULT_VALUE = true;

  private static List<IWarOutLocationChangedListener> warOutLocationChangedListeners = new ArrayList<IWarOutLocationChangedListener>();

  /**
   * The prefix used when launching against an external server.
   */
  private static final String LAUNCH_CONFIG_EXTERNAL_URL_PREFIX = "launchConfigExternalUrlPrefix";

  public static void addWarOutLocationChangedListener(
      IWarOutLocationChangedListener listener) {
    synchronized (warOutLocationChangedListeners) {
      warOutLocationChangedListeners.add(listener);
    }
  }

  public static List<IPath> getJarsExcludedFromWebInfLib(IProject project) {
    String rawPatterns = getProjectProperties(project).get(
        JARS_EXCLUDED_FROM_WEB_INF_LIB, null);
    return PropertiesUtilities.deserializePaths(rawPatterns);
  }

  /**
   * Returns the location last selected as a WAR output directory. If no WAR
   * outputs directory has been chosen for this project, null is returned.
   */
  public static IPath getLastUsedWarOutLocation(IProject project) {
    String path = getProjectProperties(project).get(
        LAST_USED_WAR_OUT_LOCATION, null);
    return path != null ? new Path(path) : null;
  }

  /**
   * Returns the location last selected as a WAR output directory. If no WAR
   * outputs directory has been chosen for this project, the project root
   * directory is returned.
   */
  public static IPath getLastUsedWarOutLocationOrProjectLocation(
      IProject project) {
    IPath path = getLastUsedWarOutLocation(project);
    return path != null ? path : project.getLocation();
  }

  public static String getLaunchConfigExternalUrlPrefix(IProject project) {
    String launchConfigExternalUrlPrefix = getProjectProperties(project).get(
        LAUNCH_CONFIG_EXTERNAL_URL_PREFIX, "");
    return launchConfigExternalUrlPrefix;
  }

  public static IPath getWarSrcDir(IProject project) {
    String warSrcDir = getProjectProperties(project).get(WAR_SRC_DIR, null);
    if (warSrcDir == null) {
      /*
       * If the property value is null, that means it was never set for this
       * project (setWarSrcDir automatically converts null to empty string). To
       * handle legacy projects (pre-GPE 1.3), we'll check to see if it contains
       * a root folder named "war" and if does, use that as the default setting.
       */
      IFolder defaultWar = project.getFolder(WebAppUtilities.DEFAULT_WAR_DIR_NAME);
      if (defaultWar.exists()) {
        return defaultWar.getProjectRelativePath();
      } else {
        return null;
      }
    } else if (warSrcDir.length() == 0) {
      return null;
    } else {
      return new Path(warSrcDir);
    }
  }

  public static boolean isWarSrcDirOutput(IProject project) {
    return getProjectProperties(project).getBoolean(WAR_SRC_DIR_IS_OUTPUT,
        WAR_SRC_DIR_IS_OUTPUT_DEFAULT_VALUE);
  }

  /**
   * If the given project is not already a Web App (i.e. it does not have a
   * defined WAR directory), and it is a Dynamic Web Project, then set the WAR
   * directory to be equivalent to the Web Project's WebContent directory, and
   * ensure that the directory is set to be "input-only".
   */
  public static void maybeSetWebAppPropertiesForDynamicWebProject(
      IProject project) throws BackingStoreException {
    if (WebAppUtilities.isWebApp(project)) {
      return;
    }

    try {
      if (!DynamicWebProjectUtilities.isDynamicWebProject(project)) {
        return;
      }

      IPath webContentFolder = DynamicWebProjectUtilities.getWebContentFolder(project);
      if (webContentFolder != null) {
        WebAppProjectProperties.setWarSrcDir(project, webContentFolder);
        WebAppProjectProperties.setWarSrcDirIsOutput(project, false);
      }
    } catch (CoreException ce) {
      CorePluginLog.logError(ce);
    }
  }

  public static void removeWarOutLocationChangedListener(
      IWarOutLocationChangedListener listener) {
    synchronized (warOutLocationChangedListeners) {
      warOutLocationChangedListeners.remove(listener);
    }
  }

  public static void setJarsExcludedFromWebInfLib(IProject project,
      List<IPath> excludedJars) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = PropertiesUtilities.serializePaths(excludedJars);
    prefs.put(JARS_EXCLUDED_FROM_WEB_INF_LIB, rawPropVal);
    prefs.flush();
  }

  public static void setLastUsedWarOutLocation(IProject project,
      IPath warOutLocation) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(LAST_USED_WAR_OUT_LOCATION, warOutLocation.toString());
    prefs.flush();
  }

  public static void setLaunchConfigExternalUrlPrefix(IProject project,
      String launchConfigExternalUrlPrefix) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    if (launchConfigExternalUrlPrefix == null) {
      launchConfigExternalUrlPrefix = "";
    }
    prefs.put(LAUNCH_CONFIG_EXTERNAL_URL_PREFIX, launchConfigExternalUrlPrefix);
    prefs.flush();
  }

  public static void setWarSrcDir(IProject project, IPath warDir)
      throws BackingStoreException {
    if (warDir == null) {
      warDir = new Path("");
    }
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(WAR_SRC_DIR, warDir.toString());
    prefs.flush();

    if (WebAppUtilities.hasManagedWarOut(project)) {
      notifyWarOutLocationChangedListeners(project);
    }
  }

  public static void setWarSrcDirIsOutput(IProject project, boolean isOutput)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putBoolean(WAR_SRC_DIR_IS_OUTPUT, isOutput);
    prefs.flush();

    if (isOutput) {
      notifyWarOutLocationChangedListeners(project);
    }
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(CorePlugin.PLUGIN_ID);
  }

  private static void notifyWarOutLocationChangedListeners(IProject project) {
    List<IWarOutLocationChangedListener> safeListeners;
    synchronized (warOutLocationChangedListeners) {
      safeListeners = new ArrayList<IWarOutLocationChangedListener>(
          warOutLocationChangedListeners);
    }

    for (IWarOutLocationChangedListener listener : safeListeners) {
      listener.warOutLocationChanged(project);
    }
  }

  private WebAppProjectProperties() {
    // Not instantiable
  }
}
