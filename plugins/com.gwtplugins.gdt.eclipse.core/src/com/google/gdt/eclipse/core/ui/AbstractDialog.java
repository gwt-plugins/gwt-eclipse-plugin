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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * Abstract {@link TrayDialog} subclass to reduce boilerplate and set common
 * defaults.
 */
public abstract class AbstractDialog extends TrayDialog {

  private final String okButtonLabel;
  private final String shellTitle;

  public AbstractDialog(Shell parentShell, String shellTitle,
      String okButtonLabel) {
    super(parentShell);

    this.shellTitle = shellTitle;
    this.okButtonLabel = okButtonLabel;

    setShellStyle(getShellStyle() | SWT.RESIZE);
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

    Button okButton = getButton(IDialogConstants.OK_ID);

    // Re-label the OK button and set it as default
    if (okButtonLabel != null) {
      okButton.setText(okButtonLabel);
    }

    getShell().setDefaultButton(okButton);
  }

}
