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
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.sdk.GWTUpdateProjectSdkCommand;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link GWTRuntimeContainerInitializer} class.
 * 
 * TODO: This class duplicates code from GaeSdkContainerInitializer, see if we
 * can unify.
 */
public class GWTRuntimeContainerInitializerTest extends AbstractGWTRuntimeTest {

  private static class ClasspathContainerAdapter implements IClasspathContainer {
    private final IClasspathContainer container;

    public ClasspathContainerAdapter(IClasspathContainer container) {
      this.container = container;
    }

    public IClasspathEntry[] getClasspathEntries() {
      return container.getClasspathEntries();
    }

    public String getDescription() {
      return container.getDescription();
    }

    public int getKind() {
      return container.getKind();
    }

    public IPath getPath() {
      return container.getPath();
    }
  }

  private Path defaultRuntimePath;

  private ClasspathContainerInitializer initializer;

  public void testCanUpdateClasspathContainer() {
    assertTrue(initializer.canUpdateClasspathContainer(
        GWTRuntimeContainer.CONTAINER_PATH, getTestProject()));

    assertFalse(initializer.canUpdateClasspathContainer(new Path(
        "InvalidContainer"), getTestProject()));
  }

  public void testInitialize() throws Exception {
    // Start with gwt-user.jar and gwt-dev-PLAT.jar on the classpath for this
    // test
    removeGWTRuntimeFromTestProject();

    // Add the default GWT runtime to the test project (this implicitly calls
    // the GWTRuntimeContainerInitializer.intialize(...) method.
    IJavaProject project = getTestProject();
    GWTUpdateProjectSdkCommand command = new GWTUpdateProjectSdkCommand(
        project, null, GWTPreferences.getDefaultRuntime(),
        UpdateType.DEFAULT_CONTAINER, null);
    command.execute();
    JobsUtilities.waitForIdle();

    // Verify the bound classpath container
    IClasspathContainer container = JavaCore.getClasspathContainer(
        defaultRuntimePath, project);
    assertEquals(IClasspathContainer.K_APPLICATION, container.getKind());
    assertEquals(defaultRuntimePath, container.getPath());
  }

  /**
   * TODO: We need to revisit this test. Since we don't allow container updates
   * right now, it is not clear that the test is sufficiently strong.
   */
  public void testRequestClasspathContainerUpdate() throws CoreException {
    IJavaProject testProject = getTestProject();

    IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(
        defaultRuntimePath, testProject);

    final List<IClasspathEntry> newClasspathEntries = new ArrayList<IClasspathEntry>();
    IClasspathEntry[] classpathEntries = classpathContainer.getClasspathEntries();
    for (IClasspathEntry classpathEntry : classpathEntries) {

      if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
        IClasspathAttribute[] extraAttributes = classpathEntry.getExtraAttributes();
        List<IClasspathAttribute> newAttributes = new ArrayList<IClasspathAttribute>();
        for (IClasspathAttribute extraAttribute : extraAttributes) {
          String attributeName = extraAttribute.getName();
          if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attributeName)) {
            String attributeValue = extraAttribute.getValue() + "modified";
            extraAttribute = JavaCore.newClasspathAttribute(attributeName,
                attributeValue);
          }

          newAttributes.add(extraAttribute);
        }

        IPath sourceAttachmentPath = new Path("/sourceAttachmentPath");
        IPath sourceAttachmentRootPath = new Path("sourceAttachmentRootPath");

        classpathEntry = JavaCore.newLibraryEntry(classpathEntry.getPath(),
            sourceAttachmentPath, sourceAttachmentRootPath,
            classpathEntry.getAccessRules(),
            newAttributes.toArray(new IClasspathAttribute[0]),
            classpathEntry.isExported());
      }

      newClasspathEntries.add(classpathEntry);
    }

    // Update the classpath container
    initializer.requestClasspathContainerUpdate(defaultRuntimePath,
        testProject, new ClasspathContainerAdapter(classpathContainer) {
          @Override
          public IClasspathEntry[] getClasspathEntries() {
            return newClasspathEntries.toArray(new IClasspathEntry[0]);
          }
        });

    // Check that the modifications took effect
    IClasspathContainer updatedContainer = JavaCore.getClasspathContainer(
        defaultRuntimePath, testProject);
    for (IClasspathEntry classpathEntry : updatedContainer.getClasspathEntries()) {
      if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
        // Ignore all non-library entries
        continue;
      }

      for (IClasspathAttribute attribute : classpathEntry.getExtraAttributes()) {
        if (IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME.equals(attribute.getName())) {
          String value = attribute.getValue();
          assertTrue(value.endsWith("modified"));
        }
      }

      IPath sourceAttachmentPath = classpathEntry.getSourceAttachmentPath();
      assertEquals(new Path("/sourceAttachmentPath"), sourceAttachmentPath);

      IPath sourceAttachmentRootPath = classpathEntry.getSourceAttachmentRootPath();
      assertEquals(new Path("sourceAttachmentRootPath"),
          sourceAttachmentRootPath);
    }
  }

  @Override
  protected boolean requiresTestProject() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    initializer = JavaCore.getClasspathContainerInitializer(GWTRuntimeContainer.CONTAINER_ID);

    SdkSet<GWTRuntime> sdkSet = GWTPreferences.getSdks();
    GWTJarsRuntime specificRuntime = new GWTJarsRuntime("specific",
        GwtRuntimeTestUtilities.getDefaultRuntime().getInstallationPath());
    sdkSet.add(this.specificRuntime);
    GWTPreferences.setSdks(sdkSet);

    defaultRuntimePath = GWTRuntimeContainer.CONTAINER_PATH;
    specificRuntimePath = GWTRuntimeContainer.CONTAINER_PATH.append(specificRuntime.getName());
  }
}
