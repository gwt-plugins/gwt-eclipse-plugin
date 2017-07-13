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
package com.google.gwt.eclipse.wtp.test;

// TODO Test is causes eclipse Oxygen to fail to load
//import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
//import com.google.gdt.eclipse.core.projects.ProjectUtilities;
//import com.google.gdt.eclipse.suite.wizards.WebAppProjectCreator;
//import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
//import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
//import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
//import com.google.gwt.eclipse.testing.GwtFacetTestingUtilities;
//import com.google.gwt.eclipse.testing.GwtMavenTestingUtilities;
//import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;
//import com.google.gwt.eclipse.testing.GwtTestUtilities;
//
//import junit.framework.TestCase;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * Tests for the GWT facet. Like the SuperDevModeArgumentProcessorTest
// */
//public class GwtFacetTest extends TestCase {
//
//  private final LaunchConfigurationProcessorTestingHelper helper =
//      new LaunchConfigurationProcessorTestingHelper();
//
//  @Override
//  public void setUp() throws Exception {
//    GwtTestUtilities.setUp();
//
//    // Create a default standard Java project
//    ProjectUtilities.setWebAppProjectCreatorFactory(WebAppProjectCreator.FACTORY);
//
//    // Setup the GWTRuntime
//    GwtRuntimeTestUtilities.addDefaultRuntime();
//
//    // Turn on the GWT Nature which uses the default container
//    helper.setUp(GwtFacetTest.class.getSimpleName(), new GwtEnablingProjectCreationParticipant());
//
//    // Persist the module entry points, like GWTSettingsTab
//    // This is required for the super dev mode linker validation in GWT 2.5 to < 2.7
//    List<String> modules = GWTProjectProperties.getEntryPointModules(helper.getProject());
//    GWTLaunchConfigurationWorkingCopy.setEntryPointModules(helper.getLaunchConfig(), modules,
//        Collections.<String>emptyList());
//  }
//
//  @Override
//  protected void tearDown() throws Exception {
//    super.tearDown();
//
//    helper.tearDown();
//  }
//
//  public void testGwtFacetDetectionForGwt27() throws Exception {
//    // Given a GWT 2.7 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.7.0");
//
//    // When the project is asserted for GWT facet
//    boolean hasFacet = GwtFacetTestingUtilities.hasGwtFacet(helper.getProject());
//
//    // Then it has it
//    assertTrue(hasFacet);
//  }
//
//  public void testGwtFacetDetectionForGwt261() throws Exception {
//    // Given a GWT 2.6.1 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.6.1");
//
//    // When the project is asserted for GWT facet
//    boolean hasFacet = GwtFacetTestingUtilities.hasGwtFacet(helper.getProject());
//
//    // Then it has it
//    assertTrue(hasFacet);
//  }
//
//}
