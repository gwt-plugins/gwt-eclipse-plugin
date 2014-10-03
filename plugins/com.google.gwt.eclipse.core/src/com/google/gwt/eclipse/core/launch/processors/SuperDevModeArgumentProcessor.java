/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the -superDevMode arg.
 */
public class SuperDevModeArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String ERROR_NO_JAR_CODESERVER = "Add " + GWTRuntime.GWT_CODESERVER_JAR
      + " to the classpath.";
  public static final String ERROR_NO_JAR_GWTDEV = "Add " + GWTRuntime.GWT_DEV_NO_PLATFORM_JAR
      + " to the classpath.";
  public static final String ERROR_NO_LEGACY_LAUNCHER_JAR =
      "Add the super dev mode legacy jar for 2.5.0 to < 2.7 jar, "
          + GWTLaunchConstants.SUPERDEVMODE_LEGACY_LAUNCHER_JAR + " to the classpath.";
  public static final String ERROR_NO_LINKER =
      "Add super dev mode linker <add-linker name=\"xsiframe\" /> to project module.";
  public static final String ERROR_NO_LINKER_NOSOURCEMAP =
      "Add <set-property name=\"compiler.useSourceMaps\" value=\"true\" /> to project module.";
  public static final String ERROR_NO_LINKER_REDIRECT =
      "Add <set-configuration-property name=\"devModeRedirectEnabled\" value=\"true\"/> to project module.";
  public static final String ERRORS = "Errors exist:";

  public static final String SUPERDEVMODE_ENABLED_ARG = "-superDevMode";
  public static final String SUPERDEVMODE_DISABLED_ARG = "-nosuperDevMode";

  @Override
  public void update(ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // GWT arg processor only
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return;
    }

    boolean superDevModeEnabled =
        GWTLaunchConfigurationWorkingCopy.getSuperDevModeEnabled(launchConfig);

    String gwtVersion = getGwtVersion(javaProject);

    boolean legacySuperDevModePossibleForGwtVersion =
        GWTLaunchConstants.SUPERDEVMODE_LAUNCH_LEGACY_VERSIONS.contains(gwtVersion);

    // -superDevMode index
    int indexEnabled = programArgs.indexOf(SUPERDEVMODE_ENABLED_ARG);

    // -nosuperDevMode index
    int indexDisabled = programArgs.indexOf(SUPERDEVMODE_DISABLED_ARG);

    // Remove enabled arg (changing back in forth between dev mode or super dev mode)
    if (!superDevModeEnabled && indexEnabled > -1) {
      programArgs.remove(indexEnabled);
    }

    // Remove disabled arg (changing back in forth between dev mode or super dev mode)
    if (superDevModeEnabled && indexDisabled > -1) {
      programArgs.remove(indexDisabled);
    }

    if (legacySuperDevModePossibleForGwtVersion) {
      // Super dev mode is on by default, on: -superDevmode off: nothing
      if (superDevModeEnabled && indexEnabled < 0) {
        programArgs.add(0, SUPERDEVMODE_ENABLED_ARG);
      }

    } else {
      // TODO in the future set nothing for Super Dev Mode
      // Super dev mode is on by default, on: -superDevmode (TODO nothing) off: -nosuperDevMode
      if (superDevModeEnabled && indexEnabled < 0) {
        programArgs.add(0, SUPERDEVMODE_ENABLED_ARG);
      } else if (!superDevModeEnabled && indexDisabled < 0) {
        programArgs.add(0, SUPERDEVMODE_DISABLED_ARG);
      }
    }
  }

  /**
   * Validates the super dev mode requirements exist, returning a string describing all validation
   * errors or null if all requirements are met.
   */
  @Override
  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    // skip this if its not enabled
    boolean superDevModeEnabled =
        GWTLaunchConfigurationWorkingCopy.getSuperDevModeEnabled(launchConfig);
    if (!superDevModeEnabled) {
      return null;
    }

    List<String> errors = new ArrayList<String>();

    // validate gwt-codeserver.jar exists
    String error = validateCodeServer(javaProject);
    if (error != null) {
      errors.add(error);
    }

    // validate gwt-dev[.*].jar exists
    error = validateGwtDev(javaProject);
    if (error != null) {
      errors.add(error);
    }

    // validate super dev mode linker exists in project.gwt.xml
    error = validateLinkerExists(javaProject, launchConfig);
    if (error != null) {
      errors.add(error);
    }

    // some errors exist tell the user
    if (errors.size() > 0) {
      return ERRORS + " " + StringUtilities.join(errors, ", ");
    }

    return null;
  }

  private String getGwtVersion(IJavaProject javaProject) {
    GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
    return sdk.getVersion();
  }

  private String validateCodeServer(IJavaProject javaProject) {
    boolean alreadyExists = false;
    try {
      IType type =
          javaProject
              .findType(GwtLaunchConfigurationProcessorUtilities.SUPERDEVMODE_MODE_MAIN_TYPE);
      if (type != null) {
        alreadyExists = true;
      }
    } catch (JavaModelException e) {}
    if (!alreadyExists) {
      return ERROR_NO_JAR_CODESERVER;
    }
    return null;
  }

  private String validateGwtDev(IJavaProject javaProject) {
    boolean alreadyExists = false;
    try {
      IType type = javaProject.findType("com.google.gwt.dev.About");
      if (type != null) {
        alreadyExists = true;
      }
    } catch (JavaModelException e) {}
    if (!alreadyExists) {
      return ERROR_NO_JAR_GWTDEV;
    }
    return null;
  }

  private String validateLinkerExists(IJavaProject javaProject, ILaunchConfiguration configuration) {
    try {
      List<String> modules = GWTLaunchConfigurationWorkingCopy.getEntryPointModules(configuration);
      if (modules == null || modules.isEmpty()) {
        return ERROR_NO_LINKER;
      }
      List<String> errors = new ArrayList<String>();
      for (String module : modules) {
        IModule imodule = ModuleUtils.findModule(javaProject, module, false);
        if (imodule != null) {
          String error = validateModule(imodule, javaProject, configuration);
          if (error != null) {
            errors.add(error);
          }
        }
      }
      if (errors.size() > 0) {
        return StringUtilities.join(errors, ", ");
      }
    } catch (CoreException e) {
      return ERROR_NO_LINKER;
    }

    return null;
  }

  /**
   * Validate gwt project module has linker setup for super dev mode.
   *
   * <!-- GWT 2.6.0 only needs this --> <add-linker name="xsiframe" />
   *
   * <!-- GWT 2.5.0+ also needs this --> <set-configuration-property name="devModeRedirectEnabled"
   * value="true"/> <set-property name="compiler.useSourceMaps" value="true" />
   *
   * @param imodule the gwt project module.
   * @param javaProject the java project.
   * @param configuration the launch config.
   * @return an error string if things are wrong, or null if the module is valid.
   */
  private String validateModule(IModule imodule, IJavaProject javaProject,
      ILaunchConfiguration configuration) {
    List<String> linkers = imodule.getAddLinkers();
    List<String> redirects = imodule.getSetConfigurationProperty("devModeRedirectEnabled");
    List<String> useSourceMaps = imodule.getSetConfigurationProperty("compiler.useSourceMaps");

    String gwtVersion = getGwtVersion(javaProject);
    boolean legacySuperDevModePossibleForGwtVersion =
        GWTLaunchConstants.SUPERDEVMODE_LAUNCH_LEGACY_VERSIONS.contains(gwtVersion);

    List<String> errors = new ArrayList<String>();

    // check for xsiframe linker
    if (legacySuperDevModePossibleForGwtVersion
        && (linkers == null || linkers.indexOf(GWTLaunchConstants.SUPERDEVMODE_LINKER_NAME) < 0)) {
      errors.add(ERROR_NO_LINKER);
    }

    // check for additional properties in 2.5.0 to < 2.6.0
    if (gwtVersion.contains("2.5") && redirects == null) {
      errors.add(ERROR_NO_LINKER_REDIRECT);
    } else if (gwtVersion.contains("2.5") && redirects != null) {
      for (String redirect : redirects) {
        // could be more than one, so this may need to be more specific
        if (redirect.toLowerCase().equals("false")) {
          errors.add(ERROR_NO_LINKER_REDIRECT);
          break;
        }
      }

      for (String useSourceMap : useSourceMaps) {
        // could be more than one, so this may need to be more specific
        if (useSourceMap.toLowerCase().equals("false")) {
          errors.add(ERROR_NO_LINKER_NOSOURCEMAP);
          break;
        }
      }
    }

    if (errors.size() > 0) {
      return StringUtilities.join(errors, ", ");
    }

    return null;
  }

}
