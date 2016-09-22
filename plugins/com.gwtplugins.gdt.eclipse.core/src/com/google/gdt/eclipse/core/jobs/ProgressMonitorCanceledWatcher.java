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

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches an {@link IProgressMonitor} for cancellations. This will spin a new
 * thread, and give the client a callback if it notices a cancel. The client
 * must stop the watcher ({@link IProgressMonitor} does not have API to query
 * for completion.)
 */
public class ProgressMonitorCanceledWatcher {

  /**
   * Listens for cancellations.
   */
  public interface Listener {
    /**
     * Called when the progress monitor is canceled. This will be called from a
     * different thread, ensure the implementation is thread-safe.
     */
    void progressMonitorCanceled();
  }

  private class WatcherThread extends Thread {
    public WatcherThread() {
      setDaemon(true);
    }

    @Override
    public void run() {
      while (!monitor.isCanceled() && !stopWatching.get()) {
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          CorePluginLog.logError(e,
              "ProgressMonitorCanceledWatcher was interrupted");
          return;
        }
      }

      if (monitor.isCanceled()) {
        listener.progressMonitorCanceled();
      }
    }
  }

  private final Listener listener;

  private final IProgressMonitor monitor;

  private final WatcherThread thread = new WatcherThread();

  private AtomicBoolean stopWatching = new AtomicBoolean(false);

  public ProgressMonitorCanceledWatcher(IProgressMonitor monitor,
      Listener listener) {
    this.monitor = monitor;
    this.listener = listener;
  }

  public void start() {
    thread.start();
  }

  public void stop() {
    stopWatching.set(true);
  }
}
