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
package com.google.gdt.eclipse.suite.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * This class is here basically just to enable the GDT toolbar
 * pulldown button.
 *
 */
public class PulldownHandler implements IHandler {

  public void addHandlerListener(IHandlerListener handlerListener) {
  }

  public void dispose() {
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    // open the button's dropdown menu
    Object trigger = event.getTrigger();
    if (trigger instanceof Event) {
      Widget widget = ((Event) trigger).widget;
      if (widget instanceof ToolItem) {
        ToolItem toolItem = (ToolItem) widget;
        Listener[] listeners = toolItem.getListeners(SWT.Selection);
        if (listeners.length > 0) {
          Listener listener = listeners[0]; // should be only one listener
          // send an event to the widget to open the menu
          // see CommandContributionItem.openDropDownMenu(Event)
          Event e = new Event();
          e.type = SWT.Selection;
          e.widget = widget;
          e.detail = 4; // dropdown detail
          e.y = toolItem.getBounds().height; // position menu
          listener.handleEvent(e);
        }
      }
    }
    return null;
  }

  public boolean isEnabled() {
    return true;
  }

  public boolean isHandled() {
    return true;
  }

  public void removeHandlerListener(IHandlerListener handlerListener) {
  }

}
