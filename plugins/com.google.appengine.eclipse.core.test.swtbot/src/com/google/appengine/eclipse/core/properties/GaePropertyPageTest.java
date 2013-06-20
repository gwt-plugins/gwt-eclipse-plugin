/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.core.properties;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

/**
 * Test the properties process.
 */
public class GaePropertyPageTest extends TestCase {

  private static final String PROJECT_NAME = "javaProject";

  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  public void testPropertyPageWithJavaProject() {
    // Open a Java Project
    SwtBotTestingUtilities.createJavaProject(bot, PROJECT_NAME);

    // Open Java project properties
    SwtBotProjectActions.openProjectProperties(bot, PROJECT_NAME);

    // Select the App Engine property
    SWTBotTreeItem appEngineTreeItem = SwtBotWorkbenchActions.getUniqueTreeItem(
        bot, bot.tree(), SwtBotProjectActions.GOOGLE_MENU_LABELS, "App Engine");
    appEngineTreeItem.select("App Engine");

    // Check that "Use Google App Engine" checkbox is unchecked
    assertTrue(bot.checkBox("Use Google App Engine").isEnabled());
    assertFalse(bot.checkBox("Use Google App Engine").isChecked());

    assertTrue(bot.button("OK").isEnabled());
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("OK"));

    // Delete project
    SwtBotProjectActions.deleteProject(bot, PROJECT_NAME);
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