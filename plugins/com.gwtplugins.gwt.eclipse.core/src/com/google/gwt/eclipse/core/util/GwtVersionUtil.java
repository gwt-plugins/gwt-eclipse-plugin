/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.util;

import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.jdt.core.IJavaProject;

public class GwtVersionUtil {
  /**
   * Returns if the version-containing directory name is >= GWT 2.5.
   */
  public static boolean isGwtVersionKnownAndAtLeast25(String directoryName) {
    // The convention for GWT installation directories is "gwt-<major>.<minor>.<micro>".
    if (!directoryName.startsWith("gwt-")) {
      return false;
    }
    return SdkUtils.compareVersionStrings(directoryName.substring(4), "2.5.0") >= 0;
  }

  /**
   * Returns if the project is < GWT 2.5.
   */
  public static boolean isGwtVersionlessThan25(IJavaProject javaProject) {
    String gwtVersionCompareTo = "2.5.0";
    String gwtVersionCurrentProject = getProjectGwtVersion(javaProject);
    int comparison = SdkUtils.compareVersionStrings(gwtVersionCurrentProject, gwtVersionCompareTo);

    return comparison < 0;
  }

  /**
   * Returns if the project is >= GWT 2.7.
   */
  public static boolean isGwtVersionGreaterOrEqualTo27(IJavaProject javaProject) {
    String gwtVersionCompareTo = "2.7.0";
    String gwtVersionCurrentProject = getProjectGwtVersion(javaProject);
    int comparison = SdkUtils.compareVersionStrings(gwtVersionCompareTo, gwtVersionCurrentProject);

    return comparison >= 0;
  }

  /**
   * Returns the default GWT version for project or it returns '0.0.0' if nothing has been found.
   *
   * @return GWT SDK version.
   */
  public static String getProjectGwtVersion(IJavaProject javaProject) {
    GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);

    // Provide a version so npe gaurds don't have to be applied to all the callers.
    String version = "999.999.999";
    if (sdk != null) {
      version = sdk.getVersion();
    } else {
      GWTPluginLog.logWarning("GWT SDK not found");
    }

    return version;
  }

}
