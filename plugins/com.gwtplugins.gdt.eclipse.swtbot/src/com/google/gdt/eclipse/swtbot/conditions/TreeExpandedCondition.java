package com.google.gdt.eclipse.swtbot.conditions;

import com.google.gdt.eclipse.swtbot.SwtBotTreeActions;

import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;

public class TreeExpandedCondition extends DefaultCondition {

  private final TreeItem tree;

  public TreeExpandedCondition(TreeItem tree) {
    this.tree = tree;
  }

  @Override
  public String getFailureMessage() {
    return "Could not expand the tree of " + tree.getText();
  }

  @Override
  public boolean test() throws Exception {
    return SwtBotTreeActions.isTreeExpanded(tree);
  }

}
