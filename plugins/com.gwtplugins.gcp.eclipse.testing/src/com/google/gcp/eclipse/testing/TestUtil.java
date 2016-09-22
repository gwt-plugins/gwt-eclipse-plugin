/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gcp.eclipse.testing;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * Testing utilities for setting up JUnit Plug-in Tests for the GPE.
 */
public class TestUtil {
  public static final String PLUGIN_ID = "com.google.gcp.eclipse.testing";

  private TestUtil() {} // Non-instantiatable utility class

  /**
   * Waits for the specified number of milliseconds.
   */
  public static void delay(long waitTimeMillis) {
    Display display = Display.getCurrent();

    // If this is the UI thread, then process input.
    if (display != null) {

      /*
       * We set up a timer on the UI thread that fires after the desired wait time. We do this
       * because we want to make sure that the UI thread wakes up from a display.sleep() call. We
       * set a flag in the runnable so that we can terminate the wait loop.
       */
      final boolean[] hasDeadlineTimerFiredPtr = {false};

      display.timerExec((int) waitTimeMillis, new Runnable() {
        @Override
        public void run() {

          /*
           * We don't have to worry about putting a lock around the update/read of this variable. It
           * is only accessed by the UI thread, and there is only one UI thread.
           */
          hasDeadlineTimerFiredPtr[0] = true;
        }
      });

      while (!hasDeadlineTimerFiredPtr[0]) {

        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }

      display.update();
    } else {
      try {
        // Otherwise, perform a simple sleep.
        Thread.sleep(waitTimeMillis);
      } catch (InterruptedException e) {
        // Ignored
      }
    }
  }

  /**
   * Wait until all background tasks are complete.
   */
  public static void waitForIdle() {
    while (!Job.getJobManager().isIdle()) {
      delay(500);
    }
  }
}
