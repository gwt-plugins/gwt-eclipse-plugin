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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import java.io.InputStream;
import java.util.List;

/**
 * Utilities related to web apps (those that have a defined WAR directory).
 */
@SuppressWarnings("restriction")
public class WebAppUtilities {

  /**
   * Utility pair of a file path and its content.
   */
  public static class FileInfo {
    public final IPath path;
    public final InputStream inputStream;

    public FileInfo(IPath path, InputStream inputStream) {
      this.path = path;
      this.inputStream = inputStream;
    }
  }

  /**
   * Interface implemented by extensions that want to locate the output war
   * directory in an alternative manner.
   */
  public interface IOutputWarDirectoryLocator {

    /**
     * Return the output war directory for the given project, or
     * <code>null</code>.
     */
    IFolder getOutputWarDirectory(IProject project);
  }

  public static final String DEFAULT_WAR_DIR_NAME = "war";

  /**
   * Returns the WAR output directory, if it's managed (which means class files
   * and jars are automatically pushed into WEB-INF). Currently, if the WAR
   * output directory is managed, it is actually the <b>same</b> directory as
   * the WAR source directory.
   * 
   * The return value is a resource handle, so don't forget to call
   * {@link IResource#exists()}.
   * 
   * @return a resource handle to the WAR output directory if it's managed, or
   *         <code>null</code> if it's not
   */
  public static IFolder getManagedWarOut(IProject project) {
    if (WebAppProjectProperties.isWarSrcDirOutput(project)) {
      return getWarSrc(project);
    }
    return null;
  }

  /**
   * Returns the same output as
   * {@link WebAppUtilities#getWarOutLocationOrPrompt(IProject)}, except that
   * this does nt prompt the user. <code>null</code> is returned if the path is
   * not found, or if the passed value is not a web app project.
   * 
   * @return the set WAR directory path, or null if not found
   */
  public static IPath getWarOutLocation(IProject project) {
    ExtensionQuery<IOutputWarDirectoryLocator> extQuery = new ExtensionQuery<IOutputWarDirectoryLocator>(
        CorePlugin.PLUGIN_ID, "warOutputDirectoryLocator", "class");
    List<ExtensionQuery.Data<IOutputWarDirectoryLocator>> warOutputDirectoryLocators = extQuery.getData();
    for (ExtensionQuery.Data<IOutputWarDirectoryLocator> warOutputDirectoryLocator : warOutputDirectoryLocators) {
      IFolder warOutputDir = warOutputDirectoryLocator.getExtensionPointData().getOutputWarDirectory(
          project);
      if (warOutputDir != null) {
        return warOutputDir.getLocation();
      }
    }

    IPath fileSystemPath = null;
    IFolder warOut = isWebApp(project) ? getManagedWarOut(project) : null;
    if (warOut != null) {
      fileSystemPath = warOut.getLocation();
    }
    return fileSystemPath;
  }

  /**
   * Returns the file-system path of the WAR output directory. If the project
   * has a managed WAR output directory, it's returned; otherwise, the user will
   * be prompted to select a location from anywhere in his file system.
   * 
   * @return the chosen WAR directory path, or null if cancelled
   */
  public static IPath getWarOutLocationOrPrompt(final IProject project) {

    final IPath[] fileSystemPath = new IPath[] {getWarOutLocation(project)};

    if (fileSystemPath[0] != null) {
      return fileSystemPath[0];
    } else {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
          DirectoryDialog dialog = new DirectoryDialog(shell);
          dialog.setText("WAR Directory Selection");
          dialog.setMessage("Select the WAR directory");

          // Set initial filter path based on last used WAR output directory
          dialog.setFilterPath(WebAppProjectProperties.getLastUsedWarOutLocationOrProjectLocation(
              project).toOSString());
          String pathString = dialog.open();
          if (pathString != null) {
            fileSystemPath[0] = new Path(pathString);

            try {
              // Remember WAR output directory for next time
              WebAppProjectProperties.setLastUsedWarOutLocation(project,
                  fileSystemPath[0]);
            } catch (BackingStoreException e) {
              CorePluginLog.logError(e,
                  "Failed to set last-used WAR output location "
                      + fileSystemPath[0].toString());
            }
          }
        }
      });
    }
    return fileSystemPath[0];
  }

  /**
   * Returns the project's configured WAR source directory.
   * 
   * The return value is a resource handle, so don't forget to call
   * {@link IResource#exists()}.
   */
  public static IFolder getWarSrc(IProject project) {
    assert (isWebApp(project));
    return project.getFolder(WebAppProjectProperties.getWarSrcDir(project));
  }

  /**
   * Returns the WAR output directory's WEB-INF/lib folder.
   * 
   * The return value is a resource handle, so don't forget to call
   * {@link IResource#exists()}.
   */
  public static IFolder getWebInfLib(IProject project) {
    assert (hasManagedWarOut(project));
    return getManagedWarOut(project).getFolder(new Path("WEB-INF/lib"));
  }

  /**
   * Returns the managed WAR <b>output</b> directory's WEB-INF folder.
   * 
   * The return value is a resource handle, so don't forget to call
   * {@link IResource#exists()}.
   */
  public static IFolder getWebInfOut(IProject project) {
    assert (hasManagedWarOut(project));
    return getManagedWarOut(project).getFolder("WEB-INF");
  }

  /**
   * Returns the WAR <b>source</b> directory's WEB-INF folder.
   * 
   * The return value is a resource handle, so don't forget to call
   * {@link IResource#exists()}.
   */
  public static IFolder getWebInfSrc(IProject project) {
    assert (isWebApp(project));
    return getWarSrc(project).getFolder("WEB-INF");
  }

  /**
   * Answers whether the project has a managed WAR output directory.
   */
  public static boolean hasManagedWarOut(IProject project) {
    if (!isWebApp(project)) {
      return false;
    }
    return (getManagedWarOut(project) != null);
  }

  /**
   * Answers whether the project is a web app.
   */
  public static boolean isWebApp(IProject project) {
    IPath path = WebAppProjectProperties.getWarSrcDir(project);
    return path != null;
  }

  /**
   * Sets the project's WAR source directory to "/war" and configures it as a
   * managed WAR output directory as well.
   * 
   * @throws BackingStoreException if there's a problem saving the properties
   */
  public static void setDefaultWarSettings(IProject project)
      throws BackingStoreException {
    WebAppProjectProperties.setWarSrcDir(project,
        new Path(DEFAULT_WAR_DIR_NAME));
    WebAppProjectProperties.setWarSrcDirIsOutput(project, true);
    CorePlugin.getDefault().savePluginPreferences();
  }

  /**
   * Sets the project's default output directory to the WAR output directory's
   * WEB-INF/classes folder. If the WAR output directory does not have a WEB-INF
   * directory, this method returns without doing anything.
   * 
   * @throws CoreException if there's a problem setting the output directory
   */
  public static void setOutputLocationToWebInfClasses(IJavaProject javaProject,
      IProgressMonitor monitor) throws CoreException {
    IProject project = javaProject.getProject();

    IFolder webInfOut = getWebInfOut(project);
    if (!webInfOut.exists()) {
      // If the project has no output <WAR>/WEB-INF directory, don't touch the
      // output location
      return;
    }

    IPath oldOutputPath = javaProject.getOutputLocation();
    IPath outputPath = webInfOut.getFullPath().append("classes");

    if (!outputPath.equals(oldOutputPath)) {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

      // Remove old output location and contents
      IFolder oldOutputFolder = workspaceRoot.getFolder(oldOutputPath);
      if (oldOutputFolder.exists()) {
        try {
          removeOldClassfiles(oldOutputFolder);
        } catch (Exception e) {
          CorePluginLog.logError(e);
        }
      }

      // Create the new output location if necessary
      IFolder outputFolder = workspaceRoot.getFolder(outputPath);
      if (!outputFolder.exists()) {
        // TODO: Could move recreate this in a utilities class
        CoreUtility.createDerivedFolder(outputFolder, true, true, null);
      }

      javaProject.setOutputLocation(outputPath, monitor);
    }
  }

  /**
   * Throws a {@link CoreException} if this project is not a web app or does not
   * have a managed WAR output directory. If no exception results, it is safe to
   * call {@link WebAppUtilities#getManagedWarOut(IProject)}.
   */
  public static void verifyHasManagedWarOut(IProject project)
      throws CoreException {
    if (!hasManagedWarOut(project)) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "{0} does not have a managed WAR output directory",
          CorePlugin.PLUGIN_ID, project.getName()));
    }
  }

  /**
   * Throws a {@link CoreException} if this project is not a web app.
   */
  public static void verifyIsWebApp(IProject project) throws CoreException {
    if (!isWebApp(project)) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "{0} is not configured as a web application", CorePlugin.PLUGIN_ID,
          project.getName()));
    }
  }

  /**
   * This is nearly a clone of the method of the same name in BuildPathsBlock,
   * except here we force delete files, so out-of-sync files don't result in
   * exceptions.
   * 
   * TODO: we can revert back to using BuildPathsBlock's method if we update
   * EnhancerJob to refresh any files it touches.
   */
  private static void removeOldClassfiles(IResource resource)
      throws CoreException {
    if (resource.isDerived()) {
      resource.delete(true, null);
    } else {
      IContainer resourceAsContainer = AdapterUtilities.getAdapter(resource,
          IContainer.class);
      if (resourceAsContainer != null) {
        IResource[] members = resourceAsContainer.members();
        for (int i = 0; i < members.length; i++) {
          removeOldClassfiles(members[i]);
        }
      }
    }
  }
}
