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
package com.google.gdt.eclipse.core.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Display;

/*
 * This is pretty straight-forward except the case where the user enters data,
 * and switches tabs before the IDLE_DELAY delay. In this special case, we only
 * call performApply() instead of calling the full
 * updateLaunchConfigurationDialog() (which would in turn call performApply()).
 * See deactivateCalled() for more information.
 */
/**
 * Batches calls to updateLaunchConfigurationDialog(). This provides for a
 * responsive UI as the user types in the launch config dialog's text boxes,
 * with the drawback of validation not appearing instantaneously (the delay is
 * defined in a constant below.)
 * 
 * Clients should:
 * <ul>
 * <li>call {@link #updateLaunchConfigurationDialogCalled()} from the tab's
 * updateLaunchConfigurationDialog() (and no call to
 * super.updateLaunchConfigurationDialog()).
 * <li>call {@link #deactivatedCalled(ILaunchConfigurationWorkingCopy)} from the
 * tab's deactivated() (and still call through to super.deactivated()
 * afterwards).
 * <li>implement the methods in {@link Listener}.
 * </ul>
 * 
 * All calls in and out of this should happen on the UI thread, and hence no
 * synchronization is required.
 */
public class UpdateLaunchConfigurationDialogBatcher {

  /**
   * Listens for callbacks from {@link UpdateLaunchConfigurationDialogBatcher}.
   */
  public interface Listener {
    /**
     * Calls through to super's updateLaunchConfigurationDialog().
     */
    void callSuperUpdateLaunchConfigurationDialog();

    /**
     * Performs the logic in performApply() without any active tab condition
     * checking.
     */
    void doPerformApply(ILaunchConfigurationWorkingCopy configuration);
  }

  /**
   * Amount of time the user must be idle before calling the super's
   * updateLaunchConfigurationDialog().
   */
  private static final int IDLE_DELAY_MS = 300;

  private final Listener listener;

  /**
   * Calls super's updateLaunchConfigurationDialog() in the context of the tab.
   */
  private final Runnable callListenerRunnable = new Runnable() {
    public void run() {
      listener.callSuperUpdateLaunchConfigurationDialog();
    }
  };

  public UpdateLaunchConfigurationDialogBatcher(Listener superCaller) {
    this.listener = superCaller;
  }

  public void deactivatedCalled(ILaunchConfigurationWorkingCopy config) {
    /*
     * Remove the callback to super's updateLaunchConfigurationDialog() since
     * that would end up calling the newly-selected tab's performApply() instead
     * of the previously-selected tab's performApply(). If the newly-selected
     * tab's performApply() were called, the new tab's potentially stale data
     * would be written to the working copy, overwriting the user's
     * last-subsecond changes in the other tab. Instead of getting into this
     * situation, we remove the call to super's
     * updateLaunchConfigurationDialog() and do a performApply() on the
     * previously-selected tab.
     */
    // Removes existing timers for this runnable
    Display.getCurrent().timerExec(-1, callListenerRunnable);

    /*
     * We always call the old tab's performApply on deactivated because E36 has
     * its own keypress batching logic, and there's a chance that a user's typed
     * data would be lost if he switches to a tab that shows the same data
     * source. (The old tab's updateLaunchConfigurationDialog()/performApply()
     * was synchronous whereas now it is async w/ delay, so when the
     * user switches to the new tab, the old data will be read from the
     * persistent store.)
     */
    listener.doPerformApply(config);
  }

  public void disposeCalled() {
    // Clear any existing timers
    Display.getCurrent().timerExec(-1, callListenerRunnable);
  }

  public void updateLaunchConfigurationDialogCalled() {
    // Existing timers for this runnable will be deleted
    Display.getCurrent().timerExec(IDLE_DELAY_MS, callListenerRunnable);
  }

}
