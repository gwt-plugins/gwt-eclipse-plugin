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
package com.google.gdt.eclipse.core.console;

import com.google.gdt.eclipse.core.ProcessUtilities.IProcessReceiver;
import com.google.gdt.eclipse.core.jobs.ProgressMonitorCanceledWatcher.Listener;

/**
 * Controller for the terminate button that destroys a process. It will start
 * disabled, and will automatically enable itself when it receives a process.
 */
public class TerminateProcessAction extends TerminateAction implements
    IProcessReceiver, Listener {

  /**
   * Synchronize on the {@link TerminateProcessAction}'s instance.
   */
  private Process process;

  public TerminateProcessAction() {
    setEnabled(false);
  }

  public boolean hasDestroyedProcess() {
    return hasTerminated();
  }

  public void progressMonitorCanceled() {
    terminate();
  }

  public void setProcess(Process process) {
    synchronized (this) {
      this.process = process;
    }

    setEnabled(true);
  }

  @Override
  protected void terminate() {
    super.terminate();

    if (process != null) {
      process.destroy();
    }
  }
}
