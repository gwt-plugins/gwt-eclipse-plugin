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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * UnzipRunnable extracts the supplied archive file to the directory identified
 * in targetDir. This runnable is authored to be included as but one step in a
 * larger Job, so the done() method on the monitor is not called.
 */
public class UnzipRunnable extends AbstractExtractRunnable implements
    IRunnableWithProgressAndStatus {

  private static final int BUFSIZE = 2048;

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

  /**
   * Constructor for an UnzipRunnable takes an archive file and a target
   * directory. On run it will extract the archive.
   * 
   * @param zip
   *          the archive file
   * @param targetDir
   *          the target directory
   * @throws IllegalArgumentException
   */
  public UnzipRunnable(File zip, File targetDir) throws IllegalArgumentException {
    super(zip, targetDir);
    try {
      if (!isZip(zip)) {
        throw new IllegalArgumentException("Parameter zip needs to reference a zip archive file.");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Error reading the provided file " + zip.toString(), e);
    }
  }

  /**
   * Executes the unzipRunnable using the provided monitor to update the user
   * and provide cancel capabilities.
   * 
   * @param monitor
   *          used to monitor progress of the DownloadRunnable process.
   * 
   * @return The completion status of the runnable. If everything goes well,
   *         then this status is OK_STATUS. If the monitor cancels the job,
   *         Status.CANCEL_STATUS. Other errors result in a status with a
   *         severity of ERROR.
   */
  public IStatus run(IProgressMonitor monitor) throws InvocationTargetException {
    ZipFile zipFile = null;
    FileOutputStream fos = null;
    BufferedOutputStream dest = null;
    BufferedInputStream is = null;
    jobStatus = Status.OK_STATUS;

    try {
      zipFile = new ZipFile(archive);
      monitor.beginTask("Uncompressing " + archive.getName(), zipFile.size());

      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();

        String currentEntry = zipEntry.getName();
        File destFile = new File(targetDir, currentEntry);
        File destinationParent = destFile.getParentFile();

        // write the current file to disk
        if (!zipEntry.isDirectory()) {
          destinationParent.mkdirs();

          is = new BufferedInputStream(zipFile.getInputStream(zipEntry));

          int numBytesRead;
          byte bytes[] = new byte[BUFSIZE];

          fos = new FileOutputStream(destFile);
          dest = new BufferedOutputStream(fos, BUFSIZE);

          while ((numBytesRead = is.read(bytes, 0, BUFSIZE)) != -1) {
            dest.write(bytes, 0, numBytesRead);
          }
          dest.flush();
        } else {
          // If it's a directory, build all the directories up to that point.
          destFile.mkdirs();
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
    } catch (ZipException ze) {
      String errormsg = errorMessage("It may be corrupted?");
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, errormsg);
      CorePluginLog.logError(ze, errormsg);
    } catch (IOException ioe) {
      String errormsg = errorMessage();
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID, errormsg);
      CorePluginLog.logError(ioe, errormsg);
    } finally {
      try {
        if (zipFile != null) {
          zipFile.close();
        }
        if (is != null) {
          is.close();
        }
        if (dest != null) {
          dest.close();
        }
      } catch (IOException ioe) {
        String errormsg = errorMessage();
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
    String out = "Error extracting zip file " + archive.getAbsolutePath() + " into directory "
        + targetDir.getAbsolutePath();

    if (explanation != null) {
      out += ". " + explanation;
    }
    return out;
  }

}
