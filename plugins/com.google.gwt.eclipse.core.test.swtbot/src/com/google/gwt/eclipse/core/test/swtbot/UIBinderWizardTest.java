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

import com.google.gdt.eclipse.swtbot.SwtBotFileActions;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;
import com.google.gwt.eclipse.core.test.swtbot.test.AbstractGWTPluginSwtBotTestCase;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

/**
 * Test the deploy process.
 */
public class UIBinderWizardTest extends AbstractGWTPluginSwtBotTestCase {

  protected static final String PROJECT_NAME = "WebApp";
  
  public void testUIBinderWizard() {
    SWTWorkbenchBot bot = getSwtWorkbenchBot();

    // When a ui binder is created
    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME, "com.example.webapp", "TestWithContent",
        true, true);

    // Then the editor will return these parts
    String text = SwtBotFileActions.getEditorText(bot, "TestWithContent.java");
    assertTrue(text.contains("implements HasText"));
    assertTrue(text.contains("public TestWithContent()"));
    assertTrue(text.contains("public TestWithContent(String firstName)"));

    // And when, create uibinder without the sample content
    // and check that the above is not there
    SwtBotProjectActions.createUiBinder(bot, PROJECT_NAME, "com.example.webapp",
        "TestWithoutContent", false, true);

    // And then verify the creating another uibinder with out content
    // doesn't have content
    String text2 = SwtBotFileActions.getEditorText(bot, "TestWithoutContent.java");
    assertFalse(text2.contains("implements HasText"));
    assertTrue(text2.contains("public TestWithoutContent()"));
    assertFalse(text2.contains("public TestWithoutContent(String firstName)"));
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    givenProjectIsCreated(PROJECT_NAME);
  }

  @Override
  protected void tearDown() throws Exception {
    thenTearDownProject(PROJECT_NAME);
    
    super.tearDown();
  }
  
}
