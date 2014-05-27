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

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.CorePluginLog;

/**
 * Provides a static method to find facet IDs of facets enabled for a specified Eclipse project. 
 */
public class FacetFinder {

  private static final String FACET_JST_JAVA = "jst.java";
  private static final String FACET_JAVA = "java";
  
  private static final List<String> NO_STRINGS = ImmutableList.of();
  
  /**
   * Obtains a list of facet IDs for all non-Java facets enabled for a specified Eclipse project.
   * 
   * @param project the specified Eclipse project
   * @return the list of facet IDs
   */
  public static List<String> getEnabledNonJavaFacetIds(IProject project) {
    IFacetedProject facetedProject;
    try {
      facetedProject = ProjectFacetsManager.create(project);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      return NO_STRINGS;
    }
    if (facetedProject == null) {
      return NO_STRINGS;
    }
    Collection<IProjectFacetVersion> projectFacetVersions = facetedProject.getProjectFacets();
    List<String> facetsEnabled = Lists.newArrayListWithCapacity(projectFacetVersions.size());
    for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
      // Skip Java facet, since that is always on by default.
      String facetId = facet.getProjectFacet().getId();
      if (!facetId.equals(FACET_JST_JAVA) && !facetId.equals(FACET_JAVA)) {
        facetsEnabled.add(facetId);
      }
    }
    return facetsEnabled;
  }

}
