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
package com.google.gdt.eclipse.core.jobs;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Like UnzipRunnable, but uses a File for input (the ZIP archive) and IResource
 * types on the output side. Consider generalizing to use an InputStream as a
 * primary input type, and select from IFile, File or just a stream.
 */
public class UnzipToIFilesRunnable implements IRunnableWithProgressAndStatus {
  /**
   * Convenience method testing the provided file to determine whether is
   * appears to be a valid zip archive.
   */
  private static boolean isZip(File file) throws IOException {
    try {
      (new ZipFile(file)).close();
    } catch (ZipException e) {
      return false;
    }

    return true;
  }
  
  protected IStatus jobStatus;

  private IFolder targetDir;

  private File archive;

  public UnzipToIFilesRunnable(File archive, IFolder targetDir)
      throws IllegalArgumentException {
    if (targetDir.exists()) {
      this.targetDir = targetDir;
    } else {
      throw new IllegalArgumentException("Specified target "
          + targetDir.getLocationURI() + " is not a directory.");
    }

    if (archive.exists() && archive.isFile() && archive.canRead()) {
      this.archive = archive;
    } else {
      throw new IllegalArgumentException("Specified archive file "
          + archive.getAbsolutePath() + " does not exist or can not be read.");
    }

    try {
      if (!isZip(archive)) {
        throw new IllegalArgumentException(
            MessageFormat.format(
                "Parameter zip needs to reference a zip archive file. Path provided is <{0}>",
                (archive == null ? "null" : archive.toString())));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Error reading the provided file "
          + archive.toString(), e);
    }
  }

  public IStatus run(IProgressMonitor monitor) throws InvocationTargetException {
    ZipFile zipFile = null;
    jobStatus = Status.OK_STATUS;
    IProgressMonitor nullProgressMonitor = new NullProgressMonitor();

    try {
      zipFile = new ZipFile(archive);
      // size is the number of entries
      monitor.beginTask("Extracting " + archive.getName(), zipFile.size());

      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        if (zipEntry.isDirectory()) {
          IFolder zipEntryDir = targetDir.getFolder(zipEntry.getName());
          ResourceUtils.createFolderIfNonExistent(zipEntryDir,
              nullProgressMonitor);
        } else {
          IFile zipEntryFile = targetDir.getFile(zipEntry.getName());
          InputStream zipInputStream = zipFile.getInputStream(zipEntry);
          zipEntryFile.create(zipInputStream, true, nullProgressMonitor);
        }
        monitor.worked(1);
        if (monitor.isCanceled()) {
          jobStatus = Status.CANCEL_STATUS;
          break;
        }
      }

      if (!monitor.isCanceled()) {
        jobStatus = Status.OK_STATUS;
      }
    } catch (IOException ioe) {
      String errormsg = errorMessage();
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, errormsg);
    } catch (CoreException ce) {
      String errormsg = errorMessage();
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, errormsg);
    } finally {
      try {
        if (zipFile != null) {
          zipFile.close();
        }
      } catch (IOException ioe) {
        String errormsg = errorMessage("Failed to close file");
        jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, errormsg);
        CorePluginLog.logError(ioe, errormsg);
      }
    }
    return jobStatus;
  }

  /**
   * Convenience method creates readable error message with no explanation.
   */
  private String errorMessage() {
    return errorMessage(null);
  }

  /**
   * Convenience method creates readable error message with the given
   * explanation.
   */
  private String errorMessage(String explanation) {
    String out = "Error extracting zip file " + archive.getAbsolutePath()
        + " into directory " + targetDir.getLocationURI();

    if (explanation != null) {
      out += ". " + explanation;
    }
    return out;
  }
}
