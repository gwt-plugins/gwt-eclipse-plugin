/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.core.preferences.ui;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.speedtracer.SourceViewerServer;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Page to manage preferences for SpeedTracer that apply across the workspace.
 */
public class SpeedTracerPreferencePage extends PreferencePage implements
    IWorkbenchPreferencePage {

  public static final String ID = SpeedTracerPreferencePage.class.getName();

  private Button speedTracerEnabledCheckbox;
  private Text portText;

  public SpeedTracerPreferencePage() {
  }

  public SpeedTracerPreferencePage(String title) {
    super(title);
  }

  public SpeedTracerPreferencePage(String title, ImageDescriptor image) {
    super(title, image);
  }

  public void init(IWorkbench workbench) {
  }

  @Override
  public boolean performOk() {

    boolean oldEnabled = GWTPreferences.getSpeedTracerEnabled();
    boolean newEnabled = speedTracerEnabledCheckbox.getSelection();
    int newPort = Integer.parseInt(portText.getText());
    int oldPort = GWTPreferences.getSourceViewerServerPort();
    
    if (preferencesChanged(oldEnabled, newEnabled, oldPort, newPort)) {
      GWTPreferences.setSpeedTracerEnabled(newEnabled);
      
      if (oldEnabled) {
        try {
          SourceViewerServer.INSTANCE.stop();
        } catch (Throwable e) {
          GWTPluginLog.logError(e, "Could not stop source viewer server.");
        }
      }

      if (newEnabled) {
        SourceViewerServer.INSTANCE.setPort(newPort);
        try {
          SourceViewerServer.INSTANCE.start();
        } catch (Throwable e) {
          GWTPluginLog.logError(e, "Could not start source viewer server.");
        }
      }
    }

    return super.performOk();
  }
  
  private static boolean preferencesChanged(
      boolean oldEnabled, boolean newEnabled, int oldPort, int newPort) {
    if (!oldEnabled && !newEnabled) {
      return false;
    } else if (oldEnabled && newEnabled) {
      return newPort != oldPort;
    } else {
      return true;
    }    
  }

  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();

    Composite outerPanel = new Composite(parent, SWT.NONE);
    GridLayout outerPanelLayout = new GridLayout();
    outerPanelLayout.numColumns = 1;
    outerPanelLayout.marginWidth = 0;
    outerPanelLayout.marginHeight = 0;
    outerPanelLayout.horizontalSpacing = 6;
    outerPanel.setLayout(outerPanelLayout);

    speedTracerEnabledCheckbox = new Button(outerPanel, SWT.CHECK);
    speedTracerEnabledCheckbox.setText("Enable Speed Tracer");
    speedTracerEnabledCheckbox.setSelection(GWTPreferences.getSpeedTracerEnabled());

    speedTracerEnabledCheckbox.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      public void widgetSelected(SelectionEvent e) {
        portText.setEnabled(speedTracerEnabledCheckbox.getSelection());
      }
    });

    Composite innerPanel = new Composite(outerPanel, SWT.NONE);
    GridLayout innerPanelLayout = new GridLayout();
    innerPanelLayout.numColumns = 2;
    innerPanelLayout.marginWidth = 0;
    innerPanelLayout.marginHeight = 0;
    innerPanelLayout.horizontalSpacing = 6;
    innerPanel.setLayout(innerPanelLayout);

    Label portLabel = new Label(innerPanel, SWT.NONE);
    portLabel.setText("Speed Tracer port:");

    portText = new Text(innerPanel, SWT.SINGLE | SWT.BORDER);
    portText.setText(GWTPreferences.getSourceViewerServerPort() + "");
    portText.setTextLimit(5);
    portText.setEnabled(speedTracerEnabledCheckbox.getSelection());

    portText.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String s = e.text;
        for (char c : s.toCharArray()) {
          if (!('0' <= c && c <= '9')) {
            e.doit = false;
          }
        }
      }
    });

    portText.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event e) {
        String text = portText.getText();
        if (text.length() > 0) {
          // the verify listener above should guarantee that the text is an int
          int port = Integer.parseInt(text);
          if (port > 65536) {
            setErrorMessage("Port number cannot be greater than 65536.");
            setValid(false);
          } else {
            setErrorMessage(null);
            setValid(true);
          }
        } else {
          setErrorMessage("Port number cannot be empty.");
          setValid(false);
        }
      }
    });

    GridData portTextLayoutData = new GridData(SWT.FILL, SWT.LEFT, false, false);
    portText.setLayoutData(portTextLayoutData);

    return outerPanel;
  }
}
