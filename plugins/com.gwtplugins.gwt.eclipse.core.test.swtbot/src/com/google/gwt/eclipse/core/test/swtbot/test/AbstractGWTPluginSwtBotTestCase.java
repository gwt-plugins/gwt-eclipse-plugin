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
package com.google.gwt.eclipse.core.test.swtbot.test;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotSdkActions;
import com.google.gdt.eclipse.swtbot.SwtBotUtils;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Abstract class with utility methods for running SWTBot unit tests.
 *
 * Note, keep this class simple and basic!
 */
public class AbstractGWTPluginSwtBotTestCase extends TestCase {

  public static final String PACKAGE_NAME = "com.example.project";

  protected final SWTWorkbenchBot bot = new SWTWorkbenchBot();

  @Override
  protected void setUp() throws Exception {
    SwtBotUtils.setUp(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    SwtBotUtils.tearDown(bot);
  }

  /**
   * Create a basic GWT project.
   *
   * @param projectName - Java project name
   */
  protected void givenProjectIsCreated(String projectName) {
    // GwtRuntimeTestUtilities.addDefaultRuntime(); // TODO ????

    // Given a gwt sdk is setup
    SwtBotSdkActions.setupGwtSdk(bot);

    // And given a project is created
    SwtBotProjectActions.createWebAppProject(bot, projectName, PACKAGE_NAME, true, true);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);

    bot.sleep(1000);
  }

  /**
   * Tear down the project and remove it.
   *
   * @param projectName - Java web project name
   */
  protected void thenTearDownProject(String projectName) {
    SwtBotProjectActions.deleteProject(bot, projectName);
  }


}
