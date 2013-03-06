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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;
import java.util.Set;

/**
 * Represents a GWT module.
 */
public interface IModule {
  /**
   * Returns the compiled name of this module. This will be fully-qualified
   * name, unless a rename-to attribute specifies another.
   * 
   * @return this module's compiled name
   */
  String getCompiledName();

  /**
   * Returns the names of all classes declared as entry points of this module.
   * These classes are returned exactly as they are defined in the module XML,
   * therefore, they may not exist, or even be valid class names.
   * 
   * @return names of all declared entry point classes
   */
  List<String> getEntryPoints();
  
  /**
   * @return The list of directly inherited modules not in jar files
   */
  Set<IModule> getInheritedModules(IJavaProject javaProject);
    
  /**
   * Returns the name of the package containing this module.
   * 
   * @return this module's package name
   */
  String getPackageName();

  /**
   * Returns all of the public paths declared by this module. The paths are
   * module-relative. The paths returned may or may not correspond to actual
   * resources in the file system.
   * 
   * @return paths of all declared public paths
   */
  List<IPath> getPublicPaths();

  /**
   * Returns the fully-qualified name of the module. This is its Java package,
   * followed by the name of the module XML, without the .gwt.xml extension.
   * 
   * @return module's qualified name
   */
  String getQualifiedName();

  /**
   * Returns the simple name of this module. This is its filename without the
   * .gwt.xml extension.
   * 
   * @return module's simple name
   */
  String getSimpleName();

  /**
   * Returns all of the client source paths declared by this module. The paths
   * are module-relative. The paths returned may or may not correspond to actual
   * resources in the file system.
   * 
   * @return paths of all declared client source paths
   */
  List<IPath> getSourcePaths();

  /**
   * Returns whether this module is contained in a JAR archive.
   * 
   * @return <code>true</code> if this module is contained in a JAR, and
   *         <code>false</code> if it is contained in a .gwt.xml file on disk
   */
  boolean isBinary();

}
