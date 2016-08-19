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
package com.google.gdt.eclipse.core.ui;

import com.google.gdt.eclipse.core.sdk.Sdk;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite that encompasses the SDK selection logic.
 *
 *  TODO: Need to fire the SDK selection event if the combo boxes' SDK selection
 * changes as a result of the configuration dialog. Right now we always reselect
 * on configure so you may lose your selection.
 *
 * @param <T> type of sdk
 */
public abstract class SdkSelectionBlock<T extends Sdk> extends Composite {
  /**
   * Describes the selected Sdk in the SdkSelectionBlock.
   *
   * @param <T> type of Sdk
   */
  public static class SdkSelection<T extends Sdk> {
    private final boolean isDefault;
    private final T sdk;

    public SdkSelection(T sdk, boolean isDefault) {
      this.sdk = sdk;
      this.isDefault = isDefault;
    }

    public T getSelectedSdk() {
      return sdk;
    }

    public boolean isDefault() {
      return isDefault;
    }
  }

  /**
   * Triggered when an SDK selection takes place.
   */
  public interface SdkSelectionListener {
    /**
     * 
     */
    class SdkSelectionEvent {
      private final SdkSelectionBlock<? extends Sdk> control;

      public SdkSelectionEvent(SdkSelectionBlock<? extends Sdk> control) {
        this.control = control;
      }

      public SdkSelectionBlock<? extends Sdk> getControl() {
        return control;
      }
    }

    void onSdkSelection(SdkSelectionEvent e);
  }

  private Link configureOrDownloadLink;
  private final Button defaultSdkButton;
  private final Combo sdkComboBox;
  private ComboViewer sdkComboViewer;
  private final List<SdkSelectionListener> selectionListeners =
      new ArrayList<SdkSelectionListener>();

  private final Button specificSdkButton;

  /**
   * Note: subclasses are expected to call {@link #updateSdkBlockControls()}
   * after delegating to this constructor.
   */
  public SdkSelectionBlock(Composite parent, int style) {
    super(parent, style);

    setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    final GridLayout gwtRuntimeGroupLayout = new GridLayout();
    gwtRuntimeGroupLayout.numColumns = 3;
    setLayout(gwtRuntimeGroupLayout);

    defaultSdkButton = new Button(this, SWT.RADIO);
    defaultSdkButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
    defaultSdkButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        if (defaultSdkButton.getSelection()) {
          updateSdkBlockControls();
          fireSdkSelectionEvent();
        }
      }
    });

    configureOrDownloadLink = new Link(this, SWT.NONE);
    configureOrDownloadLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        doConfigure();
        // This could have changed.
        initializeSdkComboBox();
        updateSdkBlockControls();

        if (!SdkSelectionBlock.this.doGetSpecificSdks().isEmpty()) {
          fireSdkSelectionEvent();
        }
      }
    });

    // choose specific runtime
    specificSdkButton = new Button(this, SWT.RADIO);
    specificSdkButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (specificSdkButton.getSelection()) {
          updateSdkBlockControls();
          fireSdkSelectionEvent();
        }
      }
    });
    specificSdkButton.setText("Use specific SDK:");

    // specific runtime selection combo
    sdkComboBox = new Combo(this, SWT.READ_ONLY);
    sdkComboBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    sdkComboBox.setEnabled(false);
    sdkComboBox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(final SelectionEvent e) {
        fireSdkSelectionEvent();
      }
    });

    sdkComboViewer = new ComboViewer(sdkComboBox);
    sdkComboViewer.setContentProvider(new ArrayContentProvider());
    sdkComboViewer.setLabelProvider(new LabelProvider() {
      @Override
      @SuppressWarnings("unchecked")
      public String getText(Object element) {
        return ((T) element).getDescription();
      }
    });

    // vertical spacer
    new Label(this, SWT.NONE);
  }

  public void addSdkSelectionListener(SdkSelectionListener listener) {
    selectionListeners.add(listener);
  }

  public List<T> getSdks() {
    return doGetSpecificSdks();
  }

  @SuppressWarnings("unchecked")
  public SdkSelection<T> getSdkSelection() {
    if (isDefault() || sdkComboBox.getSelectionIndex() < 0) {
      // If the default was selected or the selection index is invalid
      return new SdkSelection<T>(doGetDefaultSdk(), isDefault());
    } else {
      return new SdkSelection<T>(
          (T) sdkComboViewer.getElementAt(sdkComboBox.getSelectionIndex()), false);
    }
  }

  public boolean isDefault() {
    return defaultSdkButton.getSelection();
  }

  public void removeSdkSelectionListener(SdkSelectionListener listener) {
    selectionListeners.remove(listener);
  }

  @Override
  public void setEnabled(boolean enabled) {
    /*
     * The action text is never disabled...?
     */
    setEnabledImpl(enabled);
    super.setEnabled(enabled);
  }

  /**
   * Template method called when a user clicks on the configure Sdk button.
   */
  protected abstract void doConfigure();

  protected abstract T doGetDefaultSdk();

  protected abstract List<T> doGetSpecificSdks();

  protected void initializeSdkComboBox() {
    sdkComboViewer.setInput(doGetSpecificSdks());
    if (sdkComboBox.getSelectionIndex() < 0) {
      sdkComboBox.select(0);
    }
    sdkComboViewer.refresh();
  }

  protected void setSelection(int selection) {
    boolean useDefault = selection < 0;

    defaultSdkButton.setSelection(useDefault);
    specificSdkButton.setSelection(!useDefault);

    sdkComboBox.select(selection);
    setEnabledImpl(true);
  }

  protected void updateSdkBlockControls() {
    T defaultSdk = doGetDefaultSdk();
    String defaultName = (defaultSdk != null ? defaultSdk.getDescription() : "none");

    String defaultSdkButtonText = "Use default SDK";
    if (defaultName.length() != 0) {
      defaultSdkButtonText += " (" + defaultName + ")";
    }
    defaultSdkButton.setText(defaultSdkButtonText);
    defaultSdkButton.getParent().layout(true, true);

    String linkText = "Configure SDKs...";

    // Likewise, we might want to switch back to this.
    // String linkText = allNames.length > 0 ? "Configure SDKs..."
    // : "Download SDKs...";

    configureOrDownloadLink.setText("<a href=\"#\">" + linkText + "</a>");
    setEnabledImpl(true);
  }

  private void fireSdkSelectionEvent() {
    SdkSelectionListener.SdkSelectionEvent event = new SdkSelectionListener.SdkSelectionEvent(this);
    for (SdkSelectionListener selectionListener : selectionListeners) {
      selectionListener.onSdkSelection(event);
    }
  }

  /**
   * Enables or disables all of this widget's controls.
   *
   *  Disabling is straightfoward - all of the controls in the widget are
   * disabled.
   *
   *  Enabling is more complex. Specifically, the radio buttons are only enabled
   * if the enabled parameter is true, and the name provider indicates that at
   * least one SDK is present. The "Specific SDK" combo box is only enabled IF
   * the radio buttons have been enabled, AND the currently-selected radio
   * button is the "Specific SDK" radio button.
   *
   * @param enabled
   */
  private void setEnabledImpl(boolean enabled) {
    configureOrDownloadLink.setEnabled(enabled);

    boolean shouldEnableSdkSelectionControls = enabled && (!doGetSpecificSdks().isEmpty());
    defaultSdkButton.setEnabled(shouldEnableSdkSelectionControls);
    specificSdkButton.setEnabled(shouldEnableSdkSelectionControls);

    sdkComboBox.setEnabled(shouldEnableSdkSelectionControls && specificSdkButton.getSelection());
  }
}
