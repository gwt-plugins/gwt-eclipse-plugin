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
package com.google.gwt.eclipse.core.properties;

import com.google.gdt.eclipse.core.PropertiesUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gets and sets GWT project properties.
 */
public final class GWTProjectProperties {

  private static final String ENTRY_POINT_MODULES = "entryPointModules";

  private static final String FILES_COPIED_TO_WEB_INF_LIB = "filesCopiedToWebInfLib";

  private static final String GWT_COMPILE_SETTINGS_XML = "gwtCompileSettings";

  /**
   * Returns the default set of entry point modules for a project. This set
   * contains all modules defined in source (.gwt.xml) in the project.
   */
  public static List<String> getDefaultEntryPointModules(IProject project) {
    List<String> moduleNames = new ArrayList<String>();
    IJavaProject javaProject = JavaCore.create(project);
    Map<String, Set<IModule>> inheritedModulesCache = new HashMap<String, Set<IModule>>();
    Map<String, List<String>> entryPointsCache = new HashMap<String, List<String>>();

    for (IModule module : ModuleUtils.findAllModules(javaProject,
        false)) {
      if (getEntryPoints(javaProject, module, entryPointsCache).size() > 0
          || getInheritedEntryPoints(javaProject, module,
              inheritedModulesCache, entryPointsCache).size() > 0) {
        moduleNames.add(module.getQualifiedName());
      }
    }

    return moduleNames;
  }

  /**
   * Returns the user-specified set of entry point modules.
   */
  public static List<String> getDefinedEntryPointModules(IProject project) {
    String rawPropVal = getProjectProperties(project).get(ENTRY_POINT_MODULES,
        null);

    return PropertiesUtilities.deserializeStrings(rawPropVal);
  }

  /**
   * Returns the list of entry point modules for a project (user defined, or if
   * not specified, a default list).
   */
  public static List<String> getEntryPointModules(IProject project) {
    List<String> moduleNames = getDefinedEntryPointModules(project);

    // If we don't have a user-specified set of entry point modules, return the
    // default entry point modules for this project
    if (moduleNames.isEmpty()) {
      moduleNames = getDefaultEntryPointModules(project);
    }
    return moduleNames;
  }

  public static List<String> getFileNamesCopiedToWebInfLib(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = prefs.get(FILES_COPIED_TO_WEB_INF_LIB, null);
    if (rawPropVal == null || rawPropVal.length() == 0) {
      return Collections.emptyList();
    }

    return Arrays.asList(rawPropVal.split("\\|"));
  }

  public static GWTCompileSettings getGwtCompileSettings(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    byte[] settingsBytes = prefs.getByteArray(GWT_COMPILE_SETTINGS_XML,
        new byte[0]);
    GWTCompileSettings settings = GWTCompileSettings.deserialize(settingsBytes, project);
    if (settings == null) {
      // Use default GWT compilation settings
      settings = new GWTCompileSettings(project);
    }
        
    return settings;
  }

  public static void setEntryPointModules(IProject project, List<String> modules)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    String rawPropVal = PropertiesUtilities.serializeStrings(modules);
    prefs.put(ENTRY_POINT_MODULES, rawPropVal);
    prefs.flush();
  }

  public static void setFileNamesCopiedToWebInfLib(IProject project,
      List<String> fileNamesCopiedToWebInfLib) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    StringBuilder sb = new StringBuilder();
    boolean addPipe = false;
    for (String fileNameCopiedToWebInfLib : fileNamesCopiedToWebInfLib) {
      if (addPipe) {
        sb.append("|");
      } else {
        addPipe = true;
      }

      sb.append(fileNameCopiedToWebInfLib);
    }

    prefs.put(FILES_COPIED_TO_WEB_INF_LIB, sb.toString());
    prefs.flush();
  }

  public static void setFilesCopiedToWebInfLib(IProject project,
      List<File> filesCopiedToWebInfLib) throws BackingStoreException {
    setFileNamesCopiedToWebInfLib(project, toFileNames(filesCopiedToWebInfLib));
  }

  public static void setGwtCompileSettings(IProject project,
      GWTCompileSettings settings) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putByteArray(GWT_COMPILE_SETTINGS_XML, settings.toByteArray());
    prefs.flush();
  }

  /**
   * Returns the inherited modules transitive closure.
   */
  private static Set<IModule> getAllInheritedModules(IJavaProject javaProject,
      IModule module, Map<String, Set<IModule>> inheritedModulesCache) {

    return getAllInheritedModulesRecurse(javaProject, module,
        new HashSet<String>(), inheritedModulesCache);
  }

  /**
   * Computes the inherited modules closure recursively.
   * 
   * @param javaProject the associated Java project
   * @param module the module being explored
   * @param explored a set of previously explored modules
   * @param inheritedModulesCache a mapping between modules and their directly
   *          inherited siblings
   * @return the set of modules transitively inherited from the current module
   *         and which have not been previously explored
   */
  private static Set<IModule> getAllInheritedModulesRecurse(
      IJavaProject javaProject, IModule module, Set<String> explored,
      Map<String, Set<IModule>> inheritedModulesCache) {

    String moduleName = module.getQualifiedName();

    if (explored.contains(moduleName)) {
      return Collections.emptySet();
    }
    explored.add(moduleName);

    Set<IModule> inheritedModules = new HashSet<IModule>();
    for (IModule m : getInheritedModules(javaProject, module,
        inheritedModulesCache)) {
      inheritedModules.add(m);
      inheritedModules.addAll(getAllInheritedModulesRecurse(javaProject, m,
          explored, inheritedModulesCache));
    }

    return inheritedModules;
  }

  /**
   * Returns the list of entry points for a given module. Results are cached.
   */
  private static List<String> getEntryPoints(IJavaProject javaproject,
      IModule module, Map<String, List<String>> cache) {

    String moduleName = module.getQualifiedName();
    List<String> res = cache.get(moduleName);

    if (res == null) {
      res = module.getEntryPoints();
      cache.put(moduleName, res);
    }

    return res;
  }

  /**
   * Returns the list of entry points for a given module and all its
   * (transitively) inherited dependencies.
   * 
   * @param javaProject the associated Java project
   * @param module the module to evaluate
   * @param inheritedModulesCache a cache for (module -> directly inherited
   *          modules) mappings
   * @param entryPointsCache a cache for (module -> entry points) mappings
   * @return the list of entry point names
   */
  private static List<String> getInheritedEntryPoints(IJavaProject javaProject,
      IModule module, Map<String, Set<IModule>> inheritedModulesCache,
      Map<String, List<String>> entryPointsCache) {

    List<String> entryPoints = new ArrayList<String>();
    for (IModule m : getAllInheritedModules(javaProject, module,
        inheritedModulesCache)) {
      entryPoints.addAll(getEntryPoints(javaProject, m, entryPointsCache));
    }

    return entryPoints;
  }

  /**
   * Returns the list of directly inherited modules. Results are cached.
   */
  private static Set<IModule> getInheritedModules(IJavaProject javaProject,
      IModule module, Map<String, Set<IModule>> cache) {

    String moduleName = module.getQualifiedName();
    Set<IModule> res = cache.get(moduleName);

    if (res == null) {
      res = module.getInheritedModules(javaProject);
      cache.put(moduleName, res);
    }

    return res;
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(GWTPlugin.PLUGIN_ID);
  }

  private static List<String> toFileNames(List<File> files) {
    List<String> fileNames = new ArrayList<String>();
    for (File file : files) {
      fileNames.add(file.getName());
    }
    return fileNames;
  }

  private GWTProjectProperties() {
    // Not instantiable
  }
}