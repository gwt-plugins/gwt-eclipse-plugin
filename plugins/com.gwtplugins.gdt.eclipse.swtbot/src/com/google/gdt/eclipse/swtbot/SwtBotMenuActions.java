/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.gdt.eclipse.swtbot.conditions.ActiveShellMenu;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotPerspective;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;

import java.util.List;

/**
 * SWTBot utility methods that perform general file actions.
 */
public final class SwtBotMenuActions {

  public static String getEditorText(final SWTWorkbenchBot bot, String title) {
    SWTBotEclipseEditor te = bot.editorByTitle(title).toTextEditor();
    return te.getText();
  }

  /**
   * Opens the Debug Configurations... perspective.
   */
  public static void openDebugConfiguration(final SWTBot bot) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        // Be sure the main window has focus
        setFocusMainShell(bot);

        bot.menu("Run").menu("Debug Configurations...").click();
      }
    });
  }

  public static void openDebugPerspective(SWTWorkbenchBot bot) {
    openPerspective(bot, "Debug");
  }

  public static void openJavaPerpsective(final SWTWorkbenchBot bot) {
    openPerspective(bot, "Java");
  }

  public static void openNewOtherProjectDialog(final SWTWorkbenchBot bot) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("File").menu("New").menu("Other...").click();
      }
    });
  }

  /**
   * Open debug perspective and terminate the process and wait for it be terminated.
   */
  public static void openDebugPerspectiveAndTerminateProcess(SWTWorkbenchBot bot) {
    openPerspective(bot, "Debug");

    // Right click and terminate thread
    @SuppressWarnings("rawtypes")
    Matcher matcher = WidgetMatcherFactory.withPartName("Debug");
    @SuppressWarnings("unchecked")
    SWTBotView debug = bot.view(matcher);
    final SWTBotTree tree = debug.bot().tree();
    SWTBotTreeItem[] items = tree.getAllItems();
    if (items.length > 0) {
      SWTBotTreeItem first = items[0];
      first.contextMenu("Terminate").click();

      // Wait for process to spin down
      bot.waitUntil(new DefaultCondition() {
        @Override
        public String getFailureMessage() {
          return "Couldn't find terminated process.";
        }

        @Override
        public boolean test() throws Exception {
          SWTBotTreeItem[] items = tree.getAllItems();
          return items[0].getText().contains("terminated");
        }
      });
    }
  }

  public static void openNewMavenProject(SWTWorkbenchBot bot) {
    openNewOtherProjectDialog(bot);

    // filter maven options
    bot.text().setText("maven");
    bot.sleep(500);

    // click on Maven Project
    SWTBotTree tree = bot.tree();
    SWTBotTreeItem[] items = tree.getAllItems();
    SwtBotUtils.selectTreeItem(bot, items[0], "Maven Project");

    // move to next step
    bot.button("Next >").click();
  }

  /**
   * Opens a resource using the Open Resource dialog.
   */
  public static void openResource(final SWTBot bot, String fileName) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("Navigate").menu("Open Resource").click();
      }
    });

    bot.text().typeText(fileName);
    SwtBotUtils.clickButtonAndWaitForWindowChange(bot, bot.button("Open"));
  }

  public static void openViewSelections(final SWTWorkbenchBot bot) {
    SwtBotUtils.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.menu("Window").menu("Show View").menu("Other...").click();
      }
    });
  }

  /**
   * Set focus on the main shell window.
   */
  public static void setFocusMainShell(SWTBot bot) {
    SWTBotShell shell = bot.shells()[0];
    shell.setFocus();
  }

  /**
   * Show the console view. Be sure that perspective has the option first.
   */
  public static void showConsole(SWTWorkbenchBot bot) {
    bot.menu("Window").menu("Show View").menu("Console");
  }

  public static void openPerspective(SWTWorkbenchBot bot, String perspectiveLabel) {
    SWTBotShell shell = null;
    try {
      menu(bot, "Window").menu("Open Perspective").menu("Other...").click();

      shell = bot.shell("Open Perspective");

      bot.waitUntil(widgetMakeActive(shell));
      shell.bot().table().select(perspectiveLabel);

      shell.bot().button("OK").click();
      bot.waitUntil(Conditions.shellCloses(shell));
    } catch (Exception e) {
      if (shell != null && shell.isOpen()) shell.close();
      System.err.println("Couldn't open perspective '" + perspectiveLabel + "'\n"
          + "trying to activate already open perspective instead");
      // maybe somehow the perspective is already opened (by another test before us)
      SWTBotPerspective perspective = bot.perspectiveByLabel(perspectiveLabel);
      perspective.activate();
    }
  }

  public static SWTBotMenu menu(SWTWorkbenchBot bot, String name) {
    return new SWTBotMenu(waitForShellMenuList(bot, name, true).get(0));
  }

  public static List<MenuItem> waitForShellMenuList(SWTBot bot, String name, boolean recursive) {
    return new ActiveShellMenu(bot, name, recursive).getMenus();
  }

  public static ICondition widgetMakeActive(final AbstractSWTBot<? extends Widget> widget) {
    return new ICondition() {
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
    };
  }

  private SwtBotMenuActions() {
  }

}
