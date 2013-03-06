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

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

// TODO: convert existing TitleAreaDialog subclasses to this
/**
 * An abstract subclass of TitleAreaDialog that contains much of the boilerplate
 * required when using a TitleAreaDialog.
 */
public abstract class AbstractTitleAreaDialog extends TitleAreaDialog {

  private static int convertSeverity(IStatus status) {
    switch (status.getSeverity()) {
      case IStatus.ERROR:
        return IMessageProvider.ERROR;
      case IStatus.WARNING:
        return IMessageProvider.WARNING;
      case IStatus.INFO:
        return IMessageProvider.INFORMATION;
      default:
        return IMessageProvider.NONE;
    }
  }

  private final String shellTitle;
  private final String title;
  private final String summary;
  private final Image image;

  private final String okButtonLabel;

  private Button okButton;

  /**
   * @param okButtonLabel null to use the default label
   */
  public AbstractTitleAreaDialog(Shell parentShell, String shellTitle,
      String title, String summary, Image image, String okButtonLabel) {
    super(parentShell);

    this.shellTitle = shellTitle;
    this.title = title;
    this.summary = summary;
    this.image = image;
    this.okButtonLabel = okButtonLabel;

    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  public void updateStatus(IStatus status) {
    if (status.getSeverity() == IStatus.OK) {
      status = StatusUtilities.newOkStatus(summary, CorePlugin.PLUGIN_ID);
    }

    setMessage(status.getMessage(), convertSeverity(status));
    okButton.setEnabled(status.getSeverity() != IStatus.ERROR);
  }

  public void updateStatus(IStatus... statuses) {
    updateStatus(StatusUtilities.getMostImportantStatusWithMessage(statuses));
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(shellTitle);
    setHelpAvailable(false);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);

    okButton = getButton(IDialogConstants.OK_ID);

    // Re-label the OK button and set it as default
    if (okButtonLabel != null) {
      okButton.setText(okButtonLabel);
    }

    getShell().setDefaultButton(okButton);
  }

  @Override
  protected Control createContents(Composite parent) {
    Control contents = super.createContents(parent);

    setTitle(title);
    setTitleImage(image);

    // Perform initial validation
    validate();

    return contents;
  }

  /**
   * Placeholder for a stateless validation implementation.  This will be called by {@link AbstractTitleAreaDialog} initially.
   */
  protected void validate() {
  }

}
