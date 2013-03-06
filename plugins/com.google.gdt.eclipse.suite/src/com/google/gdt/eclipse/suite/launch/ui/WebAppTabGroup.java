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

import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.platform.debug.ui.CommonTab;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ui.GWTSettingsTab;
import com.google.gwt.eclipse.core.launch.ui.GWTSettingsTab.IGWTSettingsTabFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.ui.progress.WorkbenchJob;

import java.util.List;

/**
 * Tabs on the launch configuration for web apps.
 */
public class WebAppTabGroup extends AbstractLaunchConfigurationTabGroup {
  private ILaunchConfigurationDialog launchConfigurationDialog;

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    launchConfigurationDialog = dialog;

    WebAppArgumentsTab argsTab = new WebAppArgumentsTab();

    GWTSettingsTab gwtSettingsTab = null;

    ExtensionQuery<IGWTSettingsTabFactory> extQuery = new ExtensionQuery<IGWTSettingsTabFactory>(
        GWTPlugin.PLUGIN_ID, "gwtSettingsTabFactory", "class");
    List<ExtensionQuery.Data<IGWTSettingsTabFactory>> gwtSettingsTabFactories = extQuery.getData();
    for (ExtensionQuery.Data<IGWTSettingsTabFactory> factory : gwtSettingsTabFactories) {
      IGWTSettingsTabFactory tabFactory = factory.getExtensionPointData();
      gwtSettingsTab = tabFactory.newInstance(argsTab);
      break;
    }

    if (gwtSettingsTab == null) {
      gwtSettingsTab = new GWTSettingsTab(argsTab);
    }

    GaeSettingsTab gaeSettingsTab = null;
    ExtensionQuery<GaeSettingsTab> extQueryGae = new ExtensionQuery<
        GaeSettingsTab>(GdtPlugin.PLUGIN_ID, "gaeSettingsTab", "class");

        List<ExtensionQuery.Data<GaeSettingsTab>> gaeSettingsTabs = extQueryGae.getData();
    for (ExtensionQuery.Data<GaeSettingsTab> tab : gaeSettingsTabs) {
      gaeSettingsTab = tab.getExtensionPointData();
      break;
    }

    if (gaeSettingsTab == null) {
      gaeSettingsTab = new GaeSettingsTab();
    }

    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new WebAppMainTab(), new WebAppServerTab(argsTab, true, true),
        gwtSettingsTab, gaeSettingsTab, argsTab, new JavaJRETab(),
        new JavaClasspathTab(), new SourceLookupTab(), new EnvironmentTab(),
        new CommonTab()};
    setTabs(tabs);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    super.initializeFrom(configuration);

    // Since we're suppressing dialog updates during tab initialization, we
    // trigger one here to ensure the buttons are in a valid state.
    // Note that LaunchConfigurationsDialog.updateButtons() still does a
    // full refresh (and a performApply() on the current tab - why, oh, why!?),
    // but it's better to eat the cost one time here, than once for every tab.
    //
    // We're using a deferred job because LaunchConfigurationsDialog ignores
    // buttons updates during initialization.
    createUpdateJob();
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);

    WebAppLaunchUtil.setDefaults(configuration, null);
  }

  private void createUpdateJob() {
    new WorkbenchJob("WebAppTabGroup") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        ILaunchConfigurationTab tab = launchConfigurationDialog.getActiveTab();
        if (tab != null && !tab.getControl().isDisposed()) {
          launchConfigurationDialog.updateButtons();
          launchConfigurationDialog.updateMessage();
        }
        return Status.OK_STATUS;
      }
    }.schedule();
  }
}
