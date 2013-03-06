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
package com.google.gwt.eclipse.core.compile.ui;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.platform.shared.ui.IPixelConverter;
import com.google.gdt.eclipse.platform.ui.PixelConverterFactory;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock.IModulesChangeListener;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GwtCapabilityChecker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.service.prefs.BackingStoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gets the parameters for compiling a GWT application.
 * 
 * TODO: do some refactoring to have this share more code with GWTSettingsTab
 */
@SuppressWarnings("restriction")
public class GWTCompileDialog extends TitleAreaDialog {

  /**
   * Interface for extension point for plugins to validate if a project can be
   * GWT compiled by GPE.
   */
  public interface GWTCompileProjectValidator {
    /**
     * If the project cannot be GWT compiled by GPE, return a string explaining 
     * why, otherwise return null.
     */
    String validate(IProject project);
  }

  /**
   * Provides the labels for the log level and output style combos.
   * 
   * TODO: move this to utilities class
   */
  private static class DefaultComboLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      String element2 = (String) element;

      // TODO: this is sensitive to locale. Consider using a helper class
      // to do the label generation
      return element2.toUpperCase().charAt(0)
          + element2.toLowerCase().substring(1);
    }
  }

  private class FieldListener
      implements
        ModifyListener,
        ISelectionChangedListener,
        IModulesChangeListener {
    public void modifyText(ModifyEvent e) {
      fieldChanged();
    }

    public void onModulesChanged() {
      fieldChanged();
    }

    public void selectionChanged(SelectionChangedEvent event) {
      fieldChanged();
    }
  }

  // TODO: move this to utilities class
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

  private Composite advancedContainer;

  private Button applyButton;

  private Button chooseProjectButton;

  private Button compileButton;

  private EntryPointModulesSelectionBlock entryPointModulesBlock;

  private String extraArgs;

  private Text extraArgsText;

  private final FieldListener listener = new FieldListener();

  private String logLevel;

  private ComboViewer logLevelComboViewer;

  private GWTCompileSettings originalSettings;

  private String outputStyle;

  private ComboViewer outputStyleComboViewer;

  private IProject project;

  private Text projectText;

  private String vmArgs;

  private Text vmArgsText;

  public GWTCompileDialog(Shell shell, IProject project) {
    super(shell);
    this.project = project;

    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  public GWTCompileSettings getCompileSettings() {
    GWTCompileSettings settings = new GWTCompileSettings(project);
    settings.setOutputStyle(outputStyle);
    settings.setLogLevel(logLevel);
    settings.setExtraArgs(extraArgs);
    settings.setVmArgs(vmArgs);
    settings.setEntryPointModules(entryPointModulesBlock.getModules());
    return settings;
  }

  public IProject getProject() {
    return project;
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("GWT Compile");
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    compileButton = getButton(IDialogConstants.OK_ID);

    // Re-label the OK button and set it as default
    compileButton.setText("Compile");
    getShell().setDefaultButton(compileButton);

    ((GridLayout) parent.getLayout()).numColumns++;
    applyButton = new Button(parent, SWT.PUSH);
    applyButton.setText("Apply");
    applyButton.setEnabled(false);
    setButtonLayoutData(applyButton);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    setTitle("Compile");
    setTitleImage(GWTPlugin.getDefault().getImage(GWTImages.GWT_COMPILE_LARGE));

    initializeControls();
    addEventHandlers();
    fieldChanged();

    return contents;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    parent = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(parent, SWT.NONE);
    GridData containerGridData = new GridData(GridData.FILL, GridData.FILL,
        true, true, 1, 1);
    container.setLayoutData(containerGridData);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.marginHeight = 8;
    gridLayout.marginWidth = 8;
    container.setLayout(gridLayout);

    // Project field
    SWTFactory.createLabel(container, "Project:", 1);
    projectText = new Text(container, SWT.BORDER);
    projectText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    chooseProjectButton = new Button(container, SWT.NONE);
    chooseProjectButton.setText("Browse...");

    createCompilerAndShellComponent(container);
    createEntryPointModulesComponent(container);
    createAdvancedOptions(container);

    return container;
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

    logLevelComboViewer.addPostSelectionChangedListener(listener);
    outputStyleComboViewer.addPostSelectionChangedListener(listener);
    extraArgsText.addModifyListener(listener);
    vmArgsText.addModifyListener(listener);

    applyButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        try {
          GWTCompileSettings settings = getCompileSettings();
          GWTProjectProperties.setGwtCompileSettings(project, settings);
          originalSettings = settings;
          applyButton.setEnabled(false);
        } catch (BackingStoreException e) {
          GWTPluginLog.logError(e);
        }
      }
    });
  }

  private boolean areMultipleModulesAllowed() {
    try {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject != null) {
        GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
        if (sdk != null) {
          return new GwtCapabilityChecker(sdk).doesCompilerAllowMultipleModules();
        }
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }
    return false;
  }

  private IJavaProject chooseProject() {
    IJavaProject[] javaProjects;

    try {
      javaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      javaProjects = new IJavaProject[0];
    }

    // Filter the list to only show GWT projects
    List<IJavaProject> gwtProjects = new ArrayList<IJavaProject>();
    for (IJavaProject javaProject : javaProjects) {
      if (GWTNature.isGWTProject(javaProject.getProject())) {
        gwtProjects.add(javaProject);
      }
    }

    ILabelProvider labelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        getShell(), labelProvider);
    dialog.setTitle("Project Selection");
    dialog.setMessage("Choose a project to compile");
    dialog.setElements(gwtProjects.toArray(new IJavaProject[0]));
    dialog.setInitialSelections(new Object[]{JavaCore.create(project)});

    dialog.setHelpAvailable(false);
    if (dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private void createAdvancedOptions(Composite parent) {
    IPixelConverter converter = PixelConverterFactory.createPixelConverter(JFaceResources.getDialogFont());

    // Expandable panel for advanced options
    final ExpandableComposite expandPanel = new ExpandableComposite(parent,
        SWT.NONE, ExpandableComposite.TWISTIE
            | ExpandableComposite.CLIENT_INDENT);
    expandPanel.setText("Advanced");
    expandPanel.setExpanded(false);
    expandPanel.setFont(JFaceResources.getFontRegistry().getBold(
        JFaceResources.DIALOG_FONT));
    GridData expandPanelGridData = new GridData(GridData.FILL, GridData.FILL,
        true, false, 3, 1);
    expandPanelGridData.verticalIndent = converter.convertHeightInCharsToPixels(1);
    expandPanel.setLayoutData(expandPanelGridData);
    expandPanel.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        Shell shell = getShell();
        shell.setLayoutDeferred(true); // Suppress redraw flickering

        Point size = shell.getSize();
        int shellHeightDeltaOnExpand = advancedContainer.computeSize(
            SWT.DEFAULT, SWT.DEFAULT).y;
        if (expandPanel.isExpanded()) {
          shell.setSize(size.x, size.y + shellHeightDeltaOnExpand);
        } else {
          shell.setSize(size.x, size.y - shellHeightDeltaOnExpand);
        }
        shell.layout(true, true);

        shell.setLayoutDeferred(false);
      }
    });

    advancedContainer = new Composite(expandPanel, SWT.NONE);
    advancedContainer.setLayoutData(new GridData());
    advancedContainer.setFont(parent.getFont());
    advancedContainer.setLayout(new GridLayout(1, false));
    expandPanel.setClient(advancedContainer);

    // Additional compiler parameters field
    SWTFactory.createLabel(advancedContainer, "Additional compiler arguments:",
        1);
    extraArgsText = SWTUtilities.createMultilineTextbox(advancedContainer,
        SWT.BORDER, false);
    GridData extraArgsGridData = new GridData(GridData.FILL_HORIZONTAL);
    extraArgsGridData.heightHint = converter.convertHeightInCharsToPixels(5);
    extraArgsText.setLayoutData(extraArgsGridData);

    // Additional VM args field
    SWTFactory.createLabel(advancedContainer, "VM arguments:", 1);
    vmArgsText = SWTUtilities.createMultilineTextbox(advancedContainer,
        SWT.BORDER, false);
    GridData vmArgsGridData = new GridData(GridData.FILL_HORIZONTAL);
    vmArgsGridData.heightHint = converter.convertHeightInCharsToPixels(5);
    vmArgsText.setLayoutData(vmArgsGridData);
  }

  private void createCompilerAndShellComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Compiler && Shell", 2, 3,
        GridData.FILL_HORIZONTAL);
    createLogLevelControl(group);
    createOutputStyleControl(group);
  }

  private void createEntryPointModulesComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Entry Point Modules", 3, 3,
        GridData.FILL_BOTH);
    GridLayout groupLayout = (GridLayout) group.getLayout();
    groupLayout.marginBottom = 8;
    group.setLayout(groupLayout);

    entryPointModulesBlock = new EntryPointModulesSelectionBlock(null, listener);
    entryPointModulesBlock.doFillIntoGrid(group, 3);
  }

  private void createLogLevelControl(Composite parent) {
    SWTFactory.createLabel(parent, "Log level:", 1);
    logLevelComboViewer = new ComboViewer(parent, SWT.READ_ONLY);
    logLevelComboViewer.setContentProvider(new ArrayContentProvider());
    logLevelComboViewer.setLabelProvider(new DefaultComboLabelProvider());
    logLevelComboViewer.setInput(GWTLaunchAttributes.LOG_LEVELS);
  }

  private void createOutputStyleControl(Composite parent) {
    SWTFactory.createLabel(parent, "Output style:", 1);
    outputStyleComboViewer = new ComboViewer(parent, SWT.READ_ONLY);
    outputStyleComboViewer.setContentProvider(new ArrayContentProvider());
    outputStyleComboViewer.setLabelProvider(new DefaultComboLabelProvider());
    outputStyleComboViewer.setInput(GWTLaunchAttributes.OUTPUT_STYLES);
  }

  private void fieldChanged() {
    IStatus status = updateFields();
    
    boolean valid = (status.getSeverity() != IStatus.ERROR);
    compileButton.setEnabled(valid);
    
    if (valid) {
      GWTCompileSettings currentSettings = getCompileSettings();
      boolean different = !originalSettings.equals(currentSettings);
      applyButton.setEnabled(different);
    }
  }

  private void initializeControls() {
    // Set the project field if we have one set
    if (project != null) {
      projectText.setText(project.getName());
    }

    // If we have a GWT project, get its saved compilation settings; otherwise
    // just use the defaults settings.
    GWTCompileSettings settings = (project != null)
        ? GWTProjectProperties.getGwtCompileSettings(project)
        : new GWTCompileSettings();

    initializeLogLevel(settings.getLogLevel());
    initializeOutputStyle(settings.getOutputStyle());
    initializeExtraArgs(settings.getExtraArgs());
    initializeVmArgs(settings.getVmArgs());

    originalSettings = settings;
  }

  private void initializeExtraArgs(String args) {
    extraArgsText.setText(args);
  }

  private void initializeLogLevel(String level) {
    logLevelComboViewer.setSelection(new StructuredSelection(level));
  }

  private void initializeOutputStyle(String style) {
    outputStyleComboViewer.setSelection(new StructuredSelection(style));
  }

  private void initializeVmArgs(String vmArgs) {
    vmArgsText.setText(vmArgs);
  }

  private IStatus updateEntryPointModules() {
    updateEntryPointModulesIfProjectChanged();

    if (entryPointModulesBlock.getModules().isEmpty()) {
      return StatusUtilities.newErrorStatus("Add an entry point module",
          GWTPlugin.PLUGIN_ID);
    }

    if (!areMultipleModulesAllowed()
        && entryPointModulesBlock.getModules().size() > 1) {
      return StatusUtilities.newErrorStatus(
          "Projects using GWT 1.5 or lower may only specify one entry point module to compile",
          GWTPlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private void updateEntryPointModulesIfProjectChanged() {
    if (project != null) {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject != null
          && !javaProject.equals(entryPointModulesBlock.getJavaProject())) {
        // Set the project for the block (needed for adding a module)
        entryPointModulesBlock.setJavaProject(javaProject);

        GWTCompileSettings settings = GWTProjectProperties.getGwtCompileSettings(project);
        
        // Set the default and initially-selected modules for the block from
        // the saved settings
        entryPointModulesBlock.setDefaultModules(settings.getEntryPointModules());
        entryPointModulesBlock.setModules(settings.getEntryPointModules());
      }
    } else {
      entryPointModulesBlock.setJavaProject(null);
      entryPointModulesBlock.setDefaultModules(Collections.<String>emptyList());
      entryPointModulesBlock.setModules(Collections.<String>emptyList());
    }
  }

  private IStatus updateExtraArgs() {
    IStatus status = GWTCompileSettings.validateExtraArgs(extraArgsText.getText());
    if (status.getSeverity() != IStatus.ERROR) {
      extraArgs = extraArgsText.getText();
    }

    return status;
  }

  private IStatus updateFields() {
    IStatus projectStatus = updateProjectAndCompileSettings();
    IStatus logLevelStatus = updateLogLevel();
    IStatus outputStyleStatus = updateOutputStyle();
    IStatus entryPointModulesStatus = updateEntryPointModules();
    IStatus extraArgsStatus = updateExtraArgs();
    IStatus vmArgsStatus = updateVmArgs();

    return updateStatus(new IStatus[]{
        projectStatus, logLevelStatus, outputStyleStatus,
        entryPointModulesStatus, extraArgsStatus, vmArgsStatus});
  }

  private IStatus updateLogLevel() {
    logLevel = (String) ((StructuredSelection) logLevelComboViewer.getSelection()).getFirstElement();
    return StatusUtilities.OK_STATUS;
  }

  private IStatus updateOutputStyle() {
    outputStyle = (String) ((StructuredSelection) outputStyleComboViewer.getSelection()).getFirstElement();
    return StatusUtilities.OK_STATUS;
  }

  private IStatus updateProjectAndCompileSettings() {
    String oldProjectName = project != null ? project.getName() : "";
    project = null;

    String projectName = projectText.getText().trim();
    if (projectName.length() == 0) {
      return StatusUtilities.newErrorStatus("Enter the project name",
          GWTPlugin.PLUGIN_ID);
    }

    IProject enteredProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
        projectName);
    if (!enteredProject.exists()) {
      return StatusUtilities.newErrorStatus("Project does not exist",
          GWTPlugin.PLUGIN_ID);
    }

    if (!enteredProject.isOpen()) {
      return StatusUtilities.newErrorStatus("Project is not open",
          GWTPlugin.PLUGIN_ID);
    }

    if (!GWTNature.isGWTProject(enteredProject)) {
      return StatusUtilities.newErrorStatus(projectName
          + " is not a GWT project", GWTPlugin.PLUGIN_ID);
    }

    String validViaExtensionMsg = validateProjectViaExtensions(enteredProject);
    if (validViaExtensionMsg != null) {
      return StatusUtilities.newErrorStatus(validViaExtensionMsg, GWTPlugin.PLUGIN_ID);
    }
    
    // Project is valid (no errors)
    project = enteredProject;

    String newProjectName = project != null ? project.getName() : "";
    // if the project changes, update the settings file we compare to for the
    // apply button
    if (!newProjectName.equals(oldProjectName)) {
      originalSettings = GWTProjectProperties.getGwtCompileSettings(project);
    }

    try {
      if (IMarker.SEVERITY_ERROR == enteredProject.findMaxProblemSeverity(
          IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
        return StatusUtilities.newWarningStatus("The project {0} has errors.",
            GWTPlugin.PLUGIN_ID, enteredProject.getName());
      }
    } catch (CoreException e) {
      GWTPluginLog.logError(e);
    }

    return StatusUtilities.OK_STATUS;
  }

  private IStatus updateStatus(IStatus status) {
    if (status.getSeverity() == IStatus.OK) {
      status = StatusUtilities.newOkStatus(
          "Build the project with the GWT compiler", GWTPlugin.PLUGIN_ID);
    }

    this.setMessage(status.getMessage(), convertSeverity(status));

    return status;
  }

  private IStatus updateStatus(IStatus[] status) {
    return updateStatus(StatusUtilities.getMostImportantStatusWithMessage(status));
  }

  private IStatus updateVmArgs() {
    IStatus status = GWTCompileSettings.validateVmArgs(vmArgsText.getText());
    if (status.getSeverity() != IStatus.ERROR) {
      vmArgs = vmArgsText.getText();
    }

    return status;
  }
  
  private String validateProjectViaExtensions(IProject project) {

    ExtensionQuery<GWTCompileProjectValidator> extQuery = new ExtensionQuery<GWTCompileProjectValidator>(
        GWTPlugin.PLUGIN_ID, "gwtCompileProjectValidator", "class");
    List<ExtensionQuery.Data<GWTCompileProjectValidator>> enablementFinders = extQuery.getData();
    for (ExtensionQuery.Data<GWTCompileProjectValidator> enablementFinder : enablementFinders) {
      String validityString = enablementFinder.getExtensionPointData().validate(
          project);
      if (validityString != null) {
        return validityString; // take first invalid response
      }
    }
    
    return null;
  }
}
