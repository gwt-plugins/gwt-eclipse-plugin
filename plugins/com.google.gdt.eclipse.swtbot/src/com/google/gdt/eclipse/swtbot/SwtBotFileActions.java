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
package com.google.gdt.eclipse.swtbot;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.SWTBot;

/**
 * SWTBot utility methods that perform general file actions.
 */
public final class SwtBotFileActions {

  public static String getEditorText(final SWTWorkbenchBot bot, String title) {
    SWTBotEclipseEditor te = bot.editorByTitle(title).toTextEditor() ;
    return te.getText();
  }

  /**
   * Opens a resource using the Open Resource dialog.
   */
  public static void openResource(final SWTBot bot, String fileName) {

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        bot.menu("Navigate").menu("Open Resource").click();
      }
    });

    bot.text().typeText(fileName);
    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("Open"));
  }

  private SwtBotFileActions() {
  }
}
