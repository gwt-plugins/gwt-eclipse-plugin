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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

/**
 * Utility methods for getting information from Dynamic Web Projects.
 */
public class DynamicWebProjectUtilities {

  /**
   * Returns the WebContent folder for a Dynamic Web Project, or null if the
   * project does not have a WebContent folder.
   * 
   * @return A project-relative path to the WebContent folder
   */
  public static IPath getWebContentFolder(IProject project) {
    IPath path = null;
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      path = component.getRootFolder().getWorkspaceRelativePath();
      if (project.getFullPath().isPrefixOf(path)) {
        return path.removeFirstSegments(project.getFullPath().segmentCount());
      }
    }

    return null;
  }

  /**
   * Returns <code>true</code> if the given project is a Dynamic Web Project.
   */
  public static boolean isDynamicWebProject(IProject project)
      throws CoreException {
    return FacetedProjectFramework.hasProjectFacet(project, "jst.web", null);
  }

  private DynamicWebProjectUtilities() {
    // Not instantiable
  }
}
