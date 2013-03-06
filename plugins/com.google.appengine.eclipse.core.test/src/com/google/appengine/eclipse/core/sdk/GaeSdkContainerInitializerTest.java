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

import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.validators.java.PluginTestUtils;

import junit.framework.TestCase;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
 * Test cases for {@link GaeSdkContainerInitializer}.
 * 
 * TODO: This class duplicates code from GaeSdkContainerInitializer, see if we
 * can unify.
 */
public class GaeSdkContainerInitializerTest extends TestCase {

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

  private GaeProject gaeProject;

  public void testCanUpdateClasspathContainerIPathIJavaProject() {
    ClasspathContainerInitializer intializer = JavaCore.getClasspathContainerInitializer(GaeSdkContainer.CONTAINER_ID);
    assertNotNull(intializer);
    
    assertTrue(intializer.canUpdateClasspathContainer(
        GaeSdkContainer.CONTAINER_PATH, gaeProject.getJavaProject()));
    
    assertFalse(intializer.canUpdateClasspathContainer(new Path("Nonexistent"),
        gaeProject.getJavaProject()));
  }

  public void testRequestClasspathContainerUpdateIPathIJavaProjectIClasspathContainer()
      throws CoreException {
    /*
    final IJavaProject testProject = gaeProject.getJavaProject();
    final IPath defaultContainerPath = GaeSdkContainer.CONTAINER_PATH;
    final ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(GaeSdkContainer.CONTAINER_ID);
    final IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(
        defaultContainerPath, testProject);

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

    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        // Update the classpath container
        initializer.requestClasspathContainerUpdate(defaultContainerPath,
            testProject, new ClasspathContainerAdapter(classpathContainer) {
              @Override
              public IClasspathEntry[] getClasspathEntries() {
                return newClasspathEntries.toArray(new IClasspathEntry[0]);
              }
            });
      }
    }, null);

    PluginTestUtils.waitForIdle();
      
    // Check that the modifications took effect
    IClasspathContainer updatedContainer = JavaCore.getClasspathContainer(
        defaultContainerPath, testProject);
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
    */
  }

  @Override
  protected void setUp() throws Exception {
    GaeSdkTestUtilities.addDefaultSdk();
    gaeProject = PluginTestUtils.createGaeProject("GaeProject");
  }

  @Override
  protected void tearDown() throws Exception {
    PluginTestUtils.removeDefaultGaeSdk();
    gaeProject.getProject().delete(true, null);
  }
}
