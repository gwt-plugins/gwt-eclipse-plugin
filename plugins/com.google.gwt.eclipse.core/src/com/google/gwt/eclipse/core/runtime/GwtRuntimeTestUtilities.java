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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

/**
 * Testing-related utility methods dealing with GWT runtime.
 * 
 * This lives in gwt.core so gdt.suite.test can use it.
 */
public class GwtRuntimeTestUtilities {

  public static void addDefaultRuntime() throws Exception {
    String gaeHome = System.getenv("GWT_HOME");
    if (gaeHome == null) {
      throw new RuntimeException("The GWT_HOME environment variable is not set");
    }

    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      assert (sdkSet.size() == 0);

      GWTRuntime sdk = GWTRuntime.getFactory().newInstance("Default GWT SDK", new Path(gaeHome));
      IStatus status = sdk.validate();
      if (!status.isOK()) {
        throw new CoreException(status);
      }

      sdkSet.add(sdk);
      GWTPreferences.setSdks(sdkSet);
    }
  }

  public static GWTJarsRuntime getDefaultRuntime() throws Exception {
    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      throw new Exception(
          "No default runtime has been set! Did you forget to call GwtRuntimeTestUtilities.addDefaultRuntime()?");
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
    IPath gwtProjectsDir =
        workspace.getPathVariableManager().getValue("GWT_ROOT").append("eclipse");

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
