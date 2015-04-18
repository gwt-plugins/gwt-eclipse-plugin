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

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorTestingHelper;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.suite.wizards.WebAppProjectCreator;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.projects.GwtEnablingProjectCreationParticipant;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.util.GwtVersionUtil;
import com.google.gwt.eclipse.testing.GwtRuntimeTestUtilities;
import com.google.gwt.eclipse.testing.TestUtilities;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

/**
 * Tests the {@link SuperDevModeArgumentProcessor}.
 */
public class SuperDevModeArgumentProcessorTest extends TestCase {
  // Copied from Maven2Utils to avoid the extra dependency
  private static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  private final LaunchConfigurationProcessorTestingHelper helper =
      new LaunchConfigurationProcessorTestingHelper();

  @Override
  public void setUp() throws Exception {
    TestUtilities.setUp();

    // Create a default standard Java project
    ProjectUtilities.setWebAppProjectCreatorFactory(WebAppProjectCreator.FACTORY);

    // Setup the GWTRuntime
    GwtRuntimeTestUtilities.addDefaultRuntime();

    // Turn on the GWT Nature which uses the default container
    helper.setUp(SuperDevModeArgumentProcessorTest.class.getSimpleName(),
        new GwtEnablingProjectCreationParticipant());

    // Persist the module entry points, like GWTSettingsTab
    // This is required for the super dev mode linker validation in GWT 2.5 to < 2.7
    List<String> modules = GWTProjectProperties.getEntryPointModules(helper.getProject());
    GWTLaunchConfigurationWorkingCopy.setEntryPointModules(helper.getLaunchConfig(), modules,
        Collections.<String>emptyList());

  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    helper.tearDown();
  }

  /**
   * Test a GWT 2.4 project that no errors return
   */
  public void testNoErrorsWithSuperDevModeProcessorInGwt24() throws Exception {
    // Given a GWT 2.4 Maven project
    createMavenProject("2.4.0");

    // When the Super Dev Mode processor validation is called, no errors should return
    List<String> programArgs =
        LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
    IJavaProject javaProject = JavaCore.create(helper.getProject());
    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);

    // Then verify
    Assert.assertTrue(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
    Assert.assertTrue(programArgs.indexOf("-superDevMode") == -1);
    Assert.assertTrue(programArgs.indexOf("-nosuperDevMode") == -1);
    // And then no errors will have been thrown
    Assert.assertNull(error);
  }

  /**
   * Test a GWT 2.5 project that no errors return
   */
  public void testNoErrorsWithSuperDevModeProcessorInGwt25() throws Exception {
    // Given a GWT 2.5 Maven project
    createMavenProject("2.5.1");

    // When the Super Dev Mode processor validation is called for the project
    List<String> programArgs =
        LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
    IJavaProject javaProject = JavaCore.create(helper.getProject());
    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);

    // Then verify
    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
    // And then no errors will have been thrown
    Assert.assertNull(error);
  }

  /**
   * Test a GWT 2.6 project that no errors return
   */
  public void testNoErrorsWithSuperDevModeProcessorInGwt26() throws Exception {
    // Given a GWT 2.6 Maven project
    createMavenProject("2.6.1");

    // When the Super Dev Mode processor validation is called for the project
    List<String> programArgs =
        LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
    IJavaProject javaProject = JavaCore.create(helper.getProject());
    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);

    // Then verify
    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
    // And then no errors will have been thrown
    Assert.assertNull(error);
  }

  /**
   * Test a GWT 2.7 project that no errors return
   */
  public void testNoErrorsWithSuperDevModeProcessorInGwt27() throws Exception {
    // Given a GWT 2.7 Maven project
    createMavenProject("2.7.0");

    // When the Super Dev Mode processor validation is called for the project
    List<String> programArgs =
        LaunchConfigurationProcessorUtilities.parseProgramArgs(helper.getLaunchConfig());
    ILaunchConfigurationWorkingCopy launchConfig = helper.getLaunchConfig();
    IJavaProject javaProject = JavaCore.create(helper.getProject());
    SuperDevModeArgumentProcessor sdmArgProcessor = new SuperDevModeArgumentProcessor();
    String error = sdmArgProcessor.validate(launchConfig, javaProject, programArgs, null);

    // Then verify
    Assert.assertFalse(GwtVersionUtil.isGwtVersionlessThan25(javaProject));
    // And then programArgs should not exist for -superDevMode and -nosuperDevMode
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-superDevMode"));
    Assert.assertEquals("Args: " + programArgs, -1, programArgs.indexOf("-nosuperDevMode"));
    // And then no errors will have been thrown
    Assert.assertNull(error);
  }

  /**
   * Convert the standard Java project to a Maven project. This will remove the Default GWT sdk and
   * instead use a GWT Maven sdk distribution. Using the Maven classpath container will allow for
   * adding a specific GWT version easily.
   *
   * TODO Embue the WebAppCreator factory or create a Maven web app factory with Maven creation
   * options.
   *
   * TODO extract to utility on reuse
   */
  private void createMavenProject(String withGwtSdkVersion) throws Exception {
    // Remove the default GWT sdk container from classpath, instead use Maven
    IJavaProject javaProject = JavaCore.create(helper.getProject());
    IClasspathEntry[] entriesWithGwtContainer = javaProject.getRawClasspath();
    IClasspathEntry[] entriesWithOutGwtContainer =
        new IClasspathEntry[entriesWithGwtContainer.length - 1];
    int b = 0;
    for (int a = 0; a < entriesWithGwtContainer.length; a++) {
      String path = entriesWithGwtContainer[a].getPath().toString();
      if (!path.contains(GWTRuntimeContainer.CONTAINER_ID)) {
        entriesWithOutGwtContainer[b] = entriesWithGwtContainer[a];
        b++;
      }
    }
    // Removing the GWT SDK classpath entry from project
    javaProject.setRawClasspath(entriesWithOutGwtContainer, new NullProgressMonitor());
    JobsUtilities.waitForIdle();

    // Provide a pom.xml for a bare-bones configuration to convert standard project to Maven nature
    InputStream pomxmlStream = getClass().getResourceAsStream("/../resources/pom.xml");
    pomxmlStream = changeGwtSdkVersionInPom(pomxmlStream, withGwtSdkVersion);
    ResourceUtils.createFile(helper.getProject().getFullPath().append("pom.xml"), pomxmlStream);

    // Turn on the Maven nature
    NatureUtils.addNature(helper.getProject(), MAVEN2_NATURE_ID);
    JobsUtilities.waitForIdle();

    // Maven update project will add the Maven dependencies to the classpath
    IProjectConfigurationManager projectConfig = MavenPlugin.getProjectConfigurationManager();
    projectConfig.updateProjectConfiguration(helper.getProject(), new NullProgressMonitor());
    JobsUtilities.waitForIdle();

    // Verify the expected GWT SDK version exists in project configuration
    GWTRuntime gwtSdk = GWTRuntime.findSdkFor(JavaCore.create(helper.getProject()));
    Assert.assertEquals(withGwtSdkVersion, gwtSdk.getVersion());
  }

  /**
   * Replace the pom.xml GWT version properties with provided GWT version.
   */
  private InputStream changeGwtSdkVersionInPom(InputStream pomxmlStream, String withGwtSdkVersion)
      throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(pomxmlStream));
    StringBuffer modifiedPom = new StringBuffer();
    String line;
    while ((line = reader.readLine()) != null) {
      String replaceWith = "<gwt.version>" + withGwtSdkVersion + "</gwt.version>";
      line = line.replaceFirst("<gwt.version>.*?</gwt.version>", replaceWith);
      replaceWith = "<gwt.plugin.version>" + withGwtSdkVersion + "</gwt.plugin.version>";
      line = line.replaceFirst("<gwt.plugin.version>.*?</gwt.plugin.version>", replaceWith);
      modifiedPom.append(line + "\n");
    }

    return new ByteArrayInputStream(modifiedPom.toString().getBytes());
  }
}
