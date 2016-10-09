package com.google.gdt.eclipse.swtbot.conditions;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;

public class ConsoleContains implements ICondition {

  public static ICondition consoleContains(String string) {
    return new ConsoleContains(string);
  }

  public static void waitForConsoleOutput(SWTWorkbenchBot bot, String string, long timeout) {
    bot.waitUntil(consoleContains(string), timeout);
  }

  public static void waitForConsoleOutput(SWTWorkbenchBot bot, String string) {
    bot.waitUntil(consoleContains(string));
  }

  private SWTWorkbenchBot bot;
  private String searchString;
  private String msg;

  public ConsoleContains(String string) {
    this.searchString = string;
  }

  @Override
  public boolean test() throws Exception {
    msg = "Could not open Console view";
    SWTBotView console = bot.viewByTitle("Console");
    msg = "Could not find textWidget in Console view";
    SWTBotStyledText textWidget = console.bot().styledText();
    msg = "Could not get the text from the Console view";
    String text = textWidget.getText();
    msg = "Looking for: '" + searchString + "' but found \n------\n" + text + "\n-----";
    return text.contains(searchString);
  }

  @Override
  public void init(SWTBot bot) {
    this.bot = (SWTWorkbenchBot) bot;
  }

  @Override
  public String getFailureMessage() {
    return msg;
  }

}
