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
package com.google.gdt.eclipse.suite;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.suite.launch.processors.DefaultWorkingDirectoryMigrator;
import com.google.gdt.eclipse.suite.launch.processors.LaunchConfigAffectingChangesListener;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Runs migration logic on projects.
 */
public class ProjectMigrator {

  public static final int CURRENT_VERSION = 4;

  /**
   * Scans projects in workspace for those that have not been migrated to the
   * latest version.
   */
  public void migrate() {
    boolean closedProjectsInWorkspace = false;

    // Attempt to migrate all projects
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    for (IProject project : workspace.getRoot().getProjects()) {
      if (project.isOpen()) {
        safelyMigrateIfVersionChanged(project);
      } else {
        closedProjectsInWorkspace = true;
      }
    }

    // Add listeners for all closed projects, so we can rebuild them, too,
    // when they're opened (but only if they are Google projects).
    if (closedProjectsInWorkspace) {
      workspace.addResourceChangeListener(new IResourceChangeListener() {
        public void resourceChanged(IResourceChangeEvent event) {
          IResourceDelta delta = event.getDelta();
          if (delta != null) {
            // Find any project-level changes
            IResourceDelta[] projectDeltas = delta.getAffectedChildren(
                IResourceDelta.CHANGED, IResource.PROJECT);

            // The master delta may include more than one project delta
            for (IResourceDelta projectDelta : projectDeltas) {
              // Find any deltas for projects being opened/closed
              if ((projectDelta.getFlags() & IResourceDelta.OPEN) > 0) {
                IProject project = (IProject) projectDelta.getResource();
                if (project.isOpen()) {
                  safelyMigrateIfVersionChanged(project);
                }
              }
            }
          }
        }
      });
    }
  }

  /**
   * The actual migration logic. Ensure the template is followed exactly --
   * specifically that {@link ProjectMigrator#CURRENT_VERSION} has been bumped,
   * and that it will equal the projectVersion at the end of this method. Also
   * make sure you use "if" and not "else if".
   * 
   * @param project
   * @param projectVersion
   */
  private void migrateProject(IProject project, int projectVersion) {

    if (projectVersion == 0) {
      projectVersion = 1;

      /* Dummy migration logic to serve as a template/example */
      project.toString();
    }

    if (projectVersion == 1) {
      projectVersion = 2;

      /*
       * Moves to the GPE's new style of launch configs where arguments reflect
       * the actual parameters passed to the launch
       */
      LaunchConfigAffectingChangesListener.INSTANCE.updateLaunchConfigurations(project);
    }

    if (projectVersion == 2) {
      projectVersion = 3;

      /*
       * Set the launch config's working directory to default (which will always
       * resolve to the WAR directory) if it currently equals the WAR directory.
       */
      new DefaultWorkingDirectoryMigrator().migrate(project);
    }

    if (projectVersion == 3) {
      projectVersion = 4;

      /*
       * GPE now realizes GWTShell from newer SDKs supports the -remoteUI
       * argument. Update the launch configs so those that use GWTShell will
       * switch to the Dev Mode view inside Eclipse.
       */
      LaunchConfigAffectingChangesListener.INSTANCE.updateLaunchConfigurations(project);
    }

    if (projectVersion != CURRENT_VERSION) {
      GdtPlugin.getLogger().logError(
          "Project migrator did not migrate project to the latest version");
    }

    // Write back project version
    GdtPreferences.setProjectMigratorVersion(project, projectVersion);
  }

  /**
   * Migrates the given project if there is some migration that needs to be
   * done. This method ensures no exceptions escape.
   */
  private void safelyMigrateIfVersionChanged(IProject project) {
    try {
      if (NatureUtils.hasNature(project, GWTNature.NATURE_ID)
          || NatureUtils.hasNature(project, GaeNature.NATURE_ID)) {
        int projectVersion = GdtPreferences.getProjectMigratorVersion(project);
        if (projectVersion != CURRENT_VERSION) {
          migrateProject(project, projectVersion);
        }
      }
    } catch (Throwable e) {
      GdtPlugin.getLogger().logError(e,
          "Skipping project migrator for project " + project.getName());
    }
  }

}
