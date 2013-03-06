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
package com.google.gwt.eclipse.core.preferences;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.ClasspathContainerUpdateJob;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkManager.SdkUpdateEvent;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.DetailFormatter;
import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JavaDebugOptionsManager;
import org.eclipse.jdt.internal.debug.ui.JavaDetailFormattersManager;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Contains static methods for retrieving and setting GWT plug-in preferences.
 */
public class GWTPreferences {

  /**
   * The key for storing the preference for removing terminated launches when a new
   * launch is launched.
   */
  private static final String REMOVE_TERMINATED_LAUNCHES = "removeTerminatedLaunches";

  private static SdkManager<GWTRuntime> sdkManager;

  private static final String SOURCE_VIEWER_SERVER_PORT = "sourceViewerServerPort";

  private static final String SPEEDTRACER_GEN_FOLDER_NAME = "speedTracerGenFolderName";

  private static final String JSO_PROPERTY_TYPE =
      "com.google.gwt.core.client.debug.JsoInspector$JsoProperty";

  static {
    sdkManager = new SdkManager<GWTRuntime>(GWTRuntimeContainer.CONTAINER_ID,
        getEclipsePreferences(), GWTRuntime.getFactory());

    sdkManager.addSdkUpdateListener(new SdkManager.SdkUpdateListener<GWTRuntime>() {
      public void onSdkUpdate(SdkUpdateEvent<GWTRuntime> sdkUpdateEvent)
          throws CoreException {
        // Update all of the WEB-INF/lib files
        IJavaProject[] projects = JavaCore.create(
            ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
        for (IJavaProject project : projects) {
          if (!GWTNature.isGWTProject(project.getProject())
              || GWTProjectsRuntime.isGWTRuntimeProject(project)) {
            continue;
          }

          try {
            GWTRuntime sdk = GWTRuntime.findSdkFor(project);
            if (sdk != null
                && WebAppUtilities.hasManagedWarOut(project.getProject())) {
              new GWTUpdateWebInfFolderCommand(project, sdk).execute();
            }
          } catch (FileNotFoundException e) {
            GWTPluginLog.logError(e);
          } catch (BackingStoreException e) {
            GWTPluginLog.logError(e);
          }
        }

        ClasspathContainerUpdateJob classpathContainerUpdateJob = new ClasspathContainerUpdateJob(
            "ClasspathContainerUpdateJob", GWTRuntimeContainer.CONTAINER_ID);
        classpathContainerUpdateJob.schedule();
      }
    });
  }

  /**
   * Returns the folder path where the genfiles from the gwt compile are
   * located. There is no set method, but the user could manually change the
   * folder name by editing the value in the preferences file.
   * 
   * @return The folder name where the genfiles from the gwt compile are
   *         located.
   */
  public static IPath computeSpeedTracerGeneratedFolderPath(IPath warOutLocation) {
    String folderName = getEclipsePreferences().get(SPEEDTRACER_GEN_FOLDER_NAME, "genfilesforspeedtracer");
    return warOutLocation.append(folderName);
  }

  public static GWTRuntime getDefaultRuntime() {
    SdkSet<GWTRuntime> sdkSet = getSdks();
    return sdkSet.getDefault();
  }

  /**
   * Return true if the JSO detail formatter is configured for the current
   * workspace.
   */
  public static boolean getJsoDetailFormatting() {
    // Unfortunately we cannot use
    // JavaDetailFormattersManager.hasAssociatedDetailFormatter() here because
    // it requires an IJavaType which cannot be resolved without a known
    // context.
    String[] formatters = JavaDebugOptionsManager.parseList(JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(
        IJDIPreferencesConstants.PREF_DETAIL_FORMATTERS_LIST));

    for (String formatter : formatters) {
      if (JSO_PROPERTY_TYPE.equals(formatter)) {
        return true;
      }
    }

    return false;
  }

  /**
   * @return If terminated launches should be cleared from the devmode view
   * when a new launch is launched.
   */
  public static boolean getRemoveTerminatedLaunches() {
    return getEclipsePreferences().getBoolean(REMOVE_TERMINATED_LAUNCHES, true);
  }

  public static GWTRuntime getRuntime(String name) {
    SdkSet<GWTRuntime> sdkSet = getSdks();
    for (GWTRuntime runtime : sdkSet) {
      if (runtime.getName().equals(name)) {
        return runtime;
      }
    }

    return null;
  }

  public static SdkManager<GWTRuntime> getSdkManager() {
    return sdkManager;
  }

  public static SdkSet<GWTRuntime> getSdks() {
    return getSdkManager().getSdks();
  }

  /**
   * Returns the port to use for the SourceViewerServer.
   * 
   * @return the port to use for the SourceViewerServer
   */
  public static int getSourceViewerServerPort() {
    return getEclipsePreferences().getInt(SOURCE_VIEWER_SERVER_PORT, 50313);
  }

  public static boolean getUiBinderWizardGenerateContentDefault() {
    return GWTPlugin.getDefault().getPluginPreferences().getBoolean(
        GwtPreferenceConstants.UIBINDER_WIZARD_GENERATE_CONTENT_DEFAULT);
  }

  public static boolean hasRuntime(String name) {
    return (getRuntime(name) != null);
  }

  /**
   * updates the JSO detail formatter for the current workspace.
   */
  public static void setJsoDetailFormatting(boolean formatting) {
    DetailFormatter formatter = new DetailFormatter(JSO_PROPERTY_TYPE,
        "toString()", true);
    if (formatting) {
      JavaDetailFormattersManager.getDefault().setAssociatedDetailFormatter(
          formatter);
      JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue(
          IJDIPreferencesConstants.PREF_SHOW_DETAILS,
          IJDIPreferencesConstants.INLINE_FORMATTERS);
    } else {
      JavaDetailFormattersManager.getDefault().removeAssociatedDetailFormatter(
          formatter);
    }
  }

  /**
   * Sets whether terminated launches should be cleared from the devmode view
   * when a new launch is launched
   */
  public static void setRemoveTerminatedLaunches(boolean remove) {
    IEclipsePreferences workspacePreferences = getEclipsePreferences();
    workspacePreferences.putBoolean(REMOVE_TERMINATED_LAUNCHES, remove);
    try {
      workspacePreferences.flush();
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
    }
  }
  
  public static void setSdks(SdkSet<GWTRuntime> sdkSet) {
    try {
      getSdkManager().setSdks(sdkSet);
    } catch (CoreException e) {
      // TODO: Should we let the exception escape?
      GWTPluginLog.logError(e);
    }
  }

  /**
   * Sets the port number to use for the SourceViewerServer.
   * 
   * @param port the port number to save
   */
  public static void setSourceViewerServerPort(int port) {
    IEclipsePreferences workspacePreferences = getEclipsePreferences();
    workspacePreferences.putInt(SOURCE_VIEWER_SERVER_PORT, port);
    try {
      workspacePreferences.flush();
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
    }
  }
  
  public static void setUiBinderWizardGenerateContentDefault(
      boolean generateComments) {
    GWTPlugin.getDefault().getPluginPreferences().setValue(
        GwtPreferenceConstants.UIBINDER_WIZARD_GENERATE_CONTENT_DEFAULT,
        generateComments);
  }

  private static IEclipsePreferences getEclipsePreferences() {
    InstanceScope scope = new InstanceScope();
    IEclipsePreferences workspacePrefs = scope.getNode(GWTPlugin.PLUGIN_ID);
    return workspacePrefs;
  }

  private GWTPreferences() {
  }
}
