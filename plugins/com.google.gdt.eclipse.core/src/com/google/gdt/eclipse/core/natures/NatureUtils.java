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
package com.google.gdt.eclipse.core.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for manipulating natures on projects.
 */
public class NatureUtils {
  /**
   * Add the nature ID to the project unless it is already specified or the
   * project is not opened.
   * 
   * @param project
   * @param natureId
   * 
   * @throws CoreException See {@link IProject#hasNature(String)} for why this
   *           may be thrown.
   */
  public static void addNature(IProject project, String natureId)
      throws CoreException {
    if (!project.isOpen() || project.hasNature(natureId)) {
      return;
    }

    addNatures(project, Collections.singletonList(natureId));
  }

  public static void addNatures(IProject project, List<String> natureIds)
      throws CoreException {
    IProjectDescription description = project.getDescription();
    List<String> newNatureIds = new ArrayList<String>(
        Arrays.asList(description.getNatureIds()));
    boolean natureAdded = false;
    for (String natureId : natureIds) {
      if (project.hasNature(natureId)) {
        continue;
      }

      natureAdded = true;
      newNatureIds.add(natureId);
    }

    if (natureAdded) {
      // Only do this if we added a nature
      description.setNatureIds(newNatureIds.toArray(new String[0]));
      project.setDescription(description, null);
    }
  }

  /**
   * Returns <code>true</code> if the project is accessible and has the
   * specified nature ID.
   * 
   * @param project
   * @param natureId
   * 
   * @return <code>true</code> if the project is accessible and has the
   *         specified nature ID
   * @throws CoreException
   */
  public static boolean hasNature(IProject project, String natureId)
      throws CoreException {
    return project.isAccessible() && project.hasNature(natureId);
  }

  /**
   * Removes the nature ID from a project's description.
   * 
   * @param project
   * @param natureId
   * 
   * @throws CoreException
   */
  public static void removeNature(IProject project, String natureId)
      throws CoreException {
    if (!project.hasNature(natureId)) {
      return;
    }

    // Remove the Nature ID from the natures in the project description
    IProjectDescription description = project.getDescription();
    List<String> newNatures = new ArrayList<String>();
    newNatures.addAll(Arrays.asList(description.getNatureIds()));
    newNatures.remove(natureId);
    description.setNatureIds(newNatures.toArray(new String[0]));
    project.setDescription(description, null);
  }

  private NatureUtils() {
  }
}
