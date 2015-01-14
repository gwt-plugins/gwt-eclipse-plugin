/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * Cloud SDK WTP plug-in life-cycle.
 */
public final class CloudSdkPlugin extends AbstractUIPlugin {
  public static final String PLUGIN_ID = "com.google.cloudsdk.eclipse.wtp";

  private static CloudSdkPlugin instance;

  public CloudSdkPlugin() {
    instance = this;
  }

  /**
   * Gets the {@link CloudSdkPlugin} object.
   *
   * @return the {@link CloudSdkPlugin} object
   */
  public static CloudSdkPlugin getInstance() {
    return instance;
  }

  /**
   * Logs the specified message and opens an error dialog with the specified message and title.
   * Ensures that the error dialog is opened on the UI thread.
   *
   * @param parent the parent shell of the dialog, or null if none
   * @param title the dialog's title
   * @param message the message
   */
  public static void logAndDisplayError(final Shell parent, final String title,
      final String message){
    logError(message);

    if (Display.getCurrent() != null) {
      MessageDialog.openError(parent, title, message);
    } else {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          MessageDialog.openError(parent, title, message);
        }
      });
    }
  }

  /**
   * Logs the specified message and exception to platform log file which can be viewed via the
   * PDE Error Log View.
   *
   * @param message the message
   * @param exception the exception
   */
  public static void logError(String message, Throwable exception) {
    message = message == null ? "Google Cloud SDK Error" : "Google Cloud SDK: " + message;
    Status status = new Status(IStatus.ERROR, PLUGIN_ID, 1, message, exception);
    getInstance().getLog().log(status);
  }

  /**
   * Logs the specified exception to platform log file which can be viewed via the PDE Error Log
   * View.
   *
   * @param exception the exception
   */
  public static void logError(Throwable exception) {
    logError(null, exception);
  }

  /**
   * Logs the specified message to platform log file which can be viewed via the PDE Error Log
   * View.
   *
   * @param message the message
   */
  public static void logError(String message) {
    logError(message, null);
  }
}
