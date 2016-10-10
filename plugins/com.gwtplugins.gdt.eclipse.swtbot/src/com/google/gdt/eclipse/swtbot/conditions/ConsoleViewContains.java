package com.google.gdt.eclipse.swtbot.conditions;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;

public class ConsoleViewContains implements ICondition {

  public static ICondition consoleContains(String string) {
    return new ConsoleViewContains(string);
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

  protected ConsoleViewContains(String string) {
    this.searchString = string;
  }

  @Override
  public boolean test() throws Exception {
    msg = "Could not open Console view";
    SWTBotView consoleView = bot.viewById("org.eclipse.ui.console.ConsoleView");
    msg = "Could not find textWidget in Console view";
    SWTBotStyledText textWidget = consoleView.bot().styledText();
    msg = "Could not get the text from the Console view";
    String text = textWidget.getText();
    msg = "Looking for: '" + searchString + "' but found \n\t------\n\t" + text + "\n\t-----";

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
