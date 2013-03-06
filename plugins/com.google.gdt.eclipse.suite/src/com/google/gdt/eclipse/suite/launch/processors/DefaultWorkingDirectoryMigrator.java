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
package com.google.gdt.eclipse.suite.launch.processors;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.List;

/**
 * For projects that have the working directory set to their WAR directory, this
 * migrator will set the project to use the default working directory, which
 * will substitute to the WAR directory, even if that directory changes.
 */
public class DefaultWorkingDirectoryMigrator {
  public void migrate(IProject project) {
    try {
      List<ILaunchConfiguration> configs = LaunchConfigurationUtilities.getLaunchConfigurations(project);
      for (ILaunchConfiguration config : configs) {

        // This it not a web app launch config
        if (!WebAppLaunchConfiguration.TYPE_ID.equals(config.getType().getIdentifier())) {
          continue;
        }

        String workingDir = config.getAttribute(
            IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
            (String) null);
        // This is already set to the default working dir
        if (workingDir == null) {
          continue;
        }

        IJavaProject javaProject = LaunchConfigurationUtilities.getJavaProject(config);
        if (javaProject == null) {
          continue;
        }

        String warDir = WarArgumentProcessor.WarParser.parse(
            LaunchConfigurationProcessorUtilities.parseProgramArgs(config),
            javaProject).unverifiedWarDir;
        if (warDir == null) {
          continue;
        }

        IPath warDirPath = new Path(warDir);
        IPath workingDirPath = new Path(workingDir).makeRelative();
        // The WAR dir is absolute, but working dir can be workspace-relative.
        // We force the workingDirPath to be relative because the
        // warDirPath.removeFirstSegments returns a relative path.
        int numSegmentsToRemove = warDirPath.segmentCount()
            - workingDirPath.segmentCount();
        if (numSegmentsToRemove < 0
            || !warDirPath.removeFirstSegments(numSegmentsToRemove).equals(
            workingDirPath)) {
          continue;
        }

        // Working dir matches the WAR dir, set to default working dir
        ILaunchConfigurationWorkingCopy workingCopy = config.getWorkingCopy();
        workingCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
            (String) null);
        workingCopy.doSave();
      }

    } catch (CoreException e) {
      CorePluginLog.logWarning(e, "Could not migrate " + project.getName()
          + " to use the default working directory");
    }
  }
}
