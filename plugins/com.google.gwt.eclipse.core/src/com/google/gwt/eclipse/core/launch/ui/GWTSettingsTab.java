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

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.LegacyGWTLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.WebAppLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.processors.CodeServerPortArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.NoServerArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.StartupUrlArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.XStartOnFirstThreadArgumentProcessor;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock.IModulesChangeListener;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTProjectsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.Workbench;

import java.util.Collections;
import java.util.List;

/*
 * See ILaunchConfigurationProcessor for details on the UI widget <-> arguments
 * <-> persistent storage synchronization.
 */
/**
 * For webapp launch configuration, tab where you specify GWT options.
 */
@SuppressWarnings("restriction")
public class GWTSettingsTab extends JavaLaunchTab implements
    ILaunchArgumentsContainer.ArgumentsListener,
    UpdateLaunchConfigurationDialogBatcher.Listener {

  /**
   * A factory that returns a GWT settings tab bound to an arguments tab.
   */
  public interface IGWTSettingsTabFactory {
    GWTSettingsTab newInstance(ILaunchArgumentsContainer argsContainer);
  }

  /**
   * Controls how the Development Mode controls intialize themselves from an
   * {@link ILaunchConfiguration}.
   */
  protected class DevelopmentModeBlock {
    private static final String DEVMODE_GROUP_TITLE = "Development Mode";

    private final Group devModeGroup;
    private final ComboViewer logLevelComboViewer;

    private final Text serverPortText;
    private final Button autoPortSelectionButton;

    public DevelopmentModeBlock(Composite parent) {
      devModeGroup = SWTFactory.createGroup(parent, DEVMODE_GROUP_TITLE, 2, 1,
          GridData.FILL_HORIZONTAL);

      // Log level
      SWTFactory.createLabel(devModeGroup, "Log level:", 1);
      logLevelComboViewer = new ComboViewer(devModeGroup, SWT.READ_ONLY);
      logLevelComboViewer.setContentProvider(new ArrayContentProvider());
      logLevelComboViewer.setLabelProvider(new DefaultComboLabelProvider());
      logLevelComboViewer.setInput(LogLevelArgumentProcessor.LOG_LEVELS);

      logLevelComboViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          updateLaunchConfigurationDialog();
        }
      });

      Label serverPortLabel = new Label(devModeGroup, SWT.NONE);
      final GridData serverPortLabelGridData = new GridData();

      serverPortLabel.setLayoutData(serverPortLabelGridData);
      serverPortLabel.setText("Code Server Port:");

      Composite codeServerPortComposite = new Composite(devModeGroup, SWT.NONE);
      codeServerPortComposite.setLayout(new GridLayout(2, false));
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 1;
      codeServerPortComposite.setLayoutData(gd);

      serverPortText = new Text(codeServerPortComposite, SWT.BORDER);
      final GridData serverPortTextGridData = new GridData(SWT.FILL,
          SWT.CENTER, false, false);
      serverPortTextGridData.widthHint = 75;
      serverPortText.setLayoutData(serverPortTextGridData);
      serverPortText.setTextLimit(5);
      serverPortText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      autoPortSelectionButton = new Button(codeServerPortComposite, SWT.CHECK);
      autoPortSelectionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
          true, false));
      autoPortSelectionButton.setText("Automatically select an unused port");
      autoPortSelectionButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnabledState();
          updateLaunchConfigurationDialog();
        }
      });
    }

    public String getCodeServerPort() {
      return serverPortText.getText();
    }

    public boolean getCodeServerPortAuto() {
      return autoPortSelectionButton.getSelection();
    }

    public String getLogLevel() {
      StructuredSelection logLevelSelection = (StructuredSelection) logLevelComboViewer.getSelection();
      return logLevelSelection.getFirstElement() != null
          ? logLevelSelection.getFirstElement().toString()
          : LogLevelArgumentProcessor.DEFAULT_LOG_LEVEL;
    }

    public void initializeFrom(ILaunchConfiguration config)
        throws CoreException {

      SWTUtilities.setText(devModeGroup, DEVMODE_GROUP_TITLE);

      logLevelComboViewer.setSelection(new StructuredSelection(
          GWTLaunchConfiguration.getLogLevel(config)));

      serverPortText.setText(GWTLaunchConfiguration.getCodeServerPort(config));

      autoPortSelectionButton.setSelection(GWTLaunchConfiguration.getCodeServerPortAuto(config));
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      // Save the log level
      GWTLaunchConfigurationWorkingCopy.setLogLevel(configuration,
          getLogLevel());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new LogLevelArgumentProcessor(), configuration);

      GWTLaunchConfigurationWorkingCopy.setCodeServerPort(configuration,
          getCodeServerPort());

      GWTLaunchConfigurationWorkingCopy.setCodeServerPortAuto(configuration,
          getCodeServerPortAuto());

      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new CodeServerPortArgumentProcessor(), configuration);
    }

    public void updateEnabledState() {
      serverPortText.setEnabled(!autoPortSelectionButton.getSelection()
          && autoPortSelectionButton.getEnabled());
    }
  }

  /**
   * Logical grouping of the URL selection controls.
   */
  protected class UrlSelectionBlock {
    private final Group browserGroup;
    private final Text urlField;
    private final Button useOophmButton;

    public UrlSelectionBlock(Composite parent) {
      browserGroup = SWTFactory.createGroup(parent, "Browsers:", 3, 1,
          GridData.FILL_HORIZONTAL);

      SWTFactory.createLabel(browserGroup, "URL:", 1);
      urlField = SWTFactory.createSingleText(browserGroup, 1);
      urlField.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      Button urlBrowseButton = createPushButton(browserGroup, "&Browse...",
          null);
      urlBrowseButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          try {
            IJavaProject javaProject = getJavaProject();
            if (javaProject == null) {
              MessageDialog.openError(
                  Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
                  "No project found",
                  "Please make sure that this launch configuration has a valid project assigned.");
              return;
            }

            IPath url;
            ILaunchShortcutStrategy strategy;
            if (WebAppUtilities.isWebApp(javaProject.getProject())) {
              strategy = new WebAppLaunchShortcutStrategy();
            } else {
              strategy = new LegacyGWTLaunchShortcutStrategy();
            }

            String urlFromUser = strategy.getUrlFromUser(
                getJavaProject().getProject(), false);

            if (urlFromUser != null) {
              // TODO: Why do we create a new Path object here?
              // Was it to normalize the URL?
              url = new Path(urlFromUser);
              SWTUtilities.setText(urlField, url.toString());
            }
          } catch (CoreException ce) {
            GWTPluginLog.logError(ce);
            MessageDialog.openError(
                Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
                "Error while browsing for URL", ce.getLocalizedMessage());
          }
        }
      });

      useOophmButton = SWTFactory.createCheckButton(browserGroup,
          "Launch URL using OOPHM", null, false, 3);
      useOophmButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      GridData oophmButtonData = new GridData(SWT.BEGINNING, SWT.BEGINNING,
          false, false, 3, 1);
      useOophmButton.setLayoutData(oophmButtonData);
    }

    public String getUrl() {
      return urlField.getText();
    }

    public void initializeFrom(ILaunchConfiguration config)
        throws CoreException {
      IJavaProject javaProject = getJavaProject();
      GWTRuntime runtime = javaProject == null ? null : getRuntime(javaProject);

      boolean hasNoServerArg = NoServerArgumentProcessor.hasNoServerArg(LaunchConfigurationProcessorUtilities.parseProgramArgs(config));
      boolean showStartupUrl = GwtLaunchConfigurationProcessorUtilities.isGwtShell(config)
          || GwtLaunchConfigurationProcessorUtilities.isHostedMode(config)
          || (GwtLaunchConfigurationProcessorUtilities.isDevMode(config) && hasNoServerArg);
      GridData layoutData = (GridData) browserGroup.getLayoutData();
      layoutData.exclude = !showStartupUrl;
      browserGroup.setVisible(showStartupUrl);

      SWTUtilities.setText(urlField,
          GWTLaunchConfiguration.getStartupUrl(config));

      boolean useTransitionalOophm = (runtime != null && runtime.supportsTransitionalOOPHM());

      useOophmButton.setVisible(useTransitionalOophm);
      GridData useOophmButtonLayoutData = (GridData) useOophmButton.getLayoutData();
      useOophmButtonLayoutData.exclude = !useTransitionalOophm;

      useOophmButton.setSelection(GWTLaunchConfiguration.launchWithTransitionalOophm(config));
    }

    public boolean launchWithOophm() {
      return useOophmButton.getSelection();
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      // Save the startup URL
      GWTLaunchConfigurationWorkingCopy.setStartupUrl(configuration,
          getUrl().trim());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new StartupUrlArgumentProcessor(), configuration);

      // Save whether to use OOPHM
      GWTLaunchConfigurationWorkingCopy.setLaunchWithOophm(configuration,
          launchWithOophm());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new XStartOnFirstThreadArgumentProcessor(), configuration);
    }
  }

  /**
   * Provides the labels for the log level and output style combos. The label is
   * calculated by calculating just the first letter of the element.
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

  protected Composite comp;

  /**
   * May be null.
   */
  protected DevelopmentModeBlock developmentModeBlock;

  protected EntryPointModulesSelectionBlock entryPointModulesBlock;

  /**
   * May be null.
   */
  protected UrlSelectionBlock urlSelectionBlock;

  /**
   * May be null.
   */
  protected Button performGwtCompileButton;

  /**
   * See javadoc on
   * {@link com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor}
   * for information about why this is required.
   */
  protected boolean blockUpdateLaunchConfigurationDialog;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher = new UpdateLaunchConfigurationDialogBatcher(
      this);

  private final boolean showUrlSelectionBlock;

  private final boolean showDevelopmentModeBlock;

  private final boolean showPerformGwtCompileSetting;

  public GWTSettingsTab(ILaunchArgumentsContainer argsContainer) {
    this(argsContainer, true, true, false);
  }

  public GWTSettingsTab(ILaunchArgumentsContainer argsContainer,
      boolean showDevelopmentModeBlock, boolean showUrlSelectionBlock,
      boolean showPerformGwtCompileSetting) {
    this.showDevelopmentModeBlock = showDevelopmentModeBlock;
    this.showUrlSelectionBlock = showUrlSelectionBlock;
    this.showPerformGwtCompileSetting = showPerformGwtCompileSetting;

    if (argsContainer != null) {
      registerProgramArgsListener(argsContainer);
    }
  }

  protected GWTSettingsTab() {
    this(null, true, true, false);
  }

  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  public void createControl(Composite parent) {
    comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1,
        GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;
    setControl(comp);

    if (showUrlSelectionBlock) {
      createVerticalSpacer(comp, 1);
      urlSelectionBlock = new UrlSelectionBlock(comp);
    }

    if (showDevelopmentModeBlock) {
      createVerticalSpacer(comp, 1);
      developmentModeBlock = new DevelopmentModeBlock(comp);
    }

    if (showPerformGwtCompileSetting) {
      createVerticalSpacer(comp, 1);
      performGwtCompileButton = SWTFactory.createCheckButton(comp,
          "Perform GWT compile", null, true, 1);
      performGwtCompileButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateLaunchConfigurationDialog();
          updateEnabledState();
        }
      });
    }

    createVerticalSpacer(comp, 1);
    createStartupModuleComponent(comp);
  }

  @Override
  public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
    updateLaunchConfigurationDialogBatcher.deactivatedCalled(workingCopy);

    super.deactivated(workingCopy);
  }

  @Override
  public void dispose() {
    updateLaunchConfigurationDialogBatcher.disposeCalled();

    super.dispose();
  }

  public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
    if (urlSelectionBlock != null) {
      urlSelectionBlock.performApply(configuration);
    }

    if (developmentModeBlock != null) {
      developmentModeBlock.performApply(configuration);
    }

    // Save the entry point modules
    persistModules(configuration, entryPointModulesBlock.getModules());
    LaunchConfigurationProcessorUtilities.updateViaProcessor(
        new ModuleArgumentProcessor(), configuration);

    if (performGwtCompileButton != null) {
      LaunchConfigurationAttributeUtilities.set(configuration,
          SpeedTracerLaunchConfiguration.Attribute.PERFORM_GWT_COMPILE,
          performGwtCompileButton.getSelection());
    }
  }

  @Override
  public Image getImage() {
    return GWTPlugin.getDefault().getImage(GWTImages.GWT_ICON);
  }

  public String getName() {
    return "GWT";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    blockUpdateLaunchConfigurationDialog = true;
    IJavaProject javaProject = null;

    try {
      super.initializeFrom(config);
      try {

        if (urlSelectionBlock != null) {
          urlSelectionBlock.initializeFrom(config);
        }

        if (developmentModeBlock != null) {
          developmentModeBlock.initializeFrom(config);
        }

        javaProject = getJavaProject();

        entryPointModulesBlock.setJavaProject(javaProject);

        // Can't get the project's entry point modules if this launch
        // configuration has not been assigned to a project
        if (javaProject != null) {
          // The default set of entry point modules is the project's
          // set of defined entry point modules (specified in project
          // properties).
          IProject project = javaProject.getProject();
          entryPointModulesBlock.setDefaultModules(ModuleArgumentProcessor.getDefaultModules(
              project, config));

          // Initialize the selected set of entry point modules
          List<String> launchConfigModules = GWTLaunchConfiguration.getEntryPointModules(config);
          entryPointModulesBlock.setModules(launchConfigModules);
        }

        if (performGwtCompileButton != null) {
          boolean performGwtCompile = LaunchConfigurationAttributeUtilities.getBoolean(
              config,
              SpeedTracerLaunchConfiguration.Attribute.PERFORM_GWT_COMPILE);
          performGwtCompileButton.setSelection(performGwtCompile);
        }

      } catch (CoreException e) {
        // Purposely ignored; this happens when the java project does
        // not exist
      }

      maybeGrayControls(javaProject);
      updateEnabledState();

    } finally {
      blockUpdateLaunchConfigurationDialog = false;
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }

    doPerformApply(configuration);
  }

  public void persistFromArguments(List<String> args,
      ILaunchConfigurationWorkingCopy config) {
    String logLevel = LogLevelArgumentProcessor.getValidLogLevel(args);
    GWTLaunchConfigurationWorkingCopy.setLogLevel(config, logLevel);

    try {
      List<String> modules = ModuleArgumentProcessor.getModules(args, config,
          getProject());
      persistModules(config, modules);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not persist entry point modules");
    }

    try {
      String startupUrl = StartupUrlArgumentProcessor.getStartupUrl(args,
          getCurrentLaunchConfiguration());
      GWTLaunchConfigurationWorkingCopy.setStartupUrl(config, startupUrl);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not persist startup URL");
    }

    String port = CodeServerPortArgumentProcessor.getPort(args);
    if (port.equalsIgnoreCase("auto")) {
      GWTLaunchConfigurationWorkingCopy.setCodeServerPortAuto(config, true);
    } else {
      GWTLaunchConfigurationWorkingCopy.setCodeServerPort(config, port);
    }
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  protected void createStartupModuleComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Available Modules:", 3, 1,
        GridData.FILL_BOTH);
    GridLayout groupLayout = (GridLayout) group.getLayout();
    groupLayout.marginBottom = 8;
    group.setLayout(groupLayout);

    IModulesChangeListener listener = new IModulesChangeListener() {
      public void onModulesChanged() {
        updateLaunchConfigurationDialog();
      }
    };
    entryPointModulesBlock = new EntryPointModulesSelectionBlock(listener);
    entryPointModulesBlock.doFillIntoGrid(group, 3);
  }

  protected IJavaProject getJavaProject() throws CoreException {
    return JavaRuntime.getJavaProject(getCurrentLaunchConfiguration());
  }

  /**
   * If this project doesn't have the GWT nature, gray out all the controls
   * recursively.
   */
  protected void maybeGrayControls(IJavaProject javaProject) {

    boolean isGWTProject = javaProject != null
        && GWTNature.isGWTProject(javaProject.getProject());
    String message = isGWTProject
        ? null
        : "GWT is not enabled for this project. You can enable it in the project's properties.";

    setMessage(message);
    SWTUtilities.setEnabledRecursive(comp, isGWTProject);
  }

  protected void persistModules(ILaunchConfigurationWorkingCopy configuration,
      List<String> modules) {
    if (modules.equals(entryPointModulesBlock.getDefaultModules())) {
      GWTLaunchConfigurationWorkingCopy.clearAttribute(configuration,
          GWTLaunchAttributes.ENTRY_POINT_MODULES);
    } else {
      GWTLaunchConfigurationWorkingCopy.setEntryPointModules(configuration,
          modules, Collections.<String>emptyList());
    }
  }

  protected void registerProgramArgsListener(
      ILaunchArgumentsContainer argsContainer) {
    argsContainer.registerProgramArgsListener(this);
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    if (!blockUpdateLaunchConfigurationDialog) {
      updateLaunchConfigurationDialogBatcher.updateLaunchConfigurationDialogCalled();
    }
  }

  private IProject getProject() throws CoreException {
    IJavaProject javaProject = getJavaProject();
    if (javaProject == null || !javaProject.exists()) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Could not get a valid Java project"));
    }

    return javaProject.getProject();
  }

  private GWTRuntime getRuntime(IJavaProject javaProject) {
    try {
      GWTRuntime runtime = GWTRuntime.findSdkFor(javaProject);
      if (runtime == null) {

        if (GWTProjectsRuntime.isGWTRuntimeProject(javaProject)) {
          // Synthesize a contributor runtime if this launch config is
          // based on a GWT Runtime project
          GWTRuntime synthesizedRuntime = GWTProjectsRuntime.syntheziseContributorRuntime();

          if (synthesizedRuntime.validate().isOK()) {
            runtime = synthesizedRuntime;
          }
        }
      }
      return runtime;
    } catch (JavaModelException e) {
      return null;
    }
  }

  private void updateEnabledState() {
    if (performGwtCompileButton != null) {
      entryPointModulesBlock.setEnabled(performGwtCompileButton.getSelection());
    }

    if (developmentModeBlock != null) {
      developmentModeBlock.updateEnabledState();
    }
  }

}
