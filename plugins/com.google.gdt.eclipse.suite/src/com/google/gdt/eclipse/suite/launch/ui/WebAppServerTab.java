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
package com.google.gdt.eclipse.suite.launch.ui;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gdt.eclipse.core.launch.WebAppLaunchAttributes;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfigurationWorkingCopy;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.launch.processors.PortArgumentProcessor;
import com.google.gdt.eclipse.suite.launch.processors.PortArgumentProcessor.PortParser;
import com.google.gdt.eclipse.suite.launch.processors.ServerArgumentProcessor;
import com.google.gdt.eclipse.suite.resources.GdtImages;
import com.google.gwt.eclipse.core.launch.processors.NoServerArgumentProcessor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.launching.JavaRuntime;
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

import java.util.List;

/**
 * Main configuration tab for webapp or Speed Tracer launch preferences.
 */
@SuppressWarnings("restriction")
public class WebAppServerTab extends JavaLaunchTab implements
    WebAppArgumentsTab.ArgumentsListener,
    UpdateLaunchConfigurationDialogBatcher.Listener {

  // TODO: Create a subclass that contains the run server and autoport
  /**
   * May be null.
   */
  private Button runServerButton;

  /**
   * May be null.
   */
  private Button autoPortSelectionButton;

  private Text serverPortText;

  private final WebAppServerTab webAppServerTab = this;

  /**
   * See javadoc on
   * {@link com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor}
   * for information about why this is required.
   */
  private boolean blockUpdateLaunchConfigurationDialog;

  private final UpdateLaunchConfigurationDialogBatcher updateLaunchConfigurationDialogBatcher = new UpdateLaunchConfigurationDialogBatcher(
      this);

  private final boolean showAutoPortSelectionButton;

  private final boolean showRunServerButton;

  public WebAppServerTab(ILaunchArgumentsContainer argsContainer,
      boolean showAutoPortSelectionButton, boolean showRunServerButton) {
    this.showAutoPortSelectionButton = showAutoPortSelectionButton;
    this.showRunServerButton = showRunServerButton;

    argsContainer.registerProgramArgsListener(this);
  }

  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  public void createControl(Composite parent) {
    Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1,
        GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;
    setControl(comp);
    createServerComponent(comp);
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
    if (runServerButton != null) {
      WebAppLaunchConfigurationWorkingCopy.setRunServer(configuration,
          runServerButton.getSelection());
    }

    LaunchConfigurationProcessorUtilities.updateViaProcessor(
        new NoServerArgumentProcessor(), configuration);
    LaunchConfigurationProcessorUtilities.updateViaProcessor(
        new ServerArgumentProcessor(), configuration);

    WebAppLaunchConfigurationWorkingCopy.setServerPort(configuration,
        serverPortText.getText().trim());

    if (autoPortSelectionButton != null) {
      WebAppLaunchConfigurationWorkingCopy.setAutoPortSelection(configuration,
          autoPortSelectionButton.getSelection());
    }

    LaunchConfigurationProcessorUtilities.updateViaProcessor(
        new PortArgumentProcessor(), configuration);
  }

  @Override
  public Image getImage() {
    return GdtPlugin.getDefault().getImage(GdtImages.GDT_ICON);
  }

  public String getName() {
    return "Server";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    blockUpdateLaunchConfigurationDialog = true;
    try {
      try {
        super.initializeFrom(config);

        if (runServerButton != null) {
          runServerButton.setSelection(WebAppLaunchConfiguration.getRunServer(config));
        }

        if (autoPortSelectionButton != null) {
          boolean autoPortSelection = WebAppLaunchConfiguration.getAutoPortSelection(config);
          autoPortSelectionButton.setSelection(autoPortSelection);
        }

        serverPortText.setText(WebAppLaunchConfiguration.getServerPort(config));

      } catch (CoreException e) {
        CorePluginLog.logError(e);
      }

      super.initializeFrom(config);

      updateEnabledState();

    } finally {
      blockUpdateLaunchConfigurationDialog = false;
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);
    setMessage(null);

    if (!super.isValid(launchConfig)) {
      return false;
    }

    IProject project;
    try {
      IJavaProject javaProject = JavaRuntime.getJavaProject(launchConfig);
      if (javaProject == null) {
        return false;
      }

      project = javaProject.getProject();
    } catch (CoreException ce) {
      // Thrown if the Java project does not exist, which is not of concern in
      // this tab (the Main tab handles those error messages)
      return false;
    }

    if (runServerButton != null) {
      if (WebAppLaunchUtil.projectIsGaeOnly(project)
          && !runServerButton.getSelection()) {
        setErrorMessage("App Engine projects need to run the built-in server.");
        return false;
      }
    }

    return true;
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }

    doPerformApply(configuration);
  }

  public void persistFromArguments(List<String> args,
      ILaunchConfigurationWorkingCopy config) {

    boolean runServer = false;
    try {
      runServer = LaunchConfigurationAttributeUtilities.getBoolean(config, 
        WebAppLaunchAttributes.RUN_SERVER);
    } catch (CoreException e) {
      // ignored, just use false
    }
    
    PortParser portParser = PortParser.parse(args);
    WebAppLaunchConfigurationWorkingCopy.setAutoPortSelection(config,
        (!portParser.isPresent || portParser.isAuto) && runServer);
    
    if (portParser.isPresent && !portParser.isAuto) {
      WebAppLaunchConfigurationWorkingCopy.setServerPort(config,
          portParser.port);
    }

    boolean shouldRunServer = !NoServerArgumentProcessor.hasNoServerArg(args);
    WebAppLaunchConfigurationWorkingCopy.setRunServer(config, shouldRunServer);
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  protected void createServerComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Embedded Server:", 3, 1,
        GridData.FILL_HORIZONTAL);

    if (showRunServerButton) {
      runServerButton = SWTFactory.createCheckButton(group,
          "Run built-in server", null, true, 3);
      runServerButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnabledState();
          webAppServerTab.updateLaunchConfigurationDialog();
        }
      });
    }

    Label serverPortLabel = new Label(group, SWT.NONE);
    final GridData serverPortLabelGridData = new GridData();

    if (showRunServerButton) {
      // If showing the "Run server" checkbox, this should be indented
      serverPortLabelGridData.horizontalIndent = 20;
    }

    serverPortLabel.setLayoutData(serverPortLabelGridData);
    serverPortLabel.setText("Port:");

    serverPortText = new Text(group, SWT.BORDER);
    final GridData serverPortTextGridData = new GridData(SWT.FILL, SWT.CENTER,
        false, false);
    serverPortTextGridData.widthHint = 75;
    serverPortText.setLayoutData(serverPortTextGridData);
    serverPortText.setTextLimit(5);
    serverPortText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        webAppServerTab.updateLaunchConfigurationDialog();
      }
    });

    if (showAutoPortSelectionButton) {
      autoPortSelectionButton = new Button(group, SWT.CHECK);
      autoPortSelectionButton.setText("Automatically select an unused port");
      autoPortSelectionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
          true, false));
      autoPortSelectionButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnabledState();
          webAppServerTab.updateLaunchConfigurationDialog();
        }
      });
    }
  }

  @Override
  protected void updateLaunchConfigurationDialog() {
    if (!blockUpdateLaunchConfigurationDialog) {
      updateLaunchConfigurationDialogBatcher.updateLaunchConfigurationDialogCalled();
    }
  }

  private void updateEnabledState() {
    boolean usingAutoPort = autoPortSelectionButton != null
        && autoPortSelectionButton.getSelection();
    boolean runningServer = runServerButton == null
        || runServerButton.getSelection();
    serverPortText.setEnabled(!usingAutoPort && runningServer);

    if (autoPortSelectionButton != null) {
      autoPortSelectionButton.setEnabled(runServerButton.getSelection());
    }
  }
}
