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
package com.google.appengine.eclipse.core.deploy;

import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Test the deploy process.
 */
public class DeployTest extends TestCase {

//  private static final String PROJECT_NAME = "WebApp";

  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  public void testDeployWithBadAppId() {
/*  Deploy to appengine now uses google login plugin and oauth

    SwtBotSdkActions.setupAppEngineSdk(bot);

    // We'll enable GAE via project properties
    SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME,
        "com.example.webapp", false, true);

    // Open App Engine project properties
    SwtBotProjectActions.openProjectProperties(bot, PROJECT_NAME);

    // Set the app ID
    SWTBotTreeItem projectPropGoogleTreeItem = bot.tree().expandNode("Google");
    SwtBotTestingUtilities.selectTreeItem(bot, projectPropGoogleTreeItem,
        "App Engine");
    bot.textWithLabel("Application ID:").setText("thisisabogusappid");
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("OK"));

    // Pretend to deploy it
    bot.toolbarButtonWithTooltip("Deploy App Engine Project").click();
    bot.textWithLabel("Email:").setText("someone@example.com");
    bot.textWithLabel("Password:").setText("bogus");
    bot.button("Deploy").click();
*/
    // TODO: inspect the error text and look for appropriate string
  }

  @Override
  protected void setUp() throws Exception {
    SwtBotTestingUtilities.setUp(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    SwtBotTestingUtilities.tearDown();
  }
}
