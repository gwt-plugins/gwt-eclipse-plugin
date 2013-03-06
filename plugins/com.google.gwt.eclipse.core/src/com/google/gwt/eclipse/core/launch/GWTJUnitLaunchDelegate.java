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
package com.google.gwt.eclipse.core.launch;

import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

/**
 * Launch configuration for GWT JUnit tests.
 */
public class GWTJUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate {

  public static final String LAUNCH_TYPE_ID = GWTPlugin.PLUGIN_ID
      + ".launch.gwtJUnit";

  public static boolean getStandardsMode(ILaunchConfiguration config) {
    // Default to false, so we do not add any arguments
    final boolean fallbackValue = false;

    Throwable t = null;
    try {
      if (!isGwtSdkVersionAtLeast("2.0.0", config)) {
        // Does not exist prior to GWT 2.0.0
        return false;
      }
      
      return config.getAttribute(GWTLaunchConstants.ATTR_STANDARDS_MODE,
          fallbackValue);

    } catch (NumberFormatException e) {
      t = e;
    } catch (JavaModelException e) {
      t = e;
    } catch (CoreException e) {
      if (e.getStatus().getSeverity() > IStatus.INFO) {
        t = e;
      }
    }

    if (t != null) {
      GWTPluginLog.logError(t,
          "Could not determine whether to use standards mode");
    }

    return fallbackValue;
  }

  /**
   * @return true if the GWT SDK version is at least the given version or is a
   *         projects runtime
   * @throws CoreException
   */
  public static boolean isGwtSdkVersionAtLeast(String minVersion,
      ILaunchConfiguration config) throws CoreException {

    IJavaProject javaProject = LaunchConfigurationUtilities.getJavaProject(config);
    if (javaProject == null) {
      throw new CoreException(new Status(IStatus.INFO, GWTPlugin.PLUGIN_ID,
          "No Java project set on launch configuration"));
    }

    GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
    if (sdk == null || !sdk.validate().isOK()) {
      throw new CoreException(new Status(IStatus.INFO, GWTPlugin.PLUGIN_ID,
          "GWT SDK not found or isn't valid"));
    }

    String version = sdk.getVersion();
    return version.equals(GWTProjectsRuntime.VERSION)
        || SdkUtils.compareVersionStrings(version, minVersion) >= 0;
  }

  public static boolean isJUnitLaunchConfig(
      ILaunchConfigurationType launchConfigType) {
    return LAUNCH_TYPE_ID.equals(launchConfigType.getIdentifier());
  }

  /**
   * Returns gwt-specific program arguments, such as -logLevel, -style, etc.
   * 
   * TODO: Get rid of this method and references to GWTLaunchConstants in favor
   * of using the GWTLaunchConfiguration and WebAppLaunchConfiguration classes.
   * When doing this, keep in mind that the execution of a GWT JUnit test is
   * identical for both GWT 1.5 and GWT 1.6 - the war directory does not come
   * into play.
   */
  private static String getGWTJUnitProgramArgs(ILaunchConfiguration config)
      throws CoreException {
    StringBuilder argsBuilder = new StringBuilder();
    String logLevel = config.getAttribute(GWTLaunchConstants.ATTR_LOG_LEVEL, "");
    if (logLevel.length() > 0) {
      argsBuilder.append(" -logLevel ");
      argsBuilder.append(logLevel);
    }

    String obfuscation = config.getAttribute(
        GWTLaunchConstants.ATTR_OBFUSCATION, "");
    if (obfuscation.length() > 0) {
      argsBuilder.append(" -style ");
      argsBuilder.append(obfuscation);
    }

    String outDir = config.getAttribute(GWTLaunchConstants.ATTR_OUT_DIR, "");
    if (outDir.length() > 0) {
      argsBuilder.append(" -out ");
      argsBuilder.append("\"" + outDir + "\"");
    }

    boolean notHeadless = config.getAttribute(
        GWTLaunchConstants.ATTR_NOT_HEADLESS, false);
    if (notHeadless) {
      argsBuilder.append(" -notHeadless");
    }

    String webMode = config.getAttribute(GWTLaunchConstants.ATTR_WEB_MODE,
        GWTLaunchConstants.Defaults.WEB_MODE);
    if (Boolean.parseBoolean(webMode)) {
      argsBuilder.append(" -web");
    }

    if (getStandardsMode(config)) {
      try {
        if (isGwtSdkVersionAtLeast("2.0.1", config)) {
          argsBuilder.append(" -standardsMode");
        } else {
          argsBuilder.append(" -XstandardsMode");
        }
      } catch (CoreException e) {
        GWTPluginLog.logWarning(
            e,
            "Could not determine whether to use -XstandardsMode or -standardsMode, assuming latter");
        argsBuilder.append(" -standardsMode");
      }
    }

    return argsBuilder.toString().trim();
  }

  /**
   * Returns the GWT-specific arguments, such as -Dgwt.args="-logLevel ...",
   * followed by -XstartOnFirstThread and -Dgwt.devjar="/path...", with the
   * default VM arguments appearing at the end.
   */
  @Override
  public String getVMArguments(ILaunchConfiguration configuration)
      throws CoreException {
    IJavaProject javaProject = verifyJavaProject(configuration);
    /*
     * Under JUnit, gwt-specific program arguments need to be passed as a VM
     * argument.
     */
    String gwtArgs = getGWTJUnitProgramArgs(configuration);
    if (gwtArgs.length() > 0) {
      gwtArgs = " -Dgwt.args=\"" + gwtArgs + '"';
    }

    String dynamicVMArgs = GWTLaunchConfiguration.computeJunitDynamicVMArgsAsString(
        javaProject, configuration);
    gwtArgs += ' ' + dynamicVMArgs;

    /*
     * Make sure that user-specified VM Args appear at the end of the list; that
     * way, if the user defines any system properties using -D, their
     * definitions will override ours
     */
    return gwtArgs + ' ' + super.getVMArguments(configuration);
  }

}
