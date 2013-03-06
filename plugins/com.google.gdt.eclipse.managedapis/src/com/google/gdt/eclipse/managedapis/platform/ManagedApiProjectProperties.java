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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Gets and sets ManagedApi project properties.
 */
public class ManagedApiProjectProperties {
  /**
   * Return the "CopyToTargetDir", or the directory to which the plugin should
   * copy jars. This property value stores this value across sessions and for
   * shared use. (e.g. a sample value might be "war/WEB-INF/lib").
   */
  public static String getCopyToTargetDir(IProject project) {
    return project != null ? getProjectProperties(project).get(
        ManagedApiPlugin.COPY_CLASSPATH_ENTRIES_TARGET_PATH_KEY, null) : null;
  }

  /**
   * Return the ManagedApiRootFolderPath for the project. This value is
   * typically ".google-apis", but this property can be overridden on a
   * per-project basis.
   */
  public static String getManagedApiRootFolderPath(IProject project) {
    return project != null ? getProjectProperties(project).get(
        ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_PATH_KEY,
        ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_DEFAULT_PATH) : null;
  }

  /**
   * Set the CopyToTargetDirPath for the specified project. See
   * {@link #getCopyToTargetDir(IProject)} for more details. Note: changing this
   * field in the properties does not trigger the appropriate listeners. Set
   * this value on the ManagedApiProject.
   */
  public static void setCopyToTargetDirPath(IProject project,
      String copyToTargetDirPath) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    if (copyToTargetDirPath != null) {
      prefs.put(ManagedApiPlugin.COPY_CLASSPATH_ENTRIES_TARGET_PATH_KEY,
          copyToTargetDirPath);
    } else {
      prefs.remove(ManagedApiPlugin.COPY_CLASSPATH_ENTRIES_TARGET_PATH_KEY);
    }
    prefs.flush();
  }

  /**
   * Set the ManagedApiRootFolderPath for the specified project. See
   * {@link #getManagedApiRootFolderPath(IProject)} for more details. Note:
   * changing this field in the properties does not trigger the appropriate
   * listeners. Set this value on the ManagedApiProject.
   */
  public static void setManagedApiRootFolderPath(IProject project,
      String managedApiRootFolderPath) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    if (managedApiRootFolderPath == null) {
      prefs.remove(ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_PATH_KEY);
    } else {
      prefs.put(ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_PATH_KEY,
          managedApiRootFolderPath);
    }
    prefs.flush();
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(ManagedApiPlugin.PLUGIN_ID);
  }
}
