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
package com.google.gdt.eclipse.core.update.internal.core;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.WebAppUtilities;

/**
 * Static methods providing operations to query properties of Eclipse projects.
 */
public class ProjectInformationUtils {
  
  private static final String DEFAULT_WEB_INF_LOCATION =
      "/" + WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/";

  /**
   * Reports whether a specified Eclipse project is a Cloud Endpoints project.
   * @param project the specified Eclipse project
   * 
   * @return
   *     {@code true} if the project has a war/WEB-INF folder containing at least one file with
   *     a {@code .api} extension, {@code false} otherwise
   */
  public static boolean isCloudEndpointProject(IProject project) {
    IPath warPath = WebAppUtilities.getWarOutLocation(project);
    IPath webinfPath =
        warPath == null ?
            project.getFullPath().append(DEFAULT_WEB_INF_LOCATION) : warPath.append("WEB-INF");
    IFolder f = ResourcesPlugin.getWorkspace().getRoot().getFolder(webinfPath);
    try {
      if (f.exists() && f.members().length != 0) {
        for (IResource r : f.members()) {
          if (r.getName().endsWith(".api")) {
            return true;
          }
        }
      }
      return false;
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      return false;
    }
  }
}
