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
package com.google.gwt.eclipse.core.launch.ui;

import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.osgi.service.prefs.BackingStoreException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lets the user choose the specific module and host page to launch hosted mode
 * with, when we cannot infer them both from the selection.
 */
public class LegacyGWTHostPageSelectionDialog extends
    ElementTreeSelectionDialog {

  private static class LegacyGWTHostPageSelectionContentProvider extends
      ArrayContentProvider implements ITreeContentProvider {

    public Object[] getChildren(Object element) {
      return ((LegacyGWTHostPageSelectionTreeItem) element).getChildren();
    }

    public Object getParent(Object element) {
      return ((LegacyGWTHostPageSelectionTreeItem) element).getParent();
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }
  }

  /**
   * Wraps a module or host page IFile for display in the tree widget.
   */
  private static class LegacyGWTHostPageSelectionTreeItem {

    /**
     * Builds the tree nodes for a set of modules and host pages.
     * 
     * @param modulesHostPages the set of modules along with their respective
     *          host pages
     * @return tree root nodes
     */
    static LegacyGWTHostPageSelectionTreeItem[] buildTree(
        Map<String, Set<String>> modulesHostPages) {
      List<LegacyGWTHostPageSelectionTreeItem> treeItems = new ArrayList<LegacyGWTHostPageSelectionTreeItem>();
      for (String moduleName : modulesHostPages.keySet()) {
        LegacyGWTHostPageSelectionTreeItem moduleItem = new LegacyGWTHostPageSelectionTreeItem(
            Path.fromPortableString(moduleName.replace('.', '/')));
        treeItems.add(moduleItem);
        for (String hostPage : modulesHostPages.get(moduleName)) {
          new LegacyGWTHostPageSelectionTreeItem(
              Path.fromPortableString(hostPage), moduleItem);
        }
      }

      return treeItems.toArray(new LegacyGWTHostPageSelectionTreeItem[0]);
    }

    private final List<LegacyGWTHostPageSelectionTreeItem> children = new ArrayList<LegacyGWTHostPageSelectionTreeItem>();

    private final IPath path;

    private final LegacyGWTHostPageSelectionTreeItem parent;

    public LegacyGWTHostPageSelectionTreeItem(IPath modulePath) {
      this.parent = null;
      this.path = modulePath;
    }

    public LegacyGWTHostPageSelectionTreeItem(IPath hostPagePath,
        LegacyGWTHostPageSelectionTreeItem parentModule) {
      this.parent = parentModule;
      this.parent.children.add(this);
      this.path = hostPagePath;
    }

    public LegacyGWTHostPageSelectionTreeItem[] getChildren() {
      return children.toArray(new LegacyGWTHostPageSelectionTreeItem[0]);
    }

    public LegacyGWTHostPageSelectionTreeItem getParent() {
      return parent;
    }

    public IPath getPath() {
      return path;
    }

    public boolean isHostPage() {
      return this.parent != null;
    }

    public boolean isModule() {
      return this.parent == null;
    }
  }

  private static class LegacyHostPageSelectionLabelProvider extends
      LabelProvider {

    private ILabelProvider workbenchLabelProvider = new WorkbenchLabelProvider();

    @Override
    public Image getImage(Object element) {
      // TODO: Display appropriate images for XML and HTML files
      return null;
    }

    @Override
    public String getText(Object element) {
      LegacyGWTHostPageSelectionTreeItem item = (LegacyGWTHostPageSelectionTreeItem) element;

      IPath itemPath = item.getPath();

      if (item.getParent() == null) {
        return itemPath.lastSegment()
            + " - "
            + itemPath.removeLastSegments(1).toPortableString().replace('/',
                '.');
      }

      return itemPath.toPortableString();
    }
  }

  /**
   * Ensures that only host page nodes, and not modules, are valid selections.
   */
  private static class LegacyHostPageSelectionStatusValidator
      implements
        ISelectionStatusValidator {

    public IStatus validate(Object[] selection) {
      if (selection.length == 1) {
        LegacyGWTHostPageSelectionTreeItem selectedItem = (LegacyGWTHostPageSelectionTreeItem) selection[0];
        if (selectedItem.isHostPage()) {
          // Can't use Status.OK_STATUS because we don't want the "ok" visible
          return OK_STATUS;
        }
      }

      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, "");
    }
  }

  private static final IStatus OK_STATUS = new Status(IStatus.OK,
      GWTPlugin.PLUGIN_ID, "");

  private static final IStatus NO_HOST_PAGES_STATUS = new Status(
      IStatus.WARNING, GWTPlugin.PLUGIN_ID,
      "No host pages were found. Launch anyway?");

  public static String getStartupUrl(Shell shell, IProject project,
      Map<String, Set<String>> hostPagesByModule, boolean isExternal) {
    LegacyGWTHostPageSelectionDialog dialog = new LegacyGWTHostPageSelectionDialog(
        shell, project, hostPagesByModule, isExternal);

    if (dialog.open() == Window.OK) {
      try {
        WebAppProjectProperties.setLaunchConfigExternalUrlPrefix(project,
            dialog.getExternalUrlPrefix());
      } catch (BackingStoreException e) {
        GWTPluginLog.logError(e);
      }
      return dialog.getUrl();
    }

    // User clicked Cancel
    return null;
  }

  private final boolean isExternal;
  private Text externalUrlPrefixText;
  private String externalUrlPrefixTextCache;
  private IProject project;
  private Button useHostPageCheckbox;
  private boolean useHostPage;
  private final boolean isEmpty;

  private LegacyGWTHostPageSelectionDialog(Shell parent, IProject project,
      Map<String, Set<String>> hostPagesByModule, boolean isExternal) {
    super(parent, new LegacyHostPageSelectionLabelProvider(),
        new LegacyGWTHostPageSelectionContentProvider());

    this.isExternal = isExternal;
    this.project = project;
    this.isEmpty = hostPagesByModule.isEmpty();
    this.useHostPage = !isEmpty;

    setValidator(new LegacyHostPageSelectionStatusValidator());
    setTitle("Host page selection");
    setMessage("Choose a host page to launch the application with:");

    setInput(LegacyGWTHostPageSelectionTreeItem.buildTree(hostPagesByModule));
    setComparator(new ViewerComparator());
  }

  @Override
  protected Label createMessageArea(Composite composite) {

    Label label = new Label(composite, SWT.NONE);
    if (getMessage() != null) {
      label.setText(getMessage());
    }
    label.setFont(composite.getFont());

    if (isExternal) {
      Composite c = SWTFactory.createComposite(composite, 2, 1,
          GridData.FILL_HORIZONTAL);
      SWTFactory.createLabel(c, "External server root", 1);
      externalUrlPrefixText = SWTFactory.createSingleText(c, 1);
      externalUrlPrefixTextCache = WebAppProjectProperties.getLaunchConfigExternalUrlPrefix(project);
      externalUrlPrefixText.setText(externalUrlPrefixTextCache);

      externalUrlPrefixText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          externalUrlPrefixTextCache = externalUrlPrefixText.getText();
          updateOKStatus();
        }
      });

      useHostPageCheckbox = new Button(c, SWT.CHECK);
      useHostPageCheckbox.setText("Select a host page:");
      useHostPageCheckbox.setSelection(!isEmpty);

      useHostPageCheckbox.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          useHostPage = useHostPageCheckbox.getSelection();
          Tree tree = getTreeViewer().getTree();
          if (!useHostPage) {
            tree.setEnabled(false);
          } else {
            tree.setEnabled(!isEmpty);
          }
          updateOKStatus();
        }
      });
    }

    return label;
  }

  @Override
  protected void updateStatus(IStatus status) {

    if (isEmpty) {
      super.updateStatus(NO_HOST_PAGES_STATUS);
    } else {
      if (!useHostPage) {
        super.updateStatus(OK_STATUS);
      } else {
        super.updateStatus(status);
      }
    }
  }

  private String getExternalUrlPrefix() {
    if (isExternal) {
      return externalUrlPrefixTextCache.trim();
    }

    return "";
  }

  private String getUrl() {
    LegacyGWTHostPageSelectionTreeItem selection = (LegacyGWTHostPageSelectionTreeItem) getFirstResult();

    IPath url = Path.EMPTY;
    if (selection != null && selection.isHostPage()) {
      url = new Path(
          selection.getParent().getPath().toPortableString().replace('/', '.'));
      // Construct the final URL from the module name and host page path
      url = url.append(selection.getPath());
    }

    String urlString = url.toOSString();
    String externalURLPrefix = getExternalUrlPrefix();

    if (externalURLPrefix.length() > 0 && !(url.equals(Path.EMPTY))) {
      if (!externalURLPrefix.endsWith("/") && !urlString.startsWith("/")) {
        externalURLPrefix += '/';
      }
    }

    // Some people like to stick a hostname:port in the server root field and
    // expect it to work. We can handle that case here.
    String firstSegment = new Path(externalURLPrefix).segment(0);
    if (!externalURLPrefix.startsWith("http://") && firstSegment != null
        && firstSegment.indexOf(':') >= 0) {
      try {
        externalURLPrefix = new URL("http://" + externalURLPrefix).toString();
      } catch (MalformedURLException e) {
        // intentionally empty
      }
    }

    return externalURLPrefix + urlString;
  }

}
