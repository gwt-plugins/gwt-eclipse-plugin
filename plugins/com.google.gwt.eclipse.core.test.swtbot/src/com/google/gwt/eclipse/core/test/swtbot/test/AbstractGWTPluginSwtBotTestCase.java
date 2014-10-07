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
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.osgi.framework.Version;

/**
 * Abstract class with utility methods for running SWTBot unit tests.
 *
 * Note, keep this class simple and basic!
 */
public class AbstractGWTPluginSwtBotTestCase extends TestCase {

  public static final String PACKAGE_NAME = "com.example.project";

  private final SWTWorkbenchBot bot = new SWTWorkbenchBot();

  protected SWTWorkbenchBot getSwtWorkbenchBot() {
    return bot;
  }

  /**
   * Create a GWT project from Maven Archetype.
   *
   * @param projectName - Java project name
   */
  protected void givenMavenGwtProjectIsCreated(String projectName) {
    closeDialogsIfNeedBe();
    configureEclipsePreferences();

    // Given a gwt sdk is setup
    SwtBotSdkActions.setupGwtSdk(bot);

    // And create a maven project using an archetype
    String groupId = PACKAGE_NAME;
    String artifactId = projectName;
    String packageName = PACKAGE_NAME;
    // https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic
    String archetypeGroupId = "com.github.branflake2267.archetypes";
    String archetypeArtifactId = "gwt-basic-archetype";
    String archetypeVersion = "1.6-SNAPSHOT";
    String archetypeUrl = "https://oss.sonatype.org/content/repositories/snapshots";

    SwtBotProjectActions.createMavenProjectFromArchetype(bot, groupId, artifactId, packageName,
        archetypeGroupId, archetypeArtifactId, archetypeVersion, archetypeUrl);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(getSwtWorkbenchBot());
  }

  private void configureEclipsePreferences() {
    Version version = Platform.getProduct().getDefiningBundle().getVersion();
    int major = version.getMajor();
    int minor = version.getMinor();

    // Only configure this in Juno.
    if (major == 4 && minor == 2) {
      // Configure Juno JDK compiler compliance to 1.7
      // Preferences > Java > Compiler > JDK Compliance Group, Compiler compliance Level: 1.7
      SwtBotWorkbenchActions.openPreferencesDialog(bot);

      SWTBotTree projectSelectionTree = bot.tree();
      SWTBotTreeItem compilerItem =
          SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, "Java", "Compiler")
              .expand();
      SwtBotTestingUtilities.selectTreeItem(bot, compilerItem, "Compiler");

      // Set JDK Compliance to Java 1.7
      bot.comboBox().setSelection("1.7");
      bot.button("OK").click();
      bot.sleep(500);
    }
  }

  /**
   * Create a basic GWT project.
   *
   * @param projectName - Java project name
   */
  protected void givenProjectIsCreated(String projectName) {
    closeDialogsIfNeedBe();
    configureEclipsePreferences();

    // Given a gwt sdk is setup
    SwtBotSdkActions.setupGwtSdk(bot);

    // And given a project is created
    SwtBotProjectActions.createWebAppProject(bot, projectName, PACKAGE_NAME, true, false, true);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(getSwtWorkbenchBot());
  }

  /**
   * Close dialogs that may show up.
   */
  private void closeDialogsIfNeedBe() {
    // Wait for the project to finish setting up before moving on
    SwtBotWorkbenchActions.waitForMainShellProgressBarToFinish(getSwtWorkbenchBot());

    // Close dialogs that take focus from main shell
    if (getSwtWorkbenchBot().shells().length > 1) {
      System.out.println("windows = " + getSwtWorkbenchBot().shells().length);
      SWTBotShell[] shells = getSwtWorkbenchBot().shells();
      for (int i = 0; i < shells.length; i++) {
        // ADT port warning dialog
        if (shells[i].getText().equals("ddms")) {
          shells[i].close();
        }

        if (shells[i].getText().equals("Subclipse Usage")) {
          shells[i].close();
        }

        if (shells[i].getText().equals("Preferences")) {
          shells[i].close();
        }
      }
    }
  }

  @Override
  protected void setUp() throws Exception {
    SwtBotTestingUtilities.setUp(bot);
  }

  @Override
  protected void tearDown() throws Exception {
    SwtBotTestingUtilities.tearDown();
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
