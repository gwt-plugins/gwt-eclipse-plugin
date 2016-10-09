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

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectCreation;
import com.google.gdt.eclipse.swtbot.SwtBotProjectDebug;
import com.google.gdt.eclipse.swtbot.SwtBotUtils;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Create GWT Maven project and launch with SDM with Jetty debug session
 */
public class DebugConfigurationSuperDevModeAsMavenTest extends TestCase {

  private final SWTWorkbenchBot bot = new SWTWorkbenchBot();
  private static final String PROJECT_NAME = "Project";
  private static final String PACKAGE_NAME = "com.example.project";

  @Override
  protected void setUp() throws Exception {
    SwtBotUtils.setUp(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    SwtBotProjectActions.deleteProject(bot, PROJECT_NAME);
    SwtBotUtils.tearDown(bot);
  }

  public void testCreatingLauncherWithJetty1() {
    // Create project with GWT Maven Plugin 1
    SwtBotProjectCreation.createMavenGwtProjectIsCreated1(bot, PROJECT_NAME, PACKAGE_NAME);

    // When I right click and Debug GWT Super Dev Mode
    SwtBotProjectDebug.createDebugGWTWithJetty(bot, PROJECT_NAME);

    // When I get the arguments for super dev mode config
    String persistedArgs = SwtBotProjectDebug.getTheProgramArgsTextBox(bot).getText();

    // And close the debug configuration dialog
    bot.button("Close").click();
    // And closing may cause a save change dialog
    SwtBotProjectDebug.closeSaveChangesDialogIfNeedBe(bot);

    // Then the args should be
    assertTrue(persistedArgs.contains("com.example.project.Project"));
  }

  // TODO fix archetype first
  // TODO then the module name will be a different assertion
  // TODO fix building the module on import
  public void TODO_ShortcutUsingDefaults2() {
    // Create project with GWT Maven Plugin 2
    SwtBotProjectCreation.createMavenGwtProjectIsCreated2(bot, PROJECT_NAME, PACKAGE_NAME);

    // When I right click and Debug GWT Super Dev Mode
    SwtBotProjectDebug.createDebugGWTWithJetty(bot, PROJECT_NAME);

    // When I get the arguments for super dev mode config
    String persistedArgs = SwtBotProjectDebug.getTheProgramArgsTextBox(bot).getText();

    // And close the debug configuration dialog
    bot.button("Close").click();
    // And closing may cause a save change dialog
    SwtBotProjectDebug.closeSaveChangesDialogIfNeedBe(bot);

    // Then the args should be
    // TODO change for maven 2 plugin
    assertTrue(persistedArgs.contains("com.example.project.App"));
  }

}
