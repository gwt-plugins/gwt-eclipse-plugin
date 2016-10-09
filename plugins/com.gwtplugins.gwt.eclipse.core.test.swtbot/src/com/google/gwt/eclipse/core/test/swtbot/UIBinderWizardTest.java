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

import com.google.gdt.eclipse.swtbot.SwtBotMenuActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectCreation;
import com.google.gdt.eclipse.swtbot.SwtBotUtils;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public class UIBinderWizardTest extends TestCase {

  private final SWTWorkbenchBot bot = new SWTWorkbenchBot();
  private static final String PROJECT_NAME = "Project";
  private static final String PACKAGE_NAME = "com.example.project";

  @Override
  protected void setUp() throws Exception {
    SwtBotUtils.setUp(bot);
    SwtBotProjectCreation.createJavaStandardProject(bot, PROJECT_NAME, PACKAGE_NAME);
  }

  @Override
  protected void tearDown() throws Exception {
    SwtBotProjectActions.deleteProject(bot, PROJECT_NAME);
    SwtBotUtils.tearDown(bot);
  }

  /**
   * Test creating a UiBinder class
   */
  public void testUIBinderWizard() throws Exception {
    // When a ui binder is created
    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME, PACKAGE_NAME, "TestWithContent", true,
        true);

    // Then the editor will return these parts
    String text = SwtBotMenuActions.getEditorText(bot, "TestWithContent.java");
    assertTrue(text.contains("implements HasText"));
    assertTrue(text.contains("public TestWithContent()"));
    assertTrue(text.contains("public TestWithContent(String firstName)"));

    // And when, create uibinder without the sample content
    // and check that the above is not there
    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME, PACKAGE_NAME,
        "TestWithoutContent", false, true);

    // And then verify the creating another uibinder with out content
    // doesn't have content
    String text2 = SwtBotMenuActions.getEditorText(bot, "TestWithoutContent.java");
    assertFalse(text2.contains("implements HasText"));
    assertTrue(text2.contains("public TestWithoutContent()"));
    assertFalse(text2.contains("public TestWithoutContent(String firstName)"));
  }

}
