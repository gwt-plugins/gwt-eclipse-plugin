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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Uses a local file to provide an API directory.
 */
public class LocalApiDirectory implements ApiDirectory {

  class ApiDirectoryListingFileReader {

    private String contents = null;

    private String sFile = null;

    public String getListing() {
      return contents;
    }

    public IStatus run(IProgressMonitor monitor) {
      IStatus jobStatus = Status.OK_STATUS;

      InputStream input = null;
      File directoryFile = null;
      try {
        directoryFile = new File(sFile);

        input = new FileInputStream(sFile);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long totalBytesRead = 0L;
        long contentLength = directoryFile.length();
        monitor.beginTask("Loading " + sFile, (int) contentLength);

        int n = 0;
        byte[] buffer = new byte[1024];
        while (-1 != (n = input.read(buffer))) {
          output.write(buffer, 0, n);
          totalBytesRead += n;
          monitor.worked((int) totalBytesRead);
          if (monitor.isCanceled()) {
            jobStatus = Status.CANCEL_STATUS;
            break;
          }
        }
        contents = output.toString();
      } catch (IOException e) {
        jobStatus = new Status(Status.ERROR, ManagedApiPlugin.PLUGIN_ID,
            "IO error while downloading " + sFile, e);
      } finally {
        monitor.done();
      }

      closeInputStream(input);
      status = jobStatus;
      return jobStatus;
    }

    public void setSourceFile(String sourceFile) {
      this.sFile = sourceFile;
    }

    private void closeInputStream(InputStream input) {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          // Log, but continue
          // CorePluginLog.logError(e);
        }
      }
    }
  }

  private IStatus status = STATUS_UNINITIALIZED;

  private String sourceFile;

  private ApiDirectoryListing listing;

  public ApiDirectoryListing getApiDirectoryListing() {
    return listing;
  }

  public IStatus getStatus() {
    return status;
  }

  public IStatus run(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;
    SubMonitor submon = SubMonitor.convert(monitor,
        "Get directory listing for APIs", 100);

    ApiDirectoryListingFileReader reader = new ApiDirectoryListingFileReader();
    reader.setSourceFile(sourceFile);

    jobStatus = reader.run(submon.newChild(80));
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    if (!Status.OK_STATUS.equals(jobStatus)) {
      return jobStatus;
    }

    ApiDirectoryListingParser parser = new ApiDirectoryListingParser();
    parser.setSerializedListing(reader.getListing());
    jobStatus = parser.run(submon.newChild(20));
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    if (!Status.OK_STATUS.equals(jobStatus)) {
      return jobStatus;
    }
    listing = parser.getListing();
    monitor.done();

    return jobStatus;
  }

  /**
   * Specifies the file to use as content for the ApiDirectory.
   *
   * @param sourceFile
   */
  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }
}