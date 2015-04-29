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
package com.google.gwt.eclipse.wtp.maven.test;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.suite.wizards.WebAppProjectCreator;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.testing.GwtFacetTestingUtilities;
import com.google.gwt.eclipse.testing.GwtMavenTestingUtilities;
import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;
import com.google.gwt.eclipse.testing.GwtTestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import java.util.Collections;
import java.util.List;

/**
 * Test the GWT facet detection with a Maven project.
 */
public class GwtFacetTest extends TestCase {

  private final LaunchConfigurationProcessorTestingHelper helper =
      new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    GwtTestUtilities.setUp();

    // Create a default standard Java project
    ProjectUtilities.setWebAppProjectCreatorFactory(WebAppProjectCreator.FACTORY);

    // Setup the GWTRuntime
    GwtRuntimeTestUtilities.addDefaultRuntime();

    // Create project
    helper.setUp(GwtFacetTest.class.getSimpleName(), new GwtEnablingProjectCreationParticipant());

    // Persist the module entry points, like GWTSettingsTab
    // This is required for the super dev mode linker validation in GWT 2.5 to < 2.7
    IProject project = helper.getProject();
    List<String> modules = GWTProjectProperties.getEntryPointModules(project);
    GWTLaunchConfigurationWorkingCopy.setEntryPointModules(helper.getLaunchConfig(), modules,
        Collections.<String>emptyList());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    helper.tearDown();
  }

  public void testMavenGwtFacetDetection() throws Exception {
    // Given a maven project
    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.7.0");

    // When the pom.xml is retrieved
    IPath pomPath = helper.getProject().getFullPath().append("pom.xml");
    IResource pomFile = ResourceUtils.getResource(pomPath);

    // Then pom.xml exists.
    assertNotNull(pomFile);
    // And project exists
    assertNotNull(helper.getProject());
    // And has the GWT facet
    assertTrue(GwtFacetTestingUtilities.hasGwtFacet(helper.getProject()));
  }

}
