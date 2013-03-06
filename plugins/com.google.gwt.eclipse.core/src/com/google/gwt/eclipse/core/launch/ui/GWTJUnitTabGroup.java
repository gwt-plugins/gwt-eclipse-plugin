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

import com.google.gwt.eclipse.core.launch.util.GWTJUnitLaunchUtils;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.junit.launcher.JUnitTabGroup;

/**
 * Launch configuration tabs for configuring a GWT JUnit test.
 */
@SuppressWarnings("restriction")
public class GWTJUnitTabGroup extends JUnitTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {

    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
        new JUnitLaunchConfigurationTabDecorator(), new GWTJUnitSettingsTab(),
        new JavaArgumentsTab(), new JavaClasspathTab(), new JavaJRETab(),
        new SourceLookupTab(), new EnvironmentTab(), new CommonTab()};
    setTabs(tabs);
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config);
    GWTJUnitLaunchUtils.setDefaults(config);
  }
}
