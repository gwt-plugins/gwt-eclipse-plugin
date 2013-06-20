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
package com.google.gwt.eclipse.core.test.swtbot;

import com.google.gdt.eclipse.swtbot.SwtBotFileActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gdt.eclipse.swtbot.SwtBotSdkActions;
import com.google.gdt.eclipse.swtbot.SwtBotTestingUtilities;

import junit.framework.TestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Test the deploy process.
 */
public class UIBinderWizardTest extends TestCase {

  private static final String PROJECT_NAME = "WebApp";

  private SWTWorkbenchBot bot = new SWTWorkbenchBot();

  public void testUIBinderWizard() {
    SwtBotSdkActions.setupGwtSdk(bot);

    SwtBotProjectActions.createWebAppProject(bot, PROJECT_NAME, "com.example.webapp", true, false,
        true);

    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME,
        "com.example.webapp", "TestWithContent", true, true);

    String text = SwtBotFileActions.getEditorText(bot, "TestWithContent.java");
    assertTrue(text.contains("implements HasText"));
    assertTrue(text.contains("public TestWithContent()"));
    assertTrue(text.contains("public TestWithContent(String firstName)"));

    // then create one without sample content and check that the above is not
    // there
    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME,
        "com.example.webapp", "TestWithoutContent", false, true);

    String text2 = SwtBotFileActions.getEditorText(bot,
        "TestWithoutContent.java");
    assertFalse(text2.contains("implements HasText"));
    assertTrue(text2.contains("public TestWithoutContent()"));
    assertFalse(text2.contains("public TestWithoutContent(String firstName)"));
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
