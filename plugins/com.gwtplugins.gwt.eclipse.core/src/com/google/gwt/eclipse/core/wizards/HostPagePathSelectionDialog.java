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
package com.google.gwt.eclipse.core.wizards;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.internal.WorkbenchImages;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects a destination for a new HTML host page. This can be either a location
 * within one of the project's modules' public paths, or for web app projects, a
 * location within the WAR directory.
 * 
 * The public paths are displayed in the tree as they are defined in the module.
 * For example, a public path declared as "stuff/images" will be displayed in
 * the tree as a single node names "stuff/images", NOT as an "images" node
 * nested below a "stuff" node.
 * 
 * Also, this dialog will filter out any hidden sub-folders (folders whose names
 * begin with a dot).
 */
@SuppressWarnings("restriction")
public class HostPagePathSelectionDialog extends ElementTreeSelectionDialog {

  private static class HostPagePathContentProvider extends ArrayContentProvider
      implements ITreeContentProvider {

    public Object[] getChildren(Object element) {
      return ((HostPagePathTreeItem) element).getChildren();
    }

    public Object getParent(Object element) {
      return ((HostPagePathTreeItem) element).getParent();
    }

    public boolean hasChildren(Object element) {
      return ((HostPagePathTreeItem) element).getChildren().length > 0;
    }
  }

  /**
   * Declared public paths will be displayed as the qualified module name, plus
   * the public path: com.google.gwt.sample.Module/public.
   * 
   * Regular folders will be displayed by name.
   */
  private static class HostPagePathLabelProvider extends LabelProvider {

    private final Image image;

    public HostPagePathLabelProvider() {
      image = WorkbenchImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
    }

    @Override
    public Image getImage(Object element) {
      return image;
    }

    @Override
    public String getText(Object element) {
      HostPagePathTreeItem treeItem = (HostPagePathTreeItem) element;
      if (treeItem.isDeclaredPublicPath()) {
        ModuleFile moduleFile = treeItem.getModuleFile();

        // Get the module-relative public path
        IPath publicPath = treeItem.getPath().removeFirstSegments(
            moduleFile.getFile().getFullPath().segmentCount() - 1);

        // Qualified module name + public path
        return new Path(moduleFile.getQualifiedName()).append(publicPath).toString();
      } else {
        return treeItem.getPath().lastSegment();
      }
    }
  }

  private static class HostPagePathTreeItem {

    public static HostPagePathTreeItem[] createRootItems(IProject project) {
      List<HostPagePathTreeItem> rootItems = new ArrayList<HostPagePathTreeItem>();

      // Add root for war directory if this is a web app project
      if (WebAppUtilities.isWebApp(project)) {
        IFolder warFolder = WebAppUtilities.getWarSrc(project);
        if (warFolder.exists()) {
          rootItems.add(new HostPagePathTreeItem(warFolder, null));
        }
      }

      // Add roots for each public path of each module
      for (IModule module : ModuleUtils.findAllModules(
          JavaCore.create(project), false)) {
        rootItems.addAll(createItemsForModule((ModuleFile) module));
      }

      return rootItems.toArray(new HostPagePathTreeItem[0]);
    }

    private static List<HostPagePathTreeItem> createItemsForModule(
        ModuleFile moduleFile) {
      List<HostPagePathTreeItem> publicPathItems = new ArrayList<HostPagePathTreeItem>();

      // Get module's public paths and add their tree items
      for (IPath publicPath : moduleFile.getPublicPaths()) {
        publicPathItems.add(new HostPagePathTreeItem(moduleFile, publicPath));
      }

      return publicPathItems;
    }

    private static List<IFolder> getSubfolders(IFolder folder) {
      List<IFolder> folders = new ArrayList<IFolder>();

      try {
        for (IResource member : folder.members()) {
          if (member.getType() == IResource.FOLDER) {
            // Filter out derived and hidden folders (anything prefixed with .)
            if (member.isDerived() || member.getName().startsWith(".")) {
              continue;
            }
            folders.add((IFolder) member);
          }
        }
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      }

      return folders;
    }

    private final List<HostPagePathTreeItem> children = new ArrayList<HostPagePathTreeItem>();

    private ModuleFile moduleFile;

    private final HostPagePathTreeItem parent;

    private final IPath path;

    private HostPagePathTreeItem(IFolder folder, HostPagePathTreeItem parent) {
      this.parent = parent;
      this.path = folder.getFullPath();

      for (IFolder subfolder : getSubfolders(folder)) {
        this.children.add(new HostPagePathTreeItem(subfolder, this));
      }
    }

    private HostPagePathTreeItem(ModuleFile moduleFile, IPath publicPath) {
      this.parent = null;
      this.moduleFile = moduleFile;
      this.path = moduleFile.getFile().getFullPath().removeLastSegments(1).append(
          publicPath);

      IResource publicFolder = ResourcesPlugin.getWorkspace().getRoot().findMember(
          this.path);
      if (publicFolder != null) {
        // If the module's public path exist, then walk down to find its subdirs
        for (IFolder subFolder : getSubfolders((IFolder) publicFolder)) {
          this.children.add(new HostPagePathTreeItem(subFolder, this));
        }
      }
    }

    public HostPagePathTreeItem findPath(IPath path) {
      if (this.path.equals(path)) {
        return this;
      }

      if (this.path.isPrefixOf(path)) {
        for (HostPagePathTreeItem childTreeItem : getChildren()) {
          HostPagePathTreeItem foundPath = childTreeItem.findPath(path);
          if (foundPath != null) {
            return foundPath;
          }
        }
      }

      return null;
    }

    public HostPagePathTreeItem[] getChildren() {
      return children.toArray(new HostPagePathTreeItem[0]);
    }

    public ModuleFile getModuleFile() {
      return moduleFile;
    }

    public HostPagePathTreeItem getParent() {
      return parent;
    }

    public IPath getPath() {
      return path;
    }

    public boolean isDeclaredPublicPath() {
      return (moduleFile != null);
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }

  public static IPath show(Shell shell, IProject project, IPath initialPath) {
    HostPagePathSelectionDialog dialog = new HostPagePathSelectionDialog(shell,
        project);
    dialog.setInitialPath(project.getFullPath().append(initialPath));

    if (dialog.open() == Window.OK) {
      HostPagePathTreeItem selection = (HostPagePathTreeItem) dialog.getFirstResult();
      return selection.getPath();
    }

    return null;
  }

  private final HostPagePathTreeItem[] rootTreeItems;

  private HostPagePathSelectionDialog(Shell parent, IProject project) {
    super(parent, new HostPagePathLabelProvider(),
        new HostPagePathContentProvider());

    setTitle("Existing Folder Selection");
    setMessage("Choose a location for the HTML page");

    rootTreeItems = HostPagePathTreeItem.createRootItems(project);
    setInput(rootTreeItems);
    setComparator(new ViewerComparator());
  }

  private void setInitialPath(IPath initialPath) {
    if (!initialPath.isEmpty()) {
      for (HostPagePathTreeItem treeItem : rootTreeItems) {
        HostPagePathTreeItem initialSelection = treeItem.findPath(initialPath);
        if (initialSelection != null) {
          setInitialSelection(initialSelection);
          return;
        }
      }
    }
  }

}
