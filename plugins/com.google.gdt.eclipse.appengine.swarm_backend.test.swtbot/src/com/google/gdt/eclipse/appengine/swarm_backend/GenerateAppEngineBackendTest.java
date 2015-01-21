/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.swarm_backend;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;

/**
 * Tests the generation of App Engine backends.
 */
@SuppressWarnings("unused")
public class GenerateAppEngineBackendTest extends TestCase {
  private static final String JAVA_PROJECT_NAME = "JavaTestProject";
  private static final String ANDROID_PROJECT_NAME = "AccelerometerPlayActivity";
  private static final String APPENGINE_PROJECT_NAME = "AccelerometerPlayActivity-AppEngine";
  private String googleMenuLabel = "";

  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  /**
   * Tests that "Generate App Engine Backend..." for an Android project produces an App Engine
   * project.
   */
  public void testGenerateAppEngineBackend_androidProject() {

    // TODO(nbashirbello): Enable test when Android SDK is set up in test mode

    // // Set up App Engine SDK
    // SwtBotSdkActions.setupAppEngineSdk(bot);
    //
    // // Open Java Perspective
    // bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();
    //
    // try {
    // Thread.sleep(30000);
    // } catch (InterruptedException e) {
    //
    // }
    //
    // // Create an android project
    // createAndroidProject();
    //
    // // Select the project
    // SwtBotProjectActions.selectProject(bot, ANDROID_PROJECT_NAME);
    //
    // // Right-click and select "Generate App Engine Backend..."
    // MenuItem menuItem =
    // ContextMenuHelper.contextMenu(SwtBotProjectActions.getProjectRootTree(bot),
    // googleMenuLabel,
    // "Generate App Engine Backend...");
    // new SWTBotMenu(menuItem).click();
    //
    // // Click "Create" on the Create App Engine Backend for Android window
    // bot.button("Create").click();
    // SwtBotWorkbenchActions.waitForIdle(bot);
//
    // // Check for successful App Engine generation - no errors and the existence of the App Engine
    // // project
    // Assert.assertFalse(SwtBotProjectActions.hasErrorsInProblemsView(bot));
    // Assert.assertTrue(SwtBotProjectActions.doesProjectExist(bot, APPENGINE_PROJECT_NAME));
//
    // SwtBotProjectActions.deleteProject(bot, ANDROID_PROJECT_NAME);
    // SwtBotProjectActions.deleteProject(bot, APPENGINE_PROJECT_NAME);
//
    // SwtBotSdkActions.removeAppEngineSdk(bot);
  }

  /**
   * Tests that the "Generate App Engine Backend..." for a non-Android project produces appropriate
   * error message.
   */
  public void testGenerateAppEngineBackend_javaProject() {
//    // Open a Java Project
//    SwtBotProjectActions.createJavaProject(bot, JAVA_PROJECT_NAME);
//
//    // Select project
//    SwtBotProjectActions.selectProject(bot, JAVA_PROJECT_NAME);
//
//    // Right-click and select "Generate App Engine Backend..."
//    MenuItem menuItem =
//        ContextMenuHelper.contextMenu(SwtBotProjectActions.getProjectRootTree(bot),
//            googleMenuLabel,
//            "Generate App Engine Backend...");
//    new SWTBotMenu(menuItem).click();
//
//    SWTBotAssert.assertText("Error in Generating App Engine Backend", bot.activeShell());
//    bot.activeShell().close();
//
//    SwtBotProjectActions.deleteProject(bot, JAVA_PROJECT_NAME);
  }

  @Override
  protected void setUp() throws Exception {
    // SwtBotTestingUtilities.setUp(bot);
    //
    // // Determine Google context menu Label
    // googleMenuLabel = SwtBotProjectActions.getGoogleMenuLabel(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    // // Projects created during tests are deleted in test function because for each test the
    // name(s)
    // // of the project(s) created vary.
    // SwtBotTestingUtilities.tearDown();
  }

  /**
   * Creates the AccelerometerPlayActivity sample Android project.
   */
  private void createAndroidProject() {
    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Other...").click();

    // Select the Android Sample Project wizard
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, bot.tree(), "Android",
            "Android Sample Project").select();
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem,
        "Android Sample Project");
    bot.button("Next >").click();
    bot.button("Next >").click();
    bot.button("Finish").click();
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
    SwtBotWorkbenchActions.waitForIdle(bot);
    Assert.assertFalse(SwtBotProjectActions.hasErrorsInProblemsView(bot));
  }

}
