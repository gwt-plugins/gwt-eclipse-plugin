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
package com.google.gdt.eclipse.gph.egit.wizard;

import com.google.gdt.eclipse.gph.egit.EGitCheckoutProviderPlugin;
import com.google.gdt.eclipse.gph.wizards.AbstractWizardPage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.eclipse.ui.statushandlers.StatusManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The WizardProjectsImportPage is the page that allows the user to import projects from a
 * particular location.
 */
public class ImportProjectsWizardPage extends AbstractWizardPage implements IOverwriteQuery {
  /**
   * Class declared public only for test suite.
   */
  public class ProjectRecord {
    File projectSystemFile;

    String projectName;

    Object parent;

    int level;

    boolean hasConflicts;

    IProjectDescription description;

    /**
     * Create a record for a project based on the info in the file.
     *
     * @param file
     */
    ProjectRecord(File file) {
      projectSystemFile = file;
      setProjectName();
    }

    /**
     * @param file The Object representing the .project file
     * @param parent The parent folder of the .project file
     * @param level The number of levels deep in the provider the file is
     */
    ProjectRecord(Object file, Object parent, int level) {
      this.parent = parent;
      this.level = level;
      setProjectName();
    }

    /**
     * Gets the label to be used when rendering this project record in the UI.
     *
     * @return String the label
     * @since 3.4
     */
    public String getProjectLabel() {
      if (description == null) {
        return projectName;
      }

      String path = projectSystemFile.getParent();

      return NLS.bind("{0} ({1})", projectName, path);
    }

    /**
     * Get the name of the project
     *
     * @return String
     */
    public String getProjectName() {
      return projectName;
    }

    /**
     * @return Returns the hasConflicts.
     */
    public boolean hasConflicts() {
      return hasConflicts;
    }

    /**
     * Returns whether the given project description file path is in the default location for a
     * project
     *
     * @param path The path to examine
     * @return Whether the given path is the default location for a project
     */
    private boolean isDefaultLocation(IPath path) {
      // The project description file must at least be within the project,
      // which is within the workspace location
      if (path.segmentCount() < 2) {
        return false;
      }
      return path.removeLastSegments(2).toFile().equals(Platform.getLocation().toFile());
    }

    /**
     * Set the name of the project based on the projectFile.
     */
    private void setProjectName() {
      try {
        // If we don't have the project name try again
        if (projectName == null) {
          IPath path = new Path(projectSystemFile.getPath());
          // if the file is in the default location, use the directory
          // name as the project name
          if (isDefaultLocation(path)) {
            projectName = path.segment(path.segmentCount() - 2);
            description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
          } else {
            description = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
            projectName = description.getName();
          }

        }
      } catch (CoreException e) {
        // no good couldn't get the name
      }
    }
  }

  private final class ProjectLabelProvider extends LabelProvider implements IColorProvider {
    @Override
    public Color getBackground(Object element) {
      return null;
    }

    @Override
    public Color getForeground(Object element) {
      ProjectRecord projectRecord = (ProjectRecord) element;
      if (projectRecord.hasConflicts)
        return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
      return null;
    }

    @Override
    public String getText(Object element) {
      return ((ProjectRecord) element).getProjectLabel();
    }
  }

  /**
   * The name of the folder containing metadata information for the workspace.
   */
  public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

  private EGitCheckoutWizard wizard;

  private CheckboxTreeViewer projectsList;

  private ProjectRecord[] selectedProjects = new ProjectRecord[0];

  private IProject[] wsProjects;

  // The initial path to set
  // private String initialPath;

  // The last selected path to minimize searches
  private File lastPath;

  // The last time that the file or folder at the selected path was modified
  // to mimize searches
  private long lastModified;

  private WorkingSetGroup workingSetGroup;

  private IStructuredSelection currentSelection;

  List<IProject> createdProjects;

  /**
   * Create a new instance of the receiver.
   */
  public ImportProjectsWizardPage(EGitCheckoutWizard wizard) {
    super("checkoutProjectPage");

    this.wizard = wizard;

    setTitle("Import Projects from Git");
    setDescription("Select the projects to import");
    setImageDescriptor(ImageDescriptor.createFromImage(wizard.getDefaultPageImage()));

    setPageComplete(false);
  }

  /**
   * Create the selected projects
   *
   * @return boolean <code>true</code> if all project creations were successful.
   */
  public boolean createProjects() {
    final Object[] selected = projectsList.getCheckedElements();
    createdProjects = new ArrayList<IProject>();
    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
      @Override
      protected void execute(IProgressMonitor monitor) throws InvocationTargetException,
          InterruptedException {
        try {
          monitor.beginTask("", selected.length); //$NON-NLS-1$
          if (monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          for (int i = 0; i < selected.length; i++) {
            createExistingProject((ProjectRecord) selected[i], new SubProgressMonitor(monitor, 1));
          }
        } finally {
          monitor.done();
        }
      }
    };

    // run the new project creation operation
    try {
      getContainer().run(true, true, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      // one of the steps resulted in a core exception
      Throwable t = e.getTargetException();
      String message = "Creation Problems";
      IStatus status;
      if (t instanceof CoreException) {
        status = ((CoreException) t).getStatus();
      } else {
        status = new Status(IStatus.ERROR, EGitCheckoutProviderPlugin.PLUGIN_ID, 1, message, t);
      }
      ErrorDialog.openError(getShell(), message, null, status);
      return false;
    }

    // Adds the projects to the working sets
    addToWorkingSets();

    return true;
  }

  /**
   * Get the array of project records that can be imported from the source workspace or archive,
   * selected by the user. If a project with the same name exists in both the source workspace and
   * the current workspace, then the hasConflicts flag would be set on that project record. Method
   * declared public for test suite.
   *
   * @return ProjectRecord[] array of projects that can be imported into the workspace
   */
  public ProjectRecord[] getProjectRecords() {
    List<ProjectRecord> projectRecords = new ArrayList<ProjectRecord>();
    for (int i = 0; i < selectedProjects.length; i++) {
      if (isProjectInWorkspace(selectedProjects[i].getProjectName())) {
        selectedProjects[i].hasConflicts = true;
      }
      projectRecords.add(selectedProjects[i]);
    }
    return projectRecords.toArray(new ProjectRecord[projectRecords.size()]);
  }

  /**
   * Method used for test suite.
   *
   * @return CheckboxTreeViewer the viewer containing all the projects found
   */
  public CheckboxTreeViewer getProjectsList() {
    return projectsList;
  }

  /**
   * Performs clean-up if the user cancels the wizard without doing anything
   */
  public void performCancel() {

  }

  /**
   * The <code>WizardDataTransfer</code> implementation of this <code>IOverwriteQuery</code> method
   * asks the user whether the existing resource at the given path should be overwritten.
   *
   * @param pathString
   * @return the user's reply: one of <code>"YES"</code>, <code>"NO"</code>, <code>"ALL"</code>, or
   *         <code>"CANCEL"</code>
   */
  @Override
  public String queryOverwrite(String pathString) {

    Path path = new Path(pathString);

    String messageString;
    // Break the message up if there is a file name and a directory
    // and there are at least 2 segments.
    if (path.getFileExtension() == null || path.segmentCount() < 2) {
      messageString =
          NLS.bind("''{0}'' already exists.  Would you like to overwrite it?", pathString);
    } else {
      messageString =
          NLS.bind("Overwrite ''{0}'' in folder ''{1}''?", path.lastSegment(), path
              .removeLastSegments(1).toOSString());
    }

    final MessageDialog dialog =
        new MessageDialog(getContainer().getShell(), "Question", null, messageString,
            MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL,
                IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL,
                IDialogConstants.NO_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL}, 0) {
          @Override
          protected int getShellStyle() {
            return super.getShellStyle() | SWT.SHEET;
          }
        };
    String[] response = new String[] {YES, ALL, NO, NO_ALL, CANCEL};
    // run in syncExec because callback is from an operation,
    // which is probably not running in the UI thread.
    getControl().getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        dialog.open();
      }
    });
    return dialog.getReturnCode() < 0 ? CANCEL : response[dialog.getReturnCode()];
  }

  /**
   * Update the list of projects based on path. Method declared public only for test suite.
   */
  public void updateProjectsList(final File directory) {
    // on an empty path empty selectedProjects
    if (directory == null || !directory.exists()) {
      setMessage("Invalid directory for the repository clone.");
      selectedProjects = new ProjectRecord[0];
      projectsList.refresh(true);
      projectsList.setCheckedElements(selectedProjects);
      setPageComplete(projectsList.getCheckedElements().length > 0);
      lastPath = directory;
      return;
    }

    long modified = directory.lastModified();

    if (directory.equals(lastPath) && lastModified == modified) {
      // since the file/folder was not modified and the path did not
      // change, no refreshing is required
      return;
    }

    lastPath = directory;
    lastModified = modified;

    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Searching for projects", 100);
          selectedProjects = new ProjectRecord[0];
          Collection<File> files = new ArrayList<File>();
          monitor.worked(10);
          if (directory.isDirectory()) {

            if (!collectProjectFilesFromDirectory(files, directory, null, monitor)) {
              return;
            }
            Iterator<File> filesIterator = files.iterator();
            selectedProjects = new ProjectRecord[files.size()];
            int index = 0;
            monitor.worked(50);
            monitor.subTask("Processing results");
            while (filesIterator.hasNext()) {
              File file = filesIterator.next();
              selectedProjects[index] = new ProjectRecord(file);
              index++;
            }
          } else {
            monitor.worked(60);
          }
          monitor.done();
        }

      });
    } catch (InvocationTargetException e) {
      EGitCheckoutProviderPlugin.logError(e.getMessage(), e);
    } catch (InterruptedException e) {
      // Nothing to do if the user interrupts.
    }

    projectsList.refresh(true);
    ProjectRecord[] projects = getProjectRecords();
    boolean displayWarning = false;
    for (int i = 0; i < projects.length; i++) {
      if (projects[i].hasConflicts) {
        displayWarning = true;
        projectsList.setGrayed(projects[i], true);
      } else {
        projectsList.setChecked(projects[i], true);
      }
    }

    if (displayWarning) {
      setMessage("Some projects cannot be imported because they already exist in the workspace",
          WARNING);
    } else {
      setMessage("Select a directory to search for existing Eclipse projects.");
    }
    setPageComplete(projectsList.getCheckedElements().length > 0);
    if (selectedProjects.length == 0) {
      setMessage("No projects are found to import", WARNING);
    }
  }

  @Override
  protected Control createPageContents(Composite parent) {
    initializeDialogUnits(parent);

    Composite workArea = new Composite(parent, SWT.NONE);

    workArea.setLayout(new GridLayout());
    workArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL
        | GridData.GRAB_VERTICAL));

    createProjectsList(workArea);
    createWorkingSetGroup(workArea);

    Dialog.applyDialogFont(workArea);

    return workArea;
  }

  /**
   * Display an error dialog with the specified message.
   *
   * @param message the error message
   */
  protected void displayErrorDialog(String message) {
    MessageDialog.open(MessageDialog.ERROR, getContainer().getShell(), getErrorDialogTitle(),
        message, SWT.SHEET);
  }

  @Override
  protected void enteringPage() {
    File cloneDir = wizard.getRepoCloneDirectory();

    updateProjectsList(cloneDir);
  }

  /**
   * Get the title for an error dialog. Subclasses should override.
   */
  protected String getErrorDialogTitle() {
    return "Internal error";
  }

  private void addToWorkingSets() {
    IWorkingSet[] selectedWorkingSets = workingSetGroup.getSelectedWorkingSets();
    if (selectedWorkingSets == null || selectedWorkingSets.length == 0) {
      return; // no Working set is selected
    }
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for (Iterator<IProject> i = createdProjects.iterator(); i.hasNext();) {
      IProject project = i.next();
      workingSetManager.addToWorkingSets(project, selectedWorkingSets);
    }
  }

  /**
   * Collect the list of .project files that are under directory into files.
   *
   * @param files
   * @param directory
   * @param directoriesVisited Set of canonical paths of directories, used as recursion guard
   * @param monitor The monitor to report to
   * @return boolean <code>true</code> if the operation was completed.
   */
  private boolean collectProjectFilesFromDirectory(Collection<File> files, File directory,
      Set<String> directoriesVisited, IProgressMonitor monitor) {

    if (monitor.isCanceled()) {
      return false;
    }
    monitor.subTask(NLS.bind("Checking: {0}", directory.getPath()));
    File[] contents = directory.listFiles();
    if (contents == null) return false;

    // Initialize recursion guard for recursive symbolic links
    if (directoriesVisited == null) {
      directoriesVisited = new HashSet<String>();
      try {
        directoriesVisited.add(directory.getCanonicalPath());
      } catch (IOException exception) {
        StatusManager.getManager().handle(
            EGitCheckoutProviderPlugin.createStatus(IStatus.ERROR, exception.getLocalizedMessage(),
                exception));
      }
    }

    // first look for project description files
    final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
    for (int i = 0; i < contents.length; i++) {
      File file = contents[i];
      if (file.isFile() && file.getName().equals(dotProject)) {
        files.add(file);
        // don't search sub-directories since we can't have nested
        // projects
        return true;
      }
    }
    // no project description found, so recurse into sub-directories
    for (int i = 0; i < contents.length; i++) {
      if (contents[i].isDirectory()) {
        if (!contents[i].getName().equals(METADATA_FOLDER)) {
          try {
            String canonicalPath = contents[i].getCanonicalPath();
            if (!directoriesVisited.add(canonicalPath)) {
              // already been here --> do not recurse
              continue;
            }
          } catch (IOException exception) {
            StatusManager.getManager().handle(
                EGitCheckoutProviderPlugin.createStatus(IStatus.ERROR,
                    exception.getLocalizedMessage(), exception));

          }
          collectProjectFilesFromDirectory(files, contents[i], directoriesVisited, monitor);
        }
      }
    }
    return true;
  }

  /**
   * Create the project described in record. If it is successful return true.
   *
   * @param record
   * @return boolean <code>true</code> if successful
   * @throws InterruptedException
   */
  private boolean createExistingProject(final ProjectRecord record, IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException {
    String projectName = record.getProjectName();
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject project = workspace.getRoot().getProject(projectName);
    createdProjects.add(project);
    if (record.description == null) {
      // error case
      record.description = workspace.newProjectDescription(projectName);
      IPath locationPath = new Path(record.projectSystemFile.getAbsolutePath());

      // If it is under the root use the default location
      if (Platform.getLocation().isPrefixOf(locationPath)) {
        record.description.setLocation(null);
      } else {
        record.description.setLocation(locationPath);
      }
    } else {
      record.description.setName(projectName);
    }

    try {
      monitor.beginTask("Creating Projects", 100);
      project.create(record.description, new SubProgressMonitor(monitor, 30));
      project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
    } catch (CoreException e) {
      throw new InvocationTargetException(e);
    } finally {
      monitor.done();
    }
    return true;
  }

  /**
   * Create the checkbox list for the found projects.
   *
   * @param workArea
   */
  private void createProjectsList(Composite workArea) {
    Group listComposite = new Group(workArea, SWT.NONE);
    listComposite.setText("Projects to import");
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    // layout.marginWidth = 0;
    layout.makeColumnsEqualWidth = false;
    layout.marginHeight = 10;
    layout.marginWidth = 10;
    listComposite.setLayout(layout);

    listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL
        | GridData.FILL_BOTH));

    projectsList = new CheckboxTreeViewer(listComposite, SWT.BORDER);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.widthHint =
        new PixelConverter(projectsList.getControl()).convertWidthInCharsToPixels(25);
    gridData.heightHint =
        new PixelConverter(projectsList.getControl()).convertHeightInCharsToPixels(10);
    projectsList.getControl().setLayoutData(gridData);
    projectsList.setContentProvider(new ITreeContentProvider() {
      @Override
      public void dispose() {

      }

      @Override
      public Object[] getChildren(Object parentElement) {
        return null;
      }

      @Override
      public Object[] getElements(Object inputElement) {
        return getProjectRecords();
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public boolean hasChildren(Object element) {
        return false;
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    projectsList.setLabelProvider(new ProjectLabelProvider());

    projectsList.addCheckStateListener(new ICheckStateListener() {
      @Override
      public void checkStateChanged(CheckStateChangedEvent event) {
        ProjectRecord element = (ProjectRecord) event.getElement();
        if (element.hasConflicts) {
          projectsList.setChecked(element, false);
        }
        setPageComplete(projectsList.getCheckedElements().length > 0);
      }
    });

    projectsList.setInput(this);
    projectsList.setComparator(new ViewerComparator());
    createSelectionButtons(listComposite);
  }

  /**
   * Create the selection buttons in the listComposite.
   *
   * @param listComposite
   */
  private void createSelectionButtons(Composite listComposite) {
    Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    buttonsComposite.setLayout(layout);

    buttonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

    Button selectAll = new Button(buttonsComposite, SWT.PUSH);
    selectAll.setText("&Select All");
    selectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        for (int i = 0; i < selectedProjects.length; i++) {
          if (selectedProjects[i].hasConflicts) {
            projectsList.setChecked(selectedProjects[i], false);
          } else {
            projectsList.setChecked(selectedProjects[i], true);
          }
        }
        setPageComplete(projectsList.getCheckedElements().length > 0);
      }
    });
    Dialog.applyDialogFont(selectAll);
    setButtonLayoutData(selectAll);

    Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
    deselectAll.setText("&Deselect All");
    deselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {

        projectsList.setCheckedElements(new Object[0]);
        setPageComplete(false);
      }
    });
    Dialog.applyDialogFont(deselectAll);
    setButtonLayoutData(deselectAll);
  }

  /**
   * @param workArea
   */
  private void createWorkingSetGroup(Composite workArea) {
    String[] workingSetIds = new String[] {"org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
        "org.eclipse.jdt.ui.JavaWorkingSetPage"}; //$NON-NLS-1$
    workingSetGroup = new WorkingSetGroup(workArea, currentSelection, workingSetIds);
  }

  /**
   * Retrieve all the projects in the current workspace.
   *
   * @return IProject[] array of IProject in the current workspace
   */
  private IProject[] getProjectsInWorkspace() {
    if (wsProjects == null) {
      wsProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    return wsProjects;
  }

  /**
   * Determine if the project with the given name is in the current workspace.
   *
   * @param projectName String the project name to check
   * @return boolean true if the project with the given name is in this workspace
   */
  private boolean isProjectInWorkspace(String projectName) {
    if (projectName == null) {
      return false;
    }
    IProject[] workspaceProjects = getProjectsInWorkspace();
    for (int i = 0; i < workspaceProjects.length; i++) {
      if (projectName.equals(workspaceProjects[i].getName())) {
        return true;
      }
    }
    return false;
  }
}
