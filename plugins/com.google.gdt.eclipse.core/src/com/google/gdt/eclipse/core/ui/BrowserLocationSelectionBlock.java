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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.FileDialog;

import java.io.File;

/**
 * Shows and allows for selection of a browser's location.
 */
public class BrowserLocationSelectionBlock extends TextSelectionBlock {

  /**
   * Listeners for events.
   */
  public interface Listener {
    void browserLocationChanged(String location, IStatus status);
  }

  private static final boolean IS_MAC = Platform.OS_MACOSX.equals(Platform.getOS());
  private final Listener listener;

  public BrowserLocationSelectionBlock(Listener listener) {
    super("Location", "&Browse");
    this.listener = listener;
  }

  public String getLocation() {
    return getText();
  }

  @Override
  protected void initializeControls() {
  }

  @Override
  protected String onButtonClicked() {
    FileDialog dialog = new FileDialog(getShell());
    dialog.setText("Select a browser");
    return dialog.open();
  }

  @Override
  protected void onTextModified(IStatus status) {
    listener.browserLocationChanged(getLocation(), status);
  }

  @Override
  protected IStatus validate() {
    String locationStr = getText();
    if (StringUtilities.isEmpty(locationStr)) {
      return StatusUtilities.newErrorStatus(
          "Enter the location of the browser.", CorePlugin.PLUGIN_ID);
    }

    File locationFile = new File(getText());
    if (!locationFile.exists()) {
      return StatusUtilities.newErrorStatus(
          "Browser does not exist at the entered location.",
          CorePlugin.PLUGIN_ID);
    }

    // Allow directories for Mac, since that's what the app bundles are
    if (!IS_MAC && locationFile.isDirectory()) {
      return StatusUtilities.newErrorStatus(
          "Select an executable file (not a directory.)", CorePlugin.PLUGIN_ID);
    }

    if (IS_MAC && !locationFile.getName().toLowerCase().endsWith(".app")) {
      return StatusUtilities.newWarningStatus(
          "Mac applications typically end in \".app\".", CorePlugin.PLUGIN_ID);
    }
    
    return StatusUtilities.OK_STATUS;
  }

}
