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
package com.google.gwt.eclipse.core.test.swtbot;

import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotWorkbenchActions;

/**
 * Test GWT super dev mode debug configurations using a Maven project.
 *
 * Overrides are for easy running for local testing.
 */
public class DebugConfigurationSuperDevModeAsMavenTest extends DebugConfigurationSuperDevModeTest {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  public void testCreatingLauncherWithJetty1() {
    givenMavenGwtProjectIsCreated1(PROJECT_NAME);

    // When I right click and Debug GWT Super Dev Mode
    whenIRightClickandRunDebugConfigurationAndStopDebuggingIt();

    // When I get the arguments for super dev mode config
    String persistedArgs = whenIGetTheProgramArgsTextBox().getText();

    // And close the debug configuration dialog
    bot.button("Close").click();
    // And closing may cause a save change dialog
    closeSaveChangesDialogIfNeedBe();

    // Then the args should be
    assertTrue(persistedArgs.contains("com.example.project.Project"));
  }

  // TODO fix archetype first
  // TODO then the module name will be a different assertion
  // TODO fix building the module on import
  // public void testShortcutUsingDefaults2() {
  // givenMavenGwtProjectIsCreated2(PROJECT_NAME);
  //
  // // When I right click and Debug GWT Super Dev Mode
  // whenIRightClickandRunDebugConfigurationAndStopDebuggingIt();
  //
  // // When I get the arguments for super dev mode config
  // String persistedArgs = whenIGetTheProgramArgsTextBox().getText();
  //
  // // And close the debug configuration dialog
  // getSwtWorkbenchBot().button("Close").click();
  // // And closing may cause a save change dialog
  // closeSaveChangesDialogIfNeedBe();
  //
  // // Then the args should be
  // assertTrue(persistedArgs.contains("com.example.project.Project"));
  // }

  /**
   * Create a GWT project from Maven Archetype.
   *
   * @param projectName - Java project name
   */
  protected void givenMavenGwtProjectIsCreated1(String projectName) {
    // Given a gwt sdk is setup
    // SwtBotSdkActions.setupGwtSdk(bot);

    // And create a maven project using an archetype
    String groupId = PACKAGE_NAME;
    String artifactId = projectName;
    String packageName = PACKAGE_NAME;
    // https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic
    String archetypeGroupId = "com.github.branflake2267.archetypes";
    String archetypeArtifactId = "gwt-test-gwt27-archetype";
    String archetypeVersion = "1.0-SNAPSHOT";
    String archetypeUrl = "https://oss.sonatype.org/content/repositories/snapshots";

    SwtBotProjectActions.createMavenProjectFromArchetype(bot, groupId, artifactId, packageName,
        archetypeGroupId, archetypeArtifactId, archetypeVersion, archetypeUrl);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);
  }

  /**
   * Create a GWT project from Maven Archetype.
   *
   * @param projectName - Java project name
   */
  protected void givenMavenGwtProjectIsCreated2(String projectName) {
    // Given a gwt sdk is setup
    // SwtBotSdkActions.setupGwtSdk(bot);

    // And create a maven project using an archetype
    String groupId = PACKAGE_NAME;
    String artifactId = projectName;
    String packageName = PACKAGE_NAME;
    // https://github.com/branflake2267/Archetypes/tree/master/archetypes/gwt-basic
    String archetypeGroupId = "com.github.branflake2267.archetypes";
    String archetypeArtifactId = "gwt-basic-archetype";
    String archetypeVersion = "2.0-SNAPSHOT";
    String archetypeUrl = "https://oss.sonatype.org/content/repositories/snapshots";

    SwtBotProjectActions.createMavenProjectFromArchetype(bot, groupId, artifactId, packageName,
        archetypeGroupId, archetypeArtifactId, archetypeVersion, archetypeUrl);

    // And wait for the project to finish setting up
    SwtBotWorkbenchActions.waitForIdle(bot);
  }

}
