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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * SWTBot utility methods that perform SDK-related actions.
 */
public final class SwtBotSdkActions {

  public static void removeAppEngineSdk(final SWTWorkbenchBot bot) {
    removeSdk(bot, "App Engine");
  }

  public static void removeGwtSdk(SWTWorkbenchBot bot) {
    removeSdk(bot, "Web Toolkit");
  }

  public static void setupAppEngineSdk(final SWTWorkbenchBot bot) {
    setupSdk(bot, "App Engine", "GAE_HOME", "appengine-sdk");
  }

  public static void setupGwtSdk(final SWTWorkbenchBot bot) {
    setupSdk(bot, "Web Toolkit", "GWT_HOME", "gwt-sdk");
  }

  private static void removeSdk(final SWTWorkbenchBot bot, String treeItemText) {
    SwtBotWorkbenchActions.openPreferencesDialog(bot);

    SWTBotTreeItem prefGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, bot.tree(),
            SwtBotProjectActions.GOOGLE_MENU_LABELS, "Web Toolkit").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, prefGoogleTreeItem, treeItemText);

    bot.table().select(0);
    bot.button("Remove").click();

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("OK"));
  }

  private static void setupSdk(final SWTWorkbenchBot bot, String treeItemText,
      String sdkDirEnvVariableName, String sdkDisplayName) {
    SwtBotWorkbenchActions.openPreferencesDialog(bot);

    SWTBotTreeItem prefGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, bot.tree(),
            SwtBotProjectActions.GOOGLE_MENU_LABELS, "Web Toolkit").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, prefGoogleTreeItem, treeItemText);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Add..."));

    bot.textWithLabel("Installation directory:").setText(System.getenv(sdkDirEnvVariableName));
    bot.textWithLabel("Display name:").setText(sdkDisplayName);

    SWTBotButton okButton = bot.button("OK");
    if (okButton.isEnabled()) {
      // Close add SDK dialog
      SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, okButton);
    } else {
      // Already setup, in the case of running multiple tests together
      SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Cancel"));
    }

    // Close Preferences dialog
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("OK"));
  }

  private SwtBotSdkActions() {}
}
