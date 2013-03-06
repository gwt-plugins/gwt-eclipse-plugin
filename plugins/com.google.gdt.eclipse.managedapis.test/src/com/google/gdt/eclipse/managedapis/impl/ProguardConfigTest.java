/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.managedapis.EclipseProject;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Tests for the behavior of adding Google APIs in the presence of Proguard
 * configuration information for an Android project.
 * 
 * TODO(rdayal): Add tests for:
 * 
 * <p>
 * addition of an API to a project that already has the proguard api config
 * referenced in the project.properties file
 * </p>
 * 
 * <p>
 * malformed/missing descriptor.json file
 * </p>
 * 
 * <p>
 * mismatch of the location of the api proguard config file based on what's in
 * descriptor.json
 * </p>
 * 
 * <p>
 * missing project.properties file
 * </p>
 */
public class ProguardConfigTest extends TestCase {

  // TODO: Load descriptor using Gson codec and ProguardInfo, so we don't have
  // to hardcode the name of the api config file.
  private static final String PROGUARD_GOOGLE_API_CLIENT_TXT = "proguard-google-api-client.txt";

  private static final String PROJECT_NAME = ProguardConfigTest.class.getName();
  private static String DESCRIPTOR_CONTENTS = null;

  private static String API_PROGUARD_CONTENTS = null;
  private static String PROPERTIES_CONTENTS_COMMENTED_PROGUARD = null;

  private static String PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG = null;

  private IJavaProject jp = null;

  static {
    try {
      staticInit();
    } catch (CoreException e) {
      System.err.println("There was a problem initializing the static data.");
      e.printStackTrace(System.err);
    }
  }

  private static void staticInit() throws CoreException {
    DESCRIPTOR_CONTENTS = ResourceUtils.getResourceAsString(
        ProguardConfigTest.class, "descriptor.json");
    API_PROGUARD_CONTENTS = ResourceUtils.getResourceAsString(
        ProguardConfigTest.class, "proguard-google-api-client.txt");
    PROPERTIES_CONTENTS_COMMENTED_PROGUARD = ResourceUtils.getResourceAsString(
        ProguardConfigTest.class, "project.properties.commentedpg");
    PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG = ResourceUtils.getResourceAsString(
        ProguardConfigTest.class, "project.properties.uncommentedpgwithapi");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Create a java project
    jp = JavaProjectUtilities.createJavaProject(PROJECT_NAME);
  }

  /**
   * Verify that the before/after {@code State} works as expected given the
   * addition of a managed API to a project that previously did not have any
   * managed APIs.
   * 
   * @throws Exception
   */
  public void testStateAddManagedApi() throws Exception {

    final String apiName = "testapi";
    final String projRelRootApiFolder = ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_DEFAULT_PATH
        + "/" + apiName + "-v0";

    // Create project.properties file to make this an 'android' project
    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(ProguardState.PROJECT_PROPERTIES),
        PROPERTIES_CONTENTS_COMMENTED_PROGUARD);

    // Create metadata for addition of managed API
    ResourceUtils.createFolderStructure(jp.getProject(), new Path(
        projRelRootApiFolder));

    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(
            projRelRootApiFolder + "/"
                + ManagedApiJsonClasses.DESCRIPTOR_FILENAME),
        DESCRIPTOR_CONTENTS);

    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(
            projRelRootApiFolder + "/" + PROGUARD_GOOGLE_API_CLIENT_TXT),
        API_PROGUARD_CONTENTS);

    ManagedApi managedApi = new MockManagedApi(jp.getProject().getFolder(
        projRelRootApiFolder), apiName);

    EclipseProject ep = new EclipseJavaProject(jp);

    ProguardState before = ProguardState.createFromCurrent(null, ep);

    // Assert that the 'before' state is captured correctly
    assertEquals(before.androidProjPropertiesContents,
        PROPERTIES_CONTENTS_COMMENTED_PROGUARD);
    assertEquals(before.apiProguardConfigContents, null);

    ProguardConfig pc = new ProguardConfig(managedApi, ep);

    ProguardState after = ProguardState.createForFuture(before, pc, ep);

    /*
     * Assert that the 'after' state is captured correctly; this is really a
     * manipulation of the before state based on the pending managed API update.
     */
    assertEquals(after.androidProjPropertiesContents,
        PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG);
    assertEquals(after.apiProguardConfigContents, API_PROGUARD_CONTENTS);

    /*
     * Show that there was no proguard config file for APIs before the
     * application of the 'after' state.
     */
    assertTrue(!jp.getProject().getFile(PROGUARD_GOOGLE_API_CLIENT_TXT).exists());

    after.apply();

    assertEquals(
        ResourceUtils.readFileContents(jp.getProject().getLocation().append(
            ProguardState.PROJECT_PROPERTIES)), after.androidProjPropertiesContents);
    assertTrue(jp.getProject().getFile(PROGUARD_GOOGLE_API_CLIENT_TXT).exists());

    // Show that we can get back to the before state just by applying it
    before.apply();

    assertEquals(
        ResourceUtils.readFileContents(jp.getProject().getLocation().append(
            ProguardState.PROJECT_PROPERTIES)), before.androidProjPropertiesContents);

    /*
     * We don't delete the proguard api config file when applying the 'before'
     * state, just in case it was already there and not added by us..
     */
    assertTrue(jp.getProject().getFile(PROGUARD_GOOGLE_API_CLIENT_TXT).exists());
  }

  /**
   * Verify that the before/after {@code State} works as expected given the
   * removal of a managed API to a project that only has one managed API.
   * 
   * @throws Exception
   */
  public void testStateRemoveManagedApi() throws Exception {

    final String apiName = "testapi";
    final String projRelRootApiFolder = ManagedApiPlugin.MANAGED_API_ROOT_FOLDER_DEFAULT_PATH
        + "/" + apiName + "-v0";

    // Create project.properties file to make this an 'android' project
    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(ProguardState.PROJECT_PROPERTIES),
        PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG);

    // Create metadata for removal of managed API
    ResourceUtils.createFolderStructure(jp.getProject(), new Path(
        projRelRootApiFolder));

    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(
            projRelRootApiFolder + "/"
                + ManagedApiJsonClasses.DESCRIPTOR_FILENAME),
        DESCRIPTOR_CONTENTS);

    // Create Proguard Api config file at the root of the project
    ResourceUtils.createFile(
        jp.getProject().getFullPath().append(PROGUARD_GOOGLE_API_CLIENT_TXT),
        API_PROGUARD_CONTENTS);

    ManagedApi managedApi = new MockManagedApi(jp.getProject().getFolder(
        projRelRootApiFolder), apiName);

    EclipseProject ep = new EclipseJavaProject(jp);
    ProguardConfig pc = new ProguardConfig(managedApi, ep);

    ProguardState before = ProguardState.createFromCurrent(pc, ep);

    // Assert that the 'before' state is captured correctly
    assertEquals(before.androidProjPropertiesContents,
        PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG);
    assertEquals(before.apiProguardConfigContents, API_PROGUARD_CONTENTS);

    ProguardState after = ProguardState.createForFuture(before, null, ep);

    /*
     * Assert that the 'after' state is what we expect - that is, the
     * proguard.config line in the project.properties file remains unchanged,
     * even if there is a reference to the Proguard Api Config file.
     */
    assertEquals(after.androidProjPropertiesContents,
        PROPERTIES_CONTENTS_UNCOMMENTED_PROGUARD_WITH_APICONFIG);
    assertEquals(after.apiProguardConfigContents, null);

    after.apply();

    /*
     * Assert that when we apply the after state, the Proguard API config file
     * still exists.
     */
    assertTrue(jp.getProject().getFile(PROGUARD_GOOGLE_API_CLIENT_TXT).exists());
  }

  @Override
  protected void tearDown() throws Exception {
    ProjectUtilities.deleteProject(PROJECT_NAME, true);
    jp = null;
    super.tearDown();
  }

}
