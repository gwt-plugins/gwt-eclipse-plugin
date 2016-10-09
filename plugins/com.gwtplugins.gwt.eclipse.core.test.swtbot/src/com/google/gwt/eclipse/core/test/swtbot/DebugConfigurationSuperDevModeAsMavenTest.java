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

/**
 * Test GWT super dev mode debug configurations using a Maven project.
 *
 * Overrides are for easy running for local testing.
 */
public class DebugConfigurationSuperDevModeAsMavenTest extends DebugConfigurationSuperDevModeTest {

  @Override
  public void testShortcutUsingDefaults() {
    givenMavenGwtProjectIsCreated1(PROJECT_NAME);

    // When I right click and Debug GWT Super Dev Mode
    whenIRightClickandRunDebugConfigurationAndStopDebuggingIt();

    // When I get the arguments for super dev mode config
    String persistedArgs = whenIGetTheProgramArgsTextBox().getText();

    // And close the debug configuration dialog
    getSwtWorkbenchBot().button("Close").click();
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


  @Override
  protected void givenProjectIsCreated() {
    // skip this b/c each test will have it's own maven module
  }

}
