/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.login.TestGoogleLogin;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * SWTbot tests for {@link SelectAppIdDialog}.
 */
public class SelectAppIdDialogTest extends TestCase {
  private SWTWorkbenchBot bot = new SWTWorkbenchBot();
  private static final String SIGN_IN_MESSAGE = "Click <a href=\"\">here</a> to log in.";
  private static final String CHANGE_USER_MESSAGE = "You are currently  logged in as "
      + TestGoogleLogin.getEmail() + ". Click <a href=\"\">here</a> to change that.";
  
  // TODO(rdayal): Remove this method once the methods below are uncommented.
  public void testThatExistsOnlyBecauseJUnitThrowsAnExceptionWhenThereAreNoTestsInTheFile() {
  }

// TODO(rdayal): Uncomment this test once b/13447978 is addressed.
//  /**
//   * Tests that the UI is in the correct configuration when it is opened with the user already
//   * logged in i.e. the correct buttons are enabled and disabled and the appropriate message is
//   * displayed.
//   */
//  public void testUserLoggedIn() {
//    // Log in
//    TestGoogleLogin.logIn();
//
//    // Open the "New Web Application Project" wizard and then the
//    // "Select App Id" dialog
//    openSelectAppIdDialog();
//
//    // Check that "OK" button is disabled and "Create App Id" button is enabled
//    SWTBotButton createAppIdButton = bot.button("Create App Id");
//    Assert.assertTrue(createAppIdButton.isEnabled());
//    Assert.assertTrue(bot.button("OK").isEnabled());
//
//    // Check that there is a link for the user to log in with appropriate
//    // message
//    Assert.assertEquals(CHANGE_USER_MESSAGE, bot.link().getText());
//
//    // Check that clicking the "Create App Id" button opens the "Create App Id"
//    // dialog
//    createAppIdButton.click();
//    SWTBotShell createAppIdDialog = bot.activeShell();
//    Assert.assertEquals(createAppIdDialog.getText(), "Create App Id");
//    createAppIdDialog.close();
//  }

// TODO(rdayal): Uncomment this test once b/13447978 is addressed.
//  /**
//   * Tests that the UI is in the correct configuration when it is opened with the user not logged in
//   * i.e. the correct buttons are enabled and disabled and the appropriate message is displayed.
//   */
//  public void testUserNotLoggedIn() {
//    // Log off
//    TestGoogleLogin.logOut();
//
//    // Open the "New Web Application Project" wizard and then the
//    // "Select App Id" dialog
//    openSelectAppIdDialog();
//
//    // Check that "OK" and "Create App Id" buttons are disabled
//    Assert.assertFalse(bot.button("OK").isEnabled());
//    Assert.assertFalse(bot.button("Create App Id").isEnabled());
//
//    // Check that list of App Ids is empty
//    String[] appIds = bot.listInGroup("App Ids").getItems();
//    Assert.assertEquals(0, appIds.length);
//
//    // Check that there is a link for the user to log in with appropriate
//    // message
//    Assert.assertEquals(SIGN_IN_MESSAGE, bot.link().getText());
//  }
//
//  @Override
//  protected void tearDown() {
//    bot.closeAllShells();
//    TestGoogleLogin.logOut();
//  }

  /**
   * Open the "Select App Id" dialog by first opening the "New Web Application Project" wizard and
   * then selecting "Use App Id" and then clicking its "Browse..." button.
   */
  private void openSelectAppIdDialog() {
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

    // Click the "Use App Id"'s "Browse.." button to open the "Select App Id" dialog
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.buttonInGroup("Identifiers for Google App Engine"));

  }
}
