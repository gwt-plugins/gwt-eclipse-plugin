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
package com.google.gdt.eclipse.gph.util;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import java.net.URL;

/**
 * A utility class for opening URLs in an external browser.
 */
public class BrowserHelper {

  /**
   * Given a URL and a shell, open the URL in an external browser.
   * 
   * @param shell the current shell
   * @param url the URL to open
   */
  public static void openUrl(Shell shell, String url) {
    try {
      IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
      support.getExternalBrowser().openURL(new URL(url));
    } catch (Exception e) {
      ProjectHostingUIPlugin.logError(e);
      
      MessageDialog.openError(shell,
          "Error Opening Browser",
          e.getMessage());
    }
  }

}
