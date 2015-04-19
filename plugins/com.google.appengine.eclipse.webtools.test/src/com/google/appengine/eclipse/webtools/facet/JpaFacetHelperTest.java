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
import org.eclipse.jpt.common.utility.command.Command;
import org.eclipse.jpt.jpa.core.JpaPlatform.Version;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaProjectManager;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Tests for {@link JpaFacetHelper}.
 */
public class JpaFacetHelperTest extends TestCase {
  private static final String JPA_FACET_ID = "jpt.jpa"; //$NON-NLS-1$

  private JpaProject jpaProject;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    jpaProject = setupJpaProject();
    // JPA project setup appears to set up jobs that must complete before the project is fully
    // initialized, so wait for them to complete before testing the environment.
    TestUtil.waitForIdle();
  }

  @Override
  protected void tearDown() throws Exception {
    ProjectTestUtil.cleanup();
    super.tearDown();
  }

  public void testGetPersistence() throws Exception {
    try {
      assertFalse(JpaFacetHelper.getPersistence(jpaProject) == null);
    } catch (Exception e) {
      fail ("JpaFacetHelper.getPersistence() should succeed for supported versions of JPA");
    }
  }

  public void testGetJpaAnnotationDefinitionProvider() throws Exception {
    try {
      assertFalse(JpaFacetHelper.getJpaAnnotationDefinitionProvider() == null);
    } catch (Exception e) {
      fail ("JpaFacetHelper.getJpaAnnotationDefinitionProvider() should succeed "
          + "for supported versions of JPA");
    }
  }

  public void testGetJpaPlatformProvider() throws Exception {
    try {
      assertFalse(JpaFacetHelper.getJpaPlatformProvider() == null);
    } catch (Exception e) {
      fail ("JpaFacetHelper.getJpaPlatformProvider() should succeed "
          + "for supported versions of JPA");
    }
  }

  public void testGetJpaPlatformUiProvider() throws Exception {
    try {
      assertFalse(JpaFacetHelper.getJpaPlatformUiProvider() == null);
    } catch (Exception e) {
      fail ("JpaFacetHelper.getJpaPlatformUiProvider() should succeed "
          + "for supported versions of JPA");
    }
  }

  public void testSetJpaPlatformId() throws Exception {
    String platformId = "com.google.appengine.eclipse.wtp.jpa.GaePlatform";
    try {
      JpaFacetHelper.setJpaPlatformId(jpaProject.getProject(), platformId);
    } catch (Exception e) {
      fail("JpaFacetHelper.setJpaPlatformId() should succeed for supported versions of JPA");
    }
  }

  public void testBuildJpaVersion() throws Exception {
    try {
      Version version = JpaFacetHelper.buildJpaVersion();
      assertEquals("2.0", version.getVersion());
    } catch (Exception e) {
      fail("JpaFacetHelper.buildJpaVersion() should succeed for supported versions of JPA");
    }
  }

  public void testExecuteProjectManagerCommand() throws Exception {
    final boolean[] executed = new boolean[] {false};
    try {
      JpaFacetHelper.executeProjectManagerCommand(new Command() {
        @Override
        public void execute() {
          executed[0] = true;
        }
      });
    } catch (Exception e) {
      fail("JpaFacetHelper.executeProjectManagerCommand() should succeed for "
          + "supported versions of JPA: " + e.getMessage());
    }
    assertEquals("Command did not execute", true, executed[0]);
  }

  private JpaProject setupJpaProject() throws Exception {
    IJavaProject jProject = ProjectTestUtil.createProject("FOO");
    IProject project = jProject.getProject();
    ProjectTestUtil.createPersistenceFile(project);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, null);
    facetedProject.installProjectFacet(JavaFacet.FACET.getDefaultVersion(), null, null);
    IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
    IProjectFacetVersion jpaFacetVersion =
        ProjectFacetsManager.getProjectFacet(JPA_FACET_ID).getDefaultVersion();
    workingCopy.addProjectFacet(jpaFacetVersion);
    workingCopy.commitChanges(null);

    JpaProjectManager manager =
        (JpaProjectManager) ResourcesPlugin.getWorkspace().getAdapter(JpaProjectManager.class);
    for (int i = 0; manager.getJpaProjectsSize() == 0 && i < 4; i++) {
      TestUtil.delay(1000); // TODO was 250, but maven keeps having issues, so adding delay. (4/19/2014)
    }
    assertTrue("Failed to create a JPA project in a timely manner",
        manager.getJpaProjectsSize() > 0);
    return manager.getJpaProjects().iterator().next();
  }
}

