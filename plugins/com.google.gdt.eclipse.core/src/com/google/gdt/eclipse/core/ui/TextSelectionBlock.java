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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A helper to create a label, text box, and button.
 * 
 * The parent must have 3 columns. If the label is not shown, the text box will
 * span 2 columns.
 */
@SuppressWarnings("restriction")
public abstract class TextSelectionBlock {

  private final String label;

  private Text text;

  private Button button;

  private final String buttonLabel;

  protected TextSelectionBlock(String label, String buttonLabel) {
    this.label = label;
    this.buttonLabel = buttonLabel;
  }

  public void createContents(Composite parent, boolean showLabel) {
    createControls(parent, showLabel);
    initializeControls();
    addEventHandlers();
    validate();
  }

  /**
   * Sets the focus on the button, if it has been created.
   */
  public void setFocus() {
    if (button != null) {
      button.setFocus();
    }
  }

  protected Shell getShell() {
    return text.getShell();
  }

  protected String getText() {
    return text.getText();
  }

  /**
   * Subclasses should set the initial text.
   */
  protected abstract void initializeControls();

  /**
   * Called when the button is clicked. Subclasses should display a relevant
   * dialog, and return the new text or null if canceled. If the returned text
   * is not null, it will be set into the text box, and
   * {@link #onTextModified(IStatus)} will be called.
   */
  protected abstract String onButtonClicked();

  /**
   * Called when the text is modified (directly or indirectly via a selection
   * from the "Browse" dialog.) Subclasses should call any listeners.
   * {@link #validate()} will have been called prior to this method being
   * called.
   */
  protected abstract void onTextModified(IStatus status);

  protected void setText(String newTextString) {
    text.setText(newTextString != null ? newTextString : "");
  }

  /**
   * @return the status with the message as a complete sentence.
   */
  protected abstract IStatus validate();

  private void addEventHandlers() {
    text.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        IStatus status = validate();
        onTextModified(status);
      }
    });

    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String newTextString = onButtonClicked();
        if (newTextString != null) {
          text.setText(newTextString);
        }
      }
    });
  }

  private void createControls(Composite parent, boolean showLabel) {
    if (showLabel) {
      SWTFactory.createLabel(parent, label, 1);
    }

    text = new Text(parent, SWT.BORDER);
    GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
    if (!showLabel) {
      layoutData.horizontalSpan = 2;
    }
    text.setLayoutData(layoutData);

    button = SWTFactory.createPushButton(parent, buttonLabel, null);
  }
}
