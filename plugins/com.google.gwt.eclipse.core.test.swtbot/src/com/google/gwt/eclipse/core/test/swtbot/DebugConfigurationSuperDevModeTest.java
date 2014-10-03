/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core.test.swtbot;

import com.google.gdt.eclipse.swtbot.SwtBotMenuActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gwt.eclipse.core.test.swtbot.test.AbstractGWTPluginSwtBotTestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Test GWT super dev mode debug configurations using a standard Java project.
 *
 * Note: If stopping the process before it's cleaned up, a port may stay open b/c of an orphaned
 * Java process. netstat -an | egrep 'Proto|LISTEN' - list the ports
 */
public class DebugConfigurationSuperDevModeTest extends AbstractGWTPluginSwtBotTestCase {

  protected static final String MENU_GWT = "Web Application";
  protected static final String MENU_GWT_SUPERDEVMODE = "Web Application (GWT Super Dev Mode)";
  protected static final String PROJECT_NAME = "Project";

  /**
   * Test the default, running with super dev mode
   */
  public void testShortcutUsingDefaults() {
    // When I right click and Debug GWT Super Dev Mode
    whenIRightClickandRunDebugConfigurationAndStopDebuggingIt(false);

    // When I get the arguments for super dev mode config
    String persistedArgs = whenIGetTheProgramArgsTextBox().getText();

    // And close the debug configuration dialog
    getSwtWorkbenchBot().button("Close").click();
    // And closing may cause a save change dialog
    closeSaveChangesDialogIfNeedBe();

    // Then the args should be
    assertFalse(persistedArgs.contains("-superDevMode"));
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

  private void closeSaveChangesDialogIfNeedBe() {
    SwtBotTestingUtilities.performAndWaitForWindowChange(getSwtWorkbenchBot(), new Runnable() {
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

  private SWTBotText whenIGetTheProgramArgsTextBox() {
    SWTWorkbenchBot bot = getSwtWorkbenchBot();

    // When I open debug configuration
    SwtBotMenuActions.openDebugConfiguration(bot);

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
  private void whenIClickandRunDebugConfiguration(boolean isSuperDevModeMenu) {
    final SWTWorkbenchBot bot = getSwtWorkbenchBot();

    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    project.setFocus();
    project.select();

    // Since the menus have dynamic numbers in them
    // and with out a built in iteration, this is what I came up with
    for (int i = 0; i < 15; i++) {
      try {
        bot.sleep(500);
        String number = "";
        if (i < 10) {
          number = i + " ";
        }
        if (isSuperDevModeMenu) {
          bot.menu("Run").menu("Debug As").menu(number + MENU_GWT_SUPERDEVMODE).click();
        } else {
          bot.menu("Run").menu("Debug As").menu(number + MENU_GWT).click();
        }
        break;
      } catch (WidgetNotFoundException e) {}
    }
  }

  /**
   * Right click on project and goto Debug As > 4 Run GWT Super Dev Mode Then Stop the debugging
   * process
   */
  private void whenIRightClickandRunDebugConfigurationAndStopDebuggingIt(boolean isSuperDevModeMenu) {
    final SWTWorkbenchBot bot = getSwtWorkbenchBot();

    whenIClickandRunDebugConfiguration(isSuperDevModeMenu);

    // And then stop it
    SwtBotMenuActions.openDebugPerspectiveAndTerminateProcess(bot);

    // And back to the java perspective
    SwtBotMenuActions.openJavaPerpsective(bot);
    bot.sleep(500);
  }

}
