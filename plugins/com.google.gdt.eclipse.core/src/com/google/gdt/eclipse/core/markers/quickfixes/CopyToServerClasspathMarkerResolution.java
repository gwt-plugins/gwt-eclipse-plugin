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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolution to the problem of a file on the build classpath which is not
 * present on the server classpath. The resolution will copy the file from the
 * build classpath to the <WAR>/WEB-INF/lib folder, and will replace the build
 * classpath entry with a reference to the jar in <WAR>/WEB-INF/lib. The
 * updating of the build classpath will be done ansynchronously.
 * 
 * TODO: Consider extending WorkbenchMarkerResolution so that multiple
 * quick-fixes can be applied at the same time.
 */
public class CopyToServerClasspathMarkerResolution implements IMarkerResolution {

  private IPath buildClasspathFilePath;

  public CopyToServerClasspathMarkerResolution(IPath buildClasspathFilePath) {
    this.buildClasspathFilePath = buildClasspathFilePath;
  }

  public String getLabel() {
    return "Copy "
        + buildClasspathFilePath.lastSegment()
        + " to <WAR>/WEB-INF/lib and update build classpath to reference this jar.";
  }

  public void run(IMarker marker) {
    String buildClasspathFileName = buildClasspathFilePath.lastSegment();
    IProject project = marker.getResource().getProject();
    IPath projRelativeWebInfLibFolderPath = null;

    try {
      WebAppUtilities.verifyHasManagedWarOut(project);

      projRelativeWebInfLibFolderPath = WebAppUtilities.getWebInfLib(project).getProjectRelativePath();

      ResourceUtils.createFolderStructure(project,
          projRelativeWebInfLibFolderPath);
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Copy " + buildClasspathFileName,
          "Unable to create <WAR>/WEB-INF/lib directory. See the Error Log for more details.");
      return;
    }

    IFolder webInfLibFolder = project.getFolder(projRelativeWebInfLibFolderPath);

    assert (webInfLibFolder.exists());

    IFile serverClasspathFile = webInfLibFolder.getFile(buildClasspathFileName);

    try {
      serverClasspathFile.create(new FileInputStream(
          buildClasspathFilePath.toFile()), false, null);
    } catch (FileNotFoundException e) {
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Copy " + buildClasspathFileName,
          "The file " + buildClasspathFilePath.toOSString()
              + " does not exist.");
      return;
    } catch (CoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Copy " + buildClasspathFileName,
          "Unable to copy " + buildClasspathFilePath.toOSString() + " to "
              + projRelativeWebInfLibFolderPath.toString()
              + ". See the Error Log for more details.");
      return;
    }

    // Update the project's classpath to use the file copied into
    // <WAR>/WEB-INF/lib
    IJavaProject javaProject = JavaCore.create(project);

    if (!JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Update Project Classpath",
          "Unable to update project " + project.getName() + "'s classpath.");
      return;
    }

    try {

      List<IClasspathEntry> newRawClasspath = new ArrayList<IClasspathEntry>();

      for (IClasspathEntry entry : javaProject.getRawClasspath()) {
        IClasspathEntry resolvedEntry = JavaCore.getResolvedClasspathEntry(entry);
        IPath resolvedEntryPath = ResourceUtils.resolveToAbsoluteFileSystemPath(resolvedEntry.getPath());

        if (resolvedEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY
            && resolvedEntryPath.equals(buildClasspathFilePath)) {
          newRawClasspath.add(JavaCore.newLibraryEntry(
              serverClasspathFile.getFullPath(), null, null));
        } else {
          newRawClasspath.add(entry);
        }
      }

      ClasspathUtilities.setRawClasspath(javaProject,
          newRawClasspath.toArray(new IClasspathEntry[0]));

    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(
          CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
          "Error While Attempting to Update Project Classpath",
          "Unable to update project " + project.getName()
              + "'s classpath. See the Error Log for more details.");
    }
  }
}
