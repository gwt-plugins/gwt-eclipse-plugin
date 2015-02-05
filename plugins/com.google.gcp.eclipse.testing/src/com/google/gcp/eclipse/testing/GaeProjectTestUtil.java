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
package com.google.gcp.eclipse.testing;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateProjectSdkCommand;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Testing-related utility methods dealing with GAE SDKs.
 */
public class GaeProjectTestUtil {
  private static final String APPENGINE_DEPENDENCIES_PLUGIN_NAME =
      "com.google.appengine.eclipse.dependencies";

  /**
   * Adds the default GAE SDK. Uses the path pointed to by the GAE_HOME environment variable if that
   * variable is set, otherwise installs an SDK from this bundle and sets the GAE_HOME environment
   * variable to point at it.
   */
  public static void addDefaultSdk() throws CoreException {
    String gaeHome = System.getenv("GAE_HOME");
    if (gaeHome == null) {
      System.out.println("The GAE_HOME environment variable is not set, using test bundle version");
      gaeHome = installGaeTestSdk().toOSString();
      TestEnvironmentUtil.updateEnvironmentVariable("GAE_HOME", gaeHome);
    }

    SdkSet<GaeSdk> sdkSet = GaePreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      assert (sdkSet.size() == 0);

      GaeSdk sdk = GaeSdk.getFactory().newInstance("Default GAE SDK", Path.fromOSString(gaeHome));
      IStatus status = sdk.validate();
      if (!status.isOK()) {
        if (dependenciesPluginIsRunning()) {
          throw new IllegalStateException("The " + APPENGINE_DEPENDENCIES_PLUGIN_NAME
              + " plug-in  must not be in the running environment."
              + "\nUse the launch configuration's 'Plug-ins' tab to disable it.");
        }
        throw new CoreException(status);
      }

      sdkSet.add(sdk);
      GaePreferences.setSdks(sdkSet);
    }
  }

  private static boolean dependenciesPluginIsRunning() {
    return Platform.getBundle(APPENGINE_DEPENDENCIES_PLUGIN_NAME) != null;
  }

  /**
   * Installs the AppEngine SDK that is bundled with this plug-in and returns the absolute path
   * where it was installed.
   */
  public static IPath installGaeTestSdk() {
    return TestEnvironmentUtil.installTestSdk(
        TestingPlugin.getDefault().getBundle(),
        Path.fromPortableString("/resources/appengine-java-sdk-1.9.17.zip"));
  }

  /**
   * Installs the AppEngine SDK that is bundled with this plug-in and returns the absolute path
   * where it was installed.
   */
  public static IPath installGaeTestSdk() {
    return TestEnvironmentUtil.installTestSdk(
        TestingPlugin.getDefault().getBundle(),
        Path.fromPortableString("/resources/appengine-java-sdk-1.9.17.zip"));
  }
  /**
   * Removes the default GAE SDK.
   */
  public static void removeDefaultSdk() {
    SdkSet<GaeSdk> sdkSet = GaePreferences.getSdks();
    sdkSet.remove(sdkSet.getDefault());
    GaePreferences.setSdks(sdkSet);
  }

  /**
   * Creates a Java project configured with the Google AppEngine nature.
   */
  public static GaeProject createGaeProject(String projectName) throws CoreException {
    IJavaProject javaProject = ProjectTestUtil.createProject(projectName);
    IProject project = javaProject.getProject();

    GaeNature.addNatureToProject(project);
    GaeProject gaeProject = GaeProject.create(project);

    try {
      WebAppUtilities.setDefaultWarSettings(project);
      addDefaultSdk();

      UpdateWebInfFolderCommand webInfFolderUpdateCommand =
          new AppEngineUpdateWebInfFolderCommand(javaProject, GaePreferences.getDefaultSdk());
      AppEngineUpdateProjectSdkCommand command = new AppEngineUpdateProjectSdkCommand(javaProject,
          null, GaePreferences.getDefaultSdk(), UpdateType.DEFAULT_CONTAINER,
          webInfFolderUpdateCommand);
      command.execute();
    } catch (BackingStoreException | FileNotFoundException e) {
      throw new CoreException(TestingPlugin.createError(e));
    }

    return gaeProject;
  }
}
