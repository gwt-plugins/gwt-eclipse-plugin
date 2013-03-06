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
package com.google.gdt.eclipse.swtbot;

import com.google.gdt.eclipse.core.EclipseUtilities;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotWorkbenchActions {

  private static final int OPEN_PREFERENCES_DIALOG_DELAY_MS = 1000;

  /**
   * Given a tree that contains an entry with <code>itemName</code> and a direct
   * child with a name matching <code>subchildName</code>, return its tree item.
   * 
   * This method is useful when there is the possibility of a tree having two
   * similarly-named top-level nodes.
   * 
   * @param mainTree the tree
   * @param itemName the name of a top-level node in the tree
   * @param subchildName the name of a direct child of the top-level node (used
   *        to uniquely select the appropriate tree item for the given top-level
   *        node name)
   * @return the tree item corresponding to the top-level node with
   *         <code>itemName</code>that has a direct child with
   *         <code>subchildName</code>. If there are multiple tree items that
   *         satisfy this criteria, then the first one (in the UI) will be
   *         returned
   * 
   * @throws IllegalStateException if no such node can be found
   */
  public static SWTBotTreeItem getUniqueTreeItem(final SWTBot bot,
      final SWTBotTree mainTree, String itemName, String subchildName) {
    for (SWTBotTreeItem item : mainTree.getAllItems()) {
      if (itemName.equals(item.getText())) {
        try {
          item.expand();
          SwtBotTestingUtilities.waitUntilTreeHasText(bot, item);
          if (item.getNode(subchildName) != null) {
            return item;
          }
        } catch (WidgetNotFoundException e) {
          // Ignore
        }
      }
    }

    throw new IllegalStateException("The " + itemName
        + " node with a child of " + subchildName + " must exist in the tree.");
  }

  /**
   * Given a tree that contains an entry with one of <code>itemNames</code> and
   * a direct child with a name matching <code>subchildName</code>, return its
   * tree item.
   * 
   * This method is useful when the top-level names are ambiguous and/or
   * variable.
   * 
   * @param mainTree the tree
   * @param itemNames possible names of a top-level node in the tree
   * @param subchildName the name of a direct child of the top-level node (used
   *        to uniquely select the appropriate tree item for the given top-level
   *        node name)
   * @return the tree item corresponding to the top-level node with one of
   *         <code>itemNames</code> that has a direct child with
   *         <code>subchildName</code>. If there are multiple tree items that
   *         satisfy this criteria, then the first one (in the UI) will be
   *         returned
   * 
   * @throws IllegalStateException if no such node can be found
   */
  public static SWTBotTreeItem getUniqueTreeItem(final SWTBot bot, final SWTBotTree mainTree,
      String[] itemNames, String subchildName) {

    for (String itemName : itemNames) {
      try {
        return getUniqueTreeItem(bot, mainTree, itemName, subchildName);
      } catch (IllegalStateException e) {
        // Ignore
      }
    }

    throw new IllegalStateException("One of the " + itemNames + " nodes with a child of "
        + subchildName + " must exist in the tree.");
  }

  /**
   * Opens the preferences dialog from the main Eclipse window.
   * <p>
   * Note: There are some platform-specific intricacies that this abstracts
   * away.
   */
  public static void openPreferencesDialog(final SWTWorkbenchBot bot) {
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        if (SwtBotTestingUtilities.isMac()) {
          // TODO: Mac has "Preferences..." under the "Eclipse" menu item.
          // However,
          // the "Eclipse" menu item is a system menu item (like the Apple menu
          // item), and can't be reached via SWTBot.
          openPreferencesDialogViaEvents(bot);
        } else {
          SWTBotMenu windowMenu = bot.menu("Window");
          String preferencesMenuLabel = (EclipseUtilities.inEclipse33()
              ? "Preferences..." : "Preferences");
          windowMenu.menu(preferencesMenuLabel).click();
        }
      }
    });
  }

  /**
   * Wait until all background tasks are complete.
   */
  public static void waitForIdle(SWTBot bot) {
    while (!Job.getJobManager().isIdle()) {
      bot.sleep(1000);
    }
  }

  private static void openPreferencesDialogViaEvents(SWTBot bot) {
    Display display = bot.getDisplay();
    Event ev = new Event();

    // Move to the "Apple" menu item (it catches 0, 0)
    ev.type = SWT.MouseMove;
    ev.x = 0;
    ev.y = 0;
    display.post(ev);

    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Click
    ev.type = SWT.MouseDown;
    ev.button = 1;
    display.post(ev);
    bot.sleep(SwtBotTestingUtilities.EVENT_DOWN_UP_DELAY_MS);
    ev.type = SWT.MouseUp;
    display.post(ev);

    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Right to the "Eclipse" menu item
    SwtBotTestingUtilities.sendKeyDownAndUp(bot, SWT.ARROW_RIGHT, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Down two to the "Preferences..." menu item
    SwtBotTestingUtilities.sendKeyDownAndUp(bot, SWT.ARROW_DOWN, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    SwtBotTestingUtilities.sendKeyDownAndUp(bot, SWT.ARROW_DOWN, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Press enter
    SwtBotTestingUtilities.sendKeyDownAndUp(bot, 0, '\r');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);
  }

  private SwtBotWorkbenchActions() {
  }
}
