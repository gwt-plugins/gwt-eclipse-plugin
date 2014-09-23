/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm.wizards;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.swtbot.SwtBotProjectActions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Assert;

/**
 * Provides helper methods to aid in the testing of Cloud Endpoint generation.
 */
public class EndpointTestingUtilies {
  private static final String JDO_CLASS = "JdoAnnotatedClass";
  private static final String JPA_CLASS = "JpaAnnotatedClass";

  /**
   * Create an entity class with either JDO or JPA annotations. If useJDO is true, the class uses
   * JDO annotations otherwise the class uses JPA annotations.
   */
  public static void createAnotatedEntityClass(SWTWorkbenchBot bot, String projectName,
      String packageName, String className, boolean useJDO) {

    // Create a java class in the new project
    SwtBotProjectActions.createJavaClass(bot, projectName, packageName, className);

    // Copy annotated entity class text into new class
    String contents = "";
    try {
      String annotationType = JDO_CLASS;
      if (!useJDO) {
        annotationType = JPA_CLASS;
      }
      contents = ResourceUtils.getResourceAsString(GenerateCloudEndpointTest.class, annotationType);
    } catch (CoreException e) {
      Assert.assertNotNull(contents);
      Assert.assertFalse(contents.isEmpty());
    }
    SWTBotEditor editor = bot.editorByTitle(className + ".java");
    editor.toTextEditor().setText(contents);
    editor.save();
  }

  /**
   * Returns the project root tree in Package Explorer
   */
  public static SWTBotTree getProjectRootTree(SWTWorkbenchBot bot) {
    // Get the project root tree in Package Explorer
    SWTBotView explorer = null;
    for (SWTBotView view : bot.views()) {
      if (view.getTitle().equals("Package Explorer") ||
          view.getTitle().equals("Project Explorer")) {
        explorer = view;
        break;
      }
    }
    Tree tree = (Tree) bot.widget(widgetOfType(Tree.class), explorer.getWidget());
    return new SWTBotTree(tree);
  }

  /**
   * Selects "Generate Cloud Endpoint Class" by right click on the selected project and selecting
   * Google -> Generate Cloud Endpoint Class
   */
  public static void selectGenerateCloudEndpointClass(SWTWorkbenchBot bot, String googleMenuLabel) {
    MenuItem menuItem =
        ContextMenuHelper.contextMenu(getProjectRootTree(bot), googleMenuLabel,
            "Generate Cloud Endpoint Class");
    new SWTBotMenu(menuItem).click();
  }

  /**
   * Select a file/folder by providing a parent tree, and a list folders that lead to the
   * file/folder.
   * 
   * @param item Root tree item.
   * @param texts List of folder names that lead to file.
   * @return Returns a SWTBotTreeItem of the last name in texts.
   */
  public static SWTBotTreeItem selectProjectItem(SWTBotTreeItem item, String... texts) {
    for (String text : texts) {
      if (item == null) {
        return null;
      }
      item.doubleClick();
      item = item.getNode(text);
    }
    return item;
  }
}
