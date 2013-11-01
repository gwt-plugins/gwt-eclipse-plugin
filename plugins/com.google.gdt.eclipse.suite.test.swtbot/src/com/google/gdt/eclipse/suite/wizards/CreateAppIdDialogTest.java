/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.login.GoogleLogin;
import com.google.gdt.eclipse.login.TestGoogleLogin;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;

/**
 * SWTbot tests for {@link CreateAppIdDialog}.
 */
public class CreateAppIdDialogTest extends TestCase {
  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  /**
   * Tests that the "Create" button is disabled if no text is entered in the
   * "New App Id" text box.
   */
  public void testEmptyAppIdField() {
    bot.text().setText("");
    Assert.assertFalse(bot.button("Create").isEnabled());
  }

  /**
   * Tests that the "Create" button is only enabled when a valid app id is
   * entered in the "New App Id" text box.
   */
  public void testNonEmptyAppIdField() {
    bot.text().setText("a");
    Assert.assertFalse(bot.button("Create").isEnabled());

    bot.text().setText("abc123");
    Assert.assertTrue(bot.button("Create").isEnabled());

    bot.text().setText("-abc123");
    Assert.assertFalse(bot.button("Create").isEnabled());

    bot.text().setText("abc123-");
    Assert.assertFalse(bot.button("Create").isEnabled());

    bot.text().setText("abc-123");
    Assert.assertTrue(bot.button("Create").isEnabled());

    bot.text().setText("!@#$%^");
    Assert.assertFalse(bot.button("Create").isEnabled());
  }

  @Override
  protected void setUp() {
    // Log in
    TestGoogleLogin.logIn();

    // Open the Create App Id Dialog
    openCreateAppIdDialog();
  }

  @Override
  protected void tearDown() {
    bot.closeAllShells();
    TestGoogleLogin.logOut();
  }

  /**
   * Open the "Create App Id" dialog by first opening the
   * "New Web Application Project" wizard, selecting the "Use App Id" radio
   * button and clicking on its "Browse..." button. This opens the
   * "Select App Id" dialog. And then clicking on the "Create App Id" button in
   * the "Select App Id" dialog to open the "Create App Id" dialog.
   * 
   * Note: User must be logged in before calling this function.
   */
  private void openCreateAppIdDialog() {
    Assert.assertTrue("User must be logged in to access the Create App Id Dialog during tests",
        GoogleLogin.getInstance().isLoggedIn());

    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Project...").click();

    // Select the "New Web Application Project" wizard
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree,
            SwtBotProjectActions.GOOGLE_MENU_LABELS, "Web Application Project").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem,
        "Web Application Project");
    bot.button("Next >").click();

    // Select the "Use App Id" option
    bot.radio("Use App Id").click();

    // Click the "Use App Id"'s "Browse.." button to open the "Select App Id"
    // dialog
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.buttonInGroup("Identifiers for Google App Engine"));

    // In the "Select App Id" dialog, click the "Create App Id" button
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Create App Id"));
  }
}
