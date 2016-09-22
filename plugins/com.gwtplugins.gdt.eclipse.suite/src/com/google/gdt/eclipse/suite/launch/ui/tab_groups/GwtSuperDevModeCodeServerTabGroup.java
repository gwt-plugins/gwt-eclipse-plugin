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
package com.google.gdt.eclipse.suite.launch.ui.tab_groups;

import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.platform.debug.ui.CommonTab;
import com.google.gdt.eclipse.suite.launch.ui.tabs.WebAppArgumentsTab;
import com.google.gdt.eclipse.suite.launch.ui.tabs.WebAppMainTab;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.GWTLaunchConfigurationWorkingCopy;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.launch.ui.tabs.GwtSuperDevModeCodeServerSettingsTab;
import com.google.gwt.eclipse.core.launch.ui.tabs.GwtSuperDevModeCodeServerSettingsTab.IGWTSettingsTabFactory;
import com.google.gwt.eclipse.core.launch.util.GwtSuperDevModeCodeServerLaunchUtil;

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
public class GwtSuperDevModeCodeServerTabGroup extends AbstractLaunchConfigurationTabGroup {

  public interface IGwtSdmTabFactory {
    GwtSuperDevModeCodeServerTabGroup newInstance();
  }

  private ILaunchConfigurationDialog launchConfigurationDialog;

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    launchConfigurationDialog = dialog;

    WebAppArgumentsTab argsTab = new WebAppArgumentsTab();

    GwtSuperDevModeCodeServerSettingsTab gwtSettingsTab = null;

    ExtensionQuery<IGWTSettingsTabFactory> extQuery = new ExtensionQuery<IGWTSettingsTabFactory>(GWTPlugin.PLUGIN_ID, "gwtSettingsTabFactory", "class");
    List<ExtensionQuery.Data<IGWTSettingsTabFactory>> gwtSettingsTabFactories = extQuery.getData();
    for (ExtensionQuery.Data<IGWTSettingsTabFactory> factory : gwtSettingsTabFactories) {
      IGWTSettingsTabFactory tabFactory = factory.getExtensionPointData();
      gwtSettingsTab = tabFactory.newInstance(argsTab);
      break;
    }

    if (gwtSettingsTab == null) {
      gwtSettingsTab = new GwtSuperDevModeCodeServerSettingsTab(argsTab);
    }

    WebAppMainTab webAppMainTab = new WebAppMainTab();

    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        webAppMainTab,
        gwtSettingsTab,
        argsTab,
        new JavaJRETab(),
        new JavaClasspathTab(),
        new SourceLookupTab(),
        new EnvironmentTab(),
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

    GwtSuperDevModeCodeServerLaunchUtil.setDefaults(configuration, null);

    GWTLaunchConfigurationWorkingCopy.setMainType(configuration, GwtLaunchConfigurationProcessorUtilities.SUPERDEVMODE_CODESERVER_MAIN_TYPE);
  }

  private void createUpdateJob() {
    new WorkbenchJob("GwtSuperDevModeCodeServerTabGroup") {
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
