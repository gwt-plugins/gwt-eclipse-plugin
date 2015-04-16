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
package com.google.gwt.eclipse.testing;

import com.google.gcp.eclipse.testing.TestEnvironmentUtil;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTJarsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import java.net.URI;

/**
 * Testing-related utility methods dealing with GWT runtime.
 */
public class GwtRuntimeTestUtilities {
  /**
   * Adds a default GWT runtime to the {@link GWTPreferences}. Uses the {@code GWT_HOME} environment
   * variable if it is set, otherwise extracts a GWT SDK from this testing bundle and sets
   * {@code GWT_HOME} to the location where it is extracted.
   */
  public static void addDefaultRuntime() throws Exception {
    String gwtHomePath = System.getenv("GWT_HOME");
    if (gwtHomePath == null) {
      System.out.println("The GWT_HOME environment variable is not set, using test bundle version");
      gwtHomePath = getGwtTestSdkPath();
      TestEnvironmentUtil.updateEnvironmentVariable("GWT_HOME", gwtHomePath);
      System.out.println("The GWT_HOME environment variable is now set. GWT_HOME=" + gwtHomePath);
    }

    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      assert (sdkSet.size() == 0);

      GWTRuntime sdk =
          GWTRuntime.getFactory().newInstance("Default GWT SDK", new Path(gwtHomePath));
      IStatus status = sdk.validate();
      if (!status.isOK()) {
        throw new CoreException(status);
      }

      sdkSet.add(sdk);
      GWTPreferences.setSdks(sdkSet);
    }
  }

  /**
   * Get the gwt zip path.
   */
  private static String getGwtTestSdkPath() {
    Bundle bundle = GwtTestingPlugin.getDefault().getBundle();
    String basePath =
        GwtRuntimeTestUtilities.class.getProtectionDomain().getCodeSource().getLocation()
            .getPath();
    String version = TestEnvironmentUtil.getMavenPropertyVersionFor("gwt.version");
    String pathToZip = basePath + "../../resources/target/resources/gwt-" + version + ".zip";
    String gwtHomePath = TestEnvironmentUtil.installTestSdk(bundle, pathToZip).toOSString();
    return gwtHomePath;
  }

  public static GWTJarsRuntime getDefaultRuntime() throws Exception {
    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      throw new Exception("No default runtime has been set! "
          + "Did you forget to call GwtRuntimeTestUtilities.addDefaultRuntime()?");
    }

    return (GWTJarsRuntime) sdkSet.getDefault();
  }

  /**
   * Imports the gwt-dev and gwt-user projects into the workspace.
   *
   * @see #removeGwtSourceProjects()
   */
  public static void importGwtSourceProjects() throws CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    String gwtDevProjectName = "gwt-dev";

    // Get the path to the Eclipse project files for GWT
    URI gwtProjectsUri = workspace.getPathVariableManager().getURIValue("GWT_ROOT");
    IPath gwtProjectsDir = URIUtil.toPath(gwtProjectsUri).append("eclipse");

    // Import gwt-dev
    IPath gwtDevDir = gwtProjectsDir.append("dev");
    ProjectUtilities.importProject(gwtDevProjectName, gwtDevDir);

    // Import gwt-user
    IPath gwtUserDir = gwtProjectsDir.append("user");
    ProjectUtilities.importProject("gwt-user", gwtUserDir);
  }

  public static void removeDefaultRuntime() {
    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    sdkSet.remove(sdkSet.getDefault());
    GWTPreferences.setSdks(sdkSet);
  }

  /**
   * Removes the gwt-dev and gwt-user projects from the workspace.
   *
   * @see #importGwtSourceProjects()
   */
  public static void removeGwtSourceProjects() throws CoreException {
    ProjectUtilities.deleteProject("gwt-user", false);
    ProjectUtilities.deleteProject("gwt-dev", false);
  }
}
