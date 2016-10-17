/**
 *
 */
package com.google.gdt.eclipse.swtbot;

import com.google.gdt.eclipse.swtbot.conditions.ConsoleViewContains;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * @author branflake2267
 *
 */
public class SwtBotProjectDebug {

  // TODO change to Messages.get()
  private static final String MENU_GWT_SUPERDEVMODE = "GWT Development Mode with Jetty";

  private static String number;

  public static void closeSaveChangesDialogIfNeedBe(final SWTWorkbenchBot bot) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        try {
          boolean visible = bot.shell("Save Changes").isVisible();
          if (visible) {
            bot.button("Yes").click();
          }
        } catch (Exception e) {
          return;
        }
      }
    });
  }

  public static String getTheProgramArgsTextBox(SWTWorkbenchBot bot) {
    SwtBotUtils.print("Retrieve Args");

    // When I open debug configuration
    SwtBotMenuActions.openDebugConfiguration(bot);

    // Focus on the Arguments Tab
    bot.cTabItem("Arguments").activate().setFocus();

    // Get the program arguments
    SWTBotText programArgs = bot.textInGroup("Program arguments:");
    String text = programArgs.getText();

    // And close the debug configuration dialog
    bot.button("Close").click();

    // And closing may cause a save change dialog
    SwtBotProjectDebug.closeSaveChangesDialogIfNeedBe(bot);

    SwtBotUtils.print("Retrieved Args");

    return text;
  }

  /**
   * Goto Debug As > "# MENU_GWT_SUPERDEVMODE"
   */
  public static void launchDevModeWithJettyAndWaitForReady(SWTWorkbenchBot bot,
      String projectName) {
    SwtBotUtils.print("Launch DevMode with Jetty");

    // show it has focus
    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, projectName);
    project.setFocus();
    project.select();
    project.doubleClick();

    // Since the menus have dynamic numbers in them
    // and with out a built in iteration, this is what I came up with
    for (int i = 1; i < 15; i++) {
      bot.sleep(500);
      if (i < 10) {
        number = i + " ";
      } else {
        number = "";
      }

      final String menuLabel = number + MENU_GWT_SUPERDEVMODE;

      SwtBotUtils.print("Trying to select: Run > Debug As > menuLabel=" + menuLabel);

      try {
        bot.menu("Run").menu("Debug As").menu(menuLabel).click();
        break;
      } catch (Exception e) {
        SwtBotUtils.print("Skipping menu item " + menuLabel);
      }
    }

    // Wait for a successful launch
    ConsoleViewContains.waitForConsoleOutput(bot, "The code server is ready", 30000);

    SwtBotUtils.print("Launched DevMode with Jetty");
  }

  /**
   * Right click on project and goto Debug As > 4 Run GWT Super Dev Mode Then Stop the debugging
   * process
   */
  public static void launchGWTDevModeWithJettyThenTerminateIt(SWTWorkbenchBot bot,
      String projectName) {
    launchDevModeWithJettyAndWaitForReady(bot, projectName);

    // And then stop it
    SwtBotLaunchManagerActions.terminateAllLaunchConfigs(bot);

    // And back to the java perspective
    SwtBotMenuActions.openJavaPerpsective(bot);
  }

}
