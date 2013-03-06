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
package com.google.gdt.eclipse.core.launch;

import com.google.gdt.eclipse.core.jobs.JobsUtilities;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Helper class for launch processor tests. This is to be used via composition.
 * It contains common objects for most launch processor tests.
 * 
 * This assumes the default SDKs are set up already.
 */
public class LaunchConfigurationProcessorTestingHelper {

  private IProject project;
  private ILaunchConfigurationWorkingCopy launchConfig;

  public ILaunchConfigurationWorkingCopy getLaunchConfig() {
    return launchConfig;
  }

  public IProject getProject() {
    return project;
  }

  public void setUp(String projectName,
      IWebAppProjectCreator.Participant... participants) throws Exception {
    project = ProjectUtilities.createPopulatedProject(projectName, participants);
    JobsUtilities.waitForIdle();
    launchConfig = LaunchConfigurationUtilities.getLaunchConfigurations(project).get(
        0).getWorkingCopy();
  }

  public void tearDown() throws CoreException {
    project.delete(true, true, null);
  }

}
