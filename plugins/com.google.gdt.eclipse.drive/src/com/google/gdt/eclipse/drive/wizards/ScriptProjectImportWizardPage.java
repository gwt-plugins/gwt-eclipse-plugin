/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.drive.wizards;

import com.google.common.base.Preconditions;
import com.google.gdt.eclipse.core.jobs.BlockingCallableWithProgress;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.driveapi.DriveQueries;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;
import com.google.gdt.eclipse.drive.images.ImageKeys;
import com.google.gdt.eclipse.drive.model.FolderTree;
import com.google.gdt.eclipse.drive.model.FolderTreeContentProvider;
import com.google.gdt.eclipse.drive.model.FolderTreeLabelProvider;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.dialogs.FileSystemSelectionArea;
import org.eclipse.ui.internal.ide.dialogs.IDEResourceInfoUtils;
import org.eclipse.ui.internal.ide.filesystem.FileSystemConfiguration;
import org.eclipse.ui.internal.ide.filesystem.FileSystemSupportRegistry;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;


/**
 * The wizard page used by {@link ScriptProjectImportWizard}.
 */
//We make extensive use of the restricted package org.eclipse.ui.internal.ide.
@SuppressWarnings("restriction")
public class ScriptProjectImportWizardPage extends WizardPage {

  private static final int GET_FOLDER_TREE_CANCELLATION_POLLING_INTERVAL_IN_MILLIS = 100;
  
  private static final Image SCRIPT_PROJECT_IMAGE =
      DrivePlugin.getDefault().getImage(ImageKeys.APPS_SCRIPT_PROJECT_ICON);

  private static final int SIZING_TEXT_FIELD_WIDTH = 250;
  
  /**
   * A container that can be referenced by a final variable, so that its contents can be set in an
   * inner class (used in {@link #getFolderTreeWithProgress()}).
   */
  private static class FolderTreeHolder {
    public FolderTree value;
  }

  private WorkingSetGroup workingSetGroup;
  private Label locationLabel;
  private Text locationPathField;
  private Button browseButton;
  private String userPath;
  private Button useDefaultsButton;
  private IProject currentProject;
  private FileSystemSelectionArea fileSystemSelectionArea;
  private String initialProjectFieldValue;
  private ISelection previousSelection;
  private Text projectNameField;
  private FolderTree.FolderTreeLeaf selectedDriveProject;

  private final Listener nameModifyListener =
      new Listener() {
        @Override public void handleEvent(Event e) {
          setLocationForSelection();
          setPageComplete(selectedDriveProject != null && validatePage());
        }
      };

  public ScriptProjectImportWizardPage(String pageName) {
    super(Preconditions.checkNotNull(pageName));
    userPath = "";
    setPageComplete(false);
    setTitle(pageName);
    setDescription("Import an Apps Script project from Drive into the workspace");
  }

  /**
   * Check if the entry in the widget location is valid. If it is valid return null. Otherwise
   * return a string that indicates the problem.
   * 
   * @return String
   */
  public String checkValidLocation() {

    String locationFieldContents = locationPathField.getText();
    if (locationFieldContents.length() == 0) {
      return "Project location directory must be specified";
    }

    URI newPath = getProjectLocationURI();
    if (newPath == null) {
      return IDEWorkbenchMessages.ProjectLocationSelectionDialog_locationError;
    }

    if (currentProject != null) {
      URI projectPath = currentProject.getLocationURI();
      if (projectPath != null && URIUtil.equals(projectPath, newPath)) {
        return IDEWorkbenchMessages.ProjectLocationSelectionDialog_locationIsSelf;
      }
    }

    if (!useDefaultsButton.getSelection()) {
      IStatus locationStatus = ResourcesPlugin.getWorkspace().validateProjectLocationURI(
          currentProject, newPath);

      if (!locationStatus.isOK()) {
        return locationStatus.getMessage();
      }
    }

    return null;
  }

  @Override
  public void createControl(Composite parent) {
    Composite root = new Composite(parent, SWT.NULL);
    setControl(root);
    FolderTree tree = null;
    try {
      tree = getFolderTreeWithProgress();
    } catch (InterruptedException e) {
      setErrorMessage("The search for Apps Script projects in Drive was interrupted.");
      return;
    }
    if (tree == null) {
      setErrorMessage(
          "Error retrieving Apps Script project from Drive; see Error Log for details.");
    } else {
      populateWithNormalContent(root, tree);
    }
  }
  
  @Nullable
  private FolderTree getFolderTreeWithProgress() throws InterruptedException {
    final FolderTreeHolder folderTreeHolder = new FolderTreeHolder();
    IRunnableContext context = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    // Log on now, if necessary, in the UI thread, so the thread run below will not try to access
    // login plugin UI widgets:
    DriveServiceFacade.get().ensureConnection();
    try {
      context.run(
          true, false,
          new IRunnableWithProgress() {
            @Override public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
              ((ScriptProjectImportWizard) getWizard()).setCurrentMonitor(monitor);
              monitor.beginTask(
                  "Looking for Apps Script projects in Drive...", IProgressMonitor.UNKNOWN);
              try {
                BlockingCallableWithProgress<FolderTree> callable =
                    new BlockingCallableWithProgress<FolderTree>(
                        new Callable<FolderTree>() {
                          @Override public FolderTree call() throws Exception {
                            return
                                DriveServiceFacade.get().getFolderTree(
                                    DriveQueries.mimeTypeQuery(
                                        DriveQueries.SCRIPT_PROJECT_MIME_TYPE, true));
                          }
                        },
                        GET_FOLDER_TREE_CANCELLATION_POLLING_INTERVAL_IN_MILLIS);
                folderTreeHolder.value = callable.call(monitor);
              } finally {
                ((ScriptProjectImportWizard) getWizard()).setCurrentMonitor(null);
                monitor.done();
              }
            }
          });
      return folderTreeHolder.value; // null if an exception was thrown
    } catch (InvocationTargetException e) {
      DrivePlugin.logError("Error retrieving App Script projects from Drive", e.getCause());
      return null;
    }
  }
  
  private void populateWithNormalContent(Composite root, FolderTree driveTree) {
    initializeDialogUnits(root.getParent());

    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        root, "org.eclipse.ui.ide.new_project_wizard_page_context");

    root.setLayout(new GridLayout(1, false));
    root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    ScrolledComposite treeViewerContainer = new ScrolledComposite(root, SWT.NONE);
    treeViewerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    treeViewerContainer.setExpandHorizontal(true);
    treeViewerContainer.setExpandVertical(true);
    
    final TreeViewer treeViewer = new TreeViewer(treeViewerContainer, SWT.BORDER);
    Tree tree = treeViewer.getTree();
    treeViewerContainer.setContent(tree);
    treeViewerContainer.setMinSize(tree.computeSize(SWT.DEFAULT, getPreferredTreeHeight()));
    treeViewer.setContentProvider(new FolderTreeContentProvider());
    treeViewer.setLabelProvider(new FolderTreeLabelProvider(SCRIPT_PROJECT_IMAGE));
    treeViewer.setInput(driveTree.addDummyParent());
    
    treeViewer.addSelectionChangedListener(
        new ISelectionChangedListener() {
          @Override public void selectionChanged(SelectionChangedEvent event) {
            ISelection selection = event.getSelection();
            if (selection.isEmpty()) {
              return;
            }
            if (!(selection instanceof IStructuredSelection)) {
              return; // should never happen
            }
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            FolderTree selectedNode = (FolderTree) structuredSelection.getFirstElement();
            if (selectedNode instanceof FolderTree.FolderTreeLeaf) {
              projectNameField.setText(selectedNode.getTitle());
              selectedDriveProject = (FolderTree.FolderTreeLeaf) selectedNode;
              previousSelection = selection;
              setPageComplete(validatePage());
            } else {
              // Undo an attempt to select a non-leaf:
              selectedDriveProject = null;
              treeViewer.setSelection(previousSelection, true);
            }
          }
        });
    treeViewer.expandAll();

    createProjectNameGroup(root);
    createLocationGroup(root);
    if (initialProjectFieldValue != null) {
      updateProjectName(initialProjectFieldValue);
    }

    // Scale the button based on the rest of the dialog
    setButtonLayoutData(getBrowseButton());

    setPageComplete(false);
    // Show description on opening
    setErrorMessage(null);
    setMessage(null);
    Dialog.applyDialogFont(root);
  }

  private static int getPreferredTreeHeight() {
    Rectangle displayBounds = Display.getCurrent().getBounds();
    return displayBounds.height / 2;
  }

  /**
   * Create a working set group for this page. This method can only be called
   * once.
   * 
   * @param composite the composite in which to create the group
   * @param selection the current workbench selection
   * @param supportedWorkingSetTypes an array of working set type IDs that will
   *          restrict what types of working sets can be chosen in this group
   * @return the created group. If this method has been called previously the
   *         original group will be returned.
   * @since 3.4
   */
  public WorkingSetGroup createWorkingSetGroup(Composite composite,
      IStructuredSelection selection, String[] supportedWorkingSetTypes) {
    if (workingSetGroup != null) {
      return workingSetGroup;
    }
    workingSetGroup = new WorkingSetGroup(composite, selection,
        supportedWorkingSetTypes);
    return workingSetGroup;
  }

  /**
   * Return the browse button. Usually referenced in order to set the layout
   * data for a dialog.
   * 
   * @return Button
   */
  public Button getBrowseButton() {
    return browseButton;
  }

  /**
   * Returns the current project location path as entered by the user, or its
   * anticipated initial value. Note that if the default has been returned the
   * path in a project description used to create a project should not be set.
   * 
   * @return the project location path or its anticipated initial value.
   */
  public IPath getLocationPath() {
    return new Path(getProjectLocation());
  }

  /**
   * /** Returns the current project location URI as entered by the user, or
   * <code>null</code> if a valid project location has not been entered.
   * 
   * @return the project location URI, or <code>null</code>
   * @since 3.2
   */
  public URI getLocationURI() {
    return getProjectLocationURI();
  }

  /**
   * Creates a project resource handle for the current project name field value.
   * The project handle is created relative to the workspace root.
   * <p>
   * This method does not create the project resource; this is the
   * responsibility of <code>IProject::create</code> invoked by the new project
   * resource wizard.
   * </p>
   * 
   * @return the new project resource handle
   */
  public IProject getProjectHandle() {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectNameField == null ? initialProjectFieldValue : projectNameField.getText().trim());
  }

  /**
   * Return the location for the project. If we are using defaults then return
   * the workspace root so that core creates it with default values.
   * 
   * @return String
   */
  public String getProjectLocation() {
    if (useDefaultsButton.getSelection()) {
      return Platform.getLocation().toOSString();
    }
    return locationPathField.getText();
  }
  
  /**
   * @return the text in the project-name field
   */
  public String getProjectName() {
    return projectNameField.getText();
  }

  /**
   * Get the URI for the location field if possible.
   * 
   * @return URI or <code>null</code> if it is not valid.
   */
  public URI getProjectLocationURI() {

    FileSystemConfiguration configuration = getSelectedConfiguration();
    if (configuration == null) {
      return null;
    }

    return configuration.getContributor().getURI(locationPathField.getText());

  }
  
  /**
   * @return
   *     the {@link FolderTree.FolderTreeLeaf} for the Drive Apps Script project currently selected
   *     in this page, or {@code null} if no project is currently selected
   */
  @Nullable
  public FolderTree.FolderTreeLeaf getSelectedDriveProject() {
    return selectedDriveProject;
  }

  /**
   * Return the selected working sets, if any. If this page is not configured to
   * interact with working sets this will be an empty array.
   * 
   * @return the selected working sets
   * @since 3.4
   */
  public IWorkingSet[] getSelectedWorkingSets() {
    return workingSetGroup == null ? new IWorkingSet[0]
        : workingSetGroup.getSelectedWorkingSets();
  }

  /**
   * Sets the initial project name that this page will use when created. The
   * name is ignored if the createControl(Composite) method has already been
   * called. Leading and trailing spaces in the name are ignored. Providing the
   * name of an existing project will not necessarily cause the wizard to warn
   * the user. Callers of this method should first check if the project name
   * passed already exists in the workspace.
   * 
   * @param name initial project name for this page
   * 
   * @see IWorkspace#validateName(String, int)
   * 
   */
  public void setInitialProjectName(String name) {
    if (name == null) {
      initialProjectFieldValue = null;
    } else {
      initialProjectFieldValue = name.trim();
      updateProjectName(initialProjectFieldValue);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible && projectNameField != null) {
      projectNameField.setFocus();
    }
  }

  /**
   * Set the text to the default or clear it if not using the defaults.
   * 
   * @param newName the name of the project to use. If <code>null</code> use the
   *          existing project name.
   */
  public void updateProjectName(String newName) {
    if (useDefaultsButton.getSelection()) {
      locationPathField.setText(TextProcessor.process(getDefaultPathDisplayString()));
    }
  }

  /**
   * Returns the useDefaults.
   * 
   * @return boolean
   */
  public boolean useDefaults() {
    return useDefaultsButton.getSelection();
  }

  /**
   * Returns whether this page's controls currently all contain valid values.
   * 
   * @return <code>true</code> if all controls are valid, and <code>false</code>
   *         if at least one is invalid
   */
  protected boolean validatePage() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    String projectFieldContents =
        projectNameField == null ? "" : projectNameField.getText().trim();
    if (projectFieldContents.equals("")) {
      setErrorMessage(null);
      setMessage("Project name must be specified");
      return false;
    }

    IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
    if (!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      return false;
    }

    IProject handle = getProjectHandle();
    if (handle.exists()) {
      setErrorMessage("A project with that name already exists in the workspace.");
      return false;
    }

    IProject project =
        ResourcesPlugin.getWorkspace().getRoot().getProject(
            projectNameField == null ? "" : projectNameField.getText().trim());
    currentProject = project;

    String validLocationMessage = checkValidLocation();
    if (validLocationMessage != null) { // there is no destination location given
      setErrorMessage(validLocationMessage);
      return false;
    }

    setErrorMessage(null);
    setMessage(null);
    return true;
  }

  /**
   * Set the location to the default location if we are set to useDefaults.
   */
  void setLocationForSelection() {
    updateProjectName(projectNameField == null ? ""
        : projectNameField.getText().trim());
  }

  /**
   * Create the file system selection area.
   * 
   * @param composite
   */
  private void createFileSystemSelection(Composite composite) {

    // Always use the default if that is all there is.
    if (FileSystemSupportRegistry.getInstance().hasOneFileSystem()) {
      return;
    }

    fileSystemSelectionArea = new FileSystemSelectionArea();
    fileSystemSelectionArea.createContents(composite);
  }

  /**
   * Create the contents of the receiver.
   * 
   * @param composite
   * @param defaultEnabled
   */
  private void createLocationGroup(Composite composite) {

    int columns = 4;

    // project specification group
    Composite projectGroup = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = columns;
    projectGroup.setLayout(layout);
    projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    useDefaultsButton = new Button(projectGroup, SWT.CHECK | SWT.RIGHT);
    useDefaultsButton.setText("Use &default location");
    useDefaultsButton.setSelection(true);
    GridData buttonData = new GridData();
    buttonData.horizontalSpan = columns;
    useDefaultsButton.setLayoutData(buttonData);

    // location label
    locationLabel = new Label(projectGroup, SWT.NONE);
    locationLabel.setText("&Location:");

    // project location entry field
    locationPathField = new Text(projectGroup, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = SIZING_TEXT_FIELD_WIDTH;
    data.horizontalSpan = 2;
    locationPathField.setLayoutData(data);

    // browse button
    browseButton = new Button(projectGroup, SWT.PUSH);
    browseButton.setText("B&rowse...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent event) {
        handleLocationBrowseButtonPressed();
      }
    });

    createFileSystemSelection(projectGroup);
    locationPathField.setText(TextProcessor.process(getDefaultPathDisplayString()));
    locationPathField.addModifyListener(new ModifyListener() {
      @Override public void modifyText(ModifyEvent e) {
        reportError(checkValidLocation(), false);
      }
    });

    useDefaultsButton.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        boolean useDefaults = useDefaultsButton.getSelection();

        if (useDefaults) {
          userPath = locationPathField.getText();
          locationPathField.setText(TextProcessor.process(getDefaultPathDisplayString()));
        } else {
          locationPathField.setText(TextProcessor.process(userPath));
        }
        String error = checkValidLocation();
        reportError(
            error,
            error != null
                && error.equals("Project location directory must be specified"));
        setUserAreaEnabled(!useDefaults);
      }
    });
    setUserAreaEnabled(false);
  }

  /**
   * Creates the project name specification controls.
   * 
   * @param parent the parent composite
   */
  private final void createProjectNameGroup(Composite parent) {
    // project specification group
    Composite projectGroup = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    projectGroup.setLayout(layout);
    projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // new project label
    Label projectLabel = new Label(projectGroup, SWT.NONE);
    projectLabel.setText("&Project name:");
    projectLabel.setFont(parent.getFont());

    // new project name entry field
    projectNameField = new Text(projectGroup, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = SIZING_TEXT_FIELD_WIDTH;
    projectNameField.setLayoutData(data);
    projectNameField.setFont(parent.getFont());

    // Set the initial value first before listener
    // to avoid handling an event during the creation.
    if (initialProjectFieldValue != null) {
      projectNameField.setText(initialProjectFieldValue);
    }
    projectNameField.addListener(SWT.Modify, nameModifyListener);
  }

  /**
   * Return the path we are going to display. If it is a file URI then remove
   * the file prefix.
   * 
   * @return String
   */
  private String getDefaultPathDisplayString() {
    return
        ResourcesPlugin.getWorkspace().getRoot().getLocation() + "/" + projectNameField.getText();
  }

  /**
   * Return the path on the location field.
   * 
   * @return the path or the field's text if the path is invalid
   */
  private String getPathFromLocationField() {
    URI fieldURI;
    try {
      fieldURI = new URI(locationPathField.getText());
    } catch (URISyntaxException e) {
      return locationPathField.getText();
    }
    String path = fieldURI.getPath();
    return path != null ? path : locationPathField.getText();
  }

  private IDialogSettings getProjectAreaDialogSettings() {
    IDialogSettings ideDialogSettings = IDEWorkbenchPlugin.getDefault().getDialogSettings();
    IDialogSettings result = ideDialogSettings.getSection(getClass().getName());
    if (result == null) {
      result = ideDialogSettings.addNewSection(getClass().getName());
    }
    return result;
  }

  /**
   * Return the selected contributor
   * 
   * @return FileSystemConfiguration or <code>null</code> if it cannot be
   *         determined.
   */
  private FileSystemConfiguration getSelectedConfiguration() {
    if (fileSystemSelectionArea == null) {
      return FileSystemSupportRegistry.getInstance().getDefaultConfiguration();
    }

    return fileSystemSelectionArea.getSelectedConfiguration();
  }

  /**
   * Open an appropriate directory browser
   */
  private void handleLocationBrowseButtonPressed() {

    String selectedDirectory = null;
    String dirName = getPathFromLocationField();

    if (!dirName.equals(IDEResourceInfoUtils.EMPTY_STRING)) {
      IFileInfo info;
      info = IDEResourceInfoUtils.getFileInfo(dirName);

      if (info == null || !(info.exists())) {
        dirName = IDEResourceInfoUtils.EMPTY_STRING;
      }
    } else {
      String value = getProjectAreaDialogSettings().get("OUTSIDE_LOCATION");
      if (value != null) {
        dirName = value;
      }
    }

    FileSystemConfiguration config = getSelectedConfiguration();
    if (config == null
        || config.equals(FileSystemSupportRegistry.getInstance().getDefaultConfiguration())) {
      DirectoryDialog dialog = new DirectoryDialog(
          locationPathField.getShell(), SWT.SHEET);
      dialog.setMessage(IDEWorkbenchMessages.ProjectLocationSelectionDialog_directoryLabel);

      dialog.setFilterPath(dirName);

      selectedDirectory = dialog.open();

    } else {
      URI uri = getSelectedConfiguration().getContributor().browseFileSystem(
          dirName, browseButton.getShell());
      if (uri != null) {
        selectedDirectory = uri.toString();
      }
    }

    if (selectedDirectory != null) {
      locationPathField.setText(TextProcessor.process(selectedDirectory));
      getProjectAreaDialogSettings().put("OUTSIDE_LOCATION", selectedDirectory);
    }
  }

  private void reportError(String errorMessage, boolean infoOnly) {
    if (infoOnly) {
      setMessage(errorMessage, IStatus.INFO);
      setErrorMessage(null);
    } else {
      setErrorMessage(errorMessage);
    }
    boolean valid = errorMessage == null;
    if (valid) {
      valid = validatePage();
    }

    setPageComplete(valid);
  }

  /**
   * Set the enablement state of the receiver.
   * 
   * @param enabled
   */
  private void setUserAreaEnabled(boolean enabled) {

    locationLabel.setEnabled(enabled);
    locationPathField.setEnabled(enabled);
    browseButton.setEnabled(enabled);
    if (fileSystemSelectionArea != null) {
      fileSystemSelectionArea.setEnabled(enabled);
    }
  }

}
