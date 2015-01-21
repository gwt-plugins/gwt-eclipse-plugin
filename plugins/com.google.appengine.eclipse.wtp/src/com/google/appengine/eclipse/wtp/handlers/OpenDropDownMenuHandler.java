/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;

/**
 * A command handler for commands representing drop-down Menu contribution item. This handler just
 * opens drop-down menu, since Eclipse behavior for such items is to open drop-down menu only by
 * clicking on small arrow. Drop-down Menu contribution items without any handler shown as disabled.
 */
public final class OpenDropDownMenuHandler extends AbstractHandler {
  private final IMenuService menuService = (IMenuService) PlatformUI.getWorkbench().getService(
      IMenuService.class);

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Event swtEvent = (Event) event.getTrigger();
    String id = event.getCommand().getId();
    openDropDownMenu(swtEvent.widget, id);
    return null;
  }

  /**
   * Opens drop-down menu.
   */
  private void openDropDownMenu(Widget widget, final String id) {
    if (widget != null) {
      int style = widget.getStyle();
      if ((style & SWT.DROP_DOWN) != 0) {
        ToolItem toolItem = (ToolItem) widget;
        final MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(toolItem.getParent());
        menuManager.addMenuListener(new IMenuListener2() {
          @Override
          public void menuAboutToHide(IMenuManager manager) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
              @Override
              public void run() {
                menuService.releaseContributions(menuManager);
                menuManager.dispose();
              }
            });
          }

          @Override
          public void menuAboutToShow(IMenuManager manager) {
            menuService.populateContributionManager(menuManager, "menu:" + id);
          }
        });
        // place the menu below the drop-down item
        Rectangle itemBounds = toolItem.getBounds();
        Point point = toolItem.getParent().toDisplay(
            new Point(itemBounds.x, itemBounds.y + itemBounds.height));
        menu.setLocation(point.x, point.y);
        menu.setVisible(true);
      }
    }
  }
}
