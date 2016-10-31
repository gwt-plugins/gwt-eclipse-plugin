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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.runtime.GwtSdk;
import com.google.gwt.eclipse.core.util.GwtVersionUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the -superDevMode and -noSuperDevMode args.
 *
 * TODO validate argument capitalization -noSuperDevMode which is wrong.
 */
public class SuperDevModeArgumentProcessor implements ILaunchConfigurationProcessor {

  public static final String ERROR_NO_JAR_CODESERVER = "Add " + GwtSdk.GWT_CODESERVER_JAR
      + " to the classpath.";
  public static final String ERROR_NO_JAR_GWTDEV = "Add " + GwtSdk.GWT_DEV_NO_PLATFORM_JAR
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

    // No compiler arg processing
    if (GwtLaunchConfigurationProcessorUtilities.isCompiler(launchConfig)) {
      return;
    }

    String gwtVersion = GwtVersionUtil.getProjectGwtVersion(javaProject);
    boolean superDevModeEnabled =
        GWTLaunchConfigurationWorkingCopy.getSuperDevModeEnabled(launchConfig);
    int indexEnabled = programArgs.indexOf(SUPERDEVMODE_ENABLED_ARG); // -superDevMode index
    int indexDisabled = programArgs.indexOf(SUPERDEVMODE_DISABLED_ARG); // -nosuperDevMode index

    // Update according to GWT version
    if (GwtVersionUtil.isGwtVersionlessThan25(javaProject)) {
      updateLessThanGwt25(programArgs, indexDisabled, indexEnabled);

    } else if (GWTLaunchConstants.SUPERDEVMODE_LAUNCH_LEGACY_VERSIONS.contains(gwtVersion)) {
      updateGwt25toLessThan27(programArgs, indexDisabled, indexEnabled, superDevModeEnabled);

    } else {
      updateGwt27On(javaProject, programArgs, indexDisabled, indexEnabled, superDevModeEnabled);
    }
  }

  /**
   * Validates the super dev mode requirements exist, returning a string describing all validation
   * errors or null if all requirements are met.
   */
  @Override
  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    String gwtVersion = GwtVersionUtil.getProjectGwtVersion(javaProject);

    List<String> errors = new ArrayList<String>();
    // Validate specifically to version
    if (GwtVersionUtil.isGwtVersionlessThan25(javaProject)) {
      // No validation is needed for anything less than GWT 2.5
      return null;

    } else if (GWTLaunchConstants.SUPERDEVMODE_LAUNCH_LEGACY_VERSIONS.contains(gwtVersion)) {
      // Validate for GWT >= 2.5 and GWT < 2.7
      // Validate Super Dev Mode linker exists in project.gwt.xml
      String error = validateSuperDevModeLinkerExists(javaProject, launchConfig);
      if (error != null) {
        errors.add(error);
      }
    }

    // some errors exist tell the user
    if (errors.size() > 0) {
      return ERRORS + " " + StringUtilities.join(errors, ", ");
    }

    return null;
  }

  /**
   * Update program args for projects GWT < 2.5 that can't support Super Dev Mode, be sure that the
   * arguments don't get used ever.
   */
  private void updateLessThanGwt25(List<String> programArgs, int indexDisabled, int indexEnabled) {
    // Always remove args, this prevents changing version issues
    if (indexDisabled > -1) {
      programArgs.remove(indexDisabled);
    }

    if (indexEnabled > -1) {
      programArgs.remove(indexEnabled);
    }
  }

  /**
   * Update program args for project GWT >= 2.5 and GWT < 2.7, use -superDevMode or nothing at all.
   */
  private void updateGwt25toLessThan27(List<String> programArgs, int indexDisabled,
      int indexEnabled, boolean superDevModeEnabled) {
    // Remove enabled arg
    if (indexEnabled > -1) {
      programArgs.remove(indexEnabled);
    }

    // Remove disabled arg
    if (indexDisabled > -1) {
      programArgs.remove(indexDisabled);
    }

    // Super dev mode is on by default, on: nothing off: -nosuperDevMode
    if (superDevModeEnabled) {
      programArgs.add(0, SUPERDEVMODE_ENABLED_ARG);
    }
  }

  /**
   * Update program args for project GWT >= 2.7 use this for Dev Mode -nosuperDevMode and nothing
   * for super dev mode.
   */
  private void updateGwt27On(IJavaProject javaProject, List<String> programArgs, int indexDisabled,
      int indexEnabled, boolean superDevModeEnabled) {
    // Remove enabled arg
    if (indexEnabled > -1) {
      programArgs.remove(indexEnabled);
    }

    // Remove disabled arg
    if (indexDisabled > -1) {
      programArgs.remove(indexDisabled);
    }

    // This will not work for GWT 2.8
    // Verify its a GWT > 2.7, just in case another version snuck in.
    // if (!GwtVersionUtil.isGwtVersionGreaterOrEqualTo27(javaProject)) {
    // // Previously created in GWT 2.7, but switched to GWT 2.4 will do this
    // return;
    // }

    // Super dev mode is on by default, on: nothing off: -nosuperDevMode
    if (!superDevModeEnabled) {
      programArgs.add(0, SUPERDEVMODE_DISABLED_ARG);
    }
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

  /**
   * Validate if the Super Dev Mode Linker exists, for GWT <2.7.
   *
   * TODO verify this walks up parents and grandparents to verify flag exists.
   */
  private String validateSuperDevModeLinkerExists(IJavaProject javaProject,
      ILaunchConfiguration configuration) {
    try {
      List<String> modules = GWTLaunchConfigurationWorkingCopy.getEntryPointModules(configuration);
      if (modules == null || modules.isEmpty()) {
        // TODO probably should not throw this error, and be more specific.
        // This error is a symptom of another issue, project setup/config
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
  private String validateModule(IModule imodule, IJavaProject javaProject, ILaunchConfiguration configuration) {
    List<String> linkers = imodule.getAddLinkers();
    List<String> redirects = imodule.getSetConfigurationProperty("devModeRedirectEnabled");
    List<String> useSourceMaps = imodule.getSetConfigurationProperty("compiler.useSourceMaps");

    String gwtVersion = GwtVersionUtil.getProjectGwtVersion(javaProject);
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
