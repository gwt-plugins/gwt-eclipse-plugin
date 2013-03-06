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

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.core.IsInstanceOf;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotProjectActions {
  /**
   * Possible names for the GPE menu label.
   */
  public static final String[] GOOGLE_MENU_LABELS = {"GPE Tools", "Google"};

  public static void createUiBinder(final SWTWorkbenchBot bot,
      String projectName, String packageName, String name,
      boolean generateSampleContent, boolean generateComments) {
    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Other...").click();

    // Select the Web App project wizard
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem = SwtBotWorkbenchActions.getUniqueTreeItem(
        bot, projectSelectionTree, "Google Web Toolkit", "UiBinder").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem,
        "UiBinder");
    bot.button("Next >").click();

    // Configure the UiBinder and then create it
    String sourceFolder = projectName + "/src";
    bot.textWithLabel("Source folder:").setText(sourceFolder);
    bot.textWithLabel("Package:").setText(packageName);
    bot.textWithLabel("Name:").setText(name);

    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Generate sample content"),
        generateSampleContent);
    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Generate comments"),
        generateComments);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("Finish"));
  }

  public static void createWebAppProject(final SWTWorkbenchBot bot,
      String projectName, String packageName, boolean useGwt,
      boolean useAppEngine) {
    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Project...").click();

    // Select the Web App project wizard
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, GOOGLE_MENU_LABELS,
            "Web Application Project").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem,
        "Web Application Project");
    bot.button("Next >").click();

    // Configure the project and then create it
    bot.textWithLabel("Project name:").setText(projectName);
    bot.textWithLabel("Package: (e.g. com.example.myproject)").setText(
        packageName);

    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Use Google Web Toolkit"),
        useGwt);
    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Use Google App Engine"),
        useAppEngine);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("Finish"));
  }

  public static void deleteProject(final SWTWorkbenchBot bot,
      final String projectName) {

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        selectProject(bot, projectName).contextMenu("Delete").click();
        // Wait for confirmation window to come up
      }
    });

    // Select the "Delete project contents on disk (cannot be undone)"
    bot.checkBox(0).click();

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot,
        bot.button("OK"));
  }

  /**
   * Opens the Properties dialog for a given project.
   * 
   * This method assumes that either the Package Explorer or Project Explorer
   * view is visible.
   */
  public static void openProjectProperties(final SWTWorkbenchBot bot,
      String projectName) {
    selectProject(bot, projectName);

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        // Open the Project Properties menu via the File menu
        SWTBotMenu fileMenu = bot.menu("File");
        fileMenu.menu("Properties").click();
      }
    });
  }

  public static SWTBotTreeItem selectProject(final SWTWorkbenchBot bot,
      String projectName) {
    /*
     * Choose either the Package Explorer View or the Project Explorer view.
     * Eclipse 3.3 and 3.4 start with the Java Perspective, which has the
     * Package Explorer View open by default, whereas Eclipse 3.5 starts with
     * the Resource Perspective, which has the Project Explorer View open.
     */
    SWTBotView explorer = null;
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer")
          || view.getTitle().equals("Project Explorer")) {
        explorer = view;
        break;
      }
    }

    if (explorer == null) {
      throw new WidgetNotFoundException(
          "Could not find the 'Package Explorer' or 'Project Explorer' view.");
    }

    // Select the root of the project tree in the explorer view
    IsInstanceOf matcher = new IsInstanceOf(Tree.class);
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = (Tree) bot.widget(matcher, explorerWidget);
    return new SWTBotTree(explorerTree).getTreeItem(projectName).select();
  }

  private SwtBotProjectActions() {
  }
}
