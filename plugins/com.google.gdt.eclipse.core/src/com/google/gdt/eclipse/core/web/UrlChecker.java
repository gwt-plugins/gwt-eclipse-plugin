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
package com.google.gdt.eclipse.core.web;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Notifies the client when the URL is up and running.
 */
@SuppressWarnings("restriction")
public class UrlChecker {

  /**
   * Called when the URL checker finishes.
   */
  public interface Listener {
    /**
     * Called if an IOException occurs while fetching the URL.
     * 
     * @return true to continue checking, false to abort
     */
    boolean handleUrlCheckerException(IOException e);

    /**
     * Called when the URL checker is finished (if it timed out,
     * <code>success</code> will be false).
     */
    void urlCheckerFinished(boolean success);
  }

  private class UrlCheckerJob extends Job {
    public UrlCheckerJob(String name) {
      super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

      if (System.currentTimeMillis() >= expirationTime) {
        listener.urlCheckerFinished(false);
        return StatusUtilities.OK_STATUS;
      }

      try {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        url.openStream();
        /*
         * Even though we do nothing with the InputStream, we must call this
         * method otherwise getStatusCode will call it indirectly but it will
         * suppress any exceptions and _still_ return OK/200 here!
         * 
         * added 302 (redirect) because some apps redirect to a login
         * page, and then redirect back to the app
         */
        if (connection.getResponseCode() == 200 
            || connection.getResponseCode() == 302) {
          listener.urlCheckerFinished(true);
          return StatusUtilities.OK_STATUS;
        }
      } catch (IOException e) {
        if (!listener.handleUrlCheckerException(e)) {
          return StatusUtilities.newErrorStatus(e, CorePlugin.PLUGIN_ID);
        }
      }

      if (!monitor.isCanceled()) {
        checkerJob.schedule(checkFrequency);
      }

      return StatusUtilities.OK_STATUS;
    }
  }

  private final Listener listener;
  private final URL url;
  private final int checkFrequency;
  private final long expirationTime;
  private final UrlCheckerJob checkerJob;

  /**
   * @param checkFrequency the delay between HTTP requests, in milliseconds
   * @param timeout the maximum amount of time (from this call) to wait before
   *          calling the callback with failure, in milliseconds
   */
  public UrlChecker(URL url, int checkFrequency, int timeout, String jobName,
      Listener listener) {
    this.url = url;
    this.checkFrequency = checkFrequency;
    this.expirationTime = System.currentTimeMillis() + timeout;
    this.listener = listener;

    checkerJob = new UrlCheckerJob(jobName);
  }

  public void startChecking() {
    checkerJob.schedule();
  }
}
