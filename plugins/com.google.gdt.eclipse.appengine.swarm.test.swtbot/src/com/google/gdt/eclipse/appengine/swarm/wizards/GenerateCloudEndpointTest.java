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
package com.google.gdt.eclipse.appengine.swarm.wizards;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;

/**
 * Tests the generation of cloud endpoint classes for anotated entity classes.
 */
@SuppressWarnings("unused")
public class GenerateCloudEndpointTest extends TestCase {

  private static final String PROJECT_NAME = "endpointsProject";
  private static final String PACKAGE_NAME = "com.test";
  private static final String GENERATE_ENDPOINTS = "Generate Cloud Endpoint Class";
  private static final String SOURCE_FOLDER = "src";
  private String googleMenuLabel = "";
  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  /**
   * Tests the "Generate Cloud Endpoints Class" for non-anotated classes.
   */
  public void testGenerateEndpoints_appEngineProject() {
    // // Create an Web Application project with App Engine support
    // SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME, PACKAGE_NAME, false, true, true);
    //
    // // Select project
    // SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    //
    // // Select class
    // SWTBotTreeItem endpointFile =
    // SwtBotProjectActions.selectProjectItem(project, SOURCE_FOLDER, PACKAGE_NAME,
    // "EndpointsProjectServlet.java");
    // Assert.assertNotNull("Cannot find anotated entity class.", endpointFile);
    // endpointFile.select();
    //
    // // Select "Generate Cloud Endpoint Class"
    // EndpointTestingUtilies.selectGenerateCloudEndpointClass(bot, googleMenuLabel);
    //
    // SWTBotAssert.assertText("Error in Generating API", bot.activeShell());
    // bot.activeShell().close();
  }

  /**
   * Tests that an Endpoint Class is generated for a JDO annotated class.
   */
  public void testGenerateEndpoints_jdoClass() {
    // // Create web app project with app engine support
    // SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME, PACKAGE_NAME, false, true,
    // false);
    //
    // // Create an anotated entity class
    // EndpointTestingUtilies.createAnotatedEntityClass(bot, PROJECT_NAME, PACKAGE_NAME, "Note",
    // true);
    //
    // // Generate Cloud Endpoints Class
    // selectGenerateCloudEndpointClass();
    //
    // // Verify that the endpoint classes were created
    // SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    // SWTBotTreeItem item =
    // SwtBotProjectActions.selectProjectItem(project, SOURCE_FOLDER, PACKAGE_NAME);
    // List<String> actuals = item.getNodes();
    // List<String> expected = Arrays.asList("Note.java", "NoteEndpoint.java", "PMF.java");
    // Assert.assertEquals(expected, actuals);
    //
    // // Check that there were no compilation errors
    // Assert.assertFalse(SwtBotProjectActions.hasErrorsInProblemsView(bot));
  }

  /**
   * Tests that an Endpoint Class is generated for a JPA annotated class.
   */
  public void testGenerateEndpoints_jpaClass() {
    // // Create web app project with app engine support
    // SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME, PACKAGE_NAME, false, true,
    // false);
    //
    // // Create an anotated entity class
    // EndpointTestingUtilies.createAnotatedEntityClass(bot, PROJECT_NAME, PACKAGE_NAME, "Note",
    // false);
    //
    // // Generate Cloud Endpoints Class
    // selectGenerateCloudEndpointClass();
    //
    // // Verify that the endpoint classes were created
    // SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    // SWTBotTreeItem item =
    // SwtBotProjectActions.selectProjectItem(project, SOURCE_FOLDER, PACKAGE_NAME);
    // List<String> actuals = item.getNodes();
    // List<String> expected = Arrays.asList("EMF.java", "Note.java", "NoteEndpoint.java");
    // Assert.assertEquals(expected, actuals);
    //
    // // Check that there were no compilation errors
    // Assert.assertFalse(SwtBotProjectActions.hasErrorsInProblemsView(bot));
  }

  @Override
  protected void setUp() throws Exception {
    // SwtBotTestingUtilities.setUp(bot);
    //
    // // Determine Google context menu Label
    // googleMenuLabel = SwtBotProjectActions.getGoogleMenuLabel(bot);
    //
    // // Set up App Engine SDK
    // SwtBotSdkActions.setupAppEngineSdk(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    // SwtBotProjectActions.deleteProject(bot, PROJECT_NAME);
    // SwtBotSdkActions.removeAppEngineSdk(bot);
    // SwtBotTestingUtilities.tearDown();
  }

  /**
   * Selects "Generate Cloud Endpoint Class" by right click on the selected project and selecting
   * Google -> Generate Cloud Endpoint Class. Then it waits until all background tasks are
   * completed.
   */
  private void selectGenerateCloudEndpointClass() {
    MenuItem menuItem =
        ContextMenuHelper.contextMenu(SwtBotProjectActions.getProjectRootTree(bot),
            googleMenuLabel,
            GENERATE_ENDPOINTS);
    new SWTBotMenu(menuItem).click();
    SwtBotWorkbenchActions.waitForIdle(bot);
  }
}
