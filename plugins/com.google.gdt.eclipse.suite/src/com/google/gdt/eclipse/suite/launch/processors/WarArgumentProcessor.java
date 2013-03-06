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
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.util.List;

/**
 * Processes the argument for specifying the WAR folder. The following rules
 * apply:
 * <ul>
 * <li>If the project does not use a WAR layout, the argument/value should not
 * be present.
 * <li>If the project is GAE-only, the WAR folder should be the last argument
 * (an implied argument value, there is no corresponding "-war" argument).
 * <li>If the project is GWT, the WAR folder should be passed as an argument
 * value to the "-war" argument.
 * </ul>
 */
public class WarArgumentProcessor implements ILaunchConfigurationProcessor {

  /**
   * Interface used by extensions that supply their own war argument for the
   * launch configuration.
   */
  public interface WarArgFinder {
    List<String> findWarArg(ILaunchConfigurationWorkingCopy config,
        IJavaProject javaProject, boolean isWarDirValid, String unverifiedWarDir);
  }

  /**
   * Simple parser for getting the WAR directory from the arguments.
   */
  public static class WarParser {

    public static WarParser parse(List<String> args, IJavaProject javaProject) {
      // Get the WAR dir index (either after the "-war" arg or the last arg)
      int unverifiedWarDirIndex = getUnverifiedWarDirIndex(args);
      // Get the arg at the index
      String unverifiedWarDir = LaunchConfigurationProcessorUtilities.getArgValue(
          args, unverifiedWarDirIndex);
      String resolvedUnverifiedWarDir = unverifiedWarDir;
      try {
        resolvedUnverifiedWarDir = unverifiedWarDir != null
            ? VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(
                unverifiedWarDir) : null;
      } catch (CoreException e) {
        GdtPlugin.getLogger().logWarning(e,
            "Could not resolve any variables in the WAR directory.");
      }
      // Check if the WAR dir is valid
      boolean isWarDirValid = resolvedUnverifiedWarDir != null
          ? isWarDirValid(resolvedUnverifiedWarDir) : false;
      // Check if it is specified with the "-war" arg
      boolean isSpecifiedWithWarArg = unverifiedWarDirIndex != -1
          ? isSpecifiedWithWarArg(unverifiedWarDirIndex, args) : false;
      return new WarParser(unverifiedWarDirIndex, isWarDirValid,
          isSpecifiedWithWarArg, unverifiedWarDir, resolvedUnverifiedWarDir);
    }

    /**
     * Returns where the unverified WAR dir is located in the list of arguments.
     * <p>
     * Note: It is up to the client to verify the returned index is actually a
     * valid WAR dir. For example, if we are unsure, we return the last arg's
     * index since it is used as the WAR dir for GAE.
     */
    private static int getUnverifiedWarDirIndex(List<String> args) {
      // Try as -war first, since we're confident when it matches
      int indexFromWarArg = getWarDirIndexFromWarArg(args);
      if (indexFromWarArg != -1) {
        return indexFromWarArg;
      }

      // Failing that, try the last argument
      int indexFromLastArg = getWarDirIndexFromLastArg(args);
      if (indexFromLastArg != -1) {
        // We are not nearly as confident in this, but it is up to the client to
        // validate the war dir before using it
        return indexFromLastArg;
      }

      return -1;
    }

    private static int getWarDirIndexFromLastArg(List<String> args) {
      int warArgValueIndex = args.size() - 1;
      return LaunchConfigurationProcessorUtilities.getArgValue(args,
          warArgValueIndex) != null ? warArgValueIndex : -1;
    }

    private static int getWarDirIndexFromWarArg(List<String> args) {
      int warArgIndex = args.indexOf(ARG_WAR);
      if (warArgIndex == -1) {
        return -1;
      }

      int warArgValueIndex = warArgIndex + 1;
      return LaunchConfigurationProcessorUtilities.getArgValue(args,
          warArgValueIndex) != null ? warArgValueIndex : -1;
    }

    private static boolean isSpecifiedWithWarArg(int warDirIndex,
        List<String> args) {
      return warDirIndex > 0
          && args.get(warDirIndex - 1).equalsIgnoreCase(ARG_WAR);
    }

    private static boolean isWarDirValid(String warDir) {
      File war = new File(warDir);
      return war.exists() && war.isDirectory()
          && new File(warDir, "WEB-INF").exists();
    }

    public final int unverifiedWarDirIndex;
    public final boolean isWarDirValid;
    public final boolean isSpecifiedWithWarArg;
    public final String unverifiedWarDir;
    public final String resolvedUnverifiedWarDir;

    private WarParser(int unverifiedWarDirIndex, boolean isWarDirValid,
        boolean isSpecifiedWithWarArg, String unverifiedWarDir,
        String resolvedUnverifiedWarDir) {
      this.unverifiedWarDirIndex = unverifiedWarDirIndex;
      this.isWarDirValid = isWarDirValid;
      this.isSpecifiedWithWarArg = isSpecifiedWithWarArg;
      this.unverifiedWarDir = unverifiedWarDir;
      this.resolvedUnverifiedWarDir = resolvedUnverifiedWarDir;
    }
  }

  private static final String ARG_WAR = "-war";

  private static final String ATTR_IS_WAR_FROM_PROJECT_PROPERTIES = GdtPlugin.PLUGIN_ID
      + "WarArgumentProcessor.IS_WAR_FROM_PROJECT_PROPERTIES";

  public static boolean doesMainTypeTakeWarArgument(ILaunchConfiguration config)
      throws CoreException {
    return !GwtLaunchConfigurationProcessorUtilities.isGwtShell(config);
  }

  private static boolean isWarFromProjectProperties(ILaunchConfiguration config) {
    try {
      return config.getAttribute(ATTR_IS_WAR_FROM_PROJECT_PROPERTIES, false);
    } catch (CoreException e) {
      CorePluginLog.logError(
          e,
          "Could not determine whether the set WAR was from project properties, assuming it was not.");
      return false;
    }
  }

  private boolean isUserUpdate;

  private String warDirFromLaunchConfigCreation;

  public void setUserUpdate(boolean isUserUpdate) {
    this.isUserUpdate = isUserUpdate;
  }

  /**
   * A special method to be used by the launch configuration-creation code that
   * allows it to specify the exact WAR directory to use.
   * 
   * Setting this also skips the early-exit if the WAR is unmanaged. This allows
   * for creating launch configurations with the "-war" argument for projects
   * that have unmanaged WARs.
   */
  public void setWarDirFromLaunchConfigCreation(String warDir) {
    warDirFromLaunchConfigCreation = warDir;
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    // First, try to get a war argument from the extensions, then try everything
    // else.
    if (isWarArgsFromExtensions(launchConfig, javaProject, programArgs)) {
      launchConfig.setAttribute(ATTR_IS_WAR_FROM_PROJECT_PROPERTIES, false);
    } else {
      if (warDirFromLaunchConfigCreation == null) {
        // Check the scenarios where this processor should do nothing. If a WAR
        // dir is given from launch config creation, we set it regardless of
        // whether we would normally do nothing.
        if (!doesMainTypeTakeWarArgument(launchConfig)) {
          removeVerifiedWarArgAndDir(programArgs, javaProject);
          return;
        }

        if (!WebAppUtilities.isWebApp(javaProject.getProject())) {
          return;
        }

        if (!WebAppUtilities.hasManagedWarOut(javaProject.getProject())) {
          // We are not managing the runtime WAR
          return;
        }
      }

      WarParser info = WarParser.parse(programArgs, javaProject);
      String warFromProjProps = LaunchConfigurationProcessorUtilities.getWarDirectory(javaProject);
      // Did we use the WAR from the project properties when we last ran this?
      boolean wasWarFromProjProps = isWarFromProjectProperties(launchConfig);
      /*
       * If both dirs are the same, perfect, we'll record that it is from
       * project properties. Otherwise, if it is a manual user update, then we
       * say it is not from project properties. Finally, if we took it from the
       * project properties last time, or if the current last-arg-based WAR dir
       * is invalid then we say it is from project properties.
       */
      boolean isWarFromProjProps = JavaUtilities.equalsWithNullCheck(
          warFromProjProps, info.unverifiedWarDir);
      if (!isWarFromProjProps && !isUserUpdate) {
        isWarFromProjProps = wasWarFromProjProps
            || (!info.isSpecifiedWithWarArg && !info.isWarDirValid);
      }
      String newWarDir;
      if (warDirFromLaunchConfigCreation != null) {
        newWarDir = warDirFromLaunchConfigCreation;
      } else {
        newWarDir = isWarFromProjProps ? warFromProjProps
            : info.unverifiedWarDir;
      }

      if (StringUtilities.isEmpty(newWarDir)) {
        // A WAR dir is required but we do not have one, leave untouched
        return;
      }

      if (GWTNature.isGWTProject(javaProject.getProject())) {
        // Ensure there is a -war arg and the WAR dir
        if (!info.isSpecifiedWithWarArg) {
          // Remove the old style, add the new "-war" and value
          removeVerifiedWarArgAndDir(programArgs, javaProject);
          programArgs.add(0, ARG_WAR);
          programArgs.add(1, newWarDir);

        } else {
          // It is specified with the "-war", so just update the value
          programArgs.remove(info.unverifiedWarDirIndex);
          programArgs.add(info.unverifiedWarDirIndex, newWarDir);
        }

      } else if (GaeNature.isGaeProject(javaProject.getProject())) {
        // Ensure there is a valid WAR dir without "-war" arg
        if (info.isSpecifiedWithWarArg) {
          // Remove the -war and WAR dir
          removeVerifiedWarArgAndDir(programArgs, javaProject);
          // Add the last arg WAR dir
          programArgs.add(newWarDir);

        } else {
          if (info.unverifiedWarDirIndex >= 0) {
            // Remove the existing WAR dir
            programArgs.remove(info.unverifiedWarDirIndex);
          }
          // Add the last arg WAR dir
          programArgs.add(newWarDir);
        }
      }
      launchConfig.setAttribute(ATTR_IS_WAR_FROM_PROJECT_PROPERTIES,
          isWarFromProjProps);
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    return null;
  }

  /**
   * This method queries any extensions for a war directory. Note that because
   * there can only one war argument for each launch configuration, the one
   * supplied by the extension will "win" here. Furthermore, where the war
   * argument will be added depends on whether the war argument has "-war"
   * specified or not. If it does, it must not go at the end. If it doesn't, it
   * must go at the end.
   * 
   * @param launchConfig
   * @param javaProject
   * @param programArgs
   */
  private boolean isWarArgsFromExtensions(
      ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs) {
    WarParser info = WarParser.parse(programArgs, javaProject);
    boolean warArgsFound = false;
    ExtensionQuery<WarArgumentProcessor.WarArgFinder> extQuery = new ExtensionQuery<WarArgumentProcessor.WarArgFinder>(
        GdtPlugin.PLUGIN_ID, "warArgFinder", "class");
    List<ExtensionQuery.Data<WarArgumentProcessor.WarArgFinder>> warArgFinders = extQuery.getData();
    for (ExtensionQuery.Data<WarArgumentProcessor.WarArgFinder> warArgFinder : warArgFinders) {
      List<String> warArgs = warArgFinder.getExtensionPointData().findWarArg(
          launchConfig, javaProject, info.isWarDirValid, info.unverifiedWarDir);
      if ((warArgs != null) && (warArgs.size() > 0)) {
        warArgsFound = true;
        removeVerifiedWarArgAndDir(programArgs, javaProject);
        // if -war is given, it must not be at the end
        if (warArgs.size() > 1) {
          for (int i = 0; i < warArgs.size(); i++) {
            programArgs.add(i, warArgs.get(i));
          }
        } else { // add the war directory at the end
          programArgs.add(warArgs.get(0));
        }
      }
    }
    return warArgsFound;
  }

  private void removeVerifiedWarArgAndDir(List<String> programArgs,
      IJavaProject javaProject) {
    WarParser info = WarParser.parse(programArgs, javaProject);
    if (info.isWarDirValid || info.isSpecifiedWithWarArg) {
      // Either the args are explicit about the WAR dir (with -war arg), or the
      // WAR dir was valid. In either case, we can remove it/them.
      programArgs.remove(info.unverifiedWarDirIndex);

      if (info.isSpecifiedWithWarArg) {
        programArgs.remove(info.unverifiedWarDirIndex - 1);
      }
    }
  }

}
