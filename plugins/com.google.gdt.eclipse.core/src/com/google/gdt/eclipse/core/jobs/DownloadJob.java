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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * A job that downloads the specified URL to a target file.
 */
public class DownloadJob extends Job {
  private IRunnableWithProgressAndStatus download = null;

  private URL url;

  public DownloadJob(String name, URL url, File target) {
    super(name);

    this.url = url;

    download = doGetRunnable(url, target);
  }

  /**
   * 
   * Subclasses may override to provide their own
   * {@link IRunnableWithProgressAndStatus} with specific download behavior. See
   * {@link PingJob}.
   * 
   * @param url the URL to use with the download
   * @param target the File to download the data to
   * @return an {@link IRunnableWithProgressAndStatus} that will be used to
   *         perform the download
   */
  protected IRunnableWithProgressAndStatus doGetRunnable(URL url, File target) {
    return new DownloadRunnable(url, target);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    String urlString = url.getProtocol() + "://" + url.getHost()
        + url.getPath();

    Status errorStatus = new Status(IStatus.WARNING, CorePlugin.PLUGIN_ID,
        "Unable to connect to " + urlString + ".");

    IStatus jobStatus;
    try {
      jobStatus = download.run(monitor);
    } catch (InvocationTargetException e) {
      // the run methods from DownloadJob and PingJob's runnables don't
      // actually throw this exception, it's just specified by the interface.
      return errorStatus;
    }

    if (jobStatus.getException() instanceof IOException) {
      // IOExceptions are expected - don't print a stacktrace to the log.
      jobStatus = errorStatus;
    }

    monitor.done();

    return jobStatus;
  }
}
