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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A message console with additional features, such as displaying a terminate
 * button.
 */
public class CustomMessageConsole extends MessageConsole {

  /**
   * Page participant for {@link CustomMessageConsole} that gives the console
   * references to some of its active UI elements.
   */
  public static class ConsolePageParticipant implements IConsolePageParticipant {
    public void activated() {
    }

    public void deactivated() {
    }

    public void dispose() {
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
      return adapter.equals(ConsolePageParticipant.class) ? this : null;
    }

    public void init(IPageBookViewPage page, IConsole console) {
      if (console instanceof CustomMessageConsole) {
        ((CustomMessageConsole) console).setActiveToolbarManager(page.getSite().getActionBars().getToolBarManager());
      }
    }
  }

  /**
   * Synchronize on the {@link CustomMessageConsole} instance.
   */
  private TerminateAction terminateAction;

  /**
   * Synchronize on the {@link CustomMessageConsole} instance.
   */
  private IToolBarManager activeToolBarManager;

  public CustomMessageConsole(String name, ImageDescriptor imageDescriptor) {
    super(name, imageDescriptor);
  }

  /**
   * Sets the terminate action which affects the visibility of the terminate
   * button.
   * 
   * @param terminateAction the action to invoke when the terminate button is
   *          clicked, or null to remove the terminate button
   */
  public synchronized void setTerminateAction(TerminateAction terminateAction) {
    TerminateAction oldTerminateAction = this.terminateAction;
    this.terminateAction = terminateAction;

    updateToolbarAction(oldTerminateAction, terminateAction,
        IConsoleConstants.LAUNCH_GROUP);
  }

  private synchronized void setActiveToolbarManager(
      IToolBarManager toolBarManager) {
    this.activeToolBarManager = toolBarManager;

    // Ensure the terminate action exists on this toolbar
    setTerminateAction(terminateAction);
  }

  private synchronized void updateToolbarAction(IAction oldAction,
      IAction newAction, String groupName) {
    if (activeToolBarManager != null) {
      if (oldAction != null) {
        // Remove any existing action
        activeToolBarManager.remove(oldAction.getId());
      }

      if (newAction != null) {
        activeToolBarManager.appendToGroup(groupName, newAction);
      }

      /*
       * We can update the toolbar's model above from any thread, but the update
       * call must be done from the UI thread. (asyncExec since the UI thread
       * may be waiting for the lock in setActiveToolbarManager.)
       */
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          activeToolBarManager.update(false);
        }
      });
    }
  }

}
