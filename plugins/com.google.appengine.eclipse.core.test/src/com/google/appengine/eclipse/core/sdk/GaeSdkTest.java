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
package com.google.appengine.eclipse.core.sdk;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.validators.java.PluginTestUtils;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * Tests for {@link GaeSdk}.
 */
public class GaeSdkTest extends TestCase {
  private IJavaProject javaProject;

  public void testApiJarLocationAssumption() {
    GaeSdk defaultSdk = GaePreferences.getDefaultSdk();
    IPath path = defaultSdk.getInstallationPath().append("lib/user");
    File file = path.toFile();
    String[] list = file.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.matches("appengine\\-api\\-.*\\-sdk\\-.*\\.jar");
      }
    });

    assertEquals(1, list.length);
  }

  /**
   * Tests {@link GaeSdk#findSdkFor(IJavaProject)} when the project is using
   * classpath containers.
   */
  public void testFindSdkFor_UsingClasspathContainers()
      throws JavaModelException {
    IPath containerPath = SdkClasspathContainer.computeDefaultContainerPath(GaeSdkContainer.CONTAINER_ID);
    IClasspathEntry expectedEntry = JavaCore.newContainerEntry(containerPath);
    JavaProjectUtilities.addRawClassPathEntry(javaProject, expectedEntry);
    GaeSdk detectedSdk = GaeSdk.findSdkFor(javaProject);
    assertEquals(new IClasspathEntry[] {expectedEntry},
        detectedSdk.getClasspathEntries());
  }

  // FIXME: Create a test plugin fragment for the com.google.gdt.eclipse.maven
  // plugin, and move this test code
  // into there.
  // public void testGetAppEngineSdkPathInMavenRepoImpl() {
  // // Test typical case
  // IPath mavenLibPath =
  // Path.fromPortableString("/home/foo/.m2/repository/com/google/appengine/appengine-1.0-sdk/1.3.2/appengine-1.0-sdk-1.3.2.jar");
  // IPath appEngineSdkPath =
  // GaeSdk.ProjectBoundSdk.getAppEngineSdkPathInMavenRepoImpl(mavenLibPath);
  // assertEquals(
  // "/home/foo/.m2/repository/com/google/appengine/appengine-java-sdk/1.3.2/appengine-java-sdk-1.3.2/",
  // appEngineSdkPath.toPortableString());
  //
  // // Null case
  // mavenLibPath = null;
  // appEngineSdkPath =
  // GaeSdk.ProjectBoundSdk.getAppEngineSdkPathInMavenRepoImpl(mavenLibPath);
  // assertEquals(null, appEngineSdkPath);
  //
  // // Windows case - note that the test data is an approximation of where the
  // // Maven repo on a Windows sytem would be located;
  // // it is not exact
  // mavenLibPath =
  // Path.fromPortableString("C:/Documents and Settings/foo/Application Data/.m2/com/google/appengine/appengine-1.0-sdk/1.3.2/appengine-1.0-sdk-1.3.2.jar");
  // appEngineSdkPath =
  // GaeSdk.ProjectBoundSdk.getAppEngineSdkPathInMavenRepoImpl(mavenLibPath);
  // assertEquals(
  // "C:/Documents and Settings/foo/Application Data/.m2/com/google/appengine/appengine-java-sdk/1.3.2/appengine-java-sdk-1.3.2/",
  // appEngineSdkPath.toPortableString());
  // }

  /**
   * Tests {@link GaeSdk#findSdkFor(IJavaProject)} when the project is using raw
   * jars.
   *
   * FIXME: This test has been disabled until
   * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=8
   * is fixed.
   */
  // public void testFindSdkFor_UsingRawJars() throws JavaModelException {
  // GaeSdk defaultSdk = GaePreferences.getDefaultSdk();
  // IClasspathEntry[] classpathEntries = defaultSdk.getClasspathEntries();
  // for (IClasspathEntry classpathEntry : classpathEntries) {
  // JavaProjectUtilities.addRawClassPathEntry(javaProject, classpathEntry);
  // }
  //
  // GaeSdk detectedSdk = GaeSdk.findSdkFor(javaProject);
  // assertEquals(classpathEntries, detectedSdk.getClasspathEntries());
  // }
  @Override
  protected void setUp() throws Exception {
    GaeSdkTestUtilities.addDefaultSdk();
    IJavaProject findJavaProject = JavaProjectUtilities.findJavaProject(getName());
    if (findJavaProject != null) {
      findJavaProject.getProject().delete(true, null);
    }
    javaProject = PluginTestUtils.createProject(getName());
  }

  @Override
  protected void tearDown() throws Exception {
    javaProject.getProject().delete(true, null);
    PluginTestUtils.removeDefaultGaeSdk();
  }

  private void assertEquals(Object[] expected, Object[] actual) {
    assertTrue("expected:<" + Arrays.toString(expected) + "> but was:<"
        + Arrays.toString(actual) + ">", Arrays.equals(expected, actual));
  }

}
