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
package com.google.gwt.eclipse.core.launch.processors;

import junit.framework.TestCase;

/**
 * Tests the {@link SuperDevModeArgumentProcessor}.
 */
public class SuperDevModeArgumentProcessorTest extends TestCase {

  public void testNothing() {
    // TODO: These tests will not run in tycho. Disabling till later.
  }

//  private final LaunchConfigurationProcessorTestingHelper helper = new LaunchConfigurationProcessorTestingHelper();
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
//    helper.setUp(SuperDevModeArgumentProcessorTest.class.getSimpleName(), new GwtEnablingProjectCreationParticipant());
//
//    // Persist the module entry points, like GWTSettingsTab
//    // This is required for the super dev mode linker validation in GWT 2.5 to < 2.7
//    List<String> modules = GWTProjectProperties.getEntryPointModules(helper.getProject());
//    GWTLaunchConfigurationWorkingCopy.setEntryPointModules(helper.getLaunchConfig(), modules,
//        Collections.<String> emptyList());
//  }
//
//  @Override
//  protected void tearDown() throws Exception {
//    super.tearDown();
//
//    helper.tearDown();
//  }
//
//  /**
//   * Test a GWT 2.4 project that no errors return
//   */
//  public void testNoErrorsWithSuperDevModeProcessorInGwt24() throws Exception {
//    // Given a GWT 2.4 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.4.0");
//
//    // When the Super Dev Mode processor validation is called, no errors should return
//    List<String> programArgs = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
//    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
//    IJavaProject javaProject = JavaCore.create(helper.getProject());
//    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
//    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);
//
//    // Then verify
//    Assert.assertTrue(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
//    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
//    Assert.assertTrue(programArgs.indexOf("-superDevMode") == -1);
//    Assert.assertTrue(programArgs.indexOf("-nosuperDevMode") == -1);
//    // And then no errors will have been thrown
//    Assert.assertNull(error);
//  }
//
//  /**
//   * Test a GWT 2.5 project that no errors return
//   */
//  public void testNoErrorsWithSuperDevModeProcessorInGwt25() throws Exception {
//    // Given a GWT 2.5 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.5.1");
//
//    // When the Super Dev Mode processor validation is called for the project
//    List<String> programArgs = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
//    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
//    IJavaProject javaProject = JavaCore.create(helper.getProject());
//    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
//    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);
//
//    // Then verify
//    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
//    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
//    // And then no errors will have been thrown
//    Assert.assertNull(error);
//  }
//
//  /**
//   * Test a GWT 2.6 project that no errors return
//   */
//  public void testNoErrorsWithSuperDevModeProcessorInGwt26() throws Exception {
//    // Given a GWT 2.6 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.6.1");
//
//    // When the Super Dev Mode processor validation is called for the project
//    List<String> programArgs = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
//    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
//    IJavaProject javaProject = JavaCore.create(helper.getProject());
//    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
//    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);
//
//    // Then verify
//    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
//    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
//    // And then no errors will have been thrown
//    Assert.assertNull(error);
//  }
//
//  /**
//   * Test a GWT 2.7 project that no errors return
//   */
//  public void testNoErrorsWithSuperDevModeProcessorInGwt27() throws Exception {
//    // Given a GWT 2.7 Maven project
//    GwtMavenTestingUtilities.createMavenProject(helper.getProject(), "2.7.0");
//
//    // When the Super Dev Mode processor validation is called for the project
//    List<String> programArgs = LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
//    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
//    IJavaProject javaProject = JavaCore.create(helper.getProject());
//    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
//    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);
//
//    // Then verify
//    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
//    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
//    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
//    // And then no errors will have been thrown
//    Assert.assertNull(error);
//  }

}
