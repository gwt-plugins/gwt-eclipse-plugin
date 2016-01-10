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

import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * Launch configuration working copy
 */
public class GWTLaunchConfigurationWorkingCopy extends GWTLaunchConfiguration {

  public static void clearAttribute(ILaunchConfigurationWorkingCopy workingCopy, GWTLaunchAttributes launchAttribute) {
    workingCopy.setAttribute(launchAttribute.getQualifiedName(), (String) null);
  }

  public static void removeSuperDevModeEnabled(ILaunchConfigurationWorkingCopy configuration) {
    configuration.removeAttribute(GWTLaunchAttributes.SUPERDEVMODE_ENABLED.getQualifiedName());
  }

  public static void setClassicDevModeCodeServerPort(ILaunchConfigurationWorkingCopy workingCopy, String port) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.CLASSIC_DEVMODE_CODE_SERVER_PORT, port);
  }

  public static void setClassicDevModeCodeServerPortAuto(ILaunchConfigurationWorkingCopy workingCopy, boolean auto) {
    setBooleanAttribute(workingCopy, GWTLaunchAttributes.CLASSIC_DEVMODE_CODE_SERVER_PORT_AUTO, auto);
  }

  public static void setSdmCodeServerPort(ILaunchConfigurationWorkingCopy workingCopy, String port) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.SDM_CODE_SERVER_PORT, port);
  }

  public static void setSdmCodeServerPortAuto(ILaunchConfigurationWorkingCopy workingCopy, boolean auto) {
    setBooleanAttribute(workingCopy, GWTLaunchAttributes.SDM_CODE_SERVER_PORT_AUTO, auto);
  }

  public static void setCodeServerLauncherDir(ILaunchConfigurationWorkingCopy workingCopy, String launcherDir) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.CODE_SERVER_LAUNCHER_DIR, launcherDir);
  }

  /**
   * Sets the persisted set of entry point modules.
   *
   * @param entryPointModules
   */
  public static void setEntryPointModules(ILaunchConfigurationWorkingCopy workingCopy, List<String> entryPointModules,
      List<String> defaultValue) {
    setListAttribute(workingCopy, GWTLaunchAttributes.ENTRY_POINT_MODULES.getQualifiedName(), entryPointModules,
        defaultValue);
  }

  public static void setLogLevel(ILaunchConfigurationWorkingCopy workingCopy, String logLevel) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.LOG_LEVEL, logLevel);
  }

  public static void setSdkContainerPath(ILaunchConfigurationWorkingCopy workingCopy, String sdkContainerPath) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.SDK_CONTAINER_PATH, sdkContainerPath);
  }

  public static void setSuperDevModeEnabled(ILaunchConfigurationWorkingCopy configuration, boolean enabled) {
    setBooleanAttribute(configuration, GWTLaunchAttributes.SUPERDEVMODE_ENABLED, enabled);
  }

  public static void setStartupUrl(ILaunchConfigurationWorkingCopy workingCopy, String startupUrl) {
    setStringAttribute(workingCopy, GWTLaunchAttributes.URL, startupUrl);
  }

  private static void clearAttribute(ILaunchConfigurationWorkingCopy workingCopy, String qualifiedAttributeName) {
    workingCopy.setAttribute(qualifiedAttributeName, (String) null);
  }

  private static void setBooleanAttribute(ILaunchConfigurationWorkingCopy workingCopy,
      GWTLaunchAttributes launchAttribute, boolean newValue) {
    String attributeQualifiedName = launchAttribute.getQualifiedName();
    if (shouldClearAttribute(launchAttribute.getDefaultValue(), newValue)) {
      clearAttribute(workingCopy, attributeQualifiedName);
    } else {
      workingCopy.setAttribute(attributeQualifiedName, newValue);
    }
  }

  private static void setListAttribute(ILaunchConfigurationWorkingCopy workingCopy, String qualifiedAttributeName,
      List<String> newValue, List<String> defaultValue) {
    if (shouldClearAttribute(defaultValue, newValue)) {
      clearAttribute(workingCopy, qualifiedAttributeName);
    } else {
      workingCopy.setAttribute(qualifiedAttributeName, newValue);
    }
  }

  private static void setStringAttribute(ILaunchConfigurationWorkingCopy workingCopy,
      GWTLaunchAttributes launchAttribute, String newValue) {
    String attributeQualifiedName = launchAttribute.getQualifiedName();
    if (shouldClearAttribute(launchAttribute.getDefaultValue(), newValue)) {
      clearAttribute(workingCopy, attributeQualifiedName);
    } else {
      workingCopy.setAttribute(attributeQualifiedName, newValue);
    }
  }

  private static boolean shouldClearAttribute(Object defaultValue, Object newValue) {
    if (defaultValue == newValue) {
      return true;
    }

    if (defaultValue != null) {
      return defaultValue.equals(newValue);
    }

    return false;
  }

  public static void setMainType(ILaunchConfigurationWorkingCopy workingCopy, String mainType) {
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainType);
  }

  public static String getMainType(ILaunchConfigurationWorkingCopy workingCopy) throws CoreException {
    return LaunchConfigurationProcessorUtilities.getMainTypeName(workingCopy);
  }

}
