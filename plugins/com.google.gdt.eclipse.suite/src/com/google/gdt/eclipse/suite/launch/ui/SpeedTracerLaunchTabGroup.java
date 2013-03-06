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
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.launch.ui.GWTSettingsTab;
import com.google.gwt.eclipse.core.speedtracer.ui.SpeedTracerTab;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

import java.util.List;

/*
 * TODO: move this back into gwt.core.speedtracer, and create extension points
 * for the tabs from gdt.suite that this needs to use.
 */
/**
 * Tab group for the Speed Tracer launch configuration.
 */
public class SpeedTracerLaunchTabGroup extends
    AbstractLaunchConfigurationTabGroup {

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    // TODO: rename or split into base class
    WebAppArgumentsTab argsTab = new WebAppArgumentsTab();

    GWTSettingsTab gwtSettingsTab = null;

    ExtensionQuery<GWTSettingsTab> extQuery = new ExtensionQuery<GWTSettingsTab>(
        GWTPlugin.PLUGIN_ID, "gwtSettingsTab", "class");

    List<ExtensionQuery.Data<GWTSettingsTab>> gwtSettingsTabs = extQuery.getData();
    for (ExtensionQuery.Data<GWTSettingsTab> tab : gwtSettingsTabs) {
      gwtSettingsTab = tab.getExtensionPointData();
      break;
    }

    if (gwtSettingsTab == null) {
      gwtSettingsTab = new GWTSettingsTab(argsTab, false, false, true);
    }

    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new WebAppMainTab(), new SpeedTracerTab(),
        new WebAppServerTab(argsTab, false, false),
        gwtSettingsTab,
        argsTab, new JavaJRETab(), new JavaClasspathTab(),
        new SourceLookupTab(), new EnvironmentTab(), new CommonTab()};
    setTabs(tabs);
  }
}
