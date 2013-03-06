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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

/**
 * DownloadRunnable downloads the supplied URL to the file identified in target.
 * This runnable is authored to be included as but one step in a larger Job, so
 * the done() method on the monitor is not called.
 * 
 * The suppression of restriction warnings addresses the use of IResponse in the
 * org.eclipse.update.internal.core.connection package. Consider creating an
 * equivalent in a more available package.
 */
@SuppressWarnings("restriction")
public class DownloadRunnable implements IRunnableWithProgressAndStatus {

  private URL url;
  private File target;

  /**
   * Construction of the DownloadRunnable takes a url and target file.
   * 
   * @param url the source for the download
   * @param target the target file for the download .This can be a new file
   *          object that does not yet exist so long as FileOutputStream can
   *          create it.
   */
  public DownloadRunnable(URL url, File target) {
    this.url = url;
    this.target = target;
  }

  /**
   * Executes the downloadRunnable using the provided monitor to update the user
   * and provide cancel capabilities.
   * 
   * @param monitor used to monitor progress of the DownloadRunnable process.
   * 
   * @return The completion status of the runnable. If everything goes well,
   *         then this status is OK_STATUS. If the monitor cancels the job,
   *         Status.CANCEL_STATUS. Other errors result in a status with a
   *         severity of ERROR.
   */
  public IStatus run(IProgressMonitor monitor) {
    InputStream input = null;
    FileOutputStream output = null;
    IStatus jobStatus = Status.OK_STATUS;
    byte[] b = new byte[1024];
    int bytesRead;

    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      input = url.openStream();
      long totalBytesRead = 0L;
      int responseStatusCode = connection.getResponseCode();
      if (responseStatusCode >= HttpURLConnection.HTTP_BAD_REQUEST) { // 400
        jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
            MessageFormat.format("Remote ServerError: {0} ({1})",
                responseStatusCode, connection.getResponseMessage()));
      }
      long contentLength = connection.getContentLength();

      output = new FileOutputStream(target);

      monitor.beginTask("Downloading " + url.toString(), (int) contentLength);

      while (jobStatus == Status.OK_STATUS) {
        if (contentLength > 0 && totalBytesRead >= contentLength) {
          break;
        }

        bytesRead = input.read(b);

        if (bytesRead == -1) {
          break;
        }
        
        output.write(b, 0, bytesRead);

        totalBytesRead += bytesRead;
        monitor.worked(bytesRead);

        if (monitor.isCanceled()) {
          jobStatus = Status.CANCEL_STATUS;
          break;
        }
      }
    } catch (MalformedURLException e) {
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
          "Malformed URL: " + url.toExternalForm(), e);
    } catch (IOException e) {
      jobStatus = new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
          "IO error while downloading " + url.toExternalForm(), e);
    }

    closeStreams(input, output);
    return jobStatus;
  }

  /**
   * Convenience method closes stream objects used in the download.
   */
  private void closeStreams(InputStream input, FileOutputStream output) {
    if (output != null) {
      try {
        output.close();
      } catch (IOException e) {
        // Log, but continue
        CorePluginLog.logError(e);
      }
    }

    if (input != null) {
      try {
        input.close();
      } catch (IOException e) {
        // Log, but continue
        CorePluginLog.logError(e);
      }
    }
  }
}
