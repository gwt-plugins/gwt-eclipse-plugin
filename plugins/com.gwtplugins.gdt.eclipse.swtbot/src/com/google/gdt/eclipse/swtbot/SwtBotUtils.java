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
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Provides helper methods to aid in SWTBot testing.
 */
public class SwtBotUtils {




  /**
   * The delay to use between a simulated key/button press's down and up events.
   */
  public static final int EVENT_DOWN_UP_DELAY_MS = 100;

  public static void clickButtonAndWaitForWindowChange(SWTBot bot, final SWTBotButton button) {
    performAndWaitForWindowChange(bot, new Runnable() {
      @Override
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
        SwtBotTreeActions.getUniqueTreeItem(bot, projectSelectionTree, "Java", "Java Project");
    SwtBotTreeActions.selectTreeItem(bot, projectSelectionTreeItem, "Java Project");

    bot.button("Next >").click();

    // Configure the project and then create it
    bot.textWithLabel("Project name:").setText(projectName);

    SwtBotUtils.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
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
    SwtBotUtils.print("SetUp");

    SwtBotWorkbenchActions.closeDialogs(bot);

    SwtBotWorkbenchActions.closeWelcomePage(bot);

    SwtBotUtils.print("SetUp Done");
  }

  /**
   * Performs the necessary tear down work for most SWTBot tests.
   */
  public static void tearDown(SWTWorkbenchBot bot) {
    SwtBotUtils.print("Tear Down");

    bot.resetWorkbench();

    SwtBotUtils.print("Tear Down Done");
  }

  /**
   * Blocks the caller until the given shell is no longer active.
   */
  public static void waitUntilShellIsNotActive(SWTBot bot, final SWTBotShell shell) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public String getFailureMessage() {
        return "Shell " + shell.getText() + " did not close"; //$NON-NLS-1$
      }

      @Override
      public boolean test() throws Exception {
        return !shell.isActive();
      }
    });
  }



  public static void print(String message) {
    System.out.println("SwtBot Message: " + message);
  }

  public static void printError(String message) {
    System.err.println("SwtBot Error: " + message);
  }
}
