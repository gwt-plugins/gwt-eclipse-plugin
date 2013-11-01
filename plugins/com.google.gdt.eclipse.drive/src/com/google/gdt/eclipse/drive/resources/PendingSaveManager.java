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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.natures.AppsScriptNature;
import com.google.gdt.eclipse.drive.preferences.AppsScriptProjectPreferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.osgi.service.prefs.BackingStoreException;

import java.util.Collection;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A record of resources that have been updated in the Eclipse file system, but not yet written back
 * to Drive. This record is kept in memory and also stored persistently in the project preferences.
 * The {@link #initFromPreferences()} method, called at plugin startup, initializes the in-memory
 * record from the project preferences.
 * 
 * <p>The methods {@link #markAsSaved(IFile)} and {@link #markAsUnsaved(IFile)} update the record,
 * and also notify each {@link PendingSaveEventListener} registered with the
 * {@code PendingSaveManager}.
 */
@ThreadSafe
public class PendingSaveManager {
  
  @GuardedBy("this") private final Multimap<String, String> projectNamesToUnsavedPaths;
  @GuardedBy("this") private final ListenerList pendingSaveEventListeners;

  public PendingSaveManager() {
    projectNamesToUnsavedPaths = HashMultimap.create();
    pendingSaveEventListeners = new ListenerList();
  }
  
  /**
   * Initializes this {@code PendingSaveManager} according to persistent preferences.
   */
  public synchronized void initFromPreferences() {
    for (IProject project : WorkspaceUtils.allOpenAppsScriptProjects()) {
      try {
        Set<String> unsavedFiles = AppsScriptProjectPreferences.getUnsavedFileNames(project);
        projectNamesToUnsavedPaths.putAll(project.getName(), unsavedFiles);
        DrivePlugin.logInfo("Unsaved files in " + project.getName() + ": " + unsavedFiles);
      } catch (BackingStoreException e) {
        DrivePlugin.logError("Error retrieving unsaved files for project " + project.getName(), e);
      }
    }
    
    // Add listener for file-level resource changes:
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        new AppsScriptProjectResourceChangeListener(this), IResourceChangeEvent.POST_CHANGE);
    
    // Add listener for project deletions (to remove unsaved files for the project to be deleted):
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        new IResourceChangeListener(){
          @Override public void resourceChanged(IResourceChangeEvent event) {
            IProject project = (IProject) event.getResource();
            boolean mayBeAppsScriptProject;
            try {
              mayBeAppsScriptProject = project.hasNature(AppsScriptNature.NATURE_ID);
            } catch (CoreException e) {
              DrivePlugin.logError("Exception querying project nature", e);
              mayBeAppsScriptProject = true;
            }
            if (mayBeAppsScriptProject) {
              DrivePlugin.logInfo(
                  "Forgetting unsaved files for about-to-be-deleted project " + project.getName());
              projectNamesToUnsavedPaths.removeAll(project.getName());
            }
          }
        },
        IResourceChangeEvent.PRE_DELETE);
  }
  
  /**
   * Mark a specified file as saved in Eclipse, but unsaved in Drive, and notify each registered
   * {@link PendingSaveEventListener}.
   * 
   * @param file the specified file
   */
  public synchronized void markAsUnsaved(IFile file) {
    IProject project = file.getProject();
    String fileName = file.getName();
    projectNamesToUnsavedPaths.put(project.getName(), fileName);
    AppsScriptProjectPreferences.addUnsavedFileName(project, fileName);
    DrivePlugin.logInfo(project.getName() + ':' + fileName + " marked as unsaved.");
    firePendingSaveEvent(file, true);
  }
  
  /**
   * Mark a specified file as saved in Drive, and notify each registered
   * {@link PendingSaveEventListener}.
   * 
   * @param file the specified file
   */
  public synchronized void markAsSaved(IFile file) {
    IProject project = file.getProject();
    String fileName = file.getName();
    projectNamesToUnsavedPaths.remove(project.getName(), fileName);
    AppsScriptProjectPreferences.removeUnsavedFileName(project, fileName);
    DrivePlugin.logInfo(project.getName() + ':' + fileName + " marked as saved.");
    firePendingSaveEvent(file, false);
  }
  
  /**
   * Mark a specified file as deleted in Eclipse, with the deletion not yet recorded in Drive.
   * 
   * @param file the specified file
   */
  public synchronized void markAsDeleted(IFile file) {
    IProject project = file.getProject();
    String fileName = file.getName();
    Collection<String> unsavedFilesForProject = projectNamesToUnsavedPaths.get(project.getName());
    if (unsavedFilesForProject.contains(fileName)) {
      projectNamesToUnsavedPaths.remove(project.getName(), fileName);
      AppsScriptProjectPreferences.removeUnsavedFileName(project, fileName);
      DrivePlugin.logInfo(project.getName() + ':' + fileName + " marked as deleted.");
    }
  }
  
  /**
   * Indicates whether a specified file is saved in Eclipse but unsaved in Drive.
   * 
   * @param file the specified file
   * @return
   *     {@code true} if {@code file} is saved in Eclipse but unsaved in Drive, {@code false}
   *     otherwise
   */
  public synchronized boolean isUnsaved(IFile file) {
    IProject project = file.getProject();
    return projectNamesToUnsavedPaths.get(project.getName()).contains(file.getName());
  }
  
  /**
   * Retrieves the names of all files in a specified project that are saved in Eclipse, but unsaved
   * in Drive. The list returned is a snapshot that is unaffected by subsequent marking of files as
   * saved or unsaved.
   * 
   * @param project the specified project
   * @return the file names, each containing an extension but no path
   */
  public synchronized Collection<String> allUnsavedFiles(IProject project) {
    Collection<String> result = projectNamesToUnsavedPaths.get(project.getName());
    return result == null ? ImmutableList.<String>of() : ImmutableList.copyOf(result);
  }

  /**
   * Registers a specified {@link PendingSaveEventListener} with this {@code PendingSaveManager}.
   * 
   * @param listener the specified {@code PendingSaveEventListener}
   */
  public synchronized void addPendingSaveListener(PendingSaveEventListener listener) {
    pendingSaveEventListeners.add(listener);
  }
  
  /**
   * Unregisters a specified {@link PendingSaveEventListener} if it is registered with this
   * {@code PendingSaveManager}.
   * 
   * @param listener the specified {@code PendingSaveEventListener}
   */
  public synchronized void removePendingSaveListener(PendingSaveEventListener listener) {
    pendingSaveEventListeners.remove(listener);
  }
    
  private void firePendingSaveEvent(IFile file, boolean unsaved) {
    PendingSaveEvent event = new PendingSaveEvent(file, unsaved);
    for (Object listener : pendingSaveEventListeners.getListeners()) {
      ((PendingSaveEventListener) listener).onPendingSaveEvent(event);
    }
  }
}
