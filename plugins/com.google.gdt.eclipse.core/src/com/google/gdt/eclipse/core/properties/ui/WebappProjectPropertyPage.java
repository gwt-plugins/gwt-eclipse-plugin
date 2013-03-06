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
package com.google.gdt.eclipse.core.properties.ui;

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.properties.WebAppProjectProperties;
import com.google.gdt.eclipse.core.ui.AbstractProjectPropertyPage;
import com.google.gdt.eclipse.core.ui.ResourceTreeSelectionDialog;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * Sets project properties related to the WAR directory.
 */
@SuppressWarnings("restriction")
public class WebappProjectPropertyPage extends AbstractProjectPropertyPage {

  /**
   * Interface to be used by extensions that determine whether this project is
   * eligible to be a Web Application.
   */
  public interface WebAppProjectEnablementFinder {
    boolean isWebAppEnabled(IProject project);
  }

  /**
   * Interface to be used by extension to dermine if the "launch and deploy from
   * this directory" option should be enabled.
   */
  public interface ManagedWarOptionEnablementFinder {
    /**
     * @return null to indicate that the option should be enabled, or a string
     * indicating why it's disabled to disable it.
     */
    String isManagedWarOptionEnabled(IProject project);
  }
  
  private static class ExcludedJarLabelProvider extends LabelProvider
      implements ITableLabelProvider {

    private final Image elementImage;

    public ExcludedJarLabelProvider() {
      ImageDescriptorRegistry registry = JavaPlugin.getImageDescriptorRegistry();
      elementImage = registry.get(JavaPluginImages.DESC_OBJS_JAR);
    }

    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0) {
        return elementImage;
      }
      return null;
    }

    public String getColumnText(Object element, int columnIndex) {
      IPath path = (IPath) element;
      if (columnIndex == 0) {
        return path.lastSegment();
      }
      return path.removeLastSegments(1).toOSString();
    }
  }

  private class ExcludedJarSelectionAdapter implements IListAdapter {

    public void customButtonPressed(ListDialogField field, int index) {
      if (index == IDX_ADD) {
        addEntry();
      }
    }

    public void doubleClicked(ListDialogField field) {
    }

    public void selectionChanged(ListDialogField field) {
    }
  }

  private class WarDirEventHandler extends SelectionAdapter implements ModifyListener {

    public void modifyText(ModifyEvent e) {
      fieldChanged();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      if (e.widget == hasWarDirButton) {
        maybePrepopulateDynamicWebValues();
        fieldChanged();
      } else if (e.widget == isWarSrcDirOutputButton) {
        fieldChanged();
      } else if (e.widget == warDirBrowseButton) {
        chooseWarDir();
      }
    }
  }

  private static final int IDX_ADD = 0;

  private static final int IDX_REMOVE = 2;

  private static final String WAR_DIR_SELECTION_DIALOG_MESSAGE = "Choose the WAR directory";

  private static final String WAR_DIR_SELECTION_DIALOG_TITLE = "WAR Directory Selection";

  private Composite excludedJarsComponent;

  private ListDialogField excludedJarsField;

  private Button hasWarDirButton;

  private Button isWarSrcDirOutputButton;

  private IPath warDir;

  private Button warDirBrowseButton;

  private Composite warDirComponent;

  private Text warDirText;

  public WebappProjectPropertyPage() {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(3, false);
    layout.verticalSpacing = layout.horizontalSpacing = 0;
    panel.setLayout(layout);

    createWarDirComponent(panel);
    createExcludedJarsComponent(panel);

    boolean initializeAndEnable = true;
    ExtensionQuery<WebappProjectPropertyPage.WebAppProjectEnablementFinder> extQuery =
        new ExtensionQuery<WebappProjectPropertyPage.WebAppProjectEnablementFinder>(
            CorePlugin.PLUGIN_ID, "webApplicationEnablementFinder", "class");
    List<ExtensionQuery.Data<WebappProjectPropertyPage.WebAppProjectEnablementFinder>>
        enablementFinders = extQuery.getData();
    for (ExtensionQuery.Data<WebappProjectPropertyPage.WebAppProjectEnablementFinder>
        enablementFinder : enablementFinders) {
      initializeAndEnable = enablementFinder.getExtensionPointData().isWebAppEnabled(getProject());
    }
    if (!initializeAndEnable) {
      SWTUtilities.setEnabledRecursive(panel, false);
    } else {
      initializeControls();
      addEventHandlers();
      fieldChanged();
    }
    return panel;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void saveProjectProperties() throws BackingStoreException, CoreException {
    IProject project = getProject();

    // Save project properties
    List<IPath> excludedJars = excludedJarsField.getElements();
    WebAppProjectProperties.setJarsExcludedFromWebInfLib(project, excludedJars);
    WebAppProjectProperties.setWarSrcDir(project, warDir);
    WebAppProjectProperties.setWarSrcDirIsOutput(project, isWarSrcDirOutputButton.getSelection());

    // If the project has a managed WAR output directory, update the default
    // Java output directory
    if (WebAppUtilities.hasManagedWarOut(project)) {
      WebAppUtilities.setOutputLocationToWebInfClasses(JavaCore.create(project), null);
    }
  }

  @SuppressWarnings("unchecked")
  private void addEntry() {
    BuildpathJarSelectionDialog dialog = new BuildpathJarSelectionDialog(
        getShell(), JavaCore.create(getProject()), excludedJarsField.getElements());
    if (dialog.open() == Window.OK) {
      excludedJarsField.addElements(dialog.getJars());
    }
  }

  private void addEventHandlers() {
    WarDirEventHandler handler = new WarDirEventHandler();
    hasWarDirButton.addSelectionListener(handler);
    warDirBrowseButton.addSelectionListener(handler);
    isWarSrcDirOutputButton.addSelectionListener(handler);
    warDirText.addModifyListener(handler);
  }

  private void chooseWarDir() {
    IProject project = getProject();
    IResource warDirRes = project.findMember(warDir);

    ResourceTreeSelectionDialog dialog = new ResourceTreeSelectionDialog(getShell(),
        WAR_DIR_SELECTION_DIALOG_TITLE,
        WAR_DIR_SELECTION_DIALOG_MESSAGE,
        project,
        warDirRes,
        IResource.FOLDER,
        IResource.FOLDER,
        false);
    List<IPath> paths = dialog.chooseResourcePaths();
    if (paths != null) {
      assert (paths.size() == 1);
      warDirText.setText(paths.get(0).toString());
    }
  }

  private void createExcludedJarsComponent(Composite parent) {
    excludedJarsComponent = new Composite(parent, SWT.NONE);
    GridData excludedJarsComponentGridData = new GridData(GridData.FILL_BOTH);
    excludedJarsComponentGridData.horizontalSpan = ((GridLayout) parent.getLayout()).numColumns;
    excludedJarsComponentGridData.grabExcessVerticalSpace = true;
    excludedJarsComponent.setLayoutData(excludedJarsComponentGridData);
    GridLayout containerGridLayout = new GridLayout(3, false);
    containerGridLayout.marginTop = 16;
    excludedJarsComponent.setLayout(containerGridLayout);

    Label label = new Label(excludedJarsComponent, SWT.NONE);
    GridData labelGridData = new GridData(GridData.FILL_HORIZONTAL);
    labelGridData.horizontalSpan = 3;
    label.setLayoutData(labelGridData);
    label.setText("Suppress warnings about these build path entries being outside of WEB-INF/lib:");

    String[] buttons = new String[] {"Add...", null, "Remove"};
    excludedJarsField = new ListDialogField(
        new ExcludedJarSelectionAdapter(), buttons, new ExcludedJarLabelProvider());

    ColumnLayoutData[] columns = new ColumnLayoutData[] {
        new ColumnWeightData(1, 100, true), new ColumnWeightData(2, 100, true)};
    String[] columnHeaderNames = {"JAR file", "Location"};
    excludedJarsField.setTableColumns(
        new ListDialogField.ColumnsDescription(columns, columnHeaderNames, false));
    excludedJarsField.setRemoveButtonIndex(IDX_REMOVE);
    excludedJarsField.doFillIntoGrid(excludedJarsComponent, 3);

    GridData layoutData =
        (GridData) excludedJarsField.getListControl(excludedJarsComponent).getLayoutData();
    layoutData.grabExcessHorizontalSpace = true;
    layoutData.grabExcessVerticalSpace = true;
    excludedJarsField.getListControl(excludedJarsComponent).setLayoutData(layoutData);
  }

  private void createWarDirComponent(Composite parent) {
    int numColumns = ((GridLayout) parent.getLayout()).numColumns;

    hasWarDirButton = new Button(parent, SWT.CHECK);
    hasWarDirButton.setText("This project has a WAR directory");
    GridData hasWarDirButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
    hasWarDirButtonGridData.horizontalSpan = numColumns;
    hasWarDirButton.setLayoutData(hasWarDirButtonGridData);

    warDirComponent = new Composite(parent, SWT.NONE);
    GridData warDirComponentGridData = new GridData(GridData.FILL_HORIZONTAL);
    warDirComponentGridData.horizontalSpan = numColumns;
    warDirComponent.setLayoutData(warDirComponentGridData);
    GridLayout warDirComponentLayout = new GridLayout(3, false);
    warDirComponentLayout.marginLeft = 16;
    warDirComponent.setLayout(warDirComponentLayout);

    Label warDirLabel = new Label(warDirComponent, SWT.NONE);
    warDirLabel.setText("WAR directory:");

    warDirText = new Text(warDirComponent, SWT.BORDER);
    GridData warDirTextGridData = new GridData(GridData.FILL_HORIZONTAL);
    warDirText.setLayoutData(warDirTextGridData);

    warDirBrowseButton = new Button(warDirComponent, SWT.NONE);
    warDirBrowseButton.setText("&Browse...");

    isWarSrcDirOutputButton = new Button(warDirComponent, SWT.CHECK);
    GridData isWarSrcDirOutputButtonGridData = new GridData(GridData.FILL_HORIZONTAL);
    isWarSrcDirOutputButtonGridData.horizontalIndent = 16;
    isWarSrcDirOutputButtonGridData.horizontalSpan = numColumns;
    isWarSrcDirOutputButton.setLayoutData(isWarSrcDirOutputButtonGridData);
    isWarSrcDirOutputButton.setText("Launch and deploy from this directory");
    
    String enableString = null;
    ExtensionQuery<ManagedWarOptionEnablementFinder> extQuery =
        new ExtensionQuery<ManagedWarOptionEnablementFinder>(
            CorePlugin.PLUGIN_ID, "managedWarOptionEnablementFinder", "class");
    List<ExtensionQuery.Data<ManagedWarOptionEnablementFinder>>
        enablementFinders = extQuery.getData();
    for (ExtensionQuery.Data<ManagedWarOptionEnablementFinder>
        enablementFinder : enablementFinders) {
      enableString = enablementFinder.getExtensionPointData().isManagedWarOptionEnabled(getProject());
      if (enableString != null) {
        break; // take the first "disable" response
      }
    }
    
    if (enableString != null) {
      isWarSrcDirOutputButton.setEnabled(false);
      isWarSrcDirOutputButton.setText(isWarSrcDirOutputButton.getText() + 
        " (" + enableString + ")");
    }
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private void initializeControls() {
    IPath warDirProp = WebAppProjectProperties.getWarSrcDir(getProject());
    if (warDirProp != null) {
      hasWarDirButton.setSelection(true);
      warDirText.setText(warDirProp.toString());
    } else {
      hasWarDirButton.setSelection(false);
    }
    isWarSrcDirOutputButton.setSelection(WebAppProjectProperties.isWarSrcDirOutput(getProject()));

    List<IPath> excludedJars = WebAppProjectProperties.getJarsExcludedFromWebInfLib(getProject());
    excludedJarsField.setElements(excludedJars);
    excludedJarsField.selectFirstElement();
  }

  private void maybePrepopulateDynamicWebValues() {
    if (hasWarDirButton.getSelection()) {
      if (StringUtilities.isEmptyOrWhitespace(warDirText.getText())) {
        try {
          if (DynamicWebProjectUtilities.isDynamicWebProject(getProject())) {
            IPath webContentFolder = DynamicWebProjectUtilities.getWebContentFolder(getProject());
            if (webContentFolder != null) {
              warDirText.setText(webContentFolder.toPortableString());
              isWarSrcDirOutputButton.setSelection(false);
            }
          }
        } catch (CoreException ce) {
          CorePluginLog.logError(ce);
        }
      }
    }
  }

  private void updateControls() {
    boolean enableWarDirComponent = hasWarDirButton.getSelection();
    SWTUtilities.setEnabledRecursive(warDirComponent, enableWarDirComponent);
    boolean enableExcludedJarsComponent =
        (enableWarDirComponent && isWarSrcDirOutputButton.getSelection());
    SWTUtilities.setEnabledRecursive(excludedJarsComponent, enableExcludedJarsComponent);
  }

  private void validateFields() {
    IStatus warDirStatus = validateWarDir();
    updateStatus(warDirStatus);
  }

  private IStatus validateWarDir() {
    warDir = null;

    if (!hasWarDirButton.getSelection()) {
      // If the main checkbox is not set, we have no WAR directory to validate
      return StatusUtilities.OK_STATUS;
    }

    String warDirString = warDirText.getText().trim();

    if (warDirString.length() == 0) {
      return StatusUtilities.newErrorStatus("Enter the WAR source directory", CorePlugin.PLUGIN_ID);
    }

    IPath path = new Path(warDirString);
    IProject project = getProject();

    if (!(project.findMember(path) instanceof IFolder)) {
      return StatusUtilities.newErrorStatus(
          "The folder ''{0}/{1}'' does not exist", CorePlugin.PLUGIN_ID, project.getName(), path);
    }

    warDir = new Path(warDirString);
    return StatusUtilities.OK_STATUS;
  }
}
