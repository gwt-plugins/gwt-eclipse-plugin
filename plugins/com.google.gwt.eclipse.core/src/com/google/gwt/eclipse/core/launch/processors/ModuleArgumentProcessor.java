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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.modules.ModuleInfo;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processes the GWT modules at the end of the program args. It works backwards,
 * checking if each argument is a module.
 */
public class ModuleArgumentProcessor implements ILaunchConfigurationProcessor {

  private static class ModuleParser {
    private static ModuleParser parse(List<String> args, Set<String> possibleModules) {

      List<Integer> moduleIndices = new ArrayList<Integer>();
      List<String> modules = new ArrayList<String>();

      for (int i = args.size() - 1; i >= 0; i--) {
        String arg = args.get(i);
        if (arg.startsWith("-")) {
          // No longer dealing with extra args
          break;
        }

        if (!possibleModules.contains(arg)) {
          // If we are transitioning between GWT and non-GWT, this could be
          // some other processor's extra arg, skip
          continue;
        }

        moduleIndices.add(i);
        modules.add(arg);
      }

      return new ModuleParser(moduleIndices, modules);
    }

    /**
     * Each item of this list is _not_ the index of the corresponding item in
     * {@link #modules} (i.e. the orderings are different.)
     */
    private final List<Integer> moduleIndices;

    private final List<String> modules;

    private ModuleParser(List<Integer> moduleIndices, List<String> modules) {
      this.moduleIndices = moduleIndices;
      this.modules = modules;

      Collections.sort(moduleIndices);
    }
  }

  public static List<String> getDefaultModules(
      IProject project, ILaunchConfiguration configuration) {

    List<String> modules = getExtensionDefaultModules(project, configuration);

    if (modules.isEmpty()) {
      modules = GWTProjectProperties.getEntryPointModules(project);
    }

    return modules;
  }

  public static List<String> getModules(
      List<String> args, ILaunchConfiguration config, IProject project) throws CoreException {
    return ModuleParser.parse(args, getAllPossibleModules(config, project)).modules;
  }

  private static boolean doesGwtMainTypeTakeModuleArguments(ILaunchConfiguration config)
      throws CoreException {
    return !GwtLaunchConfigurationProcessorUtilities.isGwtShell(config);
  }

  private static Set<String> getAllPossibleModules(
      ILaunchConfiguration configuration, IProject project) throws CoreException {
    Set<String> possibleModules = new HashSet<String>();
    possibleModules.addAll(getExtensionDefaultModules(project, configuration));
    possibleModules.addAll(GWTLaunchConfiguration.getEntryPointModules(configuration));
    possibleModules.addAll(GWTProjectProperties.getDefinedEntryPointModules(project));
    possibleModules.addAll(GWTProjectProperties.getDefaultEntryPointModules(project));
    return possibleModules;
  }

  private static List<String> getExtensionDefaultModules(IProject project,
      ILaunchConfiguration configuration) {

    ExtensionQuery<ModuleInfo> extQuery = new ExtensionQuery<ModuleInfo>(
        GWTPlugin.PLUGIN_ID, "moduleInfo", "class");

    List<ExtensionQuery.Data<ModuleInfo>> moduleInfos = extQuery.getData();
    for (ExtensionQuery.Data<ModuleInfo> moduleInfo : moduleInfos) {
      try {
        String[] startupModules = moduleInfo.getExtensionPointData().getStartupModules(
            project, configuration);

        if (startupModules != null && startupModules.length > 0) {
          return Arrays.asList(startupModules);
        }
        break;
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      }
      break;
    }

    return Collections.emptyList();
  }

  private static void removeModules(
      List<String> programArgs, ModuleParser parser, List<String> modulesToKeep) {
    for (int i = parser.moduleIndices.size() - 1; i >= 0; i--) {
      int moduleIndex = parser.moduleIndices.get(i).intValue();
      if (modulesToKeep == null || !modulesToKeep.contains(programArgs.get(moduleIndex))) {
        programArgs.remove(moduleIndex);
      }
    }
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {

    IProject project = javaProject.getProject();
    List<String> persistedModules = GWTLaunchConfiguration.getEntryPointModules(launchConfig);
    if (persistedModules.isEmpty()) {
      persistedModules = getDefaultModules(project, launchConfig);
    }

    Set<String> possibleModules = getAllPossibleModules(launchConfig, project);
    ModuleParser parser = ModuleParser.parse(programArgs, possibleModules);

    if (!GWTNature.isGWTProject(project) || !doesGwtMainTypeTakeModuleArguments(launchConfig)) {
      removeModules(programArgs, parser, null);

    } else {
      // Remove modules that should not be present
      removeModules(programArgs, parser, persistedModules);

      // Add modules that are not present
      for (String module : persistedModules) {
        if (!parser.modules.contains(module)) {
          programArgs.add(module);
        }
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    return null;
  }
}
