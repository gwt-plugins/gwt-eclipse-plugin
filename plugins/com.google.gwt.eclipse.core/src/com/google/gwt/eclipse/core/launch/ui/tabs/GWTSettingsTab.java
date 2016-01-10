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
package com.google.gwt.eclipse.core.launch.ui.tabs;

import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.ILaunchShortcutStrategy;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.LegacyGWTLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.WebAppLaunchShortcutStrategy;
import com.google.gwt.eclipse.core.launch.processors.DevModeCodeServerPortArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.NoServerArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.RemoteUiArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.StartupUrlArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.SuperDevModeArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.XStartOnFirstThreadArgumentProcessor;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock.IModulesChangeListener;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.resources.GWTImages;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * For webapp launch configuration, tab where you specify GWT options.
 */
@SuppressWarnings("restriction")
public class GWTSettingsTab extends JavaLaunchTab implements ILaunchArgumentsContainer.ArgumentsListener,
    UpdateLaunchConfigurationDialogBatcher.Listener {

  /**
   * A factory that returns a GWT settings tab bound to an arguments tab.
   */
  public interface IGWTSettingsTabFactory {
    GWTSettingsTab newInstance(ILaunchArgumentsContainer argsContainer);
  }

  /**
   * Controls how the GWT Development Mode controls initialize themselves from an
   * {@link ILaunchConfiguration}.
   */
  protected class DevelopmentModeBlock {

    private static final String GROUP_TITLE_DEVMODE = "Classic Development Mode";
    private static final String GROUP_TITLE_SUPERDEVMODE = "Super Development Mode";

    private final Button buttonDevModeAutoPort;
    private final ComboViewer comboDevModeLogLevelViewer;

    private final Group groupDevMode;
    private final Text textDevModeCodeServerPort;

    public DevelopmentModeBlock(Composite parent) {
      groupDevMode = SWTFactory.createGroup(parent, GROUP_TITLE_DEVMODE, 2, 1, GridData.FILL_HORIZONTAL);

      // Log level combo
      SWTFactory.createLabel(groupDevMode, "Log level:", 1);
      comboDevModeLogLevelViewer = new ComboViewer(groupDevMode, SWT.READ_ONLY);
      comboDevModeLogLevelViewer.setContentProvider(new ArrayContentProvider());
      comboDevModeLogLevelViewer.setLabelProvider(new DefaultComboLabelProvider());
      comboDevModeLogLevelViewer.setInput(LogLevelArgumentProcessor.LOG_LEVELS);
      comboDevModeLogLevelViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          updateLaunchConfigurationDialog();
        }
      });

      // CodeServer port
      final GridData serverPortLabelGridData = new GridData();
      Label serverPortLabel = new Label(groupDevMode, SWT.NONE);
      serverPortLabel.setLayoutData(serverPortLabelGridData);
      serverPortLabel.setText("Code Server Port:");

      GridData gridDataCodeServer = new GridData(GridData.FILL_HORIZONTAL);
      gridDataCodeServer.horizontalSpan = 1;
      Composite codeServerPortComposite = new Composite(groupDevMode, SWT.NONE);
      codeServerPortComposite.setLayout(new GridLayout(2, false));
      codeServerPortComposite.setLayoutData(gridDataCodeServer);

      final GridData gridDataTextCodeServer = new GridData(SWT.FILL, SWT.CENTER, false, false);
      gridDataTextCodeServer.widthHint = 75;
      textDevModeCodeServerPort = new Text(codeServerPortComposite, SWT.BORDER);
      textDevModeCodeServerPort.setLayoutData(gridDataTextCodeServer);
      textDevModeCodeServerPort.setTextLimit(5);
      textDevModeCodeServerPort.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      // auto port selection
      buttonDevModeAutoPort = new Button(codeServerPortComposite, SWT.CHECK);
      buttonDevModeAutoPort.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
      buttonDevModeAutoPort.setText("Automatically select an unused port");
      buttonDevModeAutoPort.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnabledState();
          updateLaunchConfigurationDialog();
        }
      });
    }

    public String getDevModeCodeServerPort() {
      return textDevModeCodeServerPort.getText();
    }

    public boolean getDevModeCodeServerPortAuto() {
      return buttonDevModeAutoPort.getSelection();
    }

    public String getLogLevel() {
      StructuredSelection logLevelSelection = (StructuredSelection) comboDevModeLogLevelViewer.getSelection();
      return logLevelSelection.getFirstElement() != null ? logLevelSelection.getFirstElement().toString()
          : LogLevelArgumentProcessor.DEFAULT_LOG_LEVEL;
    }

    public void initializeFrom(ILaunchConfiguration config) throws CoreException {
      SWTUtilities.setText(groupDevMode, GROUP_TITLE_DEVMODE);

      comboDevModeLogLevelViewer.setSelection(new StructuredSelection(GWTLaunchConfiguration.getLogLevel(config)));
      textDevModeCodeServerPort.setText(GWTLaunchConfiguration.getClassicDevModeCodeServerPort(config));
      buttonDevModeAutoPort.setSelection(GWTLaunchConfiguration.getClassicDevModeCodeServerPortAuto(config));
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      // Log level arg
      GWTLaunchConfigurationWorkingCopy.setLogLevel(configuration, getLogLevel());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new LogLevelArgumentProcessor(), configuration);

      // Dev Mode code server port
      GWTLaunchConfigurationWorkingCopy.setClassicDevModeCodeServerPort(configuration, getDevModeCodeServerPort());
      GWTLaunchConfigurationWorkingCopy.setClassicDevModeCodeServerPortAuto(configuration, getDevModeCodeServerPortAuto());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new DevModeCodeServerPortArgumentProcessor(),
          configuration);
    }

    /**
     * Change group title to the dev mode that is selected
     */
    public void setTitle() {
      if (selectionBlock != null && selectionBlock.isVisible() && selectionBlock.isSuperDevModeSelected()) {
        groupDevMode.setText(GROUP_TITLE_SUPERDEVMODE);
      } else {
        groupDevMode.setText(GROUP_TITLE_DEVMODE);
      }
    }

    public void setVisible(boolean visible) {
      groupDevMode.setVisible(visible);
    }

    public void updateEnabledState() {
      textDevModeCodeServerPort.setEnabled(!buttonDevModeAutoPort.getSelection() && buttonDevModeAutoPort.getEnabled());
    }
  }

  protected class SelectionBlock {

    private static final String GROUP_TITLE_SELECTION = "Development Mode";

    private final Button buttonDevMode;
    private final Button buttonSuperDevMode;
    private final Group groupSelection;

    public SelectionBlock(Composite parent) {
      groupSelection = SWTFactory.createGroup(parent, GROUP_TITLE_SELECTION, 3, 1, GridData.FILL_HORIZONTAL);

      buttonSuperDevMode = new Button(groupSelection, SWT.RADIO);
      buttonSuperDevMode.setText("Super Development Mode");

      buttonDevMode = new Button(groupSelection, SWT.RADIO);
      buttonDevMode.setSelection(true);
      buttonDevMode.setText("Classic Development Mode");

      buttonSuperDevMode.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (developmentModeBlock != null) {
            developmentModeBlock.setTitle();
          }
          updateLaunchConfigurationDialog();
        }
      });

      buttonDevMode.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (developmentModeBlock != null) {
            developmentModeBlock.setTitle();
          }
          updateLaunchConfigurationDialog();
        }
      });

      Link link = new Link(groupSelection, SWT.NONE);
      link.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 1, 1));
      link.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          String surl = "http://www.gwtproject.org/usingeclipse.html";
          gotoUrl(surl);
        }
      });
      link.setToolTipText("Find out how to use GWT with Eclipse.");
      link.setText("<a>GWT Eclipse Help</a>");
    }

    public void initializeFrom(ILaunchConfiguration config) throws CoreException {
      SWTUtilities.setText(groupSelection, GROUP_TITLE_SELECTION);

      // on load, set the radio to the correct selection
      boolean enabled = GWTLaunchConfiguration.getSuperDevModeEnabled(config);
      setSdmRadioEnabled(enabled);
    }

    public void setSdmRadioEnabled(boolean enabled) {
      buttonSuperDevMode.setSelection(enabled);
      buttonDevMode.setSelection(!enabled);
    }

    public boolean isSuperDevModeSelected() {
      return buttonSuperDevMode.getSelection();
    }

    public void resetSdmRadio(ILaunchConfigurationWorkingCopy configuration) {
      // reset the selection
      setSdmRadioEnabled(true);
      GWTLaunchConfigurationWorkingCopy.setSuperDevModeEnabled(configuration, true);

      // reset the title
      if (developmentModeBlock != null) {
        developmentModeBlock.setTitle();
      }
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      // super dev mode args
      GWTLaunchConfigurationWorkingCopy.setSuperDevModeEnabled(configuration, selectionBlock.isSuperDevModeSelected());

      // Logic for determining Super Dev Mode is retrieved form working copy in
      // processors. So saving the Super Dev enabled here decouples processors.
      // TODO refer to updateArgumentProcessors todo about
      // LaunchCOnfigurationUpdater.
      try {
        configuration.doSave();
      } catch (CoreException e) {
        MessageDialog.openError(Workbench.getInstance().getActiveWorkbenchWindow().getShell(), "Error saving project",
            "Please try hitting apply agian.");
      }

      // When changing between selections update all the launch config args
      updateArgumentProcessors(configuration);
    }

    public void setVisible(boolean visible) {
      groupSelection.setVisible(visible);
    }

    public boolean isVisible() {
      return groupSelection.isVisible();
    }

    /**
     * When Super Dev Mode or Dev Mode is switched to, rerun the processors. The goal being, clean
     * args if needed.
     *
     * TODO possibly use `new LaunchConfigurationUpdater(configuration, javaProject).update();`
     * instead of this below. Would need to figure out how to import.
     */
    public void updateArgumentProcessors(ILaunchConfigurationWorkingCopy configuration) {
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new SuperDevModeArgumentProcessor(), configuration);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new RemoteUiArgumentProcessor(), configuration);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new LogLevelArgumentProcessor(), configuration);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new DevModeCodeServerPortArgumentProcessor(),
          configuration);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new StartupUrlArgumentProcessor(), configuration);
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new XStartOnFirstThreadArgumentProcessor(),
          configuration);
    }

    /**
     * Open url in default external browser. Used to link to open link to guide.
     */
    private void gotoUrl(String surl) {
      try {
        URL url = new URL(surl);
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
      } catch (PartInitException ex) {
        MessageDialog.openError(Workbench.getInstance().getActiveWorkbenchWindow().getShell(), "URL Error",
            "Error navigating to " + surl);
      } catch (MalformedURLException ex) {
        MessageDialog.openError(Workbench.getInstance().getActiveWorkbenchWindow().getShell(), "URL Error",
            "Error navigating to " + surl);
      }
    }
  }

  /**
   * Logical grouping of the URL selection controls.
   */
  protected class UrlSelectionBlock {

    private final Group browserGroup;
    private final Text urlField;

    public UrlSelectionBlock(Composite parent) {
      browserGroup = SWTFactory.createGroup(parent, "Browsers:", 3, 1, GridData.FILL_HORIZONTAL);

      SWTFactory.createLabel(browserGroup, "URL:", 1);
      urlField = SWTFactory.createSingleText(browserGroup, 1);
      urlField.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      Button urlBrowseButton = createPushButton(browserGroup, "&Browse...", null);
      urlBrowseButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          try {
            IJavaProject javaProject = getJavaProject();
            if (javaProject == null) {
              MessageDialog.openError(Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
                  "No project found", "Please make sure that this launch configuration has a valid project assigned.");
              return;
            }

            IPath url;
            ILaunchShortcutStrategy strategy;
            if (WebAppUtilities.isWebApp(javaProject.getProject())) {
              strategy = new WebAppLaunchShortcutStrategy();
            } else {
              strategy = new LegacyGWTLaunchShortcutStrategy();
            }

            String urlFromUser = strategy.getUrlFromUser(getJavaProject().getProject(), false);

            if (urlFromUser != null) {
              // TODO: Why do we create a new Path object here?
              // Was it to normalize the URL?
              url = new Path(urlFromUser);
              SWTUtilities.setText(urlField, url.toString());
            }
          } catch (CoreException ce) {
            GWTPluginLog.logError(ce);
            MessageDialog.openError(Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
                "Error while browsing for URL", ce.getLocalizedMessage());
          }
        }
      });
    }

    public String getUrl() {
      return urlField.getText();
    }

    public void initializeFrom(ILaunchConfiguration config) throws CoreException {
      boolean hasNoServerArg =
          NoServerArgumentProcessor.hasNoServerArg(LaunchConfigurationProcessorUtilities.parseProgramArgs(config));
      boolean showStartupUrl =
          GwtLaunchConfigurationProcessorUtilities.isGwtShell(config)
              || GwtLaunchConfigurationProcessorUtilities.isHostedMode(config)
              || (GwtLaunchConfigurationProcessorUtilities.isDevMode(config) && hasNoServerArg);
      GridData layoutData = (GridData) browserGroup.getLayoutData();
      layoutData.exclude = !showStartupUrl;
      browserGroup.setVisible(showStartupUrl);

      SWTUtilities.setText(urlField, GWTLaunchConfiguration.getStartupUrl(config));
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      // Save the startup URL
      GWTLaunchConfigurationWorkingCopy.setStartupUrl(configuration, getUrl().trim());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(new StartupUrlArgumentProcessor(), configuration);

      LaunchConfigurationProcessorUtilities.updateViaProcessor(new XStartOnFirstThreadArgumentProcessor(),
          configuration);
    }
  }

  /**
   * Provides the labels for the log level and output style combos. The label is calculated by
   * calculating just the first letter of the element.
   */
  private static class DefaultComboLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      String element2 = (String) element;

      // TODO: this is sensitive to locale. Consider using a helper class
      // to do the label generation
      return element2.toUpperCase().charAt(0) + element2.toLowerCase().substring(1);
    }
  }

  protected Composite comp;

  /**
   * May be null.
   */
  protected DevelopmentModeBlock developmentModeBlock;

  protected EntryPointModulesSelectionBlock entryPointModulesSelectionBlock;

  /**
   * May be null.
   */
  protected Button performGwtCompileButton;

  protected SelectionBlock selectionBlock;

  /**
   * See javadoc on {@link com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor} for
   * information about why this is required.
   */
  protected boolean updateLaunchConfigurationDialogBlock;

  /**
   * May be null.
   */
  protected UrlSelectionBlock urlSelectionBlock;

  private final boolean showDevelopmentModeBlock;
  private final boolean showPerformGwtCompileSetting;
  private final boolean showUrlSelectionBlock;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher =
      new UpdateLaunchConfigurationDialogBatcher(this);

  public GWTSettingsTab(ILaunchArgumentsContainer argsContainer) {
    this(argsContainer, true, true, false);
  }

  public GWTSettingsTab(ILaunchArgumentsContainer argsContainer, boolean showDevelopmentModeBlock,
      boolean showUrlSelectionBlock, boolean showPerformGwtCompileSetting) {
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

  @Override
  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  @Override
  public void createControl(Composite parent) {
    comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;

    setControl(comp);

    createVerticalSpacer(comp, 1);
    selectionBlock = new SelectionBlock(comp);

    createVerticalSpacer(comp, 1);

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
      performGwtCompileButton = SWTFactory.createCheckButton(comp, "Perform GWT compile", null, true, 1);
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

  @Override
  public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
    // Save the entry point modules - save first!
    persistModules(configuration, entryPointModulesSelectionBlock.getModules());

    // updates the program args
    LaunchConfigurationProcessorUtilities.updateViaProcessor(new ModuleArgumentProcessor(), configuration);

    if (selectionBlock != null) {
      selectionBlock.performApply(configuration);
    }

    if (developmentModeBlock != null) {
      developmentModeBlock.performApply(configuration);
    }

    if (urlSelectionBlock != null) {
      urlSelectionBlock.performApply(configuration);
    }

    if (performGwtCompileButton != null) {
      LaunchConfigurationAttributeUtilities.set(configuration,
          SpeedTracerLaunchConfiguration.Attribute.PERFORM_GWT_COMPILE, performGwtCompileButton.getSelection());
    }

    enableSuperDevModeSelection(configuration);
  }

  @Override
  public Image getImage() {
    return GWTPlugin.getDefault().getImage(GWTImages.GWT_ICON);
  }

  @Override
  public String getName() {
    return "GWT";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    updateLaunchConfigurationDialogBlock = true;
    IJavaProject javaProject = null;

    try {
      super.initializeFrom(config);
      try {
        if (selectionBlock != null) {
          selectionBlock.initializeFrom(config);
        }

        if (developmentModeBlock != null) {
          developmentModeBlock.initializeFrom(config);
          developmentModeBlock.setTitle();
        }

        if (urlSelectionBlock != null) {
          urlSelectionBlock.initializeFrom(config);
        }

        javaProject = getJavaProject();
        entryPointModulesSelectionBlock.setJavaProject(javaProject);

        // Can't get the project's entry point modules if this launch
        // configuration has not been assigned to a project
        if (javaProject != null) {
          // The default set of entry point modules is the project's
          // set of defined entry point modules (specified in project
          // properties).
          IProject project = javaProject.getProject();
          entryPointModulesSelectionBlock.setDefaultModules(ModuleArgumentProcessor.getDefaultModules(project, config));

          // Initialize the selected set of entry point modules
          List<String> launchConfigModules = GWTLaunchConfiguration.getEntryPointModules(config);
          entryPointModulesSelectionBlock.setModules(launchConfigModules);
        }

        if (performGwtCompileButton != null) {
          boolean performGwtCompile =
              LaunchConfigurationAttributeUtilities.getBoolean(config,
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
      updateLaunchConfigurationDialogBlock = false;
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }

    doPerformApply(configuration);
  }

  @Override
  public void persistFromArguments(List<String> args, ILaunchConfigurationWorkingCopy config) {
    String logLevel = LogLevelArgumentProcessor.getValidLogLevel(args);
    GWTLaunchConfigurationWorkingCopy.setLogLevel(config, logLevel);

    try {
      List<String> modules = ModuleArgumentProcessor.getModules(args, config, getProject());
      persistModules(config, modules);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not persist entry point modules");
    }

    try {
      String startupUrl = StartupUrlArgumentProcessor.getStartupUrl(args, getCurrentLaunchConfiguration());
      GWTLaunchConfigurationWorkingCopy.setStartupUrl(config, startupUrl);
    } catch (CoreException e) {
      GWTPluginLog.logError(e, "Could not persist startup URL");
    }

    String port = DevModeCodeServerPortArgumentProcessor.getPort(args);
    if (port.equalsIgnoreCase("auto")) {
      GWTLaunchConfigurationWorkingCopy.setClassicDevModeCodeServerPortAuto(config, true);
    } else {
      GWTLaunchConfigurationWorkingCopy.setClassicDevModeCodeServerPort(config, port);
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {}

  protected void createStartupModuleComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Available Modules:", 3, 1, GridData.FILL_BOTH);
    GridLayout groupLayout = (GridLayout) group.getLayout();
    groupLayout.marginBottom = 8;
    group.setLayout(groupLayout);

    IModulesChangeListener listener = new IModulesChangeListener() {
      @Override
      public void onModulesChanged() {
        updateLaunchConfigurationDialog();
      }
    };
    entryPointModulesSelectionBlock = new EntryPointModulesSelectionBlock(listener);
    entryPointModulesSelectionBlock.doFillIntoGrid(group, 3);
  }

  protected IJavaProject getJavaProject() throws CoreException {
    return JavaRuntime.getJavaProject(getCurrentLaunchConfiguration());
  }

  /**
   * If this project doesn't have the GWT nature, gray out all the controls recursively.
   */
  protected void maybeGrayControls(IJavaProject javaProject) {
    boolean isGWTProject = javaProject != null && GWTNature.isGWTProject(javaProject.getProject());
    String message =
        isGWTProject ? null : "GWT is not enabled for this project. You can enable it in the project's properties.";

    setMessage(message);
    SWTUtilities.setEnabledRecursive(comp, isGWTProject);
  }

  protected void persistModules(ILaunchConfigurationWorkingCopy configuration, List<String> modules) {
    GWTLaunchConfigurationWorkingCopy.setEntryPointModules(configuration, modules, Collections.<String>emptyList());
  }

  protected void registerProgramArgsListener(ILaunchArgumentsContainer argsContainer) {
    argsContainer.registerProgramArgsListener(this);
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    if (!updateLaunchConfigurationDialogBlock) {
      updateLaunchConfigurationDialogBatcher.updateLaunchConfigurationDialogCalled();
    }
  }

  /**
   * Returns true if the SDK version number supports Super Dev Mode (i.e, 2.5.0 or above).
   *
   * @param version sdk version number
   * @return true if super dev mode can be used
   */
  private boolean canSdkVersionUseSuperDevMode(String version) {
    return version.matches("^2.[5-9].*") || version.matches("^[3-9].*");
  }

  /**
   * Hides or shows Super Dev Mode UI block based on the SDK version.
   */
  private void enableSuperDevModeSelection(ILaunchConfigurationWorkingCopy configuration) {
    GWTRuntime sdk = null;
    try {
      sdk = GWTRuntime.findSdkFor(getJavaProject());
    } catch (CoreException e) {
      selectionBlock.setVisible(false);
    }
    String version = sdk.getVersion();

    // can use super dev mode 2.5.0+ or 3+
    if (canSdkVersionUseSuperDevMode(version)) {
      selectionBlock.setVisible(true);
    } else {
      selectionBlock.setVisible(false);

      // Turn off sdm
      GWTLaunchConfigurationWorkingCopy.setSuperDevModeEnabled(configuration, false);
    }

    if (developmentModeBlock != null) {
      developmentModeBlock.setTitle();
    }
  }

  private IProject getProject() throws CoreException {
    IJavaProject javaProject = getJavaProject();
    if (javaProject == null || !javaProject.exists()) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, "Could not get a valid Java project"));
    }
    return javaProject.getProject();
  }

  private void updateEnabledState() {
    if (performGwtCompileButton != null) {
      entryPointModulesSelectionBlock.setEnabled(performGwtCompileButton.getSelection());
    }

    if (developmentModeBlock != null) {
      developmentModeBlock.updateEnabledState();
    }
  }

}
