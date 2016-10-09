/**
 *
 */
package com.google.gdt.eclipse.swtbot.conditions;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;

public class ActiveWidgetCondition implements ICondition {

  public static ICondition widgetMakeActive(final AbstractSWTBot<? extends Widget> widget) {
    return new ActiveWidgetCondition(widget);
  }

  private AbstractSWTBot<? extends Widget> widget;

  protected ActiveWidgetCondition(AbstractSWTBot<? extends Widget> widget) {
    this.widget = widget;
  }

  @Override
  public boolean test() throws Exception {
    widget.setFocus();
    return widget.isActive();
  }

  @Override
  public void init(SWTBot bot) {}

  @Override
  public String getFailureMessage() {
    return "Widget not active: " + widget;
  }

}
