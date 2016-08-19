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

import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

/**
 * SWTBot utility methods that perform general workbench actions.
 */
public final class SwtBotProjectActions {

  /**
   * Possible names for the GPE menu label.
   */
  public static final String[] GOOGLE_MENU_LABELS = {"GPE Tools", "Google"};

  private static final String SOURCE_FOLDER = "src";

  /**
   * Creates a java class with the specified name.
   * 
   * @param bot The SWTWorkbenchBot.
   * @param projectName The name of the project the class should be created in.
   * @param packageName The name of the package the class should be created in.
   * @param className The name of the java class to be created.
   */
  public static void createJavaClass(final SWTWorkbenchBot bot, String projectName,
      String packageName, final String className) {
    SWTBotTreeItem project = SwtBotProjectActions.selectProject(bot, projectName);
    selectProjectItem(project, SOURCE_FOLDER, packageName).select();
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        MenuItem menuItem = ContextMenuHelper.contextMenu(getProjectRootTree(bot), "New", "Class");
        new SWTBotMenu(menuItem).click();
      }
    });

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.activeShell();
        bot.textWithLabel("Name:").setText(className);
        SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
      }
    });
  }

  /**
   * Creates a java project with the specified project name.
   * 
   * @param bot the SWTWorkbenchBot
   * @param projectName the name of the java project to create
   */
  public static void createJavaProject(SWTWorkbenchBot bot, String projectName) {
    // Open Java Perspective
    bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();

    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Project...").click();

    // Select the Java project
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, "Java", "Java Project");
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem, "Java Project");

    bot.button("Next >").click();

    // Configure the project and then create it
    bot.textWithLabel("Project name:").setText(projectName);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
  }

  /**
   * Create a Maven project from Archetype and land in the Java perspective.
   */
  public static void createMavenProjectFromArchetype(final SWTWorkbenchBot bot, String groupId,
      String artifactId, String packageName, String archetypeGroupId, String archetypeArtifactId,
      String archetypeVersion, String archetypeUrl) {
    // create maven project
    SwtBotMenuActions.openNewMavenProject(bot);

    // move to next step, archetype selection
    bot.button("Next >").click();

    // open archetype dialog
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        bot.button("Add Archetype...").click();
      }
    });

    // Dialog: "New Maven Project"
    // The Archetype project source lives here
    // Generated with this repos generator
    bot.comboBox(0).setText(archetypeGroupId);
    bot.comboBox(1).setText(archetypeArtifactId);
    bot.comboBox(2).setText(archetypeVersion);
    bot.comboBox(3).setText(archetypeUrl);

    // close archetype dialog
    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      @Override
      public void run() {
        // After OK, it will take a minute to download
        bot.button("OK").click();
      }
    });

    // enable snapshots in table view
    bot.checkBox("Include snapshot archetypes").click();

    // filter so only one row shows up
    bot.text().setText("gwt-basic-archetype");

    // select first row
    SWTBotTable table = bot.table();
    table.setFocus();
    table.getTableItem(0).select();

    // move to last wizard
    bot.button("Next >").click();

    // set archetype inputs
    bot.comboBox(0).setText(groupId);
    bot.comboBox(1).setText(artifactId);
    bot.comboBox(3).setText(packageName);

    // finish and close dialog, and it will init
    bot.button("Finish").click();

    // change to the java perpective for next stage
    SwtBotMenuActions.openJavaPerpsective(bot);

    // select the first project
    bot.tree().setFocus();
  }

  public static void createUiBinder(final SWTWorkbenchBot bot, String projectName,
      String packageName, String name, boolean generateSampleContent, boolean generateComments) {
    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Other...").click();

    // Select the Web App project wizard
    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem projectSelectionGoogleTreeItem =
        SwtBotWorkbenchActions.getUniqueTreeItem(bot, projectSelectionTree, "Google Web Toolkit",
            "UiBinder").expand();
    SwtBotTestingUtilities.selectTreeItem(bot, projectSelectionGoogleTreeItem, "UiBinder");
    bot.button("Next >").click();

    // Configure the UiBinder and then create it
    String sourceFolder = projectName + "/" + SOURCE_FOLDER;
    bot.textWithLabel("Source folder:").setText(sourceFolder);
    bot.textWithLabel("Package:").setText(packageName);
    bot.textWithLabel("Name:").setText(name);

    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Generate sample content"),
        generateSampleContent);
    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Generate comments"), generateComments);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
  }

  public static void createWebAppProject(final SWTWorkbenchBot bot, String projectName,
      String packageName, boolean useGwt, boolean useAppEngine, boolean generateSampleCode) {
    // Open Java Perspective
    bot.perspectiveById("org.eclipse.jdt.ui.JavaPerspective").activate();

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
    bot.textWithLabel("Package: (e.g. com.example.myproject)").setText(packageName);

    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Use Google Web Toolkit"), useGwt);
    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Use Google App Engine"), useAppEngine);
    SwtBotTestingUtilities.setCheckBox(bot.checkBox("Generate project sample code"),
        generateSampleCode);

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("Finish"));
  }

  public static void deleteProject(final SWTWorkbenchBot bot, final String projectName) {

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        selectProject(bot, projectName).contextMenu("Delete").click();
        // Wait for confirmation window to come up
      }
    });

    // Select the "Delete project contents on disk (cannot be undone)"
    bot.checkBox(0).click();

    SwtBotTestingUtilities.clickButtonAndWaitForWindowChange(bot, bot.button("OK"));
  }

  /**
   * Returns true if the specified project can be found in the 'Package Explorer' or 'Project View',
   * otherwise returns false. Throws a WidgetNotFoundException exception if the 'Package Explorer'
   * or 'Project Explorer' view cannot be found.
   * 
   * @param bot The SWTWorkbenchBot.
   * @param projectName The name of the project to be found.
   * @return
   */
  public static boolean doesProjectExist(final SWTWorkbenchBot bot, String projectName) {
    SWTBotView explorer = getPackageExplorer(bot);
    if (explorer == null) {
      throw new WidgetNotFoundException(
          "Could not find the 'Package Explorer' or 'Project Explorer' view.");
    }

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    SWTBotTreeItem[] allItems = new SWTBotTree(explorerTree).getAllItems();
    for (int i = 0; i < allItems.length; i++) {
      if (allItems[i].getText().equals(projectName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the Google context menu label. Throws WidgetNotFoundException if it cannot find it.
   */
  public static String getGoogleMenuLabel(SWTWorkbenchBot bot) {
    // Open the list of new project wizards
    bot.menu("File").menu("New").menu("Other...").click();

    SWTBotTree projectSelectionTree = bot.tree();
    SWTBotTreeItem[] itemsArray = projectSelectionTree.getAllItems();

    for (SWTBotTreeItem item : itemsArray) {
      for (String label : GOOGLE_MENU_LABELS) {
        if (item.getText().equals(label)) {
          bot.button("Cancel").click();
          return label;
        }
      }
    }

    bot.button("Cancel").click();
    throw new WidgetNotFoundException("Cannot determine the Google menu label");
  }

  /*
   * Choose either the Package Explorer View or the Project Explorer view. Eclipse 3.3 and 3.4 start
   * with the Java Perspective, which has the Package Explorer View open by default, whereas Eclipse
   * 3.5 starts with the Resource Perspective, which has the Project Explorer View open.
   */
  public static SWTBotView getPackageExplorer(final SWTWorkbenchBot bot) {
    SWTBotView explorer = null;
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer") || view.getTitle().equals("Project Explorer")) {
        explorer = view;
        break;
      }
    }
    return explorer;
  }

  /**
   * Returns the project root tree in Package Explorer.
   */
  public static SWTBotTree getProjectRootTree(SWTWorkbenchBot bot) {
    SWTBotView explorer = getPackageExplorer(bot);

    if (explorer == null) {
      throw new WidgetNotFoundException("Cannot find Package Explorer or Project Explorer");
    }

    Tree tree = bot.widget(widgetOfType(Tree.class), explorer.getWidget());
    return new SWTBotTree(tree);
  }

  /**
   * Returns true if there are errors in the Problem view. Returns false otherwise.
   */
  public static boolean hasErrorsInProblemsView(SWTWorkbenchBot bot) {
    // Open Problems View by Window -> show view -> Problems
    bot.menu("Window").menu("Show View").menu("Problems").click();

    SWTBotView view = bot.viewByTitle("Problems");
    view.show();
    SWTBotTree tree = view.bot().tree();

    for (SWTBotTreeItem item : tree.getAllItems()) {
      String text = item.getText();
      if (text != null && text.startsWith("Errors")) {
        return true;
      }
    }

    return false;
  }

  /**
   * Opens the Properties dialog for a given project.
   * 
   * This method assumes that either the Package Explorer or Project Explorer view is visible.
   */
  public static void openProjectProperties(final SWTWorkbenchBot bot, String projectName) {
    selectProject(bot, projectName);

    SwtBotTestingUtilities.performAndWaitForWindowChange(bot, new Runnable() {
      public void run() {
        // Open the Project Properties menu via the File menu
        SWTBotMenu fileMenu = bot.menu("File");
        fileMenu.menu("Properties").click();
      }
    });
  }

  /**
   * Refresh project tree.
   * 
   * @param bot The SWTWorkbenchBot.
   * @param projectName The project name.
   */
  public static void refreshProject(final SWTWorkbenchBot bot, String projectName) {
    SWTBotTreeItem project = selectProject(bot, projectName);
    project.contextMenu("Refresh").click();
  }

  /**
   * Returns the specified project. Throws a WidgetNotFoundException if the 'Package Explorer' or
   * 'Project Explorer' view cannot be found or if the specified project cannot be found.
   * 
   * @param bot The SWTWorkbenchBot.
   * @param projectName The name of the project to select.
   * @return
   */
  public static SWTBotTreeItem selectProject(final SWTWorkbenchBot bot, String projectName) {
    /*
     * Choose either the Package Explorer View or the Project Explorer view. Eclipse 3.3 and 3.4
     * start with the Java Perspective, which has the Package Explorer View open by default, whereas
     * Eclipse 3.5 starts with the Resource Perspective, which has the Project Explorer View open.
     */
    SWTBotView explorer = getPackageExplorer(bot);
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer") || view.getTitle().equals("Project Explorer")) {
        explorer = view;
        break;
      }
    }

    if (explorer == null) {
      throw new WidgetNotFoundException(
          "Could not find the 'Package Explorer' or 'Project Explorer' view.");
    }

    // Select the root of the project tree in the explorer view
    Widget explorerWidget = explorer.getWidget();
    Tree explorerTree = bot.widget(widgetOfType(Tree.class), explorerWidget);
    return new SWTBotTree(explorerTree).getTreeItem(projectName).select();
  }

  /**
   * Select a file/folder by providing a parent tree, and a list folders that lead to the
   * file/folder.
   * 
   * @param item Root tree item.
   * @param folderPath List of folder names that lead to file.
   * @return Returns a SWTBotTreeItem of the last name in texts.
   */
  public static SWTBotTreeItem selectProjectItem(SWTBotTreeItem item, String... folderPath) {
    for (String folder : folderPath) {
      if (item == null) {
        return null;
      }
      item.doubleClick();
      item = item.getNode(folder);
    }
    return item;
  }

  private SwtBotProjectActions() {}

}
