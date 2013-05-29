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
package com.google.appengine.eclipse.wtp.wizards.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import java.util.List;

/**
 * This class is obsolete and will possibly be removed.
 */
public class SdkSelectionGroup<T> extends Group {

  public interface SdkSelectionDelegate<T> {
    boolean configureSdks();

    T getDefaultSdk();

    String getSdkTitle(T sdk);

    List<T> getSpecificSdkList();

    String getTitle();

    void onSdkSelection(T selectedSdk);

  }

  private boolean isDefaultSdk = true;

  private Label lblDefaultSdkName;
  private Button btnUseDefault;
  private Button btnUseSpecific;
  private ComboViewer cmbvSpecificSdk;
  private Combo cmbSpecificSdk;

  private SdkSelectionDelegate<T> model;

  /**
   * Create the composite.
   */
  public SdkSelectionGroup(Composite parent, int style, SdkSelectionDelegate<T> delegate) {
    super(parent, style);
    this.model = delegate;

    createControls();

    initializeSdkCombo();
    updateControls();
  }

  @Override
  protected final void checkSubclass() {
  }

  private void createControls() {
    setText(model.getTitle());
    setLayout(new GridLayout(3, false));

    btnUseDefault = new Button(this, SWT.RADIO);
    btnUseDefault.setText("Use Default");
    btnUseDefault.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        isDefaultSdk = true;
        updateControls();
        fireSdkSelectionEvent();
      }
    });

    lblDefaultSdkName = new Label(this, SWT.NONE);
    lblDefaultSdkName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    btnUseSpecific = new Button(this, SWT.RADIO);
    btnUseSpecific.setText("Use Specific");
    btnUseSpecific.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        isDefaultSdk = false;
        updateControls();
        fireSdkSelectionEvent();
      }
    });

    cmbvSpecificSdk = new ComboViewer(this, SWT.READ_ONLY);
    cmbSpecificSdk = cmbvSpecificSdk.getCombo();
    cmbSpecificSdk.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        fireSdkSelectionEvent();
      }
    });
    cmbvSpecificSdk.setLabelProvider(new LabelProvider() {
      @Override
      @SuppressWarnings("unchecked")
      public String getText(Object element) {
        return model.getSdkTitle((T) element);
      }
    });
    cmbvSpecificSdk.setContentProvider(new ArrayContentProvider());
    cmbSpecificSdk.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Link lnkConfigure = new Link(this, SWT.NONE);
    lnkConfigure.setText("<a>Configure...</a>");
    lnkConfigure.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (model.configureSdks()) {
          initializeSdkCombo();
          updateControls();
          if (!isDefaultSdk && !model.getSpecificSdkList().isEmpty()) {
            fireSdkSelectionEvent();
          }
        }
      }
    });
  }

  private void fireSdkSelectionEvent() {
    if (isDefaultSdk) {
      model.onSdkSelection(model.getDefaultSdk());
    } else {
      model.onSdkSelection(model.getSpecificSdkList().get(cmbSpecificSdk.getSelectionIndex()));
    }
  }

  private void initializeSdkCombo() {
    cmbvSpecificSdk.setInput(model.getSpecificSdkList());
    if (cmbSpecificSdk.getSelectionIndex() < 0) {
      cmbSpecificSdk.select(0);
    }
    cmbvSpecificSdk.refresh();
  }

  private void updateControls() {
    T defaultSdk = model.getDefaultSdk();
    String defaultName;
    defaultName = defaultSdk != null ? model.getSdkTitle(defaultSdk) : "<unknown>";
    btnUseDefault.setSelection(isDefaultSdk);
    lblDefaultSdkName.setEnabled(isDefaultSdk);

    btnUseSpecific.setSelection(!isDefaultSdk);
    cmbSpecificSdk.setEnabled(!isDefaultSdk);

    lblDefaultSdkName.setText(defaultName);
    lblDefaultSdkName.getParent().layout(true, true);
  }
}
