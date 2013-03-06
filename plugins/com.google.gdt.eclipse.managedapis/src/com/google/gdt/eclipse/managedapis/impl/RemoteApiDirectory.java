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

import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.TimeProvider;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Access a remote (URL specified) API directory.
 * 
 * The suppression of restriction warnings addresses the use of IResponse in the
 * org.eclipse.update.internal.core.connection package. Consider creating an
 * equivalent in a more available package.
 */
@SuppressWarnings("restriction")
public class RemoteApiDirectory implements ApiDirectory {

  class ApiDirectoryListingLinkReader {

    private String listing = null;

    public String getListing() {
      return listing;
    }

    public IStatus run(IProgressMonitor monitor) {
      IStatus jobStatus = Status.OK_STATUS;

      InputStream input = null;
      URL directoryUrl = null;
      try {
        directoryUrl = new URL(directoryLink);
        URLConnection connection = directoryUrl.openConnection();
        input = directoryUrl.openStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long totalBytesRead = 0L;
        long contentLength = connection.getContentLength();
        monitor.beginTask("Downloading " + directoryUrl.toString(),
            (int) contentLength);

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
        listing = output.toString();
      } catch (MalformedURLException e) {
        jobStatus = new Status(Status.ERROR, ManagedApiPlugin.PLUGIN_ID,
            "Malformed URL: "
                + (null != directoryUrl ? directoryUrl.toExternalForm()
                    : "null"), e);
      } catch (IOException e) {
        jobStatus = new Status(Status.ERROR, ManagedApiPlugin.PLUGIN_ID,
            "IO error while downloading "
                + (null != directoryUrl ? directoryUrl.toExternalForm()
                    : "null"), e);
      } finally {
        monitor.done();
      }

      closeInputStream(input);
      return jobStatus;
    }

    private void closeInputStream(InputStream input) {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          ManagedApiLogger.warn(e,
              "Error closing input stream from remote directory");
        }
      }
    }
  }

  private IStatus status = STATUS_UNINITIALIZED;

  private String directoryLink;

  private ApiDirectoryListing remoteApiDirectoryListing;

  private TimeProvider timeProvider = new TimeProvider() {
    public long getCurrentTime() {
      return System.currentTimeMillis();
    }
  };

  private long expirationTimestamp = 0L;

  public RemoteApiDirectory() {
  }

  public RemoteApiDirectory(String link) {
    setDirectoryLink(link);
  }

  public void flagAsDirty() {
    if (status == Status.OK_STATUS) {
      status = STATUS_DIRTY;
    }
  }

  public ApiDirectoryListing getApiDirectoryListing() {
    return remoteApiDirectoryListing;
  }

  public IStatus getStatus() {
    if (status == Status.OK_STATUS
        && timeProvider.getCurrentTime() > expirationTimestamp) {
      status = STATUS_DIRTY;
    }
    return status;
  }

  public IStatus run(IProgressMonitor monitor) {
    IStatus localStatus = getStatus();
    if (localStatus == Status.OK_STATUS) {
      return localStatus;
    } else {
      SubMonitor submon = SubMonitor.convert(monitor,
          "Get directory listing for APIs", 100);
      IStatus jobStatus = Status.OK_STATUS;

      ApiDirectoryListingLinkReader reader = new ApiDirectoryListingLinkReader();

      jobStatus = reader.run(submon.newChild(80));
      if (submon.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      if (!Status.OK_STATUS.equals(jobStatus)) {
        return jobStatus;
      }

      ApiDirectoryListingParser parser = new ApiDirectoryListingParser();
      parser.setSerializedListing(reader.getListing());
      jobStatus = parser.run(submon.newChild(20));
      if (submon.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      if (!Status.OK_STATUS.equals(jobStatus)) {
        return jobStatus;
      }
      remoteApiDirectoryListing = parser.getListing();
      submon.done();

      expirationTimestamp = timeProvider.getCurrentTime()
          + ManagedApiPlugin.API_DIRECTORY_CACHE_TTL;
      status = jobStatus;
      return jobStatus;
    }
  }

  public void setDirectoryLink(String directoryLink) {
    this.directoryLink = directoryLink;
  }

  public void setTimeProvider(TimeProvider tp) {
    this.timeProvider = tp;
  }

}
