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
package com.google.gwt.eclipse.core.preferences.ui;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.speedtracer.SourceViewerServer;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
    
    int port = Integer.parseInt(portText.getText());
    
    if (port != GWTPreferences.getSourceViewerServerPort()) {
      GWTPreferences.setSourceViewerServerPort(port);
      
      try {
        SourceViewerServer.INSTANCE.stop();
      } catch (Throwable e) {
        GWTPluginLog.logError(e, "Could not stop source viewer server.");
      }
      
      SourceViewerServer.INSTANCE.setPort(port);
      
      try {
        SourceViewerServer.INSTANCE.start();
      } catch (Throwable e) {
        GWTPluginLog.logError(e, "Could not start source viewer server.");
      }
    }
    
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
    layout.horizontalSpacing = 6;
    panel.setLayout(layout);
    
    Label portLabel = new Label(panel, SWT.NONE);
    portLabel.setText("Speed Tracer port:");
    
    portText = new Text(panel, SWT.SINGLE | SWT.BORDER);
    portText.setText(GWTPreferences.getSourceViewerServerPort() + "");
    portText.setTextLimit(5);
    
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
    
    return panel;
  }
}
