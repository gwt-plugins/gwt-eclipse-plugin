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
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchAttributes;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfiguration;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.processors.ModuleArgumentProcessor;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock;
import com.google.gwt.eclipse.core.launch.ui.EntryPointModulesSelectionBlock.IModulesChangeListener;
import com.google.gwt.eclipse.core.launch.ui.tabs.blocks.CompilerBlock;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import java.util.Collections;
import java.util.List;

/*
 * See ILaunchConfigurationProcessor for details on the UI widget <-> arguments <-> persistent
 * storage synchronization.
 */
/**
 * For webapp launch configuration, tab where you specify GWT options.
 */
@SuppressWarnings("restriction")
public class GwtCompilerSettingsTab extends JavaLaunchTab
    implements ILaunchArgumentsContainer.ArgumentsListener, UpdateLaunchConfigurationDialogBatcher.Listener {

  /**
   * A factory that returns a GWT settings tab bound to an arguments tab.
   */
  public interface IGWTSettingsTabFactory {
    GwtCompilerSettingsTab newInstance(ILaunchArgumentsContainer argsContainer);
  }

  /**
   * See javadoc on {@link com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor} for
   * information about why this is required.
   */
  protected boolean blockUpdateLaunchConfigurationDialog;

  protected Composite comp;

  protected EntryPointModulesSelectionBlock entryPointModulesBlock;
  protected CompilerBlock compilerModeBlock;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher =
      new UpdateLaunchConfigurationDialogBatcher(this);

  public GwtCompilerSettingsTab(ILaunchArgumentsContainer argsContainer) {
    if (argsContainer != null) {
      registerProgramArgsListener(argsContainer);
    }
  }

  protected GwtCompilerSettingsTab() {
    this(null);
  }

  @Override
  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {
    comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;
    setControl(comp);

    compilerModeBlock = new CompilerBlock(comp, SWT.NONE);
    compilerModeBlock.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    compilerModeBlock.addUpdateLaunchConfigHandler(new IUpdateLaunchConfig() {
      @Override
      public void updateLaunchConfig() {
        GwtCompilerSettingsTab.this.updateLaunchConfigurationDialog();
      }
    });

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
    if (compilerModeBlock != null) {
      compilerModeBlock.performApply(configuration);
    }

    // Save the entry point modules
    persistModules(configuration, entryPointModulesBlock.getModules());
    LaunchConfigurationProcessorUtilities.updateViaProcessor(new ModuleArgumentProcessor(), configuration);
  }

  @Override
  public Image getImage() {
    return GWTPlugin.getDefault().getImage(GWTImages.GWT_ICON_CODESERVER);
  }

  @Override
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
        if (compilerModeBlock != null) {
          compilerModeBlock.initializeFrom(config);
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
          entryPointModulesBlock.setDefaultModules(ModuleArgumentProcessor.getDefaultModules(project, config));

          // Initialize the selected set of entry point modules
          List<String> launchConfigModules = GWTLaunchConfiguration.getEntryPointModules(config);
          entryPointModulesBlock.setModules(launchConfigModules);
        }

      } catch (CoreException e) {
        // Purposely ignored; this happens when the java project does
        // not exist
      }

      maybeGrayControls(javaProject);

    } finally {
      blockUpdateLaunchConfigurationDialog = false;
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
    entryPointModulesBlock = new EntryPointModulesSelectionBlock(listener);
    entryPointModulesBlock.doFillIntoGrid(group, 3);
  }

  protected IJavaProject getJavaProject() throws CoreException {
    return JavaRuntime.getJavaProject(getCurrentLaunchConfiguration());
  }

  /**
   * If this project doesn't have the GWT nature, gray out all the controls recursively.
   */
  protected void maybeGrayControls(IJavaProject javaProject) {
    boolean isGWTProject = javaProject != null && GWTNature.isGWTProject(javaProject.getProject());
    String message = isGWTProject ? null : "GWT is not enabled for this project.";

    setMessage(message);
    SWTUtilities.setEnabledRecursive(comp, isGWTProject);
  }

  protected void persistModules(ILaunchConfigurationWorkingCopy configuration, List<String> modules) {
    if (modules.equals(entryPointModulesBlock.getDefaultModules())) {
      GWTLaunchConfigurationWorkingCopy.clearAttribute(configuration, GWTLaunchAttributes.ENTRY_POINT_MODULES);
    } else {
      GWTLaunchConfigurationWorkingCopy.setEntryPointModules(configuration, modules, Collections.<String>emptyList());
    }
  }

  protected void registerProgramArgsListener(ILaunchArgumentsContainer argsContainer) {
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
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, "Could not get a valid Java project"));
    }

    return javaProject.getProject();
  }

}
