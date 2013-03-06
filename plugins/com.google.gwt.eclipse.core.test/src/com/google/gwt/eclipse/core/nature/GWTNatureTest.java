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
package com.google.gwt.eclipse.core.nature;

import com.google.gwt.eclipse.core.test.AbstractGWTPluginTestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Test cases for the {@link GWTNature} class.
 */
public class GWTNatureTest extends AbstractGWTPluginTestCase {

  public void testAddNatureToProject() throws CoreException {
    IProject project = getTestProject().getProject();

    // Make sure the test starts with the GWT nature removed
    GWTNature.removeNatureFromProject(project);
    assertFalse(GWTNature.isGWTProject(project));

    // Now add the nature and verify it was added
    GWTNature.addNatureToProject(project);
    assertTrue(GWTNature.isGWTProject(project));
  }

  public void testIsGWTProject() throws CoreException {
    IProject project = getTestProject().getProject();
    assertEquals(project.hasNature(GWTNature.NATURE_ID),
        GWTNature.isGWTProject(project));
  }

  public void testRemoveNatureFromProject() throws CoreException {
    IProject project = getTestProject().getProject();

    // Make sure the test starts with the GWT nature added
    GWTNature.addNatureToProject(project);
    assertTrue(GWTNature.isGWTProject(project));

    // Now remove the nature and verify it's gone
    GWTNature.removeNatureFromProject(project);
    assertFalse(GWTNature.isGWTProject(project));
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

}
