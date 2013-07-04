/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.server.ui;

import com.google.appengine.eclipse.wtp.GaeCommands;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.runtime.RuntimeUtils;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * An UI to edit Server instance properties.
 */
public final class ServerSection extends ServerEditorSection implements PropertyChangeListener {

  private GaeServer gaeServer;
  private Text serverPortNumberText;
  private Text autoreloadTimeText;
  private Label autoreloadTimeLabel;

  @Override
  public void createSection(Composite parent) {
    super.createSection(parent);

    FormToolkit toolkit = getFormToolkit(parent.getDisplay());

    Section section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR
        | Section.DESCRIPTION | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
        | ExpandableComposite.FOCUS_TITLE);

    section.setText("Application Server");
    section.setDescription("Edit runtime properties for the Google App Engine");
    section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

    Composite comp = toolkit.createComposite(section);
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    gl.verticalSpacing = 5;
    gl.marginWidth = 10;
    gl.marginHeight = 5;
    comp.setLayout(gl);
    comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    section.setClient(comp);
    GridDataFactory txtGDF = GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(50,
        SWT.DEFAULT);

    {
      createLabel(comp, "Dev Server Port", toolkit);
      serverPortNumberText = toolkit.createText(comp,
          String.valueOf(gaeServer.getMainPort().getPort()), SWT.BORDER);
      txtGDF.applyTo(serverPortNumberText);
    }
    {
      autoreloadTimeLabel = createLabel(comp,
          "Auto scan for resources change, sec (0 - never scan)", toolkit);
      autoreloadTimeText = toolkit.createText(comp, String.valueOf(gaeServer.getAutoReloadTime()),
          SWT.BORDER);
      txtGDF.applyTo(autoreloadTimeText);
    }
    // add listeners
    addListeners();
  }

  @Override
  public void dispose() {
    gaeServer.getServerWorkingCopy().removePropertyChangeListener(this);
    super.dispose();
  }

  @Override
  public IStatus[] getSaveStatus() {
    IStatus serverStatus = gaeServer.validate();
    return new IStatus[] {serverStatus};
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) {
    super.init(site, input);
    gaeServer = GaeServer.getGaeServer(server);
    gaeServer.getServerWorkingCopy().addPropertyChangeListener(this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if ("runtime-id".equals(evt.getPropertyName())) {
      GaeRuntime gaeRuntime = GaeServer.getGaeServer(gaeServer.getServerWorkingCopy()).getGaeRuntime();
      if (gaeRuntime != null) {
        // hide auto-reload delay controls as not applicable for SDKs < 1.8.1
        boolean isUsingAutoreload = SdkUtils.compareVersionStrings(gaeRuntime.getGaeSdkVersion(),
            RuntimeUtils.MIN_SDK_VERSION_USING_AUTORELOAD) >= 0;
        autoreloadTimeText.setVisible(isUsingAutoreload);
        autoreloadTimeLabel.setVisible(isUsingAutoreload);
        // check for EAR
        IStatus earSupported = gaeServer.validateEarSupported(gaeRuntime);
        if (!earSupported.isOK()) {
          setErrorMessage(earSupported.getMessage());
        } else {
          setErrorMessage("");
        }
      }
    }
  }

  private void addListeners() {
    serverPortNumberText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        execute(new GaeCommands(server, serverPortNumberText.getText().trim(),
            GaeServer.PROPERTY_SERVERPORT));
      }
    });
    autoreloadTimeText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        execute(new GaeCommands(server, autoreloadTimeText.getText().trim(),
            GaeServer.PROPERTY_AUTORELOAD_TIME));
      }
    });
  }

  private Label createLabel(Composite parent, String text, FormToolkit toolkit) {
    Label label = toolkit.createLabel(parent, text);
    label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    return label;
  }
}
