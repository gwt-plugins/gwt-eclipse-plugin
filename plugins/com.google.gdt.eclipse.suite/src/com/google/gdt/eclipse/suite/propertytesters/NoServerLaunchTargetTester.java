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
package com.google.gdt.eclipse.suite.propertytesters;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IResource;

/**
 * Tests whether or not the selected item is launchable in GWT's "-noserver"
 * mode.
 */
public class NoServerLaunchTargetTester extends LaunchTargetTester {

  @Override
  public boolean test(Object receiver, String property, Object[] args,
      Object expectedValue) {

    assert (receiver != null);
    IResource resource = AdapterUtilities.getAdapter(receiver, IResource.class);

    if (resource == null) {
      // Unexpected case; we were asked to test against something that's
      // not a resource.
      return false;
    }

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    boolean isStandardLaunchTarget = super.test(receiver, property, args,
        expectedValue);

    return isStandardLaunchTarget && !isGaeProjectButNotGwtProject(resource);
  }

  private boolean isGaeProjectButNotGwtProject(IResource resource) {
    return GaeNature.isGaeProject(resource.getProject())
        && !GWTNature.isGWTProject(resource.getProject());
  }
}
