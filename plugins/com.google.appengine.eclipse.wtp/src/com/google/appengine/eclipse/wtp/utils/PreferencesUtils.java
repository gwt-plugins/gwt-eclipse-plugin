/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.utils;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Utility class for preferences.
 */
public final class PreferencesUtils {
  /**
   * Displays preferences in new dialog, without reusing existing. <br>
   * Note: pageID must be direct descendant to rootID.
   *
   * @return <code>false</code> if modal result of the open dialog is not Window.OK,
   *         <code>true</code> otherwise
   * @see org.eclipse.ui.dialogs.PreferencesUtil
   */
  public static boolean showPreferencePage(Shell shell, String rootID, String pageID) {
    PreferenceManager workbenchManager = PlatformUI.getWorkbench().getPreferenceManager();
    IPreferenceNode node = workbenchManager.find(rootID);
    if (node == null) {
      return false;
    }
    IPreferenceNode subNode = node.findSubNode(pageID);
    PreferenceManager prefManager = new PreferenceManager();
    prefManager.addToRoot(subNode);
    final PreferenceDialog dialog = new PreferenceDialog(shell, prefManager);
    final boolean[] result = new boolean[] {false};
    BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
      @Override
      public void run() {
        dialog.create();
        if (dialog.open() == Window.OK) {
          result[0] = true;
        }
      }
    });
    return result[0];
  }

  /**
   * Private ctor
   */
  private PreferencesUtils() {
  }
}
