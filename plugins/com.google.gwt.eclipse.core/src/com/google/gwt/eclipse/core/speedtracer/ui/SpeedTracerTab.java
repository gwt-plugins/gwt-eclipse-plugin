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
package com.google.gwt.eclipse.core.speedtracer.ui;

import com.google.gdt.eclipse.core.launch.LaunchConfigurationAttributeUtilities;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationUtilities;
import com.google.gdt.eclipse.core.ui.BrowserSelectionBlock;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

/**
 * Tab for Speed Tracer-specific settings.
 */
@SuppressWarnings("restriction")
public class SpeedTracerTab extends JavaLaunchTab implements
    BrowserSelectionBlock.Listener, UrlSelectionBlock.Listener {

  private BrowserSelectionBlock browserSelectionBlock;
  private UrlSelectionBlock urlSelectionBlock;

  public SpeedTracerTab() {
    browserSelectionBlock = new BrowserSelectionBlock(null, this);
    urlSelectionBlock = new UrlSelectionBlock(this);
  }

  public void browserSelected(IBrowserDescriptor browserDescriptor,
      IStatus status) {
    updateLaunchConfigurationDialog();
  }

  public void createControl(Composite parent) {
    Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1,
        GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;
    createBrowserComponent(comp);
    setControl(comp);
  }

  @Override
  public Image getImage() {
    return GWTPlugin.getDefault().getImage(GWTImages.SPEED_TRACER_SMALL);
  }

  public String getName() {
    return "Speed Tracer";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);

    try {
      String browserName = LaunchConfigurationAttributeUtilities.getString(
          config, SpeedTracerLaunchConfiguration.Attribute.BROWSER);
      browserSelectionBlock.setBrowser(browserName);
    } catch (CoreException e) {
      GWTPluginLog.logWarning(e,
          "Could not get browser for launch configuration " + config.getName());
    }

    try {
      String url = LaunchConfigurationAttributeUtilities.getString(config,
          SpeedTracerLaunchConfiguration.Attribute.URL);
      urlSelectionBlock.set(url,
          LaunchConfigurationUtilities.getProject(config));
    } catch (CoreException e) {
      GWTPluginLog.logWarning(e, "Could not get URL for launch configuration "
          + config.getName());
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    setErrorMessage(null);
    setMessage(null);

    if (!super.isValid(launchConfig)) {
      return false;
    }

    IStatus[] statuses = new IStatus[] {
        browserSelectionBlock.validate(), urlSelectionBlock.validate()};
    for (IStatus status : statuses) {
      if (!status.isOK()) {
        setErrorMessage(status.getMessage());
        return false;
      }
    }

    return true;
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    LaunchConfigurationAttributeUtilities.set(config,
        SpeedTracerLaunchConfiguration.Attribute.BROWSER,
        browserSelectionBlock.getBrowserName());
    LaunchConfigurationAttributeUtilities.set(config,
        SpeedTracerLaunchConfiguration.Attribute.URL,
        urlSelectionBlock.getUrl());
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  public void urlChanged(String url, IStatus status) {
    updateLaunchConfigurationDialog();
  }

  private void createBrowserComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "Browser:", 3, 1,
        GridData.FILL_HORIZONTAL);

    browserSelectionBlock.createContents(group, true);
    urlSelectionBlock.createContents(group, true);
  }

}
