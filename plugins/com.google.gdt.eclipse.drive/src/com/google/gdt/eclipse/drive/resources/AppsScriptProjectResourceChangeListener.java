/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.resources;

import com.google.api.client.util.Joiner;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.ui.BackgroundInitiatedYesNoDialog;
import com.google.gdt.eclipse.drive.DriveEclipseProjectMediator;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.driveapi.FileTypes;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.progress.UIJob;

import java.util.List;

/**
 * Listens for {@link IResourceChangeEvent.POST_CHANGE} events and does appropriate processing for
 * the addition, removal, or modification of files in an Apps Script project. The listener assumes
 * that the resource-change event with which it is invoked has a non-null delta, so a call on
 * {@code IWorkspace.addResourceChangeListener} to add this listener should specify a mask that
 * excludes event types with no delta ({@link IResourceChangeEvent.PRE_CLOSE},
 * {@link IResourceChangeEvent.PRE_DELETE}, and {@link IResourceChangeEvent.PRE_REFRESH}).
 */
public class AppsScriptProjectResourceChangeListener implements IResourceChangeListener {
  
  private final PendingSaveManager pendingSaveManager;

  public AppsScriptProjectResourceChangeListener(PendingSaveManager pendingSaveManager) {
    this.pendingSaveManager = pendingSaveManager;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    IResourceDelta delta = event.getDelta();
    IResourceDelta[] projectDeltas = delta.getAffectedChildren();
    for (IResourceDelta projectDelta : projectDeltas) {
      IProject project = (IProject) projectDelta.getResource();
      try {
        if (project.exists() && project.isOpen() && project.hasNature(AppsScriptNature.NATURE_ID)) {
          processAppsScriptProjectDelta(project, projectDelta);
        }
      } catch (CoreException e) {
        // Log the error and skip the project.
        DrivePlugin.logError("Exception testing nature of project in resource delta", e);
      }
    }
  }
  
  private void processAppsScriptProjectDelta(IProject project, IResourceDelta projectDelta) {
    IResourceDelta[] deltasInTopLevelOfProject = projectDelta.getAffectedChildren();
    for (IResourceDelta topLevelDelta : deltasInTopLevelOfProject) {
      IResource resource = topLevelDelta.getResource();
      int resourceType = resource.getType();
      if (resourceType == IResource.FOLDER) {
        checkForMisplacedFiles(topLevelDelta);
      } else if (resourceType == IResource.FILE) {
        String extension = '.' + resource.getFileExtension();
        if (FileTypes.getSupportedExtensions().contains(extension)) {
          if (!DrivePlugin.getDefault().isBeingUpdatedFromDrive(resource.getName())) {
            switch(topLevelDelta.getKind()) {
              case IResourceDelta.ADDED:
              case IResourceDelta.CHANGED:
                IFile fileToSave = (IFile) resource;
                IProject projectToUpdate = fileToSave.getProject();
                pendingSaveManager.markAsUnsaved(fileToSave);
                if (!DrivePlugin.getDefault().saveAllInProgress()) {
                  updateProjectOnDrive(projectToUpdate);
                }
                break;
              case IResourceDelta.REMOVED:
                pendingSaveManager.markAsDeleted((IFile) resource);
                String nameOfDeletedFile = resource.getName();
                if (userWantsToDeleteFromDriveToo(nameOfDeletedFile)) {
                  AppsScriptProjectPreferences.removeFileMetadata(project, nameOfDeletedFile);
                  updateProjectOnDrive(resource.getProject());
                }
                // Leaving the persistent record of the file will cause its document ID to be
                // included in Drive project updates, so that the update will not remove the file
                // from Drive.
                break;
              default:
                DrivePlugin.logError(
                    "Unexpected delta kind " + topLevelDelta.getKind()
                        + " at top level of an AppsScript project",
                    null);
            }
          }
        }
      } else {
        DrivePlugin.logError(
            "Unexpected resource type " + resourceType + " at top level of an AppsScript project",
            null);
      }
    }
  }

  private static void updateProjectOnDrive(final IProject eclipseProject) {
    UIJob updateProjectJob =
        new UIJob("update project due to resource change") {
          @Override public IStatus runInUIThread(IProgressMonitor monitor) {
            if (GoogleLogin.getInstance().isLoggedIn()) {
              // While updating the project to reflect this resource change, take the opportunity to
              // save any other files with pending saves to Drive. (The following call on
              // writeEclipseProjectToDrive results in a login prompt if the user is not already 
              // logged in.)
              DriveEclipseProjectMediator.getInstance().writeEclipseProjectToDrive(eclipseProject);
            } else {
              DrivePlugin.displayWarningDialog(
                  "Not logged in",
                  "Files in your Eclipse project have changed, but these changes cannot be saved "
                      + "in Drive because you are not logged in. Saving any file after you log in "
                      + "to the Google Plugin for Eclipse will save of all your pending changes "
                      + "to Drive.");
            }
            return Status.OK_STATUS;
          }
        };
    updateProjectJob.schedule();
  }

  private static void checkForMisplacedFiles(IResourceDelta topLevelDelta) {
    MisplacedFileCheckingVisitor visitor = new MisplacedFileCheckingVisitor();
    try {
      topLevelDelta.accept(visitor);
      List<String> misplacedFiles = visitor.getMisplacedFiles();
      if (!misplacedFiles.isEmpty()) {
        DrivePlugin.displayWarningDialog(
            "File will not be saved on Drive",
            "The following files will not be saved on Drive because they are not at the top "
                + "level of the project:\n\n"
                + Joiner.on('\n').join(misplacedFiles));
      }
    } catch (CoreException e) {
      // Log the error and skip the folder.
      DrivePlugin.logError(
          "Exception traversing delta for folder at top level of an AppsScript project", e);
    }
  }
  
  private static boolean userWantsToDeleteFromDriveToo(final String fileName) {
    return
        new BackgroundInitiatedYesNoDialog().userAnsweredYes(
            MessageDialog.QUESTION,
            "Delete from Drive too?",
            "The file "
                + fileName
                + " has been deleted from the Eclipse project. "
                + "Would you like it to be permanently deleted from the Drive Apps Script project?",
            false);
  }
  
  private static class MisplacedFileCheckingVisitor implements IResourceDeltaVisitor {
    
    private final List<String> misplacedFiles;
    
    public MisplacedFileCheckingVisitor() {
      misplacedFiles = Lists.newLinkedList();
    }

    @Override public boolean visit(IResourceDelta delta) {
      IResource resource = delta.getResource();
      if (resource.getType() == IResource.FILE) {
        String extension = resource.getFileExtension();
        if (FileTypes.getSupportedExtensions().contains(extension)) {
          misplacedFiles.add(resource.getFullPath().toString());
        }
      }
      return true;
    }

    public List<String> getMisplacedFiles() {
      return misplacedFiles;
    }
    
  }

}
