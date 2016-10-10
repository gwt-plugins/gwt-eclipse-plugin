/**
 *
 */
package com.google.gdt.eclipse.swtbot;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class SwtBotTreeActions {

  /**
   * Given a tree that contains an entry with <code>itemName</code> and a direct child with a name
   * matching <code>subchildName</code>, return its tree item.
   *
   * This method is useful when there is the possibility of a tree having two similarly-named
   * top-level nodes.
   *
   * @param mainTree the tree
   * @param itemName the name of a top-level node in the tree
   * @param subchildName the name of a direct child of the top-level node (used to uniquely select
   *        the appropriate tree item for the given top-level node name)
   * @return the tree item corresponding to the top-level node with <code>itemName</code>that has a
   *         direct child with <code>subchildName</code>. If there are multiple tree items that
   *         satisfy this criteria, then the first one (in the UI) will be returned
   *
   * @throws IllegalStateException if no such node can be found
   */
  public static SWTBotTreeItem getUniqueTreeItem(final SWTBot bot, final SWTBotTree mainTree,
      String itemName, String subchildName) {
    for (SWTBotTreeItem item : mainTree.getAllItems()) {
      if (itemName.equals(item.getText())) {
        try {
          item.expand();
          SwtBotUtils.waitUntilTreeHasText(bot, item);
          if (item.getNode(subchildName) != null) {
            return item;
          }
        } catch (WidgetNotFoundException e) {
          // Ignore
        }
      }
    }

    throw new IllegalStateException("The '" + itemName + "' node with a child of '" + subchildName
        + "' must exist in the tree.");
  }

  /**
   * Given a tree that contains an entry with one of <code>itemNames</code> and a direct child with
   * a name matching <code>subchildName</code>, return its tree item.
   *
   * This method is useful when the top-level names are ambiguous and/or variable.
   *
   * @param mainTree the tree
   * @param itemNames possible names of a top-level node in the tree
   * @param subchildName the name of a direct child of the top-level node (used to uniquely select
   *        the appropriate tree item for the given top-level node name)
   * @return the tree item corresponding to the top-level node with one of <code>itemNames</code>
   *         that has a direct child with <code>subchildName</code>. If there are multiple tree
   *         items that satisfy this criteria, then the first one (in the UI) will be returned
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

    throw new IllegalStateException("One of the '" + itemNames + "' nodes with a child of '"
        + subchildName + "' must exist in the tree.");
  }

}
