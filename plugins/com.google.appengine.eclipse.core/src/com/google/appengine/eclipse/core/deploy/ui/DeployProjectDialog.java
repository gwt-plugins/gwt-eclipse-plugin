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
package com.google.appengine.eclipse.core.deploy.ui;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.properties.ui.GaeProjectPropertyPage;
import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.prefs.BackingStoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets the parameters for deploying an App Engine project to Google.
 */
public class DeployProjectDialog extends TitleAreaDialog {

  private class DeployTree {

    // backends will hopefully not have names with a pipe
    private static final String SETTINGS_DELIMITER = "|";

    private TreeItem backendsItem; // may be null
    private TreeItem frontendsItem;
    private Tree tree;

    public DeployTree(Composite parent, Object layoutData) {
      tree = new Tree(parent, SWT.MULTI | SWT.BORDER | SWT.CHECK);
      tree.setLayoutData(layoutData);

      tree.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          Object o = e.item.getData();
          if (o != null && o instanceof Runnable) {
            ((Runnable) o).run();
          }

          // Don't call fieldChanged() in the tree listener because
          // fieldChanged()
          // will rebuild the tree, which is for project changes. Instead just
          // call updateStatus directly.
          updateStatus(validate());
        }
      });
    }

    public DeploymentSet getDeploymentSet() {
      boolean deployFrontend = (frontendsItem != null && frontendsItem.getChecked());

      List<String> backendNames = new ArrayList<String>();
      if (backendsItem != null) {
        for (TreeItem i : backendsItem.getItems()) {
          if (i.getChecked()) {
            backendNames.add(i.getText());
          }
        }
      }

      return new DeploymentSet(deployFrontend, backendNames);
    }

    public String getSettings() {

      if (!allTreeItemsChecked()) {

        // pipe-delimited settings
        StringBuilder sb = new StringBuilder();
        for (TreeItem n : backendsItem.getItems()) {
          if (n.getChecked()) {
            String backendName = n.getText();
            sb.append(backendName);
            sb.append(SETTINGS_DELIMITER);
          }
        }

        /*
         * Append whether the frontend item is checked to the end, benefiting
         * from the leftover delimiter from the above for loop. Note that there
         * must be backends, and hence a leftover delimiter, to even get here,
         * because if there were only a frontend, that is checked by default and
         * the user can't uncheck it, and allTreeItemsChecked() will return
         * true.
         */
        sb.append(frontendsItem.getChecked() + "");

        return sb.toString();
      } else {

        /*
         * Don't save the default settings (ie, all checked). This handles the
         * case where a project originally has no backends, and then backends
         * are added later. If we saved "no backends checked", then the newly
         * added backends will not be checked when these settings are loaded
         * (ie, overriding defaults)
         */
        return "";
      }
    }

    public void setTreeContents(IProject newProject) {
      tree.removeAll();
      frontendsItem = null;
      backendsItem = null;

      if (newProject != null && GaeNature.isGaeProject(newProject)) {

        GaeProject gaeProject = GaeProject.create(newProject);

        // Add "Frontend" TreeItem
        tree.setEnabled(true);
        frontendsItem = new TreeItem(tree, SWT.NONE);
        frontendsItem.setText("Frontend");

        List<String> backendNames = gaeProject.getBackendNames();
        if (backendNames.size() > 0) {

          // Add "Backends" TreeItem
          backendsItem = new TreeItem(tree, SWT.NONE);
          backendsItem.setText("Backends");
          // when the parent TreeItem is checked or unchecked, check or uncheck
          // all its children.
          backendsItem.setData(new Runnable() {
            public void run() {
              boolean checked = backendsItem.getChecked();
              backendsItem.setGrayed(false);
              for (TreeItem i : backendsItem.getItems()) {
                i.setChecked(checked);
              }
            }
          });

          /*
           * TreeItem doesn't support SWT.Selection listeners, so give it a
           * runnable and call this runnable in the tree's selection event. See
           * DeployTree constructor. When a child TreeItem of backends is checked or
           * unchecked, determine if the parent TreeItem should be checked (ie,
           * checked if all children are checked, unchecked if all children are
           * unchecked, and grayed if some are checked).
           */
          Runnable backendItemRunnable = new Runnable() {
            public void run() {
              setCheckedStateFromChildren(backendsItem);
            }
          };

          // Add all backend TreeItems
          for (String s : backendNames) {
            TreeItem backendItem = new TreeItem(backendsItem, SWT.NONE);
            backendItem.setText(s);
            backendItem.setData(backendItemRunnable);
          }

          backendsItem.setExpanded(true);

          String settings = GaeProjectProperties.getGaeDeployDialogSettings(newProject);
          loadCheckedStates(settings);

        } else {
          // if there is a project and it's a gae project, but there are no
          // backends, then add only the frontend tree item and disable the tree
          frontendsItem.setChecked(true);
          tree.setEnabled(false);
        }

      } else {
        // if there is no project or it isn't a gae project, then the tree
        // is cleared and disabled.
        tree.setEnabled(false);
      }
    }

    public IStatus validate() {

      if (countAllCheckedTreeItems() == 0) {
        return StatusUtilities.newErrorStatus(
            "Select at least the frontend or one backend to deploy.",
            AppEngineCorePlugin.PLUGIN_ID);
      } else {
        return OK_STATUS;
      }
    }

    private boolean allTreeItemsChecked() {
      // tree is max 2 levels deep
      for (TreeItem n : tree.getItems()) {
        if (!n.getChecked()) {
          return false;
        }
        for (TreeItem m : tree.getItems()) {
          if (!m.getChecked()) {
            return false;
          }
        }
      }
      return true;
    }

    private int countAllCheckedTreeItems() {
      int totalCheckedItems = 0;
      for (int i = 0; i < tree.getItemCount(); i++) {
        TreeItem item = tree.getItem(i);
        if (item.getChecked()) {
          totalCheckedItems++;
        }
        // countCheckedTreeItems doesn't count child nodes recursively, but the 
        // tree is only two levels deep, so this is ok.
        totalCheckedItems += countCheckedChildTreeItems(item);
      }
      return totalCheckedItems;
    }

    /**
     * Counts the number of checked TreeItems under this TreeItem. This does not
     * count child nodes.
     */
    private int countCheckedChildTreeItems(TreeItem item) {
      int checkedItemsCount = 0;
      for (int i = 0; i < item.getItemCount(); i++) {
        TreeItem t = item.getItem(i);
        if (t.getChecked()) {
          checkedItemsCount++;
        }
      }
      return checkedItemsCount;
    }

    private void loadCheckedStates(String settings) {

      if (settings.length() > 0) {

        String[] strings = settings.split("[" + SETTINGS_DELIMITER + "]");

        for (int i = 0; i < strings.length - 1; i++) {
          for (TreeItem n : backendsItem.getItems()) {
            if (n.getText().equals(strings[i])) {
              n.setChecked(true);
            }
          }
        }

        setCheckedStateFromChildren(backendsItem);

        boolean frontendsItemChecked = "true".equals(strings[strings.length - 1]);
        frontendsItem.setChecked(frontendsItemChecked);
      } else {
        // tree is only ever 2 levels deep
        for (TreeItem n : tree.getItems()) {
          n.setChecked(true);
          for (TreeItem m : n.getItems()) {
            m.setChecked(true);
          }
        }
      }
    }

    private void setCheckedStateFromChildren(TreeItem n) {
      int checkedItemsCount = countCheckedChildTreeItems(n);
      if (checkedItemsCount == 0) {
        n.setChecked(false);
      } else if (checkedItemsCount == n.getItemCount()) {
        n.setChecked(true);
        n.setGrayed(false);
      } else {
        n.setChecked(true);
        n.setGrayed(true);
      }
    }
  }

  private class FieldListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      fieldChanged();
    }
  }

  private static final Status OK_STATUS = new Status(IStatus.OK,
      AppEngineCorePlugin.PLUGIN_ID, "");

  public static IStatus getMostImportantStatus(IStatus[] status) {
    IStatus max = null;
    for (int i = 0; i < status.length; i++) {
      IStatus curr = status[i];
      if (curr.matches(IStatus.ERROR)) {
        return curr;
      }
      if (max == null || curr.getSeverity() > max.getSeverity()
          || max.getMessage().length() == 0) {
        max = curr;
      }
    }
    return max;
  }

  private static int convertSeverity(IStatus status) {
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        return IMessageProvider.ERROR;
      case IStatus.WARNING:
        return IMessageProvider.WARNING;
      case IStatus.INFO:
        return IMessageProvider.INFORMATION;
      default:
        return IMessageProvider.NONE;
    }
  }

  private String appId;

  private String appVersion;

  private Button chooseProjectButton;

  private Button launchBrowserButton;

  private Button deployButton;

  private DeployTree deployTree;

  private DeploymentSet deploymentSet;

  private final FieldListener listener = new FieldListener();

  private Link moreInfoLink;

  // TODO: this really should be a GaeProject instance
  private IProject project;

  private Link projectPropertiesLink;

  private Link compliancePreferenceLink;

  private Text projectText;

  public DeployProjectDialog(IProject project, Shell parentShell) {
    super(parentShell);
    this.project = project;
  }

  public DeploymentSet getDeploymentSet() {
    return deploymentSet;
  }

  public IProject getProject() {
    return project;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Deploy Project to Google App Engine");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    deployButton = getButton(IDialogConstants.OK_ID);

    // Re-label the OK button and set it as default
    deployButton.setText("Deploy");
    getShell().setDefaultButton(deployButton);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    setTitle("Deploy");
    setTitleImage(AppEngineCorePlugin.getDefault().getImage(
        GaeImages.APP_ENGINE_DEPLOY_LARGE));

    initializeControls();
    addEventHandlers();
    fieldChanged();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(parent, SWT.NONE);
    GridData containerGridData = new GridData(GridData.FILL_HORIZONTAL);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    gridLayout.makeColumnsEqualWidth = false;
    container.setLayout(gridLayout);

    // Project field
    Label projectLabel = new Label(container, SWT.NONE);
    projectLabel.setText("Project:");
    projectText = new Text(container, SWT.BORDER);
    projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    chooseProjectButton = new Button(container, SWT.NONE);
    chooseProjectButton.setText("Browse...");

    // spacer
    new Label(container, SWT.NONE).setLayoutData(new GridData(
        GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));

    // Frontends / backends deploy tree
    Label treeLabel = new Label(container, SWT.NONE);
    treeLabel.setText("Select the frontend and backends to deploy:");
    treeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
        true, true, 2, 1));

    moreInfoLink = new Link(container, SWT.NONE);
    moreInfoLink.setText("<a href=\"#\">Learn more</a>");
    moreInfoLink.setLayoutData(new GridData(GridData.END, GridData.CENTER,
        false, false, 1, 1));

    GridData treeGridData = new GridData(GridData.FILL_BOTH);
    treeGridData.horizontalSpan = 3;
    treeGridData.minimumHeight = 150;
    treeGridData.heightHint = 150;
    deployTree = new DeployTree(container, treeGridData);

    launchBrowserButton = new Button(container, SWT.CHECK);
    launchBrowserButton.setText("Launch app in browser after successful deploy");
    launchBrowserButton.setSelection(true);

    projectPropertiesLink = new Link(container, SWT.NONE);
    GridData createAppIdLinkGridData = new GridData(SWT.LEAD, SWT.TOP, true,
        false, 3, 1);
    createAppIdLinkGridData.verticalIndent = 15;
    projectPropertiesLink.setLayoutData(createAppIdLinkGridData);
    projectPropertiesLink.setText("<a href=\"#\">App Engine project settings...</a>");

    new Label(container, SWT.NONE).setText("To deploy on a Java 7 runtime, change the JDK compliance level to 1.7. ");
    compliancePreferenceLink = new Link(container, SWT.NONE);
    compliancePreferenceLink.setText("<a href=\"#\">More info here.</a>");

    return container;
  }

  @Override
  protected void okPressed() {

    this.deploymentSet = deployTree.getDeploymentSet();

    String settings = deployTree.getSettings();

    try {
      GaeProjectProperties.setGaeDeployDialogSettings(project, settings);
    } catch (BackingStoreException e) {
      AppEngineCorePluginLog.logError(e,
          "Could not save deploy dialog settings.");
    }

    try {
      GaeProjectProperties.setGaeLaunchAppInBrowser(project, launchBrowserButton.getSelection());
    } catch (BackingStoreException e) {
      AppEngineCorePluginLog.logError(e,
          "Could not save preference to lunch app in browser after successful deploy");
    }

    // do this last so that the widgets aren't disposed so that info
    // can be still taken from them in deployTree.getSettings()
    super.okPressed();
  }

  private void addEventHandlers() {
    projectText.addModifyListener(listener);
    chooseProjectButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IJavaProject selectedProject = chooseProject();
        if (selectedProject != null) {
          projectText.setText(selectedProject.getElementName());
        }
      }
    });

    projectPropertiesLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        assert (project != null);
        PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(
            DeployProjectDialog.this.getShell(), project,
            GaeProjectPropertyPage.ID, new String[]{GaeProjectPropertyPage.ID},
            null);
        if (Window.OK == page.open()) {
          // Invalidate cached app id and version
          GaeProject gaeProject = GaeProject.create(project);
          assert (gaeProject != null);

          appId = gaeProject.getAppId();
          appVersion = gaeProject.getAppVersion();

          // Refresh to pick up settings changes
          fieldChanged();
        }
      }
    });

    moreInfoLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions("http://code.google.com/appengine/docs/java/backends");
      }
    });

    compliancePreferenceLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtilities.launchBrowserAndHandleExceptions("https://developers.google.com/eclipse/docs/jdk_compliance");
      }
    });
  }

  private IJavaProject chooseProject() {
    IJavaProject[] javaProjects;
    try {
      javaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
    } catch (JavaModelException e) {
      AppEngineCorePluginLog.logError(e);
      javaProjects = new IJavaProject[0];
    }

    // Filter the list to only show App Engine projects
    List<IJavaProject> gaeProjects = new ArrayList<IJavaProject>();
    for (IJavaProject javaProject : javaProjects) {
      if (GaeNature.isGaeProject(javaProject.getProject())) {
        gaeProjects.add(javaProject);
      }
    }

    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Choose a project to deploy");
    dialog.setElements(gaeProjects.toArray(new IJavaProject[0]));
    dialog.setInitialSelections(new Object[]{JavaCore.create(project)});

    dialog.setHelpAvailable(false);
    if (dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private void fieldChanged() {
    validateFields();
    updateControls();
  }

  private void initializeControls() {
    // Set the project field if we have one set
    if (project != null) {
      projectText.setText(project.getName());

      // Initialize the app ID and version
      if (GaeNature.isGaeProject(project)) {
        GaeProject gaeProject = GaeProject.create(project);
        assert (gaeProject != null);

        appId = gaeProject.getAppId();
        appVersion = gaeProject.getAppVersion();
        launchBrowserButton.setSelection(GaeProjectProperties.getGaeLaunchAppInBrowser(project));
      }
    }
  }

  private void updateControls() {
    projectPropertiesLink.setEnabled(project != null);
    deployTree.setTreeContents(project);
  }

  private void updateStatus(IStatus status) {
    boolean allFieldsValid = false;

    if (status.getSeverity() == IStatus.OK && status.getMessage().length() == 0) {
      String msg = "Ready to deploy application '" + appId + "', version "
          + appVersion;

      status = new Status(IStatus.INFO, AppEngineCorePlugin.PLUGIN_ID, msg);
      allFieldsValid = true;
    }

    if (status.getSeverity() == IStatus.INFO
        || status.getSeverity() == IStatus.WARNING) {
      allFieldsValid = true;
    }

    this.setMessage(status.getMessage(), convertSeverity(status));
    deployButton.setEnabled(allFieldsValid);
  }

  private void updateStatus(IStatus[] status) {
    updateStatus(getMostImportantStatus(status));
  }

  private void validateFields() {
    IStatus projectStatus = validateProject();
    // don't need to call validateDeployTree() here, see addEventHandlers()
    updateStatus(new IStatus[]{projectStatus});
  }

  private IStatus validateProject() {
    IProject originalProject = project;
    project = null;

    String projectName = projectText.getText().trim();
    if (projectName.length() == 0) {
      return StatusUtilities.newOkStatus("Enter the project name",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    IProject enteredProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    if (!enteredProject.exists()) {
      return StatusUtilities.newErrorStatus("Project does not exist",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    if (!enteredProject.isOpen()) {
      return StatusUtilities.newErrorStatus("Project is not open",
          AppEngineCorePlugin.PLUGIN_ID);
    }

    if (!GaeNature.isGaeProject(enteredProject)) {
      return StatusUtilities.newErrorStatus(
          (projectName + " is not an App Engine project"),
          AppEngineCorePlugin.PLUGIN_ID);
    }

    // Update the project now that we know we have an App Engine nature
    project = enteredProject;

    // If we've changed projects, update the cached app ID and version
    if (!enteredProject.equals(originalProject)) {
      GaeProject gaeProject = GaeProject.create(enteredProject);
      appId = gaeProject.getAppId();
      appVersion = gaeProject.getAppVersion();
    }

    // Validate that the project has an app ID and version
    if (appId == null || appId.length() == 0) {
      return StatusUtilities.newErrorStatus(
          (projectName + " does not have an application ID.\nClick the project settings link below to set it."),
          AppEngineCorePlugin.PLUGIN_ID);
    }
    if (appVersion == null || appVersion.length() == 0) {
      return StatusUtilities.newErrorStatus(
          (projectName + " does not have a version.\nClick the project settings link below to set it."),
          AppEngineCorePlugin.PLUGIN_ID);
    }

    return OK_STATUS;
  }
}
