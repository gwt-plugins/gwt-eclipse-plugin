/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.drive;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.gdt.eclipse.core.jobs.BlockingCallableWithProgress;
import com.google.gdt.eclipse.core.ui.BackgroundInitiatedYesNoDialog;
import com.google.gdt.eclipse.drive.driveapi.DriveScriptInfo;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;
import com.google.gdt.eclipse.drive.driveapi.DriveWritingException;
import com.google.gdt.eclipse.drive.driveapi.FileTypes;
import com.google.gdt.eclipse.drive.model.AppsScriptProject;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences;
import com.google.gdt.eclipse.drive.resources.PendingSaveManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.osgi.service.prefs.BackingStoreException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

/**
 * Manages the correspondence between a Drive Apps Script project and an Eclipse Apps Script
 * project.
 */
public class DriveEclipseProjectMediator {
  
  private static final int READ_PROJECT_CANCELLATION_POLLING_INTERVAL_IN_MILLIS = 100;
  private static final Set<String> NO_EXCLUDED_FILES = ImmutableSet.of();
  
  private static DriveEclipseProjectMediator instance;
  
  public static DriveEclipseProjectMediator getInstance() {
    if (instance == null) {
      instance = new DriveEclipseProjectMediator();
    }
    return instance;
  }
  
  private final Executor driveUpdateExecutor;

  // Prevent instantiation except by getInstance().
  private DriveEclipseProjectMediator() {
    driveUpdateExecutor = Executors.newSingleThreadExecutor();
  }

  /**
   * Creates a new Eclipse project from a specified Drive project.  A call on this method results in
   * a login prompt if the user is not already logged in.
   * 
   * @param driveFileId the Drive file ID for the Drive project
   * @param projectLocation
   *     if the Eclipse project is to be stored in the default workspace location, {@code null};
   *     otherwise, the {@code IPath} for the location where the Eclipse project is to be stored
   * @param eclipseProjectName the name of the Eclipse project to be created
   * @param monitor an {@link IProgressMonitor} for the operation
   * @throws IOException if there is an error reading from Drive
   * @throws CoreException if there is an error creating the Eclipse project
   */
  public void importDriveProjectIntoEclipse(
          String driveFileId, @Nullable IPath projectLocation, String eclipseProjectName,
          IProgressMonitor monitor)
      throws IOException, CoreException, InterruptedException {
    monitor.subTask("reading the project from Drive");
    AppsScriptProject driveProject = readProjectFromDrive(driveFileId, monitor);
    monitor.subTask("creating the Eclipse project " + eclipseProjectName);
    createNewEclipseProject(
        driveProject, eclipseProjectName, projectLocation, driveFileId, monitor);
  }
  
  /**
   * Updates an existing Eclipse project from a specified Drive project.  A call on this method
   * results in a login prompt if the user is not already logged in.
   * 
   * @param eclipseProject the existing Eclipse project
   * @param monitor an {@link IProgressMonitor} for the operation
   * @throws CoreException if there is an error creating, rewriting, or deleting an Eclipse file
   * @throws IOException if there is an error reading the project from Drive
   * @throws BackingStoreException
   *     if there is a pesky problem persisting project properties in preferences
   * @throws InterruptedException if the reading of the Drive project is interupted
   */
  public void updateEclipseProjectFromDrive(IProject eclipseProject, IProgressMonitor monitor)
        throws CoreException, IOException, BackingStoreException, InterruptedException {
    monitor.subTask("reading the project from Drive");
    String driveFileId = AppsScriptProjectPreferences.readDriveFileId(eclipseProject);
    AppsScriptProject driveProject = readProjectFromDrive(driveFileId, monitor);
    monitor.subTask("updating the Eclipse project " + eclipseProject.getName());
    updateEclipseProject(eclipseProject, driveProject, monitor);
  }

  private static AppsScriptProject readProjectFromDrive(
      final String driveFileId, IProgressMonitor monitor)
      throws InterruptedException, IOException, CoreException {
    BlockingCallableWithProgress<AppsScriptProject> callable =
        new BlockingCallableWithProgress<AppsScriptProject>(
            new Callable<AppsScriptProject>(){
              @Override public AppsScriptProject call() throws IOException {
                return DriveServiceFacade.get().readProject(driveFileId);
              }
            },
            READ_PROJECT_CANCELLATION_POLLING_INTERVAL_IN_MILLIS);
    AppsScriptProject driveProject;
    try {
      driveProject = callable.call(monitor);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      } else if (cause instanceof CoreException) {
        throw (CoreException) cause;
      } else {
        throw new Error("Unexpected exception reading Drive project", cause);
      }
    }
    return driveProject;
  }

  private static void createNewEclipseProject(
      AppsScriptProject driveProject,
      String eclipseProjectName,
      IPath projectLocation,
      final String driveFileId,
      IProgressMonitor monitor) throws CoreException, IllegalArgumentException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(eclipseProjectName);
    IProjectDescription description =
        ResourcesPlugin.getWorkspace().newProjectDescription(eclipseProjectName);
    if (projectLocation != null) {
      description.setLocation(projectLocation.append(eclipseProjectName));
    }
    description.setNatureIds(new String[]{AppsScriptNature.NATURE_ID, JavaScriptCore.NATURE_ID});
    project.create(description, IResource.NONE, monitor);
    project.open(monitor);
    try {
      AppsScriptProjectPreferences.initializeProjectPreferences(project);
      AppsScriptProjectPreferences.writeDriveFileId(project, driveFileId);
      AppsScriptProjectPreferences.writeDriveVersionFingerprint(
          project, driveProject.getDriveVersionFingerprint());
      populateEclipseProjectFiles(project, driveProject, monitor);
    } catch (BackingStoreException e) {
      throw makeCoreException("Error persistently storing project preferences.", e);
    }
  }
  
  private static void updateEclipseProject(
      IProject eclipseProject, AppsScriptProject driveProjectModel, IProgressMonitor monitor)
      throws BackingStoreException, CoreException {
    populateEclipseProjectFiles(eclipseProject, driveProjectModel, monitor);
    handleDriveDeletions(eclipseProject, driveProjectModel, monitor);
    AppsScriptProjectPreferences.writeDriveVersionFingerprint(
        eclipseProject, driveProjectModel.getDriveVersionFingerprint());
  }

  private static void handleDriveDeletions(
      IProject eclipseProject, AppsScriptProject modelForDriveProject, IProgressMonitor monitor)
      throws BackingStoreException, CoreException {
    Set<String> filesWithEclipseMetadata =
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(eclipseProject);
    Set<String> filesRemainingOnDrive = modelForDriveProject.getScriptFileNames();
    Set<String> filesDeletedOnDriveWithIdsInEclipse =
        Sets.difference(filesWithEclipseMetadata, filesRemainingOnDrive);
    for (String nameOfFileDeletedOnDrive : filesDeletedOnDriveWithIdsInEclipse) {
      IFile eclipseFileDeletedOnDrive = (IFile) eclipseProject.findMember(nameOfFileDeletedOnDrive);
      if (eclipseFileDeletedOnDrive == null) {
        // The file had been deleted from Eclipse earlier, but marked for retention in Drive, so the
        // metadata about the file stored in Eclipse is a tombstone. Now that the file has been
        // independently deleted from Drive as well, we remove the tombstone, so that the file's
        // document ID will not be included in future project updates.
        AppsScriptProjectPreferences.removeFileMetadata(eclipseProject, nameOfFileDeletedOnDrive);
      } else {
        // The file has been deleted from Drive, but still exists on Eclipse. Its document ID is no
        // longer known to Drive, so an update request containing that document ID will fail.
        String fileName = eclipseFileDeletedOnDrive.getName();
        boolean deleteFromEclipseToo =
            askUserWhetherToMirrorDriveDeletionOnEclipse(fileName, eclipseProject.getName());
        if (deleteFromEclipseToo) {
          AppsScriptProjectPreferences.removeFileMetadata(eclipseProject, fileName);
          DrivePlugin.getDefault().startUpdatingFileFromDrive(fileName);
          eclipseFileDeletedOnDrive.delete(IResource.FORCE | IResource.KEEP_HISTORY, monitor);
          DrivePlugin.getDefault().endUpdatingFileFromDrive();
        } else {
          // We make the file look like a file newly created on Eclipse. Then, if the user chooses
          // to overwrite changes on Drive with changes in Eclipse, the file will be restored on
          // Drive with a new document ID.
          AppsScriptProjectPreferences.removeDriveScriptId(
              eclipseProject, nameOfFileDeletedOnDrive);
          DrivePlugin.getDefault().getPendingSaveManager().markAsUnsaved(eclipseFileDeletedOnDrive);
        }
      }
    }
  }

  private static void populateEclipseProjectFiles(
      IProject targetEclipseProject, AppsScriptProject driveProject, IProgressMonitor monitor)
      throws BackingStoreException {
    for (String fileNameWithExtension : driveProject.getScriptFileNames()) {
      if (monitor.isCanceled()) {
        return;
      }
      IFile scriptFile = targetEclipseProject.getFile(fileNameWithExtension);
      DriveScriptInfo scriptInfo = driveProject.getScriptInfo(fileNameWithExtension);
      String scriptChars = scriptInfo.getContents();
      byte[] scriptBytesUtf8 = scriptChars.getBytes(Charsets.UTF_8);
      InputStream fileContentsInputStream = new ByteArrayInputStream(scriptBytesUtf8);
      DrivePlugin.getDefault().startUpdatingFileFromDrive(fileNameWithExtension);
      try {
        if (scriptFile.exists()) {
          scriptFile.setContents(
              fileContentsInputStream, IResource.FORCE | IResource.KEEP_HISTORY, monitor);
        } else {
          AppsScriptProjectPreferences.writeDriveImportName(
              targetEclipseProject,
              fileNameWithExtension,
              FileTypes.stripExtension(fileNameWithExtension));
          AppsScriptProjectPreferences.writeDriveScriptId(
              targetEclipseProject, fileNameWithExtension, scriptInfo.getDocumentId());
          AppsScriptProjectPreferences.writeDriveType(
              targetEclipseProject, fileNameWithExtension, scriptInfo.getType());
          scriptFile.create(fileContentsInputStream, false, monitor);
        }
      } catch (CoreException e) {
        DrivePlugin.logError(
            "Error trying to create new file \"" + fileNameWithExtension + "\" in project \""
                + targetEclipseProject.getName() + "\"",
            e);
      } finally {
        DrivePlugin.getDefault().endUpdatingFileFromDrive();
      }
    }
  }

  /**
   * Initiates the rewriting of a specified Eclipse Apps Script project to Drive, in a single
   * operation that may affect multiple files in the project. The call queues the update to be
   * performed by another thread, then returns. One thread is responsible for performing all
   * updates serially, in the order they were queued.
   * 
   * <p>We consider a file to be <i>updatable</i> if it is at the top level of the project and has
   * one of the file-type extensions recognized by the {@link FileTypes} class as a supported
   * extension. Updatable files with new content (either files that were modified in the Eclipse
   * file system or files that were created in the Eclipse file system since the project was
   * imported or last written) are updated in Drive if they previously existed, or created on Drive
   * if not. If the write succeeds, then the persistent record of these files as modified in Eclipse
   * but not Drive is removed, and the document IDs assigned by Drive to newly created files are
   * recorded persistently in the Eclipse project preferences.
   * 
   * <p>Files present in the Drive project but not present in the Eclipse project are deleted from
   * the Drive project, unless the user specified when the file was deleted from Eclipse that the
   * file should be preserved in Drive. The Eclipse project retains a persistent record (called a
   * "tombstone") of the document ID, import name, and file type of any file that is deleted from
   * Eclipse but that should be preserved in Drive. 
   * 
   * <p>A call on this method results in a login prompt if the user is not already logged in.
   * 
   * @param eclipseProject the specified Eclipse Apps Script project
   */
  public void writeEclipseProjectToDrive(final IProject eclipseProject) {
    driveUpdateExecutor.execute(
        new Runnable() {
          @Override public void run() {
            performDriveUpdate(eclipseProject);
          }
        });
  }
  
  public void performDriveUpdate(IProject eclipseProject) {
    AppsScriptProject modelForEclipseProject;
    AppsScriptProject modelForDriveProject;
    
    try {
      modelForEclipseProject = getModelForEclipseProject(eclipseProject);
      // The following call on modelProjectForDriveId results in a login prompt if the user is not
      // already logged in.
      modelForDriveProject =
          DriveServiceFacade.get().modelProjectForDriveId(modelForEclipseProject.getDriveFileId());
      
      boolean atLeastOneTombstoneRemoved =
          removeTombstonesForFilesDeletedOnDrive(eclipseProject, modelForDriveProject);
      if (atLeastOneTombstoneRemoved) {
        // The model of the Eclipse project must be recomputed.
        modelForEclipseProject = getModelForEclipseProject(eclipseProject);
      }
      
      if (shouldAbortDueToConflict(eclipseProject, modelForEclipseProject, modelForDriveProject)) {
        return;
      }
    } catch (CoreException e) {
      // TODO(nhcohen): Once we upgrade to Java 7 compliance level, combine these handlers into one
      // multicatch, and inline reportEclipseReadingException.
      reportEclipseReadingException(e, eclipseProject.getName());
      return;
    } catch (IOException e) {
      reportEclipseReadingException(e, eclipseProject.getName());
      return;
    } catch (BackingStoreException e) {
      reportEclipseReadingException(e, eclipseProject.getName());
      return;
    }
    
    Collection<String> filesModifiedOnEclipse = 
        DrivePlugin.getDefault().getPendingSaveManager().allUnsavedFiles(eclipseProject);
    Map<String, DriveScriptInfo> newFileInfo;
    try {
      newFileInfo =
          DriveServiceFacade.get().writeProject(modelForEclipseProject, filesModifiedOnEclipse);
    } catch (DriveWritingException e) {
      handleDriveWritingException(
          e, eclipseProject, modelForEclipseProject, filesModifiedOnEclipse.size());
      return;
    }

    // We reach this point only if no conflict was detected and the write succeeded, or the user
    // authorizes the clobbering write and the write succeeded. Otherwise, all files previously
    // marked as unsaved remain marked as unsaved.
    
    saveNewDriveFileIdsInProjectSettings(newFileInfo, eclipseProject);
    markFilesAsSaved(eclipseProject, filesModifiedOnEclipse);
    String newFingerprint = modelForEclipseProject.getContentHash(NO_EXCLUDED_FILES);
    DrivePlugin.logInfo(
        "About to change project version fingerprint on Eclipse to " + newFingerprint);
    AppsScriptProjectPreferences.writeDriveVersionFingerprint(eclipseProject, newFingerprint);
  }

  /**
   * Reports whether the update should be aborted because a write-write conflict exists between a
   * given Eclipse project and a given Drive project and the user does not want to overwrite changes
   * that have been made on Drive. The method first checks whehter a write-write conflict exists.
   * If so, it asks the user whether to perform the update anyway.
   * 
   * <p>Conflict detection works at the granularity of the entire project, and is based on the
   * names and contents of the files in the project, ignoring files that have been deleted on
   * Eclipse but deliberately retained in Drive.
   * 
   * @param eclipseProject the given Eclipse project, represented as an {@link IProject}
   * @param modelForEclipseProject
   *     the given Eclipse project, represented as an {@link AppsScriptProject}
   * @param modelForDriveProject
   *     the given Drive project, represented as an {@link AppsScriptProject}
   * @return
   *     {@code true} if a conflict exists and the user wants to abort the update because of it;
   *     {@code false} if no conflict exists, or if a conflict exists but the user wants to
   *     perform the update anyway and clobber the Drive version
   * @throws BackingStoreException
   *     if there is an error reading persistent settings for the Eclipse project
   * @throws IOException if there is an error reading the corresponding project from Drive
   */
  private static boolean shouldAbortDueToConflict(
      IProject eclipseProject,
      AppsScriptProject modelForEclipseProject,
      AppsScriptProject modelForDriveProject)
      throws BackingStoreException, IOException {
    
    String syncedDriveFingerprint = modelForEclipseProject.getDriveVersionFingerprint();
    
    // Find names of files that were deleted from Eclipse but intentionally retained in Drive.
    // These files will be ignored when computing the fingerprint of the Drive project.
    Set<String> filesWithTombstonesInEclipse =
        Sets.difference(
            AppsScriptProjectPreferences.getFileNamesWithDocumentIds(eclipseProject),
            modelForEclipseProject.getScriptFileNames());
    
    String currentDriveFingerprint =
        modelForDriveProject.getContentHash(filesWithTombstonesInEclipse);
    
    if (currentDriveFingerprint.equals(syncedDriveFingerprint)) {
      // No conflict up to now. (There is a very small chance that a foreign update to the Drive
      // project will occur just before the update we are about to perform, in which case we will
      // miss the conflict and clobber the foreign update. C'est la vie.)
      DrivePlugin.logInfo(
          "No conflict detected.\n\tEclipse base version and current Drive version: "
              + syncedDriveFingerprint);
      return false;
    } else {
      // Project was changed on Drive after we imported or last wrote to it.
      DrivePlugin.logInfo(
          "Write-write conflict detected.\n\tEclipse base version: "
              + syncedDriveFingerprint
              + "\n\tCurrent Drive version: "
              + currentDriveFingerprint);
      
      boolean userWantsToOverwrite = askUserWhetherToOverwrite();
      if (userWantsToOverwrite) {
        DrivePlugin.logInfo("Overwriting Drive changes with Eclipse changes.");
        return false;
      } else {
        DrivePlugin.logInfo("No update performed, so no change to project timestamp on Eclipse.");
        return true;
      }
    }
  }

  private static boolean removeTombstonesForFilesDeletedOnDrive(
      IProject eclipseProject, AppsScriptProject modelForDriveProject)
      throws BackingStoreException {
    Set<String> filesWithEclipseMetadata =
        AppsScriptProjectPreferences.getFileNamesWithDocumentIds(eclipseProject);
    Set<String> filesRemainingOnDrive = modelForDriveProject.getScriptFileNames();
    Set<String> filesDeletedOnDriveWithIdsInEclipse =
        Sets.difference(filesWithEclipseMetadata, filesRemainingOnDrive);
    boolean tombstonesRemoved = false;
    for (String nameOfFileDeletedOnDrive : filesDeletedOnDriveWithIdsInEclipse) {
      IFile eclipseFileDeletedOnDrive = (IFile) eclipseProject.findMember(nameOfFileDeletedOnDrive);
      if (eclipseFileDeletedOnDrive == null) {
        // The file had been deleted from Eclipse earlier, but marked for retention in Drive, so the
        // metadata about the file stored in Eclipse is a tombstone. Now that the file has been
        // independently deleted from Drive as well, we remove the tombstone, so that the file's
        // document ID will not be included in future project updates.
        AppsScriptProjectPreferences.removeFileMetadata(eclipseProject, nameOfFileDeletedOnDrive);
        tombstonesRemoved = true;
      }
    }
    return tombstonesRemoved;
  }

  private static AppsScriptProject getModelForEclipseProject(IProject eclipseProject)
      throws CoreException {
    Preconditions.checkArgument(eclipseProject.isOpen());
    Preconditions.checkArgument(
        eclipseProject.hasNature(AppsScriptNature.NATURE_ID),
        "Project " + eclipseProject.getName() + " does not have Apps Script nature.");

    String driveFileId = AppsScriptProjectPreferences.readDriveFileId(eclipseProject);
    if (driveFileId == null) {
      throw makeCoreException(
          "There is no Drive file ID saved in the .settings file of project "
              + eclipseProject.getName(),
          null);
    }
    
    Map<String, DriveScriptInfo> fileNamesToInfo = informationAboutExistingFiles(eclipseProject);    
    addInformationAboutRetainedDeletions(fileNamesToInfo, eclipseProject);
    
    String driveVersionFingerprint =
        AppsScriptProjectPreferences.readDriveVersionFingerprint(eclipseProject);
    if (driveVersionFingerprint == null) {
      throw makeCoreException(
          "There is no Drive version fingerprint saved in the .settings file of project "
              + eclipseProject.getName() + ", or the saved timestamp is malformed.",
          null);
    }
    return AppsScriptProject.make(driveFileId, fileNamesToInfo, driveVersionFingerprint);
  }
  
  private static void reportEclipseReadingException(Exception e, String eclipseProjectName) {
    String message =
        "Error extracting information from Eclipse project " + eclipseProjectName
        + " to update the Drive project";
    DrivePlugin.logError(message, e);
    DrivePlugin.displayLoggedErrorDialog(message);
  }

  /**
   * Gathers information for files that have been deleted from the Eclipse project but are to be
   * retained in the Drive project, and adds it to a specified map.
   * 
   * @param fileNamesToInfo the specified map
   * @param eclipseProject the Eclipse project
   * @throws CoreException
   *     if there is an error accessing Eclipse project preferences, or if those preferences are
   *     malformed
   */
  private static void addInformationAboutRetainedDeletions(
      Map<String, DriveScriptInfo> fileNamesToInfo, IProject eclipseProject)
      throws CoreException {
    Set<String> allFileNamesWithDocumentIds;
    try {
      allFileNamesWithDocumentIds =
          AppsScriptProjectPreferences.getFileNamesWithDocumentIds(eclipseProject);
    } catch (BackingStoreException e) {
      throw makeCoreException(
          "Error retrieving names of files deleted from Eclipse but preserved in Drive", e);
    }
    for (String fileName : allFileNamesWithDocumentIds) {
      if (!fileNamesToInfo.containsKey(fileName)) {
        fileNamesToInfo.put(fileName, infoForFile(eclipseProject, fileName, null));
      }
    }
  }

  private static Map<String, DriveScriptInfo> informationAboutExistingFiles(IProject eclipseProject)
      throws CoreException {
    Map<String, DriveScriptInfo> fileNamesToInfo = Maps.newHashMap();
    for (IResource member : eclipseProject.members()) {
      if (member.exists() && member instanceof IFile) {
        IFile file = (IFile) member;
        String fileName = file.getName();
        if (FileTypes.hasSupportedExtension(fileName)) {
          String fileContents = eclipseFileContents(file);
          DriveScriptInfo info = infoForFile(eclipseProject, fileName, fileContents);
          if (info.getDocumentId() == null) {
          }
          fileNamesToInfo.put(fileName, info);
        }
      }
    }
    return fileNamesToInfo;
  }

  private static String eclipseFileContents(IFile file) throws CoreException {
    InputStream contentsStream = file.getContents();
    InputStreamReader contentsReader =
        new InputStreamReader(contentsStream, Charsets.UTF_8);
    String fileContents;
    try {
      try {
        fileContents = CharStreams.toString(contentsReader);
      } finally {
        contentsReader.close();
      }
    } catch (IOException e) {
      throw makeCoreException("Error reading contents of " + file.getName(), e);
    }
    return fileContents;
  }

  private static DriveScriptInfo infoForFile(
      IProject eclipseProject, String fileName, @Nullable String fileContents)
          throws CoreException {
    String driveImportName;
    String driveType;
    String driveScriptId =
        AppsScriptProjectPreferences.readDriveScriptId(eclipseProject, fileName);
    if (driveScriptId == null) {
      // A new file created in Eclipse and not yet saved on Drive
      int lastDotPos = fileName.lastIndexOf('.');
      if (lastDotPos == -1) {
        // should never happen
        throw makeCoreException(
            "File name \"" + fileName + "\" without extension found in Eclipse project preferences",
            null);
      }
      driveImportName = fileName.substring(0, lastDotPos); // fileName with extension removed
      String extension = fileName.substring(lastDotPos); // Include the dot.
      driveType = FileTypes.driveTypeForExtension(extension);
    } else if (fileContents == null) {
      // A file deleted from Eclipse but meant to be retained in Drive
      driveImportName = null;
      driveType = AppsScriptProjectPreferences.readDriveType(eclipseProject, fileName);
    } else {
      driveImportName =
          AppsScriptProjectPreferences.readDriveImportName(eclipseProject, fileName);
      driveType = AppsScriptProjectPreferences.readDriveType(eclipseProject, fileName);
    }
    return new DriveScriptInfo(driveImportName, driveScriptId, driveType, fileContents);
  }

  private static boolean askUserWhetherToOverwrite() {
    return
        new BackgroundInitiatedYesNoDialog().userAnsweredYes(
            MessageDialog.WARNING,
            "Write-write conflict!",
            "The Apps Script project has been updated on Drive. "
                + "If you save the Eclipse version to Drive, you will lose that update."
                + "\n\nSave to Drive anyway?",
            false);
  }
  
  private static boolean askUserWhetherToMirrorDriveDeletionOnEclipse(
      String fileName, String eclipseProjectName) {
    return
        new BackgroundInitiatedYesNoDialog().userAnsweredYes(
            MessageDialog.QUESTION,
            "Mirror Drive deletion on Eclipse?",
            "The file "
                + fileName + " was deleted from the Drive project. "
                + " Would you like to delete it from the Eclipse project "
                + eclipseProjectName
                + " too?",
            true);
  }

  private static void saveNewDriveFileIdsInProjectSettings(
      Map<String, DriveScriptInfo> newFileNamesToInfos, IProject eclipseProject) {
    for (Entry<String, DriveScriptInfo> nameToInfo : newFileNamesToInfos.entrySet()) {
      String fileName = nameToInfo.getKey();
      DriveScriptInfo scriptInfo = nameToInfo.getValue();
      AppsScriptProjectPreferences.writeDriveImportName(
          eclipseProject, fileName, scriptInfo.getImportName());
      AppsScriptProjectPreferences.writeDriveType(
          eclipseProject, fileName, scriptInfo.getType());
      AppsScriptProjectPreferences.writeDriveScriptId(
          eclipseProject, fileName, scriptInfo.getDocumentId());
    }
  }

  private static void markFilesAsSaved(IProject eclipseProject, Collection<String> modifiedFiles) {
    PendingSaveManager pendingSaves = DrivePlugin.getDefault().getPendingSaveManager();
    for (String fileName : modifiedFiles) {
      pendingSaves.markAsSaved(eclipseProject.getFile(fileName));
    }
  }

  private static void handleDriveWritingException(
      DriveWritingException e,
      IProject eclipseIProject,
      AppsScriptProject eclipseProjectModel,
      int modifiedFilesCount) {
    String fileWasOrFilesWere = modifiedFilesCount == 1 ? "file was" : "files were";
    String javaScriptError = e.getJavaScriptError();
    if (javaScriptError == null) {
      String message = "Error saving Eclipse project " + eclipseIProject.getName() + " to Drive";
      DrivePlugin.logError(message + " (file ID: " + eclipseProjectModel.getDriveFileId() + ")", e);
      DrivePlugin.displayLoggedErrorDialog(
          message + ", but " + fileWasOrFilesWere + " saved locally");
    } else {
      // TODO(nhcohen): If DriveWritingException evolves to contain details about the
      // JavaScript error detected on the server, display those details in this dialog.
      DrivePlugin.displayUnloggedErrorDialog(
          "The " + fileWasOrFilesWere + " saved locally, "
          + "but could not be saved in Drive because the script contains an error."); 
    }
  }
  
  private static CoreException makeCoreException(String message, Throwable cause) {
    return new CoreException(new Status(IStatus.ERROR, DrivePlugin.PLUGIN_ID, message, cause));
  }
}
