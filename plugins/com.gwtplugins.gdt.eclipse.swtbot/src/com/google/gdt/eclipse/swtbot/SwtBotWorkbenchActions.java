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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.hamcrest.Matcher;

import java.util.List;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotWorkbenchActions {

  private static final int OPEN_PREFERENCES_DIALOG_DELAY_MS = 1000;



  /**
   * Opens the preferences dialog from the main Eclipse window.
   * <p>
   * Note: There are some platform-specific intricacies that this abstracts
   * away.
   */
  public static void openPreferencesDialog(final SWTWorkbenchBot bot) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        if (SwtBotUtils.isMac()) {
          // TODO: Mac has "Preferences..." under the "Eclipse" menu item.
          // However,
          // the "Eclipse" menu item is a system menu item (like the Apple menu
          // item), and can't be reached via SWTBot.
          openPreferencesDialogViaEvents(bot);
        } else {
          SWTBotMenu windowMenu = bot.menu("Window");
          windowMenu.menu("Preferences").click();
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

  /**
   * Wait for the main shell progress bar to get removed.
   */
  public static void waitForMainShellProgressBarToFinish(final SWTWorkbenchBot bot) {
    // wait for progress bar
    bot.waitUntil(new ICondition() {
      @Override
      public boolean test() throws Exception {
        // First lower the amount of timeout, otherwise waiting for widget not to be found exception
        // is a long time
        SwtBotTimeoutManager.setTimeout(3000);
        try {
          // Find the progress bar in the main shell and wait for it to be removed
          @SuppressWarnings("unchecked")
          Matcher<ProgressBar> matcher =
              org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory
                  .allOf(WidgetMatcherFactory.widgetOfType(ProgressBar.class));
          List<? extends ProgressBar> bars = bot.widgets(matcher);
          if (bars == null || bars.isEmpty()) {
            // Restore the original timeout
            SwtBotTimeoutManager.setTimeout();
            return true;
          }
        } catch (Exception e) {
          // Restore the original timeout
          SwtBotTimeoutManager.setTimeout();
          return true;
        }

        // found the progress bar so keep polling for its removal
        return false;
      }

      @Override
      public void init(SWTBot bot) {}

      @Override
      public String getFailureMessage() {
        return "waitForMainShellProgressBarToFinish() error.";
      }
    });
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
    bot.sleep(SwtBotUtils.EVENT_DOWN_UP_DELAY_MS);
    ev.type = SWT.MouseUp;
    display.post(ev);

    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Right to the "Eclipse" menu item
    SwtBotUtils.sendKeyDownAndUp(bot, SWT.ARROW_RIGHT, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Down two to the "Preferences..." menu item
    SwtBotUtils.sendKeyDownAndUp(bot, SWT.ARROW_DOWN, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    SwtBotUtils.sendKeyDownAndUp(bot, SWT.ARROW_DOWN, '\0');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);

    // Press enter
    SwtBotUtils.sendKeyDownAndUp(bot, 0, '\r');
    bot.sleep(OPEN_PREFERENCES_DIALOG_DELAY_MS);
  }

  /**
   * Close dialogs that may show up.
   */
  public static void closeDialogs(SWTWorkbenchBot bot) {
    // Close dialogs that take focus from main shell
    if (bot.shells().length > 1) {
      System.out.println("Has windows/shells: shells quanity=" + bot.shells().length);
      SWTBotShell[] shells = bot.shells();
      for (int i = 0; i < shells.length; i++) {
        // ADT port warning dialog
        if (shells[i].getText().equals("ddms")) {
          shells[i].close();
        }

        if (shells[i].getText().equals("Subclipse Usage")) {
          shells[i].close();
        }

        if (shells[i].getText().equals("Preferences")) {
          shells[i].close();
        }
      }
    }
  }

  public static void closeWelcomePage(SWTWorkbenchBot bot) {
    try {
      bot.viewByTitle("Welcome").close();
    } catch (WidgetNotFoundException e) {
      // Ignore if Welcome view already closed
    }
  }


  private SwtBotWorkbenchActions() {
  }

}
