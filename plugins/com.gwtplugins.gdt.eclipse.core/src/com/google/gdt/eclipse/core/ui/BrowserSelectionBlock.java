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
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

/**
 * A helper to create a label, text box, and button to choose a browser from
 * those defined in the Eclipse preferences.
 * 
 * The parent must have 3 columns. If the label is not shown, the text box will
 * span 2 columns.
 */
@SuppressWarnings("restriction")
public class BrowserSelectionBlock extends TextSelectionBlock {

  /**
   * Listens for callbacks from {@link BrowserSelectionBlock}.
   */
  public interface Listener {
    void browserSelected(IBrowserDescriptor browserDescriptor, IStatus status);
  }

  private final Listener listener;

  private IBrowserDescriptor browser;

  public BrowserSelectionBlock(IBrowserDescriptor browser, Listener listener) {
    super("Browser:", "&Browse...");

    this.browser = browser;
    this.listener = listener;
  }

  public IBrowserDescriptor getBrowser() {
    return browser;
  }

  public String getBrowserName() {
    return browser != null ? browser.getName() : "";
  }

  public void setBrowser(String browserName) {
    browser = BrowserUtilities.findBrowser(browserName);
    setBrowserText(browser);
  }

  @Override
  public IStatus validate() {
    browser = null;

    String browserName = getText().trim();
    if (browserName.length() == 0) {
      return StatusUtilities.newErrorStatus(
          "Enter a name for the browser (for example, Chrome.)",
          CorePlugin.PLUGIN_ID);
    }

    browser = BrowserUtilities.findBrowser(browserName);
    if (browser == null) {
      return StatusUtilities.newErrorStatus("Enter a valid browser.",
          CorePlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  @Override
  protected void initializeControls() {
    setBrowserText(browser);
  }

  @Override
  protected String onButtonClicked() {
    IBrowserDescriptor selectedBrowser = BrowserSelectionDialog.chooseBrowser(
        getShell(), browser);
    return selectedBrowser != null ? selectedBrowser.getName() : null;
  }

  @Override
  protected void onTextModified(IStatus status) {
    listener.browserSelected(browser, status);
  }

  private void setBrowserText(IBrowserDescriptor browser) {
    setText(browser != null ? browser.getName() : "");
  }
}
