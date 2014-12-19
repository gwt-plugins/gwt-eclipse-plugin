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
package com.google.appengine.eclipse.webtools.facet;

import com.google.gcp.eclipse.testing.ProjectTestUtil;
import com.google.gcp.eclipse.testing.TestUtil;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Tests for {@link JpaFacetHelper}.
 */
public class JpaFacetHelperTest extends TestCase {
  private JpaProject jpaProject;

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testGetPersistence() throws Exception {
    IJavaProject jProject = ProjectTestUtil.createProject("FOO");
    IProject project = jProject.getProject();
    ProjectTestUtil.createPersistenceFile(project);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, null);
    facetedProject.installProjectFacet(JavaFacet.FACET.getDefaultVersion(), null, null);
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    workingCopy.addProjectFacet(JpaProject.FACET.getDefaultVersion());
    workingCopy.commitChanges(null);

    JpaProjectManager manager =
        (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(JpaProjectManager.class);
    for (int i = 0; manager.getJpaProjectsSize() == 0 && i < 4; i++) {
      TestUtil.delay(250);
    }
    assertTrue("Failed to create a JPA project in a timely manner",
        manager.getJpaProjectsSize() > 0);
    jpaProject = manager.getJpaProjects().iterator().next();
    try {
      assertFalse(JpaFacetHelper.getPersistence(jpaProject) == null);
    } catch (Exception e) {
      fail ("JpaFacetHelper.getPersistence() should always succeed for supported versions of JPA");
    }
  }
}
