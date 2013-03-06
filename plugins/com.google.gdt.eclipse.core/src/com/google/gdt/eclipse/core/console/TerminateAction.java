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

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.resources.CoreImages;

import org.eclipse.jface.action.Action;

/**
 * Action for the terminate button.
 */
public abstract class TerminateAction extends Action {

  private boolean hasTerminated = false;

  public TerminateAction() {
    super("Terminate", CorePlugin.getDefault().getImageDescriptor(
        CoreImages.TERMINATE_ICON));

    setEnabled(true);
  }

  @Override
  public String getId() {
    return TerminateAction.class.getName();
  }

  public boolean hasTerminated() {
    return hasTerminated;
  }

  @Override
  public void run() {
    terminate();
  }

  /**
   * Terminate and set the action to disabled.
   */
  protected void terminate() {
    hasTerminated = true;

    /*
     * Assume the termination will succeed. This is similar to how Eclipse
     * handles a click on the job's terminate button (it disables the button
     * right away).
     */
    setEnabled(false);
  }
}
