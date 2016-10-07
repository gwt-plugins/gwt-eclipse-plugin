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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;

/**
 * Provides helper methods to aid in SWTBot testing.
 */
public class SwtBotTestingUtilities {

  // TODO: Extract tree-specific functionality into a new class
  private static class TreeCollapsedCondition extends DefaultCondition {
    private final TreeItem tree;

    private TreeCollapsedCondition(TreeItem tree) {
      this.tree = tree;
    }

    public String getFailureMessage() {
      return "Could not collapse the tree of " + tree.getText();
    }

    public boolean test() throws Exception {
      return !isTreeExpanded(tree);
    }
  }

  private static class TreeExpandedCondition extends DefaultCondition {
    private final TreeItem tree;

    private TreeExpandedCondition(TreeItem tree) {
      this.tree = tree;
    }

    public String getFailureMessage() {
      return "Could not expand the tree of " + tree.getText();
    }

    public boolean test() throws Exception {
      return isTreeExpanded(tree);
    }
  }

  /**
   * The delay to use between a simulated key/button press's down and up events.
   */
  public static final int EVENT_DOWN_UP_DELAY_MS = 100;

  public static void clickButtonAndWaitForWindowChange(SWTBot bot, final SWTBotButton button) {
    performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        button.click();
      }
    });
  }

  public static void clickOnTableCellValue(SWTBotTable table, int col, String value) {
    String column = table.columns().get(col);
    for (int row = 0; row < table.rowCount(); row++) {
      String cellValue = table.cell(row, column);
      if (cellValue.equals(value)) {
        table.click(row, col);
        break;
      }
    }
  }

  /**
   * Create a java project with the specified project name. This function opens up the Java
   * Perspective.
   * 
   * @param bot The current SWTWorkbenchBot object
   * @param projectName Name of java project to be created
   */
  public static void createJavaProject(SWTWorkbenchBot bot, String projectName) {
    // Open Java Perspective
    bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();

    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Project...").click();

    // Select the Java project
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, "Java", "Java Project");
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionTreeItem, "Java Project");

    bot.button("Next >").click();

    // Configure the project and then create it
    bot.textWithLabel("Project name:").setText(projectName);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
  }

  /**
   * Gets the item matching the given name from a tree item.
   * 
   * @param widget the tree item to search
   * @param nodeText the text on the node.
   * @return the child tree item with the specified text.
   */
  public static TreeItem getTreeItem(final TreeItem widget, final String nodeText) {
    return UIThreadRunnable.syncExec(new WidgetResult<TreeItem>() {
      public TreeItem run() {
        TreeItem[] items = widget.getItems();
        for (TreeItem item : items) {
          if (item.getText().equals(nodeText)) {
            return item;
          }
        }
        return null;
      }
    });
  }

  /**
   * @return true if the operating system is Mac
   */
  public static boolean isMac() {
    String platform = SWT.getPlatform();
    return ("carbon".equals(platform) || "cocoa".equals(platform));
  }

  /**
   * Simple wrapper to block for actions that either open or close a window.
   */
  public static void performAndWaitForWindowChange(SWTBot bot, Runnable runnable) {
    SWTBotShell shell = bot.activeShell();
    runnable.run();
    waitUntilShellIsNotActive(bot, shell);
  }

  public static SWTBotTreeItem selectTreeItem(SWTBot bot, SWTBotTreeItem tree, String itemText) {
    waitUntilTreeItemHasItem(bot, tree, itemText);
    SWTBotTreeItem item = tree.select(itemText);
    return item;
  }

  /**
   * Injects a key or character via down and up events.
   * 
   * @param keyCode the keycode of the key (only this or character have to be provided.)
   * @param character the character to press (only this or keyCode have to be provided.)
   */
  public static void sendKeyDownAndUp(SWTBot bot, int keyCode, char character) {
    Event ev = new Event();
    ev.keyCode = keyCode;
    ev.character = character;
    ev.type = SWT.KeyDown;
    bot.getDisplay().post(ev);
    bot.sleep(EVENT_DOWN_UP_DELAY_MS);
    ev.type = SWT.KeyUp;
    bot.getDisplay().post(ev);
  }

  public static void setCheckBox(SWTBotCheckBox checkBox, boolean checked) {
    if (checked) {
      checkBox.select();
    } else {
      checkBox.deselect();
    }
  }

  /**
   * Performs the necessary set up work for most SWTBot tests.
   */
  public static void setUp(SWTWorkbenchBot bot) {
    // Increase the timeout (note: this is not a delay, so it's okay to increase
    // for reliability). This is required when we create a new project and wait
    // for it to show up in the package explorer.
    SwtBotTimeoutManager.setTimeout();

    try {
      bot.viewByTitle("Welcome").close();
    } catch (WidgetNotFoundException e) {
      // Ignore if Welcome view already closed
    }
  }

  /**
   * Performs the necessary tear down work for most SWTBot tests.
   */
  public static void tearDown() {
    // Set the SWTBot timeout back to the default value
    SwtBotTimeoutManager.resetTimeout();
  }

  /**
   * Blocks the caller until the given shell is no longer active.
   */
  public static void waitUntilShellIsNotActive(SWTBot bot, final SWTBotShell shell) {
    bot.waitUntil(new DefaultCondition() {
      public String getFailureMessage() {
        return "Shell " + shell.getText() + " did not close"; //$NON-NLS-1$
      }

      public boolean test() throws Exception {
        return !shell.isActive();
      }
    });
  }

  /**
   * Blocks the caller until all of the direct children of the tree have text. The assumption is
   * that the tree does not have any "empty" children.
   * 
   * TODO: Refactor some of this logic; it follows the same general pattern as
   * {@link #waitUntilTreeItemHasItem(SWTBot, SWTBotTreeItem, String)}.
   * 
   * @param tree the tree to search
   * @throws TimeoutException if all of the direct children of the tree do not have text within the
   *         timeout period
   */
  public static void waitUntilTreeHasText(SWTBot bot, final SWTBotTreeItem tree)
      throws TimeoutException {
    // Attempt #1
    if (!waitUntilTreeHasTextImpl(bot, tree.widget)) {
      // Attempt #2: Something went wrong, try to cautiously reopen it.
      bot.sleep(1000);

      // There isn't a method to collapse, so double-click instead
      tree.doubleClick();
      bot.waitUntil(new TreeCollapsedCondition(tree.widget));

      bot.sleep(1000);

      tree.expand();
      bot.waitUntil(new TreeExpandedCondition(tree.widget));

      if (!waitUntilTreeHasTextImpl(bot, tree.widget)) {
        printTree(tree.widget);
        throw new TimeoutException(
            "Timed out waiting for text of the tree's children, giving up...");
      }
    }
  }

  private static boolean doesTreeItemHaveText(final TreeItem widget) {
    return UIThreadRunnable.syncExec(new Result<Boolean>() {
      public Boolean run() {
        TreeItem[] items = widget.getItems();
        for (TreeItem item : items) {
          if (item.getText() == null || item.getText().length() == 0) {
            return false;
          }
        }
        return true;
      }
    });
  }

  /**
   * Helper method to check whether a given tree is expanded which can be called from any thread.
   */
  private static boolean isTreeExpanded(final TreeItem tree) {
    return UIThreadRunnable.syncExec(new Result<Boolean>() {
      public Boolean run() {
        return tree.getExpanded();
      }
    });
  }

  private static void printTree(final TreeItem tree) {
    UIThreadRunnable.syncExec(new VoidResult() {
      public void run() {
        System.err.println(String.format("%s has %d items:", tree.getText(), tree.getItemCount()));
        for (TreeItem item : tree.getItems()) {
          System.err.println(String.format("  - %s", item.getText()));
        }
      }
    });
  }

  private static boolean waitUntilTreeHasItemImpl(SWTBot bot, final TreeItem tree,
      final String nodeText) {
    try {
      bot.waitUntil(new DefaultCondition() {
        public String getFailureMessage() {
          return "Could not find node with text " + nodeText;
        }

        public boolean test() throws Exception {
          return getTreeItem(tree, nodeText) != null;
        }
      });
    } catch (TimeoutException e) {
      return false;
    }

    return true;
  }

  private static boolean waitUntilTreeHasTextImpl(SWTBot bot, final TreeItem tree) {
    try {
      bot.waitUntil(new DefaultCondition() {
        public String getFailureMessage() {
          return "Not all of the nodes in the tree have text.";
        }

        public boolean test() throws Exception {
          return doesTreeItemHaveText(tree);
        }
      });
    } catch (TimeoutException e) {
      return false;
    }

    return true;
  }

  /**
   * Blocks the caller until the tree item has the given item text.
   * 
   * @param tree the tree item to search
   * @param nodeText the item text to look for
   * @throws org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException if the item could not
   *         be found within the timeout period
   */
  private static void waitUntilTreeItemHasItem(SWTBot bot, final SWTBotTreeItem tree,
      final String nodeText) {

    // Attempt #1
    if (!waitUntilTreeHasItemImpl(bot, tree.widget, nodeText)) {
      // Attempt #2: Something went wrong, try to cautiously reopen it.
      bot.sleep(1000);

      // There isn't a method to collapse, so double-click instead
      tree.doubleClick();
      bot.waitUntil(new TreeCollapsedCondition(tree.widget));

      bot.sleep(1000);

      tree.expand();
      bot.waitUntil(new TreeExpandedCondition(tree.widget));

      if (!waitUntilTreeHasItemImpl(bot, tree.widget, nodeText)) {
        printTree(tree.widget);
        throw new TimeoutException(
            String.format("Timed out waiting for %s, giving up...", nodeText));
      }
    }
  }

}
