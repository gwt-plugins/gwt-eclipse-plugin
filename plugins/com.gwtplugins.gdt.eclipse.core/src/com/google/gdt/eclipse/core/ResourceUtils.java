/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Static utility methods that have to do with resources and editors.
 */

public class ResourceUtils {

  /**
   * Introduced in Eclipse 3.4, copied locally so earlier versions don't break.
   */
  private static final int IRESOURCEDELTA_LOCAL_CHANGED = 0x200000;

  /**
   * @return true if both files have the same contents, false if not or if a
   *         file does not exist
   *
   * @throws IOException
   */
  public static boolean areFileContentsEqual(File f1, File f2)
      throws IOException {

    if (!f1.exists() || !f2.exists()) {
      return false;
    }

    BufferedInputStream bis1 = null;
    BufferedInputStream bis2 = null;

    try {
      bis1 = new BufferedInputStream(new FileInputStream(f1));
      bis2 = new BufferedInputStream(new FileInputStream(f2));

      byte[] buf1 = new byte[4096];
      byte[] buf2 = new byte[4096];

      int numRead1 = 0;
      int numRead2 = 0;

      while (numRead1 >= 0 && numRead2 >= 0) {
        numRead1 = bis1.read(buf1);
        numRead2 = bis2.read(buf2);

        if (numRead1 == numRead2) {
          for (int i = 0; i < numRead1; i++) {
            if (buf1[i] != buf2[i]) {
              return false;
            }
          }
        } else {
          return false;
        }
      }

      return numRead1 == -1 && numRead2 == -1;

    } finally {
      if (bis1 != null) {
        bis1.close();
      }

      if (bis2 != null) {
        bis2.close();
      }
    }
  }

  /**
   * @return true if set1 and set2 contain only identical files (filename +
   *         contents)
   * @throws IOException
   */
  public static boolean areFileListsEqual(List<File> set1, List<File> set2)
      throws IOException {
    List<File> set1Copy = new ArrayList<File>(set1);

    for (File f2 : set2) {
      String f2Name = f2.getName();

      File f1 = findFileWithName(set1, f2Name);
      if (f1 == null || !areFileContentsEqual(f1, f2)) {
        return false;
      }

      set1Copy.remove(f1);
    }

    return set1Copy.isEmpty();
  }

  /**
   * Returns whether two filenames are equal, according to platform conventions
   * (e.g. Unix allows sibling files to have names that differ only by case, but
   * Windows does not).
   */
  public static boolean areFilenamesEqual(String fileName1, String fileName2) {
    return new File(fileName1).equals(new File(fileName2));
  }

  /**
   * Copies the source file into the destination file. The destination file's
   * parent directories will be created (if they do not exist already). The
   * destination file will be overwritten if it already exists.
   *
   * @throws IOException
   */
  public static void copyFile(File sourceFile, File destFile)
      throws IOException {
    assert sourceFile.exists() && sourceFile.isFile();

    destFile.getParentFile().mkdirs();
    if (!destFile.getParentFile().exists()) {
      throw new IOException("Could not make parent directories");
    }

    destFile.createNewFile();
    if (!destFile.exists()) {
      throw new IOException("Could not create destination file");
    }

    FileOutputStream fos = null;
    FileInputStream fis = null;

    try {
      fos = new FileOutputStream(destFile);
      fis = new FileInputStream(sourceFile);

      byte[] buf = new byte[4096];
      int numRead;

      while ((numRead = fis.read(buf)) >= 0) {
        fos.write(buf, 0, numRead);
      }
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } finally {
        if (fos != null) {
          fos.close();
        }
      }
    }
  }

  /**
   * Copies all the contents of srcFolder to destFolder.
   */
  public static void copyFolder(File srcFolder, File destFolder)
      throws CoreException, IOException {
    for (File file : srcFolder.listFiles()) {
      if (file.isDirectory()) {
        copyFolder(file, new File(destFolder.getAbsolutePath() + File.separator
            + file.getName()));
      } else {
        ResourceUtils.copyFile(file, new File(destFolder.getAbsolutePath()
            + File.separator + file.getName()));
      }
    }
  }

  /**
   * Create a file at the given workspace-relative path with the contents from
   * the {@link InputStream}. If the file already exists, then a CoreException
   * will be thrown.
   */
  public static IFile createFile(IPath path, InputStream contents)
      throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(path);
    file.create(contents, false, null);
    return file;
  }

  /**
   * Create a file at the given path with the given contents. If the file
   * already exists, then a CoreException will be thrown.
   */
  public static IFile createFile(IPath path, String contents)
      throws CoreException, UnsupportedEncodingException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        contents.getBytes("UTF-8"));
    return createFile(path, byteArrayInputStream);
  }

  /**
   * Creates the specified folder if it does not already exist. See the
   * {@link IFolder#create(boolean, boolean, IProgressMonitor)} for more
   * information.
   */
  public static void createFolderIfNonExistent(IFolder folder,
      IProgressMonitor monitor) throws CoreException {
    if (!folder.exists()) {
      folder.create(true, true, monitor);
    }
  }

  /**
   * Creates the folder specified by projectRelativeFolderPath, and all parent
   * folders, if they do not already exist.
   *
   * @param project
   * @param projectRelativeFolderPath
   * @throws CoreException
   */
  public static void createFolderStructure(IProject project,
      IPath projectRelativeFolderPath) throws CoreException {

    IContainer curContainer = project;

    for (String pathSegment : projectRelativeFolderPath.segments()) {
      curContainer = curContainer.getFolder(new Path(pathSegment));
      createFolderIfNonExistent((IFolder) curContainer,
          new NullProgressMonitor());
    }
  }

  /**
   * Delete the specified file. If the {@link File} is a directory child
   * directories will also be deleted.
   */
  public static void deleteFileRecursively(File fileOrDir) {
    if (fileOrDir.isDirectory()) {
      for (File file : fileOrDir.listFiles()) {
        deleteFileRecursively(file);
      }
    }

    fileOrDir.delete();
  }

  /**
   * Like {@link #deleteFiles(File, List)}, except at the
   * <code>java.io.File</code> layer. The workspace is not refreshed after the
   * deletions occur.
   */
  public static void deleteFiles(File folder, List<String> filesToRemoveList)
      throws CoreException {
    if (!folder.exists()) {
      return;
    }
    for (File resource : folder.listFiles()) {
      if (resource.isDirectory()) {
        deleteFiles(resource, filesToRemoveList);
      } else if (filesToRemoveList.contains(resource.getName())) {
        resource.delete();
      }
    }
  }

  /**
   * Deletes from the folder tree any file present in filesToRemoveList.
   *
   * @throws CoreException
   */
  public static void deleteFiles(IFolder folder, List<String> filesToRemoveList)
      throws CoreException {
    deleteFiles(folder.getLocation().toFile(), filesToRemoveList);
    folder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
  }

  /**
   * Deletes files from targetFolder that are not in fileList.
   *
   * @param folder The folder files would be deleted from.
   * @param dependecyFiles List of files that should not be removed from targetFolder.
   */
  public static void deleteUnlistedFiles(File folder, Set<String> dependecyFiles) {
    if (!folder.exists() || (dependecyFiles == null)) {
      return;
    }

    for (File resource : folder.listFiles()) {
      if (resource.isDirectory()) {
        deleteUnlistedFiles(resource, dependecyFiles);
      } else if (!dependecyFiles.contains(resource.getName())) {
          resource.delete();
      }
    }
  }

  /**
   * Returns whether the given file name ends with the given suffix.
   */
  public static boolean endsWith(String fileName, String suffix) {
    return isFilesystemCaseSensitive() ? fileName.endsWith(suffix)
        : fileName.toLowerCase().endsWith(suffix.toLowerCase());
  }

  /**
   * Returns the filename without its file extension (if present). Note that
   * this only removes the last extension, in the case of multiple extensions.
   * Note that if the file has a leading dot (e.g. hidden files), this method
   * will return an empty string.
   */
  public static String filenameWithoutExtension(IFile file) {
    String filename = file.getName();
    int extPos = filename.lastIndexOf('.');
    if (extPos == -1) {
      return filename;
    }
    return filename.substring(0, extPos);
  }

  /**
   * Given an absolute workspace path to a resource, returns a handle to the
   * first resource which contains this resource, and exists.
   *
   * @return null if no resource that could be found which exists and contains
   *         the resource described by the path
   */
  public static IResource findFirstEnclosingResourceThatExists(
      IPath nonExistentWorkspaceResource) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

    int pathSegments = nonExistentWorkspaceResource.segmentCount();
    for (int i = 1; i < pathSegments; i++) {
      IResource resource = workspaceRoot.findMember(nonExistentWorkspaceResource.removeLastSegments(i));
      if (resource != null) {
        return resource;
      }
    }

    return null;
  }

  /**
   * Checks if the given file exists, is a directory, and has something in it.
   *
   * @param folder The folder to check
   * @return True if the file exists, is a directory, and has something in it.
   */
  public static boolean folderExistsAndHasContents(File folder) {
    if (folder.exists()) {
      if (folder.isDirectory()) {
        if (folder.list().length > 0) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns the classpath-relative path (with slashes, not dots) of the file
   * with the given filename.
   */
  public static IPath getClasspathRelativePath(IPackageFragment pckg,
      String filename) {
    if (pckg.isDefaultPackage()) {
      return new Path(filename);
    }
    return new Path(pckg.getElementName().replace('.', '/')).append(filename);
  }

  /**
   * Get the file being edited by a given editor. Note that this method can
   * return null if the specified editor isn't an IFileEditor.
   */
  public static IFile getEditorInput(IEditorPart editor) {
    IFileEditorInput fileEditorInput = AdapterUtilities.getAdapter(
        editor.getEditorInput(), IFileEditorInput.class);
    if (fileEditorInput != null) {
      return fileEditorInput.getFile();
    }
    return null;
  }

  /**
   * @return returns an {@link IFile} for the given {@link File}, or null (if
   *         the {@link File} is not in the workspace.)
   */
  public static IFile getFile(File file) {
    return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
        Path.fromOSString(file.getAbsolutePath()));
  }

  /**
   * Returns a resource for the given absolute or workspace-relative path.
   * <p>
   * If the path has a device (e.g. c:\ on Windows), it will be tried as an
   * absolute path. Otherwise, it is first tried as a workspace-relative path,
   * and failing that an absolute path.
   *
   * @param path the absolute or workspace-relative path to a resource
   * @return the resource, or null
   */
  // TODO(tparker): Check against the internal implementation, which was tricky to get right.
  public static IResource getResource(IPath path) {
    IResource res = null;
    if (path != null) {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      if (path.getDevice() == null) {
        // try searching relative to the workspace first
        res = root.findMember(path);
      }

      if (res == null) {
        // look for files
        IFile[] files = root.findFilesForLocation(path);
        // Check for accessibility since for directories, the above will return
        // a non-accessible IFile
        if (files.length > 0 && files[0].isAccessible()) {
          res = files[0];
        }
      }

      if (res == null) {
        // look for folders
        IContainer[] containers = root.findContainersForLocation(path);
        if (containers.length > 0) {
          res = containers[0];
        }
      }
    }
    return res;
  }

  /**
   * Loads a text resource via a particular class's class loader.
   * <p>
   * The resource may be a template containing place-holder tokens, which this
   * method will replace with real values. The place-holders can be any tokens
   * that will not appear anywhere else in the template; the convention we've
   * used is: &#064;SomeTokenIdentifier&#064;.
   * <p>
   * The place-holder tokens and replacement values should be passed in via the
   * Map argument as its keys and values, respectively. Note that the
   * place-holder tokens are interpreted as regular expressions, so be sure to
   * do any necessary escaping of special characters.
   */
  public static InputStream getResourceAsStreamAndFilterContents(
      Class<?> context, Map<String, String> replacements, String resourceName)
      throws CoreException {
    String filteredContent = getResourceAsStringAndFilterContents(context,
        replacements, resourceName);
    if (filteredContent != null) {
      try {
        return new ByteArrayInputStream(filteredContent.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new CoreException(StatusUtilities.newErrorStatus(e,
            CorePlugin.PLUGIN_ID));
      }
    }

    return null;
  }

  /**
   * Loads a text resource via a particular class's class loader.
   */
  public static String getResourceAsString(Class<?> context, String resourceName)
      throws CoreException {
    return getResourceAsString(context.getResource(resourceName));
  }

  /**
   * Loads a text resource via a particular class loader.
   */
  public static String getResourceAsString(ClassLoader classLoader,
      String resourceName) throws CoreException {
    return getResourceAsString(classLoader.getResource(resourceName));
  }

  /**
   * Loads a text resource via a particular class's class loader.
   *
   * The resource may be a template containing place-holder tokens which this
   * method will replace with real values. See
   * {@link ResourceUtils#getResourceAsStreamAndFilterContents(Class, Map, String)}
   * for details on template usage.
   */
  public static String getResourceAsStringAndFilterContents(Class<?> context,
      Map<String, String> replacements, String resourceName)
      throws CoreException {
    String contents = getResourceAsString(context, resourceName);
    if (contents == null) {
      return null;
    }
    return makeTemplateReplacements(replacements, contents);
  }

  /**
   * Get the resource pointed at by the specified selection. Note that this
   * method can return null in the case that the selection is empty, or it isn't
   * an IStructuredSelection.
   */
  public static IResource getSelectionResource(ISelection selection) {
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      Object element = ((IStructuredSelection) selection).getFirstElement();
      return AdapterUtilities.getAdapter(element, IResource.class);
    }
    return null;
  }

  /**
   * Returns true if the resource has an extension of <code>jsp</code> or <code>
   * html</code> .
   *
   * Note that the resource need not be an IFile.
   */
  public static boolean hasJspOrHtmlExtension(IResource resource) {
    String extension = resource.getFileExtension();
    return ("jsp".equalsIgnoreCase(extension) || "html".equalsIgnoreCase(extension));
  }

  /**
   * Returns whether the platform treates file paths in a case-sensitive manner.
   * (e.g. Unix allows sibling files to have names that differ only by case, but
   * Windows does not)
   */
  public static boolean isFilesystemCaseSensitive() {
    return !areFilenamesEqual("a", "A");
  }

  /**
   * Given a resource delta, indicate whether or the delta contains a type of
   * resource change that GPE generally cares about.
   */
  public static boolean isRelevantResourceChange(IResourceDelta delta) {

    if (delta == null) {
      return false;
    }

    int relevantChanges = IResourceDelta.CONTENT | IResourceDelta.DESCRIPTION
        | IRESOURCEDELTA_LOCAL_CHANGED | IResourceDelta.OPEN
        | IResourceDelta.REPLACED | IResourceDelta.TYPE;
    // Either added or removed (through != CHANGED), or changed in some
    // relevant way (through (.. & relevantChanges) != 0)
    if (delta.getKind() != IResourceDelta.CHANGED
        || (delta.getFlags() & relevantChanges) != 0) {
      return true;
    }

    return false;
  }

  /**
   * Opens the file in the default editor for the file's type.
   *
   * @param shell
   * @param file
   * @param activate whether this editor should be the active (selected) editor
   */
  public static void openInDefaultEditor(Shell shell, final IFile file,
      final boolean activate) {
    assert (file != null);

    shell.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        try {
          IWorkbenchPage page = CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
          IDE.openEditor(page, file, activate);
        } catch (PartInitException e) {
          CorePluginLog.logError(e);
        }
      }
    });
  }

  /**
   * Reads the contents of a file.
   *
   * @param file A file object
   * @return the contents of the file
   * @throws IOException
   */
  public static String readFileContents(File file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(file));
    StringBuilder sb = new StringBuilder((int) file.length());

    try {
      char[] buf = new char[4096];
      int read;
      while ((read = br.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }
    } finally {
      br.close();
    }

    return sb.toString();
  }

  /**
   * Reads the contents of a file.
   *
   * @param path the absolute filesystem path to the file
   * @return the contents of the file
   * @throws IOException
   */
  public static String readFileContents(IPath path) throws IOException {
    File file = new File(path.toOSString());
    return readFileContents(file);
  }

  /**
   * Resolves a linked resource to its target resource, or returns the given
   * resource if it is not linked or the target resource cannot be resolved.
   */
  public static IResource resolveTargetResource(IResource resource) {
    if (!resource.isLinked()) {
      return resource;
    }

    IResource resolvedResource = getResource(resource.getLocation());
    return resolvedResource != null ? resolvedResource : resource;
  }

  /**
   * Returns the absolute file system path for an absolute file system or
   * workspace relative path.
   */
  public static IPath resolveToAbsoluteFileSystemPath(IPath path) {
    if (path != null) {
      IResource res = null;
      if (path.getDevice() == null) {
        // if there is no device specified, find the resource
        res = getResource(path);
      }
      if (res == null) {
        return path;
      }
      IPath location = res.getLocation();
      if (location != null) {
        return location;
      }
    }
    return null;
  }

  /**
   * Writes the contents to the file at the given location (this creates the
   * file or deletes its existing contents.)
   *
   * @throws IOException
   */
  public static void writeToFile(File file, String contents) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(contents.getBytes());
    fos.close();
  }

  private static File findFileWithName(List<File> files, String fileName) {
    for (File file : files) {
      String curFileName = file.getName();

      if (isFilesystemCaseSensitive()) {
        if (curFileName.equals(fileName)) {
          return file;
        }
      } else {
        if (curFileName.equalsIgnoreCase(fileName)) {
          return file;
        }
      }
    }

    return null;
  }

  private static String getResourceAsString(URL resource) throws CoreException {
    if (resource == null) {
      return null;
    }

    InputStream inputStream = null;
    try {
      URLConnection openConnection = resource.openConnection();
      openConnection.setUseCaches(false);

      inputStream = openConnection.getInputStream();
      int contentLength = openConnection.getContentLength();
      if (contentLength < 0) {
        return null;
      }

      byte[] bytes = new byte[contentLength];
      int byteOffset = 0;
      while (byteOffset < contentLength) {
        int bytesReadCount = inputStream.read(bytes, byteOffset, contentLength
            - byteOffset);
        if (bytesReadCount == -1) {
          return null;
        }

        byteOffset += bytesReadCount;
      }

      return new String(bytes, "UTF-8");
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          throw new CoreException(new Status(IStatus.ERROR,
              CorePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
        }
      }
    }
  }

  private static String makeTemplateReplacements(
      Map<String, String> replacements, String contents) {
    String replacedContents = contents;
    Set<Entry<String, String>> entries = replacements.entrySet();
    for (Iterator<Entry<String, String>> iter = entries.iterator(); iter.hasNext();) {
      Entry<String, String> entry = iter.next();
      String replaceThis = entry.getKey();
      String withThis = entry.getValue();
      replacedContents = replacedContents.replaceAll(replaceThis, withThis);
    }

    return replacedContents;
  }

}
