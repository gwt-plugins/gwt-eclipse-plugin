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

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock.IModulesChangeListener;
import com.google.gwt.eclipse.core.modules.ModuleFile;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Wizard page for selecting the options for a new HTML host page.
 */
@SuppressWarnings("restriction")
public class NewHostPageWizardPage extends WizardPage {

  private class HostPageFieldAdapter implements IDialogFieldListener,
      IModulesChangeListener {

    public void dialogFieldChanged(DialogField field) {
      validateFields();
    }

    public void onModulesChanged() {
      validateFields();
    }
  }

  private class PathFieldAdapter extends HostPageFieldAdapter implements
      IStringButtonAdapter {

    public void changeControlPressed(DialogField field) {
      IPath path = choosePath(new Path(pathField.getText()));
      if (path != null) {
        pathField.setText(path.removeFirstSegments(1).toString());
      }
    }
  }

  private class ProjectFieldAdapter extends HostPageFieldAdapter implements
      IStringButtonAdapter {

    public void changeControlPressed(DialogField field) {
      IJavaProject jproject = chooseProject();
      if (jproject != null) {
        IPath path = jproject.getProject().getFullPath().makeRelative();
        projectField.setText(path.toString());
      }
    }
  }

  private String fileName;

  private StringDialogField fileNameField;

  private SelectionButtonDialogFieldGroup hostPageElementsButtons;

  private IPath hostPagePath;

  private IProject hostPageProject;

  private EntryPointModulesSelectionBlock modulesBlock;

  private IStatus pageStatus;

  private boolean pageVisible;

  private StringButtonDialogField pathField;

  private StringButtonDialogField projectField;

  protected NewHostPageWizardPage() {
    super("newHostPage");
    setTitle("HTML page");
    setDescription("Create a new HTML page.");

    ProjectFieldAdapter projectAdapter = new ProjectFieldAdapter();
    projectField = new StringButtonDialogField(projectAdapter);
    projectField.setDialogFieldListener(projectAdapter);
    projectField.setLabelText("Project:");
    projectField.setButtonLabel("Browse...");

    PathFieldAdapter publicPathAdapter = new PathFieldAdapter();
    pathField = new StringButtonDialogField(publicPathAdapter);
    pathField.setDialogFieldListener(publicPathAdapter);
    pathField.setLabelText("Path:");
    pathField.setButtonLabel("Browse...");

    fileNameField = new StringDialogField();
    fileNameField.setDialogFieldListener(new HostPageFieldAdapter());
    fileNameField.setLabelText("File name:");

    modulesBlock = new EntryPointModulesSelectionBlock("Modules:",
        new HostPageFieldAdapter());

    String[] buttonNames = new String[] {"Support for browser history (Back, Forward, bookmarks)"};
    hostPageElementsButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK,
        buttonNames, 1);
    hostPageElementsButtons.setLabelText("Which elements do you want to include in your page?");
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);
    Composite composite = new Composite(parent, SWT.NONE);
    int columns = 3;
    GridLayout layout = new GridLayout();
    layout.numColumns = columns;
    composite.setLayout(layout);
    setControl(composite);

    createProjectControls(composite, columns);
    createPathControls(composite, columns);

    createSeparator(composite, columns);

    createFileNameControls(composite, columns);
    createModulesComponent(composite, columns);
    createPageElementsControls(composite, columns);
  }

  public String getFileName() {
    return fileName;
  }

  public List<ModuleFile> getModules() {
    List<ModuleFile> modules = new ArrayList<ModuleFile>();

    // Convert fully-qualified module names into actual ModuleFile's
    for (String moduleName : modulesBlock.getModules()) {
      ModuleFile module = (ModuleFile) ModuleUtils.findModule(getJavaProject(),
          moduleName, false);
      if (module != null) {
        modules.add(module);
      }
    }

    return modules;
  }

  public IPath getPath() {
    return hostPagePath;
  }

  public IProject getProject() {
    return hostPageProject;
  }

  public void init(IResource selection) {
    if (initProject(selection)) {
      initPath(selection);
      initModules(selection);
    }

    setPageElementsSelection(true);

    // Validate the initial field values
    validateFields();
  }

  public boolean isHistorySupportIncluded() {
    return hostPageElementsButtons.isSelected(0);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    pageVisible = visible;

    // Wizards are not allowed to start up with an error message
    if (visible) {
      setFocus();

      if (pageStatus.matches(IStatus.ERROR)) {
        StatusInfo status = new StatusInfo();
        status.setError("");
        pageStatus = status;
      }
    }
  }

  private IPath choosePath(IPath initialPath) {
    return HostPagePathSelectionDialog.show(getShell(), hostPageProject,
        initialPath);
  }

  private IJavaProject chooseProject() {
    IJavaProject[] projects;
    try {
      projects = JavaCore.create(Util.getWorkspaceRoot()).getJavaProjects();
    } catch (JavaModelException e) {
      JavaPlugin.log(e);
      projects = new IJavaProject[0];
    }

    // Filter the list to only show GWT projects
    List<IJavaProject> gwtProjects = new ArrayList<IJavaProject>();
    for (IJavaProject project : projects) {
      if (GWTNature.isGWTProject(project.getProject())) {
        gwtProjects.add(project);
      }
    }

    // TODO: refactor this into utility function
    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Choose a project for the new HTML page");
    dialog.setElements(gwtProjects.toArray(new IJavaProject[0]));
    dialog.setInitialSelections(new Object[] {getJavaProject()});
    dialog.setHelpAvailable(false);
    if (dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private void createFileNameControls(Composite composite, int columns) {
    fileNameField.doFillIntoGrid(composite, columns - 1);
    DialogField.createEmptySpace(composite);

    setFieldWidthHint(fileNameField);
    Text text = fileNameField.getTextControl(null);
    LayoutUtil.setHorizontalGrabbing(text);
  }

  private void createModulesComponent(Composite parent, int columns) {
    modulesBlock.doFillIntoGrid(parent, columns);
  }

  private void createPageElementsControls(Composite composite, int nColumns) {
    Control labelControl = hostPageElementsButtons.getLabelControl(composite);
    LayoutUtil.setHorizontalSpan(labelControl, nColumns);

    DialogField.createEmptySpace(composite);

    Control buttonGroup = hostPageElementsButtons.getSelectionButtonsGroup(composite);
    LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
  }

  private void createPathControls(Composite composite, int columns) {
    pathField.doFillIntoGrid(composite, columns);
    setFieldWidthHint(pathField);
  }

  private void createProjectControls(Composite composite, int columns) {
    projectField.doFillIntoGrid(composite, columns);
    setFieldWidthHint(projectField);
  }

  private void createSeparator(Composite composite, int nColumns) {
    (new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite,
        nColumns, convertHeightInCharsToPixels(1));
  }

  private IJavaProject getJavaProject() {
    return JavaCore.create(hostPageProject);
  }

  private boolean initModules(IResource selection) {
    // If the selection was module file, use that as the default
    if (ModuleUtils.isModuleXml(selection)) {
      ModuleFile module = ModuleUtils.create((IFile) selection);
      List<String> moduleNames = Arrays.asList(new String[] {module.getQualifiedName()});
      modulesBlock.setModules(moduleNames);
    } else {
      // Otherwise, include all modules in the project
      modulesBlock.setModules(GWTProjectProperties.getEntryPointModules(hostPageProject));
    }

    modulesBlock.setDefaultModules(GWTProjectProperties.getEntryPointModules(hostPageProject));

    return true;
  }

  private boolean initPath(IResource selection) {
    if (!WebAppUtilities.isWebApp(hostPageProject)) {
      return false;
    }

    IFolder container = null;

    try {
      IFolder warFolder = WebAppUtilities.getWarSrc(hostPageProject);

      // If the selection was a subfolder of war, initialize to that
      if (selection.getType() == IResource.FOLDER) {
        if (warFolder.getFullPath().isPrefixOf(selection.getFullPath())) {
          container = (IFolder) selection;
          return true;
        }
      }

      // Otherwise, use the war folder as the default path
      if (warFolder.exists()) {
        container = warFolder;
        return true;
      }

      return false;

    } finally {
      if (container != null) {
        pathField.setText(container.getFullPath().removeFirstSegments(1).toString());
      }
    }
  }

  private boolean initProject(IResource selection) {
    IProject project = selection.getProject();
    if (project != null && GWTNature.isGWTProject(project)) {
      projectField.setText(project.getFullPath().makeRelative().toString());
      return true;
    }

    return false;
  }

  private void setFieldWidthHint(StringDialogField field) {
    Text text = field.getTextControl(null);
    LayoutUtil.setWidthHint(text, convertWidthInCharsToPixels(40));
  }

  private void setFocus() {
    fileNameField.setFocus();
  }

  private void setPageElementsSelection(boolean historySupport) {
    hostPageElementsButtons.setSelection(0, historySupport);
  }

  private void updateModulesIfProjectChanged() {
    if (hostPageProject != null) {
      if (hostPageProject != null
          && !getJavaProject().equals(modulesBlock.getJavaProject())) {
        // Set the project for the block (needed for adding a module)
        modulesBlock.setJavaProject(getJavaProject());

        // Set the default and initially-selected modules for the block.
        modulesBlock.setDefaultModules(GWTProjectProperties.getEntryPointModules(hostPageProject));
        modulesBlock.setModules(GWTProjectProperties.getEntryPointModules(hostPageProject));
      }
    } else {
      modulesBlock.setJavaProject(null);
      modulesBlock.setDefaultModules(Collections.<String> emptyList());
      modulesBlock.setModules(Collections.<String> emptyList());
    }
  }

  private void updateStatus(IStatus status) {
    pageStatus = status;
    setPageComplete(!status.matches(IStatus.ERROR));
    if (pageVisible) {
      if (status.isOK()) {
        setErrorMessage(null);
        setMessage(null);
      } else {
        StatusUtil.applyToStatusLine(this, status);
      }
    }
  }

  private void updateStatus(IStatus[] status) {
    updateStatus(StatusUtil.getMostSevere(status));
  }

  private void validateFields() {
    IStatus projectStatus = validateProject();
    IStatus moduleStatus = validateModules();
    IStatus pathStatus = validatePath();
    IStatus fileNameStatus = validateFileName();

    updateStatus(new IStatus[] {
        projectStatus, moduleStatus, pathStatus, fileNameStatus});
  }

  private IStatus validateFileName() {
    fileName = null;

    String str = fileNameField.getText().trim();
    if (str.length() == 0) {
      return Util.newErrorStatus("Enter a file name");
    }

    // Validate the file name
    IStatus nameStatus = ResourcesPlugin.getWorkspace().validateName(str,
        IResource.FILE);
    if (nameStatus.matches(IStatus.ERROR)) {
      return Util.newErrorStatus("Invalid file name. {0}",
          nameStatus.getMessage());
    }

    // Make sure the host page doesn't already exist in the public path
    if (hostPagePath != null) {
      IPath htmlFilePath = hostPagePath.append(str).removeFileExtension().addFileExtension(
          ((AbstractNewFileWizard) getWizard()).getFileExtension());
      IFile htmlFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
          htmlFilePath);
      if (htmlFile.exists()) {
        return Util.newErrorStatus("''{0}'' already exists",
            htmlFilePath.toString());
      }
    }

    fileName = str;
    return Status.OK_STATUS;
  }

  private IStatus validateModules() {
    updateModulesIfProjectChanged();

    if (modulesBlock.getModules().isEmpty()) {
      return StatusUtilities.newErrorStatus("Add one or more modules",
          GWTPlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus validatePath() {
    hostPagePath = null;

    pathField.enableButton(hostPageProject != null);

    String str = pathField.getText().trim();
    if (str.length() == 0) {
      return Util.newErrorStatus("Enter the file path");
    }

    if (hostPageProject == null) {
      // The rest of the path validation relies on having a valid project, so if
      // we don't have one, bail out here with an OK (the project's error will
      // supercede this one).
      return Status.OK_STATUS;
    }

    IPath path = new Path(hostPageProject.getName()).append(str).makeAbsolute();
    IStatus pathStatus = ResourcesPlugin.getWorkspace().validatePath(
        path.toString(), IResource.FOLDER);
    if (pathStatus.matches(IStatus.ERROR)) {
      return Util.newErrorStatus("Invalid path. {0}", pathStatus.getMessage());
    }

    // Path is valid
    hostPagePath = path;

    IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
    if (!folder.exists()) {
      return Util.newWarningStatus(MessageFormat.format(
          "The path ''{0}'' does not exist.  It will be created when you click Finish.",
          path.toString()));
    }

    return Status.OK_STATUS;
  }

  private IStatus validateProject() {
    hostPageProject = null;

    String str = projectField.getText().trim();
    if (str.length() == 0) {
      return Util.newErrorStatus("Enter the project name");
    }

    IPath path = new Path(str);
    if (path.segmentCount() != 1) {
      return Util.newErrorStatus("Invalid project path");
    }

    IProject project = Util.getWorkspaceRoot().getProject(str);
    if (!project.exists()) {
      return Util.newErrorStatus("Project does not exist");
    }

    if (!project.isOpen()) {
      return Util.newErrorStatus("Project is not open");
    }

    if (!GWTNature.isGWTProject(project)) {
      return Util.newErrorStatus("Project is not a GWT project");
    }

    hostPageProject = project;
    return Status.OK_STATUS;
  }

}
