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

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.validators.java.PluginTestUtils;
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;

import junit.framework.TestCase;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Test case for {@link AppEngineUpdateProjectSdkCommand}.
 */
public class AppEngineUpdateProjectSdkCommandTest extends TestCase {

  private IJavaProject javaProject;

  /**
   * Tests that an expanded SDK gets replaced with the specified SDK container.
   */
  public void testExecute() throws CoreException {
    final GaeSdk defaultSdk = GaePreferences.getDefaultSdk();

    // Set the project's classpath contain the expanded container
    ClasspathUtilities.setRawClasspath(javaProject,
        defaultSdk.getClasspathEntries());
    PluginTestUtils.waitForIdle();

    final GaeSdk oldSdk = GaeSdk.findSdkFor(javaProject);

    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        try {
          AppEngineUpdateProjectSdkCommand command = new AppEngineUpdateProjectSdkCommand(
              javaProject, oldSdk, defaultSdk, UpdateType.DEFAULT_CONTAINER,
              null);
          command.execute();
        } catch (FileNotFoundException e) {
          throw new CoreException(StatusUtilities.newErrorStatus(e,
              AppEngineCorePlugin.PLUGIN_ID));
        } catch (BackingStoreException e) {
          throw new CoreException(StatusUtilities.newErrorStatus(e,
              AppEngineCorePlugin.PLUGIN_ID));
        }
      }
    }, null);

    PluginTestUtils.waitForIdle();
    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();

    // Ensure that the entries were collapsed back into a single container entry
    assertEquals(1, rawClasspath.length);
    assertEquals(GaeSdkContainer.CONTAINER_PATH, rawClasspath[0].getPath());
    assertEquals(IClasspathEntry.CPE_CONTAINER, rawClasspath[0].getEntryKind());
  }

  @Override
  protected void setUp() throws Exception {
    GaeSdkTestUtilities.addDefaultSdk();
    IJavaProject findJavaProject = JavaProjectUtilities.findJavaProject("TestProject");
    if (findJavaProject != null) {
      findJavaProject.getProject().delete(true, null);
    }
    javaProject = PluginTestUtils.createProject("TestProject");
  }

  @Override
  protected void tearDown() throws Exception {
    javaProject.getProject().delete(true, null);
    PluginTestUtils.removeDefaultGaeSdk();
  }
}
