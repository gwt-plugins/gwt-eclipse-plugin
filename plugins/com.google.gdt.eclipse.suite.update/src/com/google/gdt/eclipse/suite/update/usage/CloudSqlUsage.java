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
package com.google.gdt.eclipse.suite.update.usage;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Indicates the usage of Cloud SQL in an App Engine project. The possibilities are that it is
 * not used at all, that it is used only in production (with MySql used on the dev server), or that
 * it is used both on the dev server and in production.
 */
public enum CloudSqlUsage {

  NOT_USED, // Project does not use SQL.
  USED_IN_PRODUCTION, // Project uses MySql on dev server, Cloud SQL in production.
  USED_IN_DEVSERVER_AND_PRODUCTION; // Project uses Cloud SQL both places.

  /**
   * Reports the Cloud SQL usage for a specified Eclipse project.
   *
   * @param project the specified Eclipse project
   * @return the Cloud SQL usage for the project
   */
  public static CloudSqlUsage forProject(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences prefs = projectScope.getNode(AppEngineCorePlugin.PLUGIN_ID);
    if (prefs == null || !prefs.getBoolean("googleCloudSqlEnabled", false)) {
      return NOT_USED;
    }
    if (prefs.getBoolean("localDevMySqlEnabled", false)) {
      return USED_IN_PRODUCTION;
    } else {
      return USED_IN_DEVSERVER_AND_PRODUCTION;
    }
  }
}
