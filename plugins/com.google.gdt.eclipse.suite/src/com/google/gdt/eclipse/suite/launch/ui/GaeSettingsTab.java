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

import com.google.appengine.eclipse.core.launch.AppEngineLaunchConfiguration;
import com.google.appengine.eclipse.core.launch.AppEngineLaunchConfigurationWorkingCopy;
import com.google.appengine.eclipse.core.launch.processors.HrdArgumentProcessor;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.properties.ui.GaeProjectPropertyPage;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchArgumentsContainer;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.UpdateLaunchConfigurationDialogBatcher;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.resources.GdtImages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.List;

/**
 * Settings tab for App Engine.
 */
@SuppressWarnings("restriction")
public class GaeSettingsTab extends JavaLaunchTab implements
    ILaunchArgumentsContainer.ArgumentsListener, UpdateLaunchConfigurationDialogBatcher.Listener {

  /**
   * Datastore options block.
   */
  protected class DatastoreSettingsBlock {
    private static final String DATASTORE_GROUP_TITLE = "High Replication Datastore (HRD) Settings:";

    private final Group datastoreGroup;
    private final Label hrLabel;
    private final Link configLink;
    private final Text unappliedJobPctText;

    public DatastoreSettingsBlock(Composite parent) {
      datastoreGroup =
          SWTFactory.createGroup(parent, DATASTORE_GROUP_TITLE, 2,
          1, GridData.FILL_HORIZONTAL);

      hrLabel = new Label(datastoreGroup, SWT.NONE);
      hrLabel.setText("Unapplied job percentage:");
      unappliedJobPctText = new Text(datastoreGroup, SWT.BORDER);
      unappliedJobPctText.setLayoutData(new GridData(40, SWT.DEFAULT));
      unappliedJobPctText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          updateLaunchConfigurationDialog();
        }
      });

      configLink = new Link(datastoreGroup, SWT.NONE);
      configLink.setText("<a href=\"#\">HRD Configuration</a>");
      configLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          IJavaProject javaProject = getJavaProject();

          if (javaProject == null) {
            return;
          }

          PreferenceDialog page =
              PreferencesUtil.createPropertyDialogOn(getShell(), javaProject.getProject(),
                  GaeProjectPropertyPage.ID, new String[] {GaeProjectPropertyPage.ID}, null);
          if (Window.OK == page.open()) {
            updateState();
            updateLaunchConfigurationDialog();
          }
        }
      });
    }

    public void initializeFrom(ILaunchConfiguration config) {
      try {
        unappliedJobPctText.setText(AppEngineLaunchConfiguration.getUnappliedJobPct(config));
      } catch (CoreException e) {
        CorePluginLog.logError(e);
      }

      updateState();
    }

    public boolean isValid(ILaunchConfiguration config) {
      int val = -1;

      try {
        val = Integer.parseInt(unappliedJobPctText.getText().trim());
      } catch (NumberFormatException e) {
        // intentionally empty
      }

      if (val < 0 || val > 100) {
        setErrorMessage("The unapplied job percentage must be between 0 and 100.");
        return false;
      }

      return true;
    }

    public void performApply(ILaunchConfigurationWorkingCopy config) {
      AppEngineLaunchConfigurationWorkingCopy.setUnappliedJobPct(config,
          unappliedJobPctText.getText().trim());
      LaunchConfigurationProcessorUtilities.updateViaProcessor(
          new HrdArgumentProcessor(), config);
    }

    private boolean isHrdEnabled() throws JavaModelException {
      IJavaProject javaProject = getJavaProject();
      GaeSdk sdk = (javaProject != null) ? GaeSdk.findSdkFor(javaProject) : null;

      return sdk != null && sdk.getCapabilities().contains(GaeSdkCapability.HRD)
          && GaeProjectProperties.getGaeHrdEnabled(javaProject.getProject());
    }

    private void updateState() {
      boolean hrdEnabled = false;

      try {
        hrdEnabled = isHrdEnabled();
      } catch (JavaModelException e) {
        CorePluginLog.logError(e);
      }

      hrLabel.setEnabled(hrdEnabled);
      unappliedJobPctText.setEnabled(hrdEnabled);
    }
  }

  private static final String NOT_SET = "(not set)";

  protected Label appIdLabel;
  protected Label appVersionLabel;
  protected Composite composite;
  protected DatastoreSettingsBlock datastoreBlock;

  public void callSuperInitializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);
  }

  public void callSuperUpdateLaunchConfigurationDialog() {
    super.updateLaunchConfigurationDialog();
  }

  public void createControl(Composite parent) {
    composite =
        SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_HORIZONTAL);
    setControl(composite);

    createVerticalSpacer(composite, 1);
    createAppEngineStatusGroup(composite);

    datastoreBlock = new DatastoreSettingsBlock(composite);
  }

  public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
    datastoreBlock.performApply(configuration);
  }

  @Override
  public Image getImage() {
    return GdtPlugin.getDefault().getImage(GdtImages.GAE_ICON);
  }

  public String getName() {
    return "App Engine";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);
    updateFromProjectSettings();
    // Ordering is important here: updateFromProjectSettings() does a recursive
    // setEnable on the whole group, so the datastore block initialization must
    // occur after that to avoid its controls state being reset.
    datastoreBlock.initializeFrom(config);
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    setErrorMessage(null);
    setMessage(null);

    return super.isValid(config) && datastoreBlock.isValid(config);
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    if (!this.equals(getLaunchConfigurationDialog().getActiveTab())) {
      return;
    }
    doPerformApply(configuration);
  }

  public void persistFromArguments(List<String> args, ILaunchConfigurationWorkingCopy config) {
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  protected IJavaProject getJavaProject() {
    try {
      return JavaRuntime.getJavaProject(getCurrentLaunchConfiguration());
    } catch (CoreException e) {
      // CoreException happens in the case where the named project doesn't
      // exist.
      return null;
    }
  }

  private void createAppEngineStatusGroup(final Composite parent) {
    final Group group =
        SWTFactory.createGroup(parent, "Application Settings:", 2, 1, GridData.FILL_HORIZONTAL);

    SWTFactory.createLabel(group, "Application ID:", 1);
    appIdLabel = SWTFactory.createLabel(group, NOT_SET, 1);

    SWTFactory.createLabel(group, "Version:", 1);
    appVersionLabel = SWTFactory.createLabel(group, NOT_SET, 1);
  }

  private void updateFromProjectSettings() {
    appIdLabel.setText(NOT_SET);
    appVersionLabel.setText(NOT_SET);
    setMessage(null);

    IJavaProject javaProject = getJavaProject();
    GaeProject gaeProject =
        javaProject != null ? GaeProject.create(javaProject.getProject()) : null;

    SWTUtilities.setEnabledRecursive(composite, gaeProject != null);

    if (gaeProject != null) {
      String appid = gaeProject.getAppId();
      String version = gaeProject.getAppVersion();

      if (appid != null && appid.length() > 0) {
        appIdLabel.setText(appid);
      }
      if (version != null && version.length() > 0) {
        appVersionLabel.setText(version);
      }
    } else {
      setMessage(
          "App Engine is not enabled for this project. You can enable it in the project's properties.");
    }
    updateLaunchConfigurationDialog();
  }
}
