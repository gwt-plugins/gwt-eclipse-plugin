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

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTJUnitLaunchDelegate;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.processors.LogLevelArgumentProcessor;
import com.google.gwt.eclipse.core.launch.util.GWTJUnitLaunchUtils;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.SWTFactory;
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
import org.eclipse.swt.widgets.Text;

/**
 * Launch configuration tab for GWT-specific GWT JUnit test settings.
 */
@SuppressWarnings("restriction")
public class GWTJUnitSettingsTab extends JavaLaunchTab {

  /**
   * Provides the labels for the log level and output style combos. The label is
   * calculated by calculating just the first letter of the element.
   * 
   * Pulled in from GWTSettingsTab. How can we do this better?
   */
  private static class DefaultComboLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      String element2 = (String) element;

      // TODO: this is sensitive to locale. Consider using a helper class
      // to do the label generation
      return element2.toUpperCase().charAt(0)
          + element2.toLowerCase().substring(1);
    }
  }

  private class SettingChangedListener extends SelectionAdapter implements
      ModifyListener, ISelectionChangedListener {
    public void modifyText(ModifyEvent e) {
      updateLaunchConfigurationDialog();
    }

    public void selectionChanged(SelectionChangedEvent event) {
      updateLaunchConfigurationDialog();
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  private Button notHeadlessButton;
  private Button webModeButton;
  private Button standardsModeButton;
  private ComboViewer logLevelComboViewer;
  private ComboViewer outputStyleComboViewer;
  private Composite comp;
  private Text outputDirectoryField;
  private SettingChangedListener settingChangedListener = new SettingChangedListener();

  public void createControl(Composite parent) {
    comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1,
        GridData.FILL_BOTH);
    ((GridLayout) comp.getLayout()).verticalSpacing = 0;
    setControl(comp);

    createVerticalSpacer(comp, 1);

    createGWTJUnitSettingsComponent(comp);

    SWTFactory.createHorizontalSpacer(comp, 1);
  }

  @Override
  public Image getImage() {
    return GWTPlugin.getDefault().getImage(GWTImages.GWT_ICON);
  }

  public String getName() {
    return "GWT JUnit";
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    super.initializeFrom(config);
    initializeLogLevel(config);
    initializeOutputStyle(config);
    initializeNotHeadless(config);
    initializeWebMode(config);
    initializeStandardsMode(config);
    initializeOutputDirectory(config);
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    StructuredSelection logLevelSelection = (StructuredSelection) logLevelComboViewer.getSelection();
    config.setAttribute(GWTLaunchConstants.ATTR_LOG_LEVEL,
        logLevelSelection.getFirstElement().toString());

    StructuredSelection outputStyleSelection = (StructuredSelection) outputStyleComboViewer.getSelection();
    config.setAttribute(GWTLaunchConstants.ATTR_OBFUSCATION,
        outputStyleSelection.getFirstElement().toString());

    config.setAttribute(GWTLaunchConstants.ATTR_NOT_HEADLESS,
        notHeadlessButton.getSelection());

    config.setAttribute(GWTLaunchConstants.ATTR_WEB_MODE,
        Boolean.toString(webModeButton.getSelection()));

    config.setAttribute(GWTLaunchConstants.ATTR_STANDARDS_MODE,
        standardsModeButton.getSelection());

    config.setAttribute(GWTLaunchConstants.ATTR_OUT_DIR,
        outputDirectoryField.getText());
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    GWTJUnitLaunchUtils.setDefaults(configuration);
  }

  protected void createGWTJUnitSettingsComponent(Composite parent) {
    Group group = SWTFactory.createGroup(parent, "GWT JUnit Settings", 2, 1,
        GridData.FILL_HORIZONTAL);

    SWTFactory.createLabel(group, "Log level:", 1);
    logLevelComboViewer = new ComboViewer(group, SWT.READ_ONLY);
    logLevelComboViewer.setContentProvider(new ArrayContentProvider());
    logLevelComboViewer.setLabelProvider(new DefaultComboLabelProvider());
    logLevelComboViewer.setInput(LogLevelArgumentProcessor.LOG_LEVELS);
    // logLevelComboViewer.setSelection(new StructuredSelection("DEBUG"));
    logLevelComboViewer.addSelectionChangedListener(settingChangedListener);

    SWTFactory.createLabel(group, "Output style:", 1);
    outputStyleComboViewer = new ComboViewer(group, SWT.READ_ONLY);
    outputStyleComboViewer.setContentProvider(new ArrayContentProvider());
    outputStyleComboViewer.setLabelProvider(new DefaultComboLabelProvider());
    outputStyleComboViewer.setInput(GWTLaunchConstants.OUTPUT_STYLES);
    // outputStyleComboViewer.setSelection(new StructuredSelection("PRETTY"));
    outputStyleComboViewer.addSelectionChangedListener(settingChangedListener);

    notHeadlessButton = SWTFactory.createCheckButton(group,
        "Display the log window and browser windows (useful for debugging)",
        null, true, 2);
    notHeadlessButton.addSelectionListener(settingChangedListener);

    webModeButton = SWTFactory.createCheckButton(group,
        "Run tests in production mode", null, false, 2);
    webModeButton.addSelectionListener(settingChangedListener);

    standardsModeButton = SWTFactory.createCheckButton(group,
        "Use standards mode", null, false, 2);
    standardsModeButton.addSelectionListener(settingChangedListener);
    GridData standardsModeButtonData = new GridData(SWT.BEGINNING,
        SWT.BEGINNING, false, false, 2, 1);
    standardsModeButton.setLayoutData(standardsModeButtonData);

    SWTFactory.createLabel(group, "Output directory:", 1);
    outputDirectoryField = new Text(group, SWT.BORDER);
    outputDirectoryField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    outputDirectoryField.addModifyListener(settingChangedListener);
  }

  private void initializeLogLevel(ILaunchConfiguration config) {
    String logLevel = GWTLaunchConstants.Defaults.LOG_LEVEL;

    try {
      logLevel = config.getAttribute(GWTLaunchConstants.ATTR_LOG_LEVEL,
          logLevel);
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }
    logLevelComboViewer.setSelection(new StructuredSelection(logLevel));
  }

  private void initializeNotHeadless(ILaunchConfiguration config) {
    boolean notHeadless = GWTLaunchConstants.Defaults.NOT_HEADLESS;
    try {
      notHeadless = config.getAttribute(GWTLaunchConstants.ATTR_NOT_HEADLESS,
          notHeadless);
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }
    notHeadlessButton.setSelection(notHeadless);
  }

  private void initializeOutputDirectory(ILaunchConfiguration config) {
    String outDir = GWTLaunchConstants.Defaults.OUT_DIR_TEST;

    try {
      outDir = config.getAttribute(GWTLaunchConstants.ATTR_OUT_DIR, outDir);
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }
    outputDirectoryField.setText(outDir);
  }

  private void initializeOutputStyle(ILaunchConfiguration config) {
    String outStyle = GWTLaunchConstants.Defaults.OBFUSCATION;

    try {
      outStyle = config.getAttribute(GWTLaunchConstants.ATTR_OBFUSCATION,
          outStyle);
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }
    outputStyleComboViewer.setSelection(new StructuredSelection(outStyle));
  }

  private void initializeStandardsMode(ILaunchConfiguration config) {
    standardsModeButton.setSelection(GWTJUnitLaunchDelegate.getStandardsMode(config));

    try {
      boolean visible = GWTJUnitLaunchDelegate.isGwtSdkVersionAtLeast(
          "2.0.0", config);
      standardsModeButton.setVisible(visible);
      GridData layoutData = (GridData) standardsModeButton.getLayoutData();
      layoutData.exclude = !visible;

    } catch (CoreException e) {
      if (e.getStatus().getSeverity() > IStatus.INFO) {
        GWTPluginLog.logWarning(e,
            "Could not determine whether standards mode checkbox should be visible");
      }
    }
  }

  private void initializeWebMode(ILaunchConfiguration config) {
    String webMode = GWTLaunchConstants.Defaults.WEB_MODE;

    // webMode will always be one of the strings "true" or "false".
    // We make it be a string instead of a boolean because that's what the
    // JUnit launch code knows how to compare.
    // See JUnitLaunchShortcut's method hasSameAttributes.
    try {
      webMode = config.getAttribute(GWTLaunchConstants.ATTR_WEB_MODE, webMode);
      assert ("true".equals(webMode) || "false".equals(webMode));
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }
    webModeButton.setSelection(Boolean.parseBoolean(webMode));
  }
}
