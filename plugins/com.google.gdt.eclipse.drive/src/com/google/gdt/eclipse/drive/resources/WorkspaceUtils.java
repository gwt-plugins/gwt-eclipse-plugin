/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import java.util.Arrays;

/**
 * Provides static methods for navigating the Eclipse workspace.
 */
public class WorkspaceUtils {

  private WorkspaceUtils() { } // prevent instantiation
  
  private static IProject[] mockProjects = null;
  
  /**
   * @return an {@code Iterable<IProject>} containing all open Apps Script projects in the workspace
   */
  public static Iterable<IProject> allOpenAppsScriptProjects() {
    IProject[] allProjects =
        mockProjects == null ?
            ResourcesPlugin.getWorkspace().getRoot().getProjects() // production
            : mockProjects; // unit tests
    return
        Iterables.filter(
            Arrays.asList(allProjects),
            new Predicate<IProject>() {
              @Override public boolean apply(IProject project) {
                try {
                  return project.isOpen() && project.hasNature(AppsScriptNature.NATURE_ID);
                } catch (CoreException e) {
                  return false;
                }
              }
            });
  }
  
  @VisibleForTesting public static void setMockProjects(IProject... projects) {
    mockProjects = projects;
  }

}
