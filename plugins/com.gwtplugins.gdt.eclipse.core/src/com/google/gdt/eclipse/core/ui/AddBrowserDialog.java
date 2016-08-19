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
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Streamlined "Add a browser" dialog with behavior suited for GPE use. For
 * example, this takes care of all the nastiness surrounding Mac browser
 * launches.
 */
@SuppressWarnings("restriction")
public class AddBrowserDialog extends AbstractTitleAreaDialog {

  /**
   * Validates the entered browser details.
   */
  public interface Validator {
    /**
     * @param name non-null entered name of the browser
     * @param location non-null entered location of the browser
     * @return the validation status with the message as a complete sentence.
     */
    IStatus validateEnteredBrowser(String name, String location);
  }

  private BrowserLocationSelectionBlock browserLocationBlock = new BrowserLocationSelectionBlock(
      new BrowserLocationSelectionBlock.Listener() {
        public void browserLocationChanged(String location, IStatus status) {
          validate();
        }
      });

  private Text nameText;

  private IBrowserDescriptor browser;

  private final String defaultBrowserName;

  private final String note;

  private final List<Validator> validators = new ArrayList<Validator>();

  public AddBrowserDialog(Shell parentShell) {
    this(parentShell, "Add a Browser", "Add a Browser", "", null);
  }

  /**
   * @param defaultBrowserName the default value for the browser name, or null
   * @param note a message shown at the bottom of the dialog, or null
   */
  public AddBrowserDialog(Shell parentShell, String shellTitle, String title,
      String defaultBrowserName, String note) {
    super(parentShell, shellTitle, title, "", null, "Add");

    this.note = note;
    this.defaultBrowserName = defaultBrowserName != null ? defaultBrowserName
        : "";
  }

  public void addValidator(Validator validator) {
    validators.add(validator);
  }

  /**
   * @return the added browser (or null)
   */
  public String getBrowserName() {
    return browser != null ? browser.getName() : null;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite dialogArea = (Composite) super.createDialogArea(parent);
    Composite composite = SWTFactory.createComposite(dialogArea, 3, 1,
        GridData.FILL_HORIZONTAL);

    SWTFactory.createLabel(composite, "Name", 1);
    nameText = new Text(composite, SWT.BORDER);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.widthHint = 250;
    nameText.setLayoutData(gridData);
    new Label(composite, SWT.NONE);
    nameText.setText(defaultBrowserName);
    nameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });

    browserLocationBlock.createContents(composite, true);

    if (note != null) {
      SWTFactory.createVerticalSpacer(composite, 1);
      SWTFactory.createLabel(composite, note, 3);
    }

    if (!StringUtilities.isEmpty(defaultBrowserName)) {
      // The browser name has a default value in it, switch focus to something
      // that does not
      browserLocationBlock.setFocus();
    }

    return composite;
  }

  @Override
  protected void okPressed() {
    createBrowserModel();

    super.okPressed();
  }

  @Override
  protected void validate() {
    String browserName = nameText.getText();
    if (StringUtilities.isEmpty(browserName)) {
      updateStatus(StatusUtilities.newErrorStatus("Enter a browser name.",
          CorePlugin.PLUGIN_ID));
      return;
    }

    if (BrowserUtilities.findBrowser(browserName) != null) {
      updateStatus(StatusUtilities.newErrorStatus(
          "The entered browser name already exists.", CorePlugin.PLUGIN_ID));
      return;
    }

    List<IStatus> allStatus = new ArrayList<IStatus>();
    allStatus.add(browserLocationBlock.validate());
    for (Validator validator : validators) {
      String location = browserLocationBlock.getLocation();
      if (location == null) {
        location = "";
      }

      allStatus.add(validator.validateEnteredBrowser(browserName, location));
    }

    updateStatus(allStatus.toArray(new IStatus[allStatus.size()]));
  }

  private void createBrowserModel() {
    this.browser = BrowserUtilities.createBrowserDescriptor(nameText.getText(),
        browserLocationBlock.getLocation());
  }
}
