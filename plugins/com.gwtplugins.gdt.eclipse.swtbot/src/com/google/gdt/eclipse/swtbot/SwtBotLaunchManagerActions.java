/**
 *
 */
package com.google.gdt.eclipse.swtbot;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swtbot.swt.finder.SWTBot;

/**
 * Launch Manager Actions
 */
public class SwtBotLaunchManagerActions {

  protected SwtBotLaunchManagerActions() {
  }

  public static void terminateAllLaunchConfigs(SWTBot bot) {
    SwtBotUtils.print("Terminating Launches");

    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunch[] launches = manager.getLaunches();
    if (launches == null || launches.length == 0) {
      SwtBotUtils.print("No Launches to terminate");
    }

    for (ILaunch launch : launches) {
      if (!launch.isTerminated()) {
        try {
          launch.terminate();
        } catch (DebugException e) {
          SwtBotUtils.printError("Could not terminate launch." + e.getMessage());
        }
      }
    }

    SwtBotWorkbenchActions.waitForIdle(bot);

    SwtBotUtils.print("Terminated Launches");
  }

}
