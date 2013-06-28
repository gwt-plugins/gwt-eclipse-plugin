/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.jpa;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.IDynamicPreset;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectBase;
import org.eclipse.wst.common.project.facet.core.IPresetFactory;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.PresetDefinition;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.internal.DefaultFacetsExtensionPoint;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.common.collect.Sets;

/**
 * {@link IPresetFactory} instance which adds JPA facet into Google App Engine default
 * configuration.
 */
@SuppressWarnings("restriction")
public final class GaeJpaPresetFactory implements IPresetFactory {

  @Override
  public PresetDefinition createPreset(String presetId, Map<String, Object> context)
      throws CoreException {
    IFacetedProjectBase fproj = (IFacetedProjectBase) context.get(IDynamicPreset.CONTEXT_KEY_FACETED_PROJECT);
    IRuntime runtime = fproj.getPrimaryRuntime();
    if (runtime != null) {
      IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
      // only add preset for Google App Engine runtime
      if (runtime.supports(gaeFacet)) {
        IProjectFacet jpaFacet = ProjectFacetsManager.getProjectFacet("jpt.jpa");
        if (jpaFacet != null) {
          IProjectFacetVersion jpaFacetVersion = jpaFacet.getVersion("2.0");
          if (jpaFacetVersion != null) {
            Set<IProjectFacetVersion> facets = Sets.newHashSet(DefaultFacetsExtensionPoint.getDefaultFacets(fproj));
            facets.add(jpaFacetVersion);
            String label = "Google App Engine with JPA";
            String description = "Adds JPA facet into Google App Engine default configuration.";
            return new PresetDefinition(label, description, facets);
          }
        }
      }
    }
    return null;
  }
}
