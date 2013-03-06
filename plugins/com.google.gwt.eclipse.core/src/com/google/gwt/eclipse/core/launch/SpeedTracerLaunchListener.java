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
package com.google.gwt.eclipse.core.launch;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;

/**
 * Listens for launches and terminations of speed tracer.
 * 
 */
public class SpeedTracerLaunchListener implements ILaunchesListener2 {

  public static final String SPEED_TRACER_LAUNCH_TAG = "com.google.gwt.eclipse.core.launch.speedTracerLaunchTag";

  public static final SpeedTracerLaunchListener INSTANCE = new SpeedTracerLaunchListener();
  
  private final Object speedTracerRunningLock = new Object();
  private boolean speedTracerRunning = false;

  private SpeedTracerLaunchListener() {
    // singleton
  }

  /**
   * @return true if there is a speed tracer launch already running
   */
  public boolean isSpeedTracerRunning() {
    // synchronized because this method is called from the UI in
    // SpeedTracerToolbarAction
    synchronized (speedTracerRunningLock) {
      return speedTracerRunning;
    }
  }

  public void launchesAdded(ILaunch[] launches) {
    setRunning(launches, true);
  }

  public void launchesChanged(ILaunch[] launches) {
    // ignored
  }

  public void launchesRemoved(ILaunch[] launches) {
    setRunning(launches, false);
  }

  public void launchesTerminated(ILaunch[] launches) {
    setRunning(launches, false);
  }

  public void setRunning(boolean running) {
    synchronized (speedTracerRunningLock) {
      this.speedTracerRunning = running;
    }
  }

  @SuppressWarnings("restriction")
  private boolean isSpeedTracerLaunch(ILaunch launch) {
    try {
      ILaunchConfiguration config = launch.getLaunchConfiguration();
      if (config != null) {
        String launchId = config.getType().getIdentifier();
        // Sometimes a launch config is launched for the purposes of using Speed
        // Tracer, but isn't in fact a Speed Tracer launch config. These launches
        // should have the attribute SPEED_TRACER_LAUNCH_TAG set to "true"
        if (launchId.equals(SpeedTracerLaunchConfiguration.TYPE_ID)
            || "true".equals(launch.getAttribute(SPEED_TRACER_LAUNCH_TAG))) {
          return true;
        }
      }
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }
    return false;
  }

  private void setRunning(ILaunch[] launches, boolean running) {
    for (ILaunch launch : launches) {
      if (isSpeedTracerLaunch(launch)) {

        synchronized (speedTracerRunningLock) {
          speedTracerRunning = running;
        }
        break;
      }
    }
  }

}
