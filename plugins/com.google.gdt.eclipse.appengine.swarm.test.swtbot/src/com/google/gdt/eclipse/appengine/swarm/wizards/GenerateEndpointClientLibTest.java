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
package com.google.gdt.eclipse.appengine.swarm.wizards;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotSdkActions;

import junit.framework.TestCase;

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;

import java.util.List;

/**
 * Tests the generation of cloud endpoint client libraries.
 */
@SuppressWarnings("unused")
public class GenerateEndpointClientLibTest extends TestCase {
  private static final String PROJECT_NAME = "endpointsProject";
  private static final String PACKAGE_NAME = "com.test";
  private SWTWorkbenchBot bot = new SWTWorkbenchBot();
  private String googleMenuLabel = "";
  private boolean setupAppEngineSdk = false;

  /**
   * Tests that "Generate Cloud Endpoint Client Library" creates endpoint-libs for projects with JDO
   * persistence supported endpoints.
   *
   * @throws InterruptedException
   */
  public void testGenerateClientLibrary_JdoEndpoints() throws InterruptedException {
    // testGenerateClientLibrary_withEndpoints(true);
  }

  /**
   * Tests that "Generate Cloud Endpoint Client Library" creates endpoint-libs for projects with JPA
   * persistence supported endpoints.
   *
   * @throws InterruptedException
   */
  public void testGenerateClientLibrary_JpaEndpoints() throws InterruptedException {
    // testGenerateClientLibrary_withEndpoints(false);
  }

  /**
   * Tests that "Generate Cloud Endpoint Client Library" does not create endpoint-libs for projects
   * with no endpoints.
   *
   * @throws InterruptedException
   */
  public void testGenerateClientLibrary_noEndpoints() throws InterruptedException {
    // // Create Java project
    // SwtBotTestingUtilities.createJavaProject(bot, PROJECT_NAME);
    //
    // // Select "Generate Cloud Endpoint Client Library"
    // SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    //
    // final MenuItem menuItem =
    // ContextMenuHelper.contextMenu(EndpointTestingUtilies.getProjectRootTree(bot),
    // googleMenuLabel, "Generate Cloud Endpoint Client Library");
    // new SWTBotMenu(menuItem).click();
    // SWTBotAssert.assertText("Error in Generating API", bot.activeShell());
    // bot.activeShell().close();
  }

  @Override
  protected void setUp() throws Exception {
    // SwarmApiCreator.setTestClientLibGenApiUrl();
    // SwtBotTestingUtilities.setUp(bot);
    //
    // // Determine Google context menu Label
    // googleMenuLabel = SwtBotProjectActions.getGoogleMenuLabel(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    // SwtBotProjectActions.deleteProject(bot, PROJECT_NAME);
    // if (setupAppEngineSdk) {
    // SwtBotSdkActions.removeAppEngineSdk(bot);
    // setupAppEngineSdk = false;
    // }
    // SwtBotTestingUtilities.tearDown();
  }

  /**
   * Tests that "Generate Cloud Endpoint Client Library" creates endpoint-libs for projects using
   * the specified persistence type.
   *
   * @param useJdo Determines persistence object type to use. Uses JDO if true and JPA otherwise.
   * @throws InterruptedException
   */
  private void testGenerateClientLibrary_withEndpoints(boolean useJdo) throws InterruptedException {
    // Create web app project with app engine support
    SwtBotSdkActions.setupAppEngineSdk(bot);
    setupAppEngineSdk = true;
    SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME, PACKAGE_NAME, false, true, false);

    // Create endpoint class
    EndpointTestingUtilies.createAnotatedEntityClass(bot, PROJECT_NAME, PACKAGE_NAME, "Note",
        useJdo);

    // Select project with endpoint class
    EndpointTestingUtilies.selectGenerateCloudEndpointClass(bot, googleMenuLabel);

    // Select "Generate Cloud Endpoint Client Library"
    final SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, PROJECT_NAME);
    final MenuItem menuItem =
        ContextMenuHelper.contextMenu(EndpointTestingUtilies.getProjectRootTree(bot),
            googleMenuLabel, "Generate Cloud Endpoint Client Library");
    new SWTBotMenu(menuItem).click();

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!project.getNodes().contains("endpoint-libs")) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // Timed out, do nothing.
          }
        }
      }
    });

    thread.start();
    thread.join(300000);

    Assert.assertFalse(thread.isAlive());

    // Check for creation of "endpoint-libs"
    List<String> actuals = project.getNodes();
    Assert.assertTrue(actuals.contains("endpoint-libs"));

    // TODO(nbashirbello): Include checks for the contents of the endpoint libs folder to make sure
    // that the appropriate directories are being generated.
  }
}
