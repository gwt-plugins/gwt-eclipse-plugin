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
package com.google.gdt.eclipse.suite.preferences.ui;

import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * 
 */
public class GooglePreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {
  private Button updateNotificationCheckbox;
  private boolean updateNotificationsEnabledInitalValue;

  private Button removeTerminatedLaunchesButton;
  private boolean removeTerminatedLaunchesInitialValue;
  
  private Button jsoDetailFormattingButton;
  private boolean jsoDetailFormattingInitialValue;

  private boolean performDeferredOk;

  /**
   * 
   */
  public GooglePreferencePage() {
  }

  /**
   * @param title
   */
  public GooglePreferencePage(String title) {
    super(title);
  }

  /**
   * @param title
   * @param image
   */
  public GooglePreferencePage(String title, ImageDescriptor image) {
    super(title, image);
  }

  @Override
  public void dispose() {
    // Updating the detail formatter settings is somewhat tricky because we
    // don't really own the related properties: the dedicated detail
    // formatters preferences page is unaware of us and also saves on
    // performOK. So in order to avoid having our fine settings overwritten,
    // we actually apply the setting when the preferences dialog is closed.
    if (performDeferredOk) {
      boolean jsoDetailFormatting = jsoDetailFormattingButton.getSelection();
      if (jsoDetailFormattingInitialValue != jsoDetailFormatting) {
        GWTPreferences.setJsoDetailFormatting(jsoDetailFormatting);
      }
    }

    super.dispose();
  }

  public void init(IWorkbench workbench) {
  }

  @Override
  public boolean performOk() {
    boolean selection = updateNotificationCheckbox.getSelection();

    if (selection != updateNotificationsEnabledInitalValue) {
      GdtPreferences.setUpdateNotificationsEnabled(selection);
    }

    boolean removeTerminatedLaunches = removeTerminatedLaunchesButton.getSelection();
    
    if (removeTerminatedLaunchesInitialValue != removeTerminatedLaunches) {
      GWTPreferences.setRemoveTerminatedLaunches(removeTerminatedLaunches);
    }

    performDeferredOk = true;

    return super.performOk();
  }

  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();

    Composite panel = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    panel.setLayout(layout);

    updateNotificationCheckbox = new Button(panel, SWT.CHECK);
    GridData autoUpdateGridData = new GridData();
    autoUpdateGridData.horizontalSpan = 2;
    updateNotificationCheckbox.setLayoutData(autoUpdateGridData);
    updateNotificationCheckbox.setText("Notify me about updates");
    updateNotificationsEnabledInitalValue = GdtPreferences.areUpdateNotificationsEnabled();
    updateNotificationCheckbox.setSelection(updateNotificationsEnabledInitalValue);

    removeTerminatedLaunchesButton = new Button(panel, SWT.CHECK);
    GridData removeTerminatedGridData = new GridData();
    removeTerminatedGridData.horizontalSpan = 2;
    removeTerminatedLaunchesButton.setLayoutData(removeTerminatedGridData);
    // Remove terminated launches when a new launch is created
    removeTerminatedLaunchesButton.setText("Remove terminated launches from " +
      "Development Mode view");
    removeTerminatedLaunchesInitialValue = GWTPreferences.getRemoveTerminatedLaunches();
    removeTerminatedLaunchesButton.setSelection(removeTerminatedLaunchesInitialValue);

    jsoDetailFormattingButton = new Button(panel, SWT.CHECK);
    GridData jsoDetailFormattingGridData = new GridData();
    jsoDetailFormattingGridData.horizontalSpan = 2;
    jsoDetailFormattingButton.setLayoutData(jsoDetailFormattingGridData);
    jsoDetailFormattingButton.setText("Display Javascript object properties");
    jsoDetailFormattingInitialValue = GWTPreferences.getJsoDetailFormatting();
    jsoDetailFormattingButton.setSelection(jsoDetailFormattingInitialValue);
    
    return panel;
  }
}
