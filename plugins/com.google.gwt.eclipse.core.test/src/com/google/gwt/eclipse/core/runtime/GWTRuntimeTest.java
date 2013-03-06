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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.TestUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer.Type;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.Arrays;

/**
 * Tests the {@link GWTRuntime} class.
 */
public class GWTRuntimeTest extends TestCase {
  private static void assertEquals(IClasspathEntry[] expected, IClasspathEntry[] actual) {
    assertTrue("Expected " + Arrays.toString(expected) + " does not match actual "
        + Arrays.toString(actual), Arrays.equals(expected, actual));
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when the default classpath
   * containers for jars are being used on the project classpath.
   * 
   * @throws Exception
   */
  public void testFindSdkFor_DefaultClasspathContainers_UsingJars() throws Exception {
    IPath installationDir = GwtRuntimeTestUtilities.getDefaultRuntime().getInstallationPath();
    checkSdkDetectionUsingClasspathContainers(installationDir, Type.DEFAULT);
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when named classpath containers
   * for jars are being used on the project classpath.
   * 
   * @throws Exception
   */
  public void testFindSdkFor_DefaultClasspathContainers_UsingProjects() throws Exception {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    IPath sdkPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    checkSdkDetectionUsingClasspathContainers(sdkPath, Type.DEFAULT);
  }

  /**
   * Tests that we find an {@link com.google.gdt.eclipse.core.sdk.Sdk} on the
   * gwt-user project. Note this test uses gwt-dev instead of gwt-dev-${PLAT} as
   * the dev project name.
   */
  public void testFindSdkFor_GwtDevProject() throws Exception {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    try {
      IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      IJavaProject javaProject = javaModel.getJavaProject("gwt-dev");
      GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
      assertEquals(new IClasspathEntry[] {JavaCore.newSourceEntry(javaModel.getJavaProject(
          "gwt-dev").getPath().append("core/src"))}, sdk.getClasspathEntries());
    } finally {
      GwtRuntimeTestUtilities.removeGwtSourceProjects();
    }
  }

  /**
   * Tests that we find an {@link com.google.gdt.eclipse.core.sdk.Sdk} on the
   * gwt-user project.
   * 
   * @throws Exception
   */
  public void testFindSdkFor_GwtUserProject() throws Exception {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    try {
      IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      IJavaProject javaProject = javaModel.getJavaProject("gwt-user");
      GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
      IClasspathEntry gwtUserEntry =
          JavaCore.newSourceEntry(
              javaModel.getJavaProject("gwt-user").getPath().append("core/src"),
              new IPath[] {new Path("**/super/**")});
      /*
       * NOTE: Passing null for the IClasspathAttribute array tickles a bug in
       * eclipse 3.3.
       */
      IClasspathEntry gwtDevEntry =
          JavaCore.newProjectEntry(javaModel.getJavaProject("gwt-dev").getPath(), null, false,
              new IClasspathAttribute[0] /* */, false);
      IClasspathEntry[] expected = new IClasspathEntry[] {gwtUserEntry, gwtDevEntry};
      assertEquals(expected, sdk.getClasspathEntries());
    } finally {
      GwtRuntimeTestUtilities.removeGwtSourceProjects();
    }
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when named classpath containers
   * for jars are being used on the project classpath.
   */
  public void testFindSdkFor_NamedClasspathContainers_UsingJars() throws Exception {
    IPath installationDir = GwtRuntimeTestUtilities.getDefaultRuntime().getInstallationPath();
    checkSdkDetectionUsingClasspathContainers(installationDir, Type.NAMED);
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when named classpath containers
   * for jars are being used on the project classpath.
   * 
   * @throws Exception
   */
  public void testFindSdkFor_NamedClasspathContainers_UsingProjects() throws Exception {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    IPath sdkPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    checkSdkDetectionUsingClasspathContainers(sdkPath, Type.NAMED);
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when raw jars are being used on
   * the project classpath.
   */
  public void testFindSdkFor_RawJars() throws Exception {
    IPath installationDir = GwtRuntimeTestUtilities.getDefaultRuntime().getInstallationPath();
    checkSdkDetectionUsingRawClasspathEntries(installationDir);
  }

  /**
   * Tests that {@link GWTRuntime#findSdkFor(IJavaProject)} returns a valid
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} when raw projects are being
   * used on the project classpath.
   * 
   * @throws Exception
   */
  public void testFindSdkFor_RawProjects() throws Exception {
    GwtRuntimeTestUtilities.importGwtSourceProjects();
    IPath sdkPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    checkSdkDetectionUsingRawClasspathEntries(sdkPath);
  }

  @Override
  protected void setUp() throws Exception {
    TestUtilities.setUp();
    GwtRuntimeTestUtilities.addDefaultRuntime();
  }

  @Override
  protected void tearDown() throws Exception {
    GwtRuntimeTestUtilities.removeDefaultRuntime();
  }

  private void checkSdkDetectionUsingClasspathContainers(IPath sdkPath,
      SdkClasspathContainer.Type containerType) throws CoreException {
    String sdkName = getName();
    GWTRuntime sdk = GWTRuntime.getFactory().newInstance(sdkName, sdkPath);
    SdkSet<GWTRuntime> registeredSdks = GWTPreferences.getSdks();
    registeredSdks.add(sdk);
    GWTPreferences.setSdks(registeredSdks);

    IPath containerPath =
        SdkClasspathContainer.computeContainerPath(GWTRuntimeContainer.CONTAINER_ID, sdk,
            containerType);

    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(getName());
    try {
      JavaProjectUtilities.addRawClassPathEntry(javaProject,
          JavaCore.newContainerEntry(containerPath));
      GWTRuntime detectedSdk = GWTRuntime.findSdkFor(javaProject);
      assertNotNull(detectedSdk);
      assertEquals(new IClasspathEntry[] {JavaCore.newContainerEntry(containerPath)},
          detectedSdk.getClasspathEntries());
    } finally {
      javaProject.getProject().delete(true, null);
      GWTPreferences.setSdks(new SdkSet<GWTRuntime>());
    }
  }

  private void checkSdkDetectionUsingRawClasspathEntries(IPath installPath) throws CoreException,
      JavaModelException {
    GWTRuntime sdk = GWTRuntime.getFactory().newInstance(getName(), installPath);
    IJavaProject javaProject = JavaProjectUtilities.createJavaProject(getName());
    try {
      for (IClasspathEntry entry : sdk.getClasspathEntries()) {
        JavaProjectUtilities.addRawClassPathEntry(javaProject, entry);
      }

      GWTRuntime detectedSdk = GWTRuntime.findSdkFor(javaProject);
      assertNotNull(detectedSdk);
      assertTrue(detectedSdk.validate().isOK());
    } finally {
      javaProject.getProject().delete(true, null);
    }
  }
}
