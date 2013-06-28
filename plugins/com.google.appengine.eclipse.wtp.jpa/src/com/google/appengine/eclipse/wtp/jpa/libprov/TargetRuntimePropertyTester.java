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
package com.google.appengine.eclipse.wtp.jpa.libprov;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;

import java.util.Set;

/**
 * A {@link PropertyTester} which tests does the selected runtime support given facet version.
 */
public final class TargetRuntimePropertyTester extends PropertyTester {
  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IProjectFacet facet = ProjectFacetsManager.getProjectFacet((String) expectedValue);
    if (facet == null || receiver == null) {
      return false;
    }
    @SuppressWarnings("unchecked")
    Set<IRuntime> runtimes = (Set<IRuntime>) receiver;
    for (IRuntime runtime : runtimes) {
      if (runtime.supports(facet)) {
        return true;
      }
    }
    return false;
  }
}
