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
package com.google.gwt.eclipse.core.modules;

import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Answers questions about the modules available in a GWT project.
 */
public final class ModuleUtils {

  private interface IPackageFragmentVisitor<T> {
    T visit(IPackageFragment fragment) throws JavaModelException;
  }

  private static final String FILE_EXTENSION = ".gwt.xml";

  /**
   * Returns the module corresponding to the given file.
   * 
   * @param file the module XML file
   * @return the module, or <code>null</code> if unable to associate the given
   *         file with a module
   */
  public static ModuleFile create(IFile file) {
    if (!isModuleXml(file)) {
      return null;
    }

    return new ModuleFile(file);
  }

  /**
   * Returns the module corresponding to the given JAR resource.
   * 
   * @param jarResource the module JAR resource
   * @return the module, or <code>null</code> if unable to associate the given
   *         JAR resource with a module
   */
  public static ModuleJarResource create(IJarEntryResource jarResource) {
    if (!isModuleXml(jarResource)) {
      return null;
    }

    return new ModuleJarResource(jarResource);
  }

  /**
   * Finds all GWT modules in a project.
   * 
   * @param javaProject project in which to search for modules
   * @param includeJars indicates whether to include JAR files in search
   * @return the list of modules found
   */
  public static IModule[] findAllModules(IJavaProject javaProject,
      final boolean includeJars) {
    final Map<String, IModule> modules = new HashMap<String, IModule>();

    // TODO: search super-source also
    visitFragments(javaProject, includeJars,
        new IPackageFragmentVisitor<Void>() {
          public Void visit(IPackageFragment pckg) throws JavaModelException {
            for (Object resource : pckg.getNonJavaResources()) {
              IModule module = create(resource, includeJars);
              if (module != null) {
                String moduleName = module.getQualifiedName();

                if (!modules.containsKey(moduleName)) {
                  modules.put(moduleName, module);
                }
              }
            }

            return null;
          }
        });

    return modules.values().toArray(new IModule[modules.size()]);
  }

  /**
   * Finds all GWT modules located directly in a particular container.
   * 
   * @param container container to search within
   * @return the list of modules found
   */
  public static IModule[] findChildModules(IContainer container) {
    final List<IModule> modules = new ArrayList<IModule>();

    IResourceVisitor moduleVisitor = new IResourceVisitor() {
      public boolean visit(IResource resource) throws CoreException {
        if (resource.getType() == IResource.FILE) {
          IModule module = create((IFile) resource);
          if (module != null) {
            modules.add(module);
          }
        }
        return true;
      }
    };

    try {
      container.accept(moduleVisitor, IResource.DEPTH_ONE, false);
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }

    return modules.toArray(new IModule[modules.size()]);
  }

  /**
   * Finds a particular GWT module by its fully-qualified name.
   * 
   * @param javaProject project in which to search for modules
   * @param qualifiedName fully-qualified module name
   * @param includeJars indicates whether to include JAR files in search
   * @return the module, if found; otherwise <code>null</code>
   */
  public static IModule findModule(IJavaProject javaProject,
      String qualifiedName, final boolean includeJars) {
    final String modulePckg = Signature.getQualifier(qualifiedName);
    final String simpleName = Signature.getSimpleName(qualifiedName);

    return visitFragments(javaProject, includeJars,
        new IPackageFragmentVisitor<IModule>() {
          public IModule visit(IPackageFragment pckg) throws JavaModelException {
            // Look for the package fragment matching the module qualifier
            if (modulePckg.equals(pckg.getElementName())) {
              for (Object resource : pckg.getNonJavaResources()) {
                IModule module = create(resource, includeJars);
                // Now compare the resource name to the module name
                if (module != null && module.getSimpleName().equals(simpleName)) {
                  return module;
                }
              }
            }

            return null;
          }
        });
  }

  /**
   * Returns whether a JAR resource is a module XML.
   * 
   * @param jarResource the JAR resource to check
   * @return <code>true</code> if the resource is a module XML, and
   *         <code>false</code> otherwise
   */
  public static boolean isModuleXml(IJarEntryResource jarResource) {
    return (jarResource.isFile() && jarResource.getName().endsWith(
        FILE_EXTENSION));
  }

  /**
   * Returns whether a workspace resource is a module XML.
   * 
   * @param resource the resource to check
   * @return <code>true</code> if the resource is a module XML, and
   *         <code>false</code> otherwise
   */
  public static boolean isModuleXml(IResource resource) {
    if (resource.getType() != IResource.FILE) {
      return false;
    }

    if (resource.getParent().getType() != IResource.FOLDER) {
      return false;
    }

    if (!resource.getName().endsWith(FILE_EXTENSION)) {
      return false;
    }

    return true;
  }

  /**
   * Validates a fully-qualified module name. Module names are validated like
   * fully-qualified Java type names; the package should be made up of
   * lower-case segments that are valid Java identifiers, and the name should be
   * a camel-cased valid Java identifier.
   * 
   * @param qualifiedName fully-qualified module name
   * @return a status object with code <code>IStatus.OK</code> if the given
   *         name is valid, otherwise a status object indicating what is wrong
   *         with the name
   */
  public static IStatus validateQualifiedModuleName(String qualifiedName) {
    // Validate the module package name according to Java conventions
    String pckg = Signature.getQualifier(qualifiedName);
    if (!Util.isValidPackageName(pckg)) {
      return Util.newErrorStatus("The module package name is invalid");
    }

    return validateSimpleModuleName(Signature.getSimpleName(qualifiedName));
  }

  /**
   * Validates a simple module name. The name should be a camel-cased valid Java
   * identifier.
   * 
   * @param simpleName the simple module name
   * @return a status object with code <code>IStatus.OK</code> if the given
   *         name is valid, otherwise a status object indicating what is wrong
   *         with the name
   */
  public static IStatus validateSimpleModuleName(String simpleName) {
    String complianceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
    String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");

    // Make sure that the simple name does not have any dots in it. We need
    // to do this validation before passing the simpleName to JavaConventions,
    // because validateTypeName accepts both simple and fully-qualified type
    // names.
    if (simpleName.indexOf('.') != -1) {
      return Util.newErrorStatus("Module name should not contain dots.");
    }

    // Validate the module name according to Java type name conventions
    IStatus nameStatus = JavaConventions.validateJavaTypeName(simpleName,
        complianceLevel, sourceLevel);
    if (nameStatus.matches(IStatus.ERROR)) {
      return Util.newErrorStatus("The module name is invalid");
    }

    return Status.OK_STATUS;
  }

  private static IModule create(Object resource, boolean allowModulesInJars) {
    IFile file = AdapterUtilities.getAdapter(resource, IFile.class);
    if (file != null) {
      return create(file);
    } else {
      IJarEntryResource jarEntryRes = AdapterUtilities.getAdapter(resource,
          IJarEntryResource.class);
      if (jarEntryRes != null && allowModulesInJars) {
        return create(jarEntryRes);
      }
    }

    return null;
  }

  /**
   * Scans the package fragments (including jars if includeJars is true)
   * invoking the visitor callback.
   * 
   * Stops if the callback returns a non-null result, and passes that result
   * back to the caller.
   */
  private static <T> T visitFragments(IJavaProject project, boolean includeJars,
      IPackageFragmentVisitor<T> visitor) {
    try {
      for (IPackageFragmentRoot pckgRoot : project.getPackageFragmentRoots()) {
        if (pckgRoot.isArchive() && !includeJars) {
          continue;
        }

        for (IJavaElement elem : pckgRoot.getChildren()) {
          T result = visitor.visit((IPackageFragment) elem);
          if (result != null) {
            return result;
          }
        }
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }

    return null;
  }

  private ModuleUtils() {
    // Not instantiable
  }
}
