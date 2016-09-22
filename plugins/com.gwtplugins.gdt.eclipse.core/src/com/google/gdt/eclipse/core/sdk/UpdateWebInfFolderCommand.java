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
package com.google.gdt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Updates the managed WAR output directory's WEB-INF/lib directory so it
 * contains the correct jars from a particular SDK.
 */
public abstract class UpdateWebInfFolderCommand {

  private static boolean areFilesEqual(IFolder set1Folder, List<String> set1FileNames,
      File[] set2) throws CoreException, IOException {

    List<File> set1 = new ArrayList<File>();
    for (String fileName : set1FileNames) {
      IPath location = set1Folder.getFile(fileName).getLocation();
      if (location == null) {
        return false;
      }

      set1.add(location.toFile());
    }

    return ResourceUtils.areFileListsEqual(set1, Arrays.asList(set2));
  }

  private final IJavaProject javaProject;

  protected final Sdk sdk;

  protected UpdateWebInfFolderCommand(IJavaProject javaProject, Sdk sdk) {
    this.javaProject = javaProject;
    this.sdk = sdk;
  }

  public final void execute() throws CoreException, FileNotFoundException,
      BackingStoreException {
    IProject project = javaProject.getProject();

    /*
     * We should not be here if the project does not have a managed WAR output directory, but
     * it may be the case that this check was done before the project was fully configured
     * (i.e. in the Maven case), so we perform a re-verification here.
     */
    if (!WebAppUtilities.hasManagedWarOut(project)) {
      return;
    }

    /*
     * There are cases when this command executes but the project is not fully
     * refreshed. In that case, we may fail to locate the war folder and
     * therefore fail to update the war/WEB-INF/lib folder. Ultimately, we
     * should explore triggering this code based on IJavaElementDeltas.
     */
    project.refreshLocal(IResource.DEPTH_INFINITE, null);

    IFolder webInfLibFolder = WebAppUtilities.getWebInfLib(project);
    ResourceUtils.createFolderStructure(project,
        webInfLibFolder.getProjectRelativePath());

    // Remove the old files
    List<String> fileNamesToRemove = computeWebInfLibFilesToRemove();
    File[] filesToAdd = sdk.getWebAppClasspathFiles(project);

    try {
      // This gets called from a classpath changed listener; if the classpath
      // changed that does not affect us, exit early
      if (areFilesEqual(webInfLibFolder, fileNamesToRemove, filesToAdd)) {
        // Nothing to do, the old set and new set of files are the same
        return;
      }
    } catch (Throwable t) {
      // Ignore and replace files
      CorePluginLog.logWarning(t,
          "Could not check if old files are equal to the new");
    }

    for (String fileToRemove : fileNamesToRemove) {
      IFile file = webInfLibFolder.getFile(fileToRemove);
      if (file.exists()) {
        file.delete(true, false, new NullProgressMonitor());
      }
    }

    // Copy the new files in
    for (File fileToAdd : filesToAdd) {
      IFile file = webInfLibFolder.getFile(fileToAdd.getName());
      if (file.exists()) {
        file.delete(true, false, new NullProgressMonitor());
      }
      file.create(new FileInputStream(fileToAdd), true, null);
    }

    saveFilesCopiedToWebInfLib(Arrays.asList(filesToAdd));
  }

  protected abstract List<String> computeWebInfLibFilesToRemove()
      throws CoreException;

  protected IJavaProject getJavaProject() {
    return javaProject;
  }

  protected abstract void saveFilesCopiedToWebInfLib(List<File> webInfLibFiles)
      throws BackingStoreException;
}

