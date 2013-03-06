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

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
public class GWTLaunchConfiguration {
  public static List<String> computeCompileDynamicVMArgsAsList(
      IJavaProject javaProject, boolean wantsTransitionalOOPHM)
      throws CoreException {

    return computeDynamicVMArgs(javaProject, wantsTransitionalOOPHM);
  }

  public static List<String> computeJunitDynamicVMArgsAsList(
      IJavaProject javaProject, boolean wantsTransitionalOOPHM)
      throws CoreException {
    List<String> out = computeDynamicVMArgs(javaProject,
        wantsTransitionalOOPHM);

    // Default the maximum heap size to 512 MB
    out.add("-Xmx512m");

    return out;
  }

  public static List<String> computeJunitDynamicVMArgsAsList(
      IJavaProject javaProject, ILaunchConfiguration configuration)
      throws CoreException {
    return computeJunitDynamicVMArgsAsList(javaProject,
        launchWithTransitionalOophm(configuration));
  }

  public static String computeJunitDynamicVMArgsAsString(IJavaProject javaProject,
      ILaunchConfiguration configuration) throws CoreException {
    return StringUtilities.join(computeJunitDynamicVMArgsAsList(javaProject,
        configuration), " ");
  }

  public static String getCodeServerPort(ILaunchConfiguration launchConfiguration) 
      throws CoreException {
    return getStringAttribute(launchConfiguration, GWTLaunchAttributes.CODE_SERVER_PORT);
  }
  
  public static boolean getCodeServerPortAuto(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return getBooleanAttribute(launchConfiguration, GWTLaunchAttributes.CODE_SERVER_PORT_AUTO);
  }
  
  /**
   * @return the persisted entry point modules, or an empty list if the default
   *         modules should be used
   * @see com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor#getDefaultModules(org.eclipse.core.resources.IProject project, ILaunchConfiguration configuration)
   */
  public static List<String> getEntryPointModules(
      ILaunchConfiguration launchConfiguration) throws CoreException {
    return getListAttribute(launchConfiguration,
        GWTLaunchAttributes.ENTRY_POINT_MODULES);
  }

  public static String getLogLevel(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return getStringAttribute(launchConfiguration,
        GWTLaunchAttributes.LOG_LEVEL);
  }

  public static String getOutputStyle(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return getStringAttribute(launchConfiguration,
        GWTLaunchAttributes.OUTPUT_STYLE);
  }

  public static IPath getSdkContainerPath(
      ILaunchConfiguration launchConfiguration) throws CoreException {
    String pathAsString = getStringAttribute(launchConfiguration,
        GWTLaunchAttributes.SDK_CONTAINER_PATH);
    return new Path(pathAsString);
  }

  public static String getStartupUrl(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return getStringAttribute(launchConfiguration, GWTLaunchAttributes.URL);
  }

  public static boolean launchWithTransitionalOophm(
      ILaunchConfiguration launchConfiguration) throws CoreException {
    return getBooleanAttribute(launchConfiguration,
        GWTLaunchAttributes.LAUNCH_WITH_OOPHM);
  }

  /**
   * Dynamic VM args common to both computeJUnitDynamicVMArgsAsList and
   * computeCompileDynamicVMArgsAsList.
   */
  private static List<String> computeDynamicVMArgs(
      IJavaProject javaProject, boolean wantsTransitionalOOPHM)
      throws CoreException {
    ArrayList<String> out = new ArrayList<String>();

    String devJarPath = maybeGetDevJarPath(javaProject);
    if (devJarPath.length() > 0) {
      out.add("-Dgwt.devjar=\"" + devJarPath + '"');
    }

    return out;
  }

  private static boolean getBooleanAttribute(
      ILaunchConfiguration launchConfiguration,
      GWTLaunchAttributes launchAttribute) throws CoreException {
    return launchConfiguration.getAttribute(launchAttribute.getQualifiedName(),
        ((Boolean) launchAttribute.getDefaultValue()).booleanValue());
  }

  @SuppressWarnings("unchecked")
  private static List<String> getListAttribute(
      ILaunchConfiguration launchConfiguration,
      GWTLaunchAttributes launchAttribute) throws CoreException {
    return launchConfiguration.getAttribute(launchAttribute.getQualifiedName(),
        Collections.emptyList());
  }

  private static String getStringAttribute(
      ILaunchConfiguration launchConfiguration,
      GWTLaunchAttributes launchAttribute) throws CoreException {
    // TODO: Unify this method which is duped in GaeLaunchConfiguration,
    // WebAppLaunchConfiguration and GWTLaunchConfiguration
    return launchConfiguration.getAttribute(launchAttribute.getQualifiedName(),
        (String) launchAttribute.getDefaultValue());
  }

  /**
   * Returns the path to the gwt-dev-xxx.jar in the event that the launch
   * configuration depends on a GWT Contributor Runtime. Otherwise, returns the
   * empty string.
   */
  private static String maybeGetDevJarPath(IJavaProject project) {

    /*
     * In order to figure out whether or not to add the -Dgwt.devjar argument to
     * the list of VM args, we have to figure out the runtime that this launch
     * configuration depends on. If the project is one of the GWT Runtime
     * projects, then we'll definitely have to add the -Dgwt.devjar argument to
     * the launch configuration.
     */
    try {
      if (GWTProjectsRuntime.isGWTRuntimeProject(project)) {
        // Synthesize a temporary contributor SDK so that we can use it
        // to compute the devjar path
        GWTRuntime tempContribSDK = GWTProjectsRuntime.syntheziseContributorRuntime();

        if (tempContribSDK.validate().isOK()) {
          return tempContribSDK.getDevJar().getAbsolutePath();
        } else {
          return "";
        }
      }

      GWTRuntime sdk = GWTRuntime.findSdkFor(project);
      if (sdk.usesGwtDevProject()) {
        File gwtDevJarFile = sdk.getDevJar();
        return gwtDevJarFile.getAbsolutePath();
      }
    } catch (SdkException sdke) {
      GWTPluginLog.logError(sdke,
          "Unable to extract gwt dev jar argument from GWTProjectsRuntime");
    } catch (JavaModelException jme) {
      GWTPluginLog.logError(jme,
          "Unable to extract gwt dev jar argument from GWTProjectsRuntime");
    }
    return "";
  }
}
