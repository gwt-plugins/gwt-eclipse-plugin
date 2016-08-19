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

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Base class for project properties pages backed by Eclipse preferences store.
 */
public abstract class AbstractProjectPropertyPage extends PropertyPage
    implements IWorkbenchPropertyPage {

  /**
   * Converts a standard IStatus's severity into the severity flags used by
   * dialogs and property pages.
   */
  protected static int convertSeverity(IStatus status) {
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

  /**
   * Returns the IStatus with the most critical severity.
   */
  protected static IStatus getMostImportantStatus(IStatus[] status) {
    IStatus max = null;
    for (int i = 0; i < status.length; i++) {
      IStatus curr = status[i];
      if (curr.matches(IStatus.ERROR)) {
        return curr;
      }
      if (max == null || curr.getSeverity() > max.getSeverity()
          || max.getMessage().length() == 0) {
        max = curr;
      }
    }
    return max;
  }

  /**
   * Saves the project properties, logging any errors and displaying them to the
   * user in a dialog.
   */
  @Override
  public final boolean performOk() {
    try {
      saveProjectProperties();
      return true;
    } catch (BackingStoreException e) {
      CorePluginLog.logError(e);
      MessageDialog.openError(getShell(), "Project Properties",
          "Failed to save project properties.  Try refreshing your project.");
    } catch (Exception e) {
      // We're catching all errors here, because if we let one escape from this
      // method, the user will not be able to click OK to close the dialog, and
      // the only indication that anything wrong will be in the error log (which
      // will probably not be open and may not even be accessible).
      CorePluginLog.logError(e);
      MessageDialog.openError(getShell(), "Project Properties",
          "An error occurred while saving project properties:\n\n"
              + e.getLocalizedMessage()
              + "\n\nSee the Eclipse error log for more details.");
    }
    return false;
  }

  protected IJavaProject getJavaProject() {
    return JavaCore.create(getProject());
  }

  protected IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

  /**
   * Subclasses override this instead of performOk() to handle saving the
   * project properties.
   */
  protected abstract void saveProjectProperties() throws Exception;

  protected final void updateStatus(IStatus... status) {
    updateStatus(getMostImportantStatus(status));
  }

  private void updateStatus(IStatus status) {
    String message = status.getMessage();

    // If the status is not OK (either an error, warning, or info), then there
    // needs to be a user-visible message displayed.
    assert (status.isOK() || (message != null && message.length() > 0));

    if (message != null && message.length() == 0) {
      // If we have no errors or warnings, we must pass in a null message. If
      // we pass in an empty string, it will overwrite the title of the property
      // page
      message = null;
    }

    setMessage(message, convertSeverity(status));
    setValid(status.getSeverity() != IStatus.ERROR);
  }
}
