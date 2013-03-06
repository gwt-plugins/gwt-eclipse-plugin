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

import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.sdk.GWTUpdateProjectSdkCommand;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Tests the {@link GWTRuntimeContainer} class.
 */
public class GWTRuntimeContainerTest extends AbstractGWTRuntimeTest {

  private static IClasspathContainer getClasspathContainer(GWTRuntime sdk,
      IJavaProject project, SdkClasspathContainer.Type containerType)
      throws JavaModelException {
    IPath containerPath = SdkClasspathContainer.computeContainerPath(
        GWTRuntimeContainer.CONTAINER_ID, sdk, containerType);
    IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(
        containerPath, project);
    JobsUtilities.waitForIdle();
    return classpathContainer;
  }

  public void testGetClasspathEntries() throws JavaModelException {
    IClasspathEntry[] expectedCpEntries = specificRuntime.getClasspathEntries();
    IClasspathContainer specificRuntimeContainer = getClasspathContainer(
        specificRuntime, getTestProject(), SdkClasspathContainer.Type.NAMED);
    IClasspathEntry[] actualCpEntries = specificRuntimeContainer.getClasspathEntries();
    assertEquals(expectedCpEntries.length, actualCpEntries.length);

    for (int i = 0; i < actualCpEntries.length; i++) {
      assertEquals(expectedCpEntries[i].getPath(), actualCpEntries[i].getPath());
    }
  }

  public void testIsPathForGWTRuntimeContainer() {
    assertTrue(GWTRuntimeContainer.isPathForGWTRuntimeContainer(defaultRuntimePath));
    assertTrue(GWTRuntimeContainer.isPathForGWTRuntimeContainer(specificRuntimePath));

    IPath jrePath = new Path("org.eclipse.jdt.launching.JRE_CONTAINER");
    assertFalse(GWTRuntimeContainer.isPathForGWTRuntimeContainer(jrePath));
  }

  public void testUpdateProjectClasspathWithGWTJars() throws Exception {
    IJavaProject project = getTestProject();

    // Start with gwt-user.jar and gwt-dev-PLAT.jar on the classpath for this
    // test
    removeGWTRuntimeFromTestProject();

    GWTRuntime sdk = GWTPreferences.getSdkManager().findSdkForPath(
        specificRuntimePath);
    // Replace existing gwt-user.jar with GWT runtime
    GWTUpdateProjectSdkCommand command = new GWTUpdateProjectSdkCommand(
        project, null, sdk, UpdateType.NAMED_CONTAINER, null);
    command.execute();
    JobsUtilities.waitForIdle();
    assertGWTRuntimeEntry(specificRuntimePath, project.getRawClasspath());
  }

  public void testUpdateProjectClasspathWithGWTRuntime() throws Exception {
    // Update existing GWT runtime
    IJavaProject project = getTestProject();
    GWTRuntime oldSdk = GWTRuntime.findSdkFor(project);
    GWTRuntime newSdk = GWTPreferences.getSdkManager().findSdkForPath(
        specificRuntimePath);

    // Replace existing gwt-user.jar with GWT runtime
    GWTUpdateProjectSdkCommand command = new GWTUpdateProjectSdkCommand(
        project, oldSdk, newSdk, UpdateType.NAMED_CONTAINER, null);

    command.execute();
    JobsUtilities.waitForIdle();
    assertGWTRuntimeEntry(specificRuntimePath, project.getRawClasspath());
  }
}
