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
package com.google.gdt.eclipse.core.projects;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests for the {@link ProjectChangeTimestampTracker}. Currently disabled due
 * to http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=15
 */
public class ProjectChangeTimestampTrackerTest extends TestCase {

  private IProject project;

  public void testChanges() throws Exception {
    // // Wait for file changes to settle
    // JobsUtilities.waitForIdle();
    //
    // // File in the WAR output directory should not count as a change
    // final long previousStamp = getProjectStamp();
    // IFile htmlFile = project.getFolder("war").getFile(
    // project.getName() + ".html");
    // htmlFile.touch(new NullProgressMonitor());
    // /*
    // * The IResourceChangeEvent propagates through the event system
    // * synchronously as part of the IFile.touch method, so it is safe to check
    // * the project's time stamp now.
    // */
    // assertTrue(previousStamp == getProjectStamp());
    //
    // // Source file should count as a change
    // final long previousStamp2 = getProjectStamp();
    // IFile sourceFile = project.getFolder("src").getFile("dummyFile");
    // /*
    // * Note that this action causes two IResourceChangeEvents to occur. One
    // * results from the createFile call, and the other results from the Java
    // * builder asynchronously running to copy the src/dummy file over to
    // * classes/dummy. The second, asynchronous event does not interfere wth
    // the
    // * test because the ProjectChangeTimestampTracker ignores any resource
    // * deltas that occur in output folders.
    // */
    // ResourceUtils.createFile(sourceFile.getFullPath(), "Dummy file!");
    // assertTrue(previousStamp2 != getProjectStamp());
    //
    // // After project's WAR is set to unmanaged, file in WAR directory should
    // // count as change
    // final long previousStamp3 = getProjectStamp();
    // /*
    // * Note that setting this property causes a IResourceChangedEvent to
    // occur,
    // * because a .settings file is modified. This event does not interfere
    // with
    // * the test because we're ignoring any changes to hidden files.
    // */
    // WebAppProjectProperties.setWarSrcDirIsOutput(project, false);
    // htmlFile = project.getFolder("war").getFile(project.getName() + ".html");
    // htmlFile.touch(new NullProgressMonitor());
    // assertTrue(previousStamp3 != getProjectStamp());
  }

  @Override
  protected void setUp() throws Exception {
//    TestUtilities.setUp();
//
//    GaeSdkTestUtilities.addDefaultSdk();
//    GwtRuntimeTestUtilities.addDefaultRuntime();
//
//    project = ProjectUtilities.createPopulatedProject(
//        ProjectChangeTimestampTrackerTest.class.getSimpleName(),
//        new GwtEnablingProjectCreationParticipant(),
//        new GaeEnablingProjectCreationParticipant());
  }

  @Override
  protected void tearDown() throws Exception {
//    project.delete(true, true, new NullProgressMonitor());
  }

  private long getProjectStamp() throws CoreException {
    return ProjectChangeTimestampTracker.getProjectTimestamp(project);
  }

}
