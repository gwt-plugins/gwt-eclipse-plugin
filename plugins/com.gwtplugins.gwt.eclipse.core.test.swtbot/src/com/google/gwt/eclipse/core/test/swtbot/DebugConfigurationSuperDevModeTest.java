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
package com.google.gwt.eclipse.core.test.swtbot;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import com.google.gdt.eclipse.swtbot.SwtBotMenuActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotUtils;
import com.google.gdt.eclipse.swtbot.conditions.ConsoleContains;
import com.google.gwt.eclipse.core.test.swtbot.test.AbstractGWTPluginSwtBotTestCase;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Test GWT super dev mode debug configurations using a standard Java project.
 *
 * Note: If stopping the process before it's cleaned up, a port may stay open b/c of an orphaned
 * Java process. netstat -an | egrep 'Proto|LISTEN' - list the ports
 */
public class DebugConfigurationSuperDevModeTest extends AbstractGWTPluginSwtBotTestCase {

  protected static final String MENU_GWT_LEGACY = "GWT Legacy Development Mode with Jetty";
  protected static final String MENU_GWT_SUPERDEVMODE = "GWT Development Mode with Jetty";
  protected static final String PROJECT_NAME = "Project";

  private String number;

  /**
   * Test the default, running with super dev mode
   */
  public void testShortcutUsingDefaults() {
    // When I right click and Debug GWT Super Dev Mode
    whenIRightClickandRunDebugConfigurationAndStopDebuggingIt();

    // When I get the arguments for super dev mode config
    String persistedArgs = whenIGetTheProgramArgsTextBox().getText();

    // And close the debug configuration dialog
    getSwtWorkbenchBot().button("Close").click();
    // And closing may cause a save change dialog
    closeSaveChangesDialogIfNeedBe();

    // Then the args should be
    assertTrue(persistedArgs.contains("com.example.project.Project"));
  }

  /**
   * can use for maven swtbot tests override
   */
  protected void givenProjectIsCreated() {
    givenProjectIsCreated(PROJECT_NAME);
  }

  /**
   * Can use to for maven swtbot tests override
   */
  protected String maybeConvertSrcToMaven(String src) {
    return src;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    givenProjectIsCreated();
  }

  @Override
  protected void tearDown() throws Exception {
    thenTearDownProject(PROJECT_NAME);

    getSwtWorkbenchBot().resetWorkbench();

    super.tearDown();
  }

  protected void closeSaveChangesDialogIfNeedBe() {
    SwtBotUtils.performAndWaitForWindowChange(getSwtWorkbenchBot(), new Runnable() {
      @Override
      public void run() {
        try {
          boolean visible = getSwtWorkbenchBot().shell("Save Changes").isVisible();
          if (visible) {
            getSwtWorkbenchBot().button("Yes").click();
          }
        } catch (Exception e) {
          return;
        }
      }
    });
  }

  protected SWTBotText whenIGetTheProgramArgsTextBox() {
    SWTWorkbenchBot bot = getSwtWorkbenchBot();

    // When I open debug configuration
    SwtBotMenuActions.openDebugConfiguration(bot);

    bot.activeShell().setFocus();

    // Focus on the Arguments Tab
    bot.cTabItem("Arguments").activate().setFocus();

    // Get the program arguments
    SWTBotText programArgs = bot.textInGroup("Program arguments:");

    return programArgs;
  }

  /**
   * Goto Debug As > 4 Run GWT Super Dev Mode
   *
   * Note: terminate this manually!
   */
  protected void whenIClickandRunDebugConfiguration() {
    final SWTWorkbenchBot bot = getSwtWorkbenchBot();

    // show it has focus
    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    project.setFocus();
    project.select();
    project.doubleClick();

    // Since the menus have dynamic numbers in them
    // and with out a built in iteration, this is what I came up with
    for (int i = 1; i < 15; i++) {
      bot.sleep(500);
      if (i < 10) {
        number = i + " ";
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
    SwtBotUtils.print("Waiting for the code server to be ready.");
    ConsoleContains.waitForConsoleOutput(bot, "The code server is ready", 30000);
    SwtBotUtils.print("It was a successful launch.");
  }

  /**
   * Returns the project root tree in Package Explorer.
   */
  public static SWTBotTree getProjectRootTree(SWTWorkbenchBot bot) {
    SWTBotView explorer = getPackageExplorer(bot);

    if (explorer == null) {
      throw new WidgetNotFoundException("Cannot find Package Explorer or Project Explorer");
    }

    Tree tree = bot.widget(widgetOfType(Tree.class), explorer.getWidget());
    return new SWTBotTree(tree);
  }


  /*
   * Choose either the Package Explorer View or the Project Explorer view. Eclipse 3.3 and 3.4 start
   * with the Java Perspective, which has the Package Explorer View open by default, whereas Eclipse
   * 3.5 starts with the Resource Perspective, which has the Project Explorer View open.
   */
  public static SWTBotView getPackageExplorer(final SWTWorkbenchBot bot) {
    SWTBotView explorer = null;
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer")
          || view.getTitle().equals("Project Explorer")) {
        explorer = view;
        break;
      }
    }
    return explorer;
  }

  /**
   * Right click on project and goto Debug As > 4 Run GWT Super Dev Mode Then Stop the debugging
   * process
   */
  protected void whenIRightClickandRunDebugConfigurationAndStopDebuggingIt() {
    final SWTWorkbenchBot bot = getSwtWorkbenchBot();

    whenIClickandRunDebugConfiguration();

    // And then stop it
    SwtBotUtils.print("Opening Debug Perspective and Terminate Process");
    SwtBotMenuActions.openDebugPerspectiveAndTerminateProcess(bot);

    bot.sleep(1000);

    // And back to the java perspective
    SwtBotUtils.print("Opening Java Perspective");
    SwtBotMenuActions.openJavaPerpsective(bot);
    SwtBotUtils.print("Opened Java Perspective");

    bot.sleep(500);
  }

}
