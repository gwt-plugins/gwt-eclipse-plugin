package com.google.gdt.eclipse.swtbot;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

public abstract class AbstractedWaitCondition implements ICondition {

  protected SWTBot bot;

  protected AbstractedWaitCondition(SWTBot bot) {
    this.bot = bot;
  }

  @Override
  public void init(SWTBot bot) {}

  public void waitForTest() {
    bot.waitUntil(this);
  }

  public void waitForTest(long timeout) {
    bot.waitUntil(this, timeout);
  }

}
