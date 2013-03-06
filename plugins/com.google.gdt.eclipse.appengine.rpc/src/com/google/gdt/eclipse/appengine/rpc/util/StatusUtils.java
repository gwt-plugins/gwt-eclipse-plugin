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
package com.google.gdt.eclipse.appengine.rpc.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;

/**
 * Status utilities.
 */
public class StatusUtils {

  /**
   * A settable IStatus. Can be an error, warning, info or ok. For error, info
   * and warning states, a message describes the problem.
   */
  public static class StatusInfo implements IStatus {

    public static final IStatus OK_STATUS = new StatusInfo();

    private String statusMessage;
    private int severity;

    /**
     * Creates a status set to OK (no message)
     */
    public StatusInfo() {
      this(OK, null);
    }

    /**
     * Creates a status .
     * 
     * @param severity The status severity: ERROR, WARNING, INFO and OK.
     * @param message The message of the status. Applies only for ERROR, WARNING
     *          and INFO.
     */
    public StatusInfo(int severity, String message) {
      statusMessage = message;
      this.severity = severity;
    }

    /**
     * Returns always an empty array.
     * 
     * @see IStatus#getChildren()
     */
    public IStatus[] getChildren() {
      return new IStatus[0];
    }

    /**
     * Returns always the error severity.
     * 
     * @see IStatus#getCode()
     */
    public int getCode() {
      return severity;
    }

    /**
     * Returns always <code>null</code>.
     * 
     * @see IStatus#getException()
     */
    public Throwable getException() {
      return null;
    }

    /**
     * @see IStatus#getMessage
     */
    public String getMessage() {
      return statusMessage;
    }

    /*
     * @see IStatus#getPlugin()
     */
    public String getPlugin() {
      return JavaUI.ID_PLUGIN;
    }

    /*
     * @see IStatus#getSeverity()
     */
    public int getSeverity() {
      return severity;
    }

    /**
     * Returns if the status' severity is ERROR.
     */
    public boolean isError() {
      return severity == IStatus.ERROR;
    }

    /**
     * Returns if the status' severity is INFO.
     */
    public boolean isInfo() {
      return severity == IStatus.INFO;
    }

    /**
     * Returns always <code>false</code>.
     * 
     * @see IStatus#isMultiStatus()
     */
    public boolean isMultiStatus() {
      return false;
    }

    /**
     * Returns if the status' severity is OK.
     */
    public boolean isOK() {
      return severity == IStatus.OK;
    }

    /**
     * Returns if the status' severity is WARNING.
     */
    public boolean isWarning() {
      return severity == IStatus.WARNING;
    }

    /*
     * @see IStatus#matches(int)
     */
    public boolean matches(int severityMask) {
      return (severity & severityMask) != 0;
    }

    /**
     * Sets the status to ERROR.
     * 
     * @param errorMessage The error message (can be empty, but not null)
     */
    public void setError(String errorMessage) {
      Assert.isNotNull(errorMessage);
      statusMessage = errorMessage;
      severity = IStatus.ERROR;
    }

    /**
     * Sets the status to INFO.
     * 
     * @param infoMessage The info message (can be empty, but not null)
     */
    public void setInfo(String infoMessage) {
      Assert.isNotNull(infoMessage);
      statusMessage = infoMessage;
      severity = IStatus.INFO;
    }

    /**
     * Sets the status to OK.
     */
    public void setOK() {
      statusMessage = null;
      severity = IStatus.OK;
    }

    /**
     * Sets the status to WARNING.
     * 
     * @param warningMessage The warning message (can be empty, but not null)
     */
    public void setWarning(String warningMessage) {
      Assert.isNotNull(warningMessage);
      statusMessage = warningMessage;
      severity = IStatus.WARNING;
    }

    /**
     * Returns a string representation of the status, suitable for debugging
     * purposes only.
     */
    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("StatusInfo "); //$NON-NLS-1$
      if (severity == OK) {
        buf.append("OK"); //$NON-NLS-1$
      } else if (severity == ERROR) {
        buf.append("ERROR"); //$NON-NLS-1$
      } else if (severity == WARNING) {
        buf.append("WARNING"); //$NON-NLS-1$
      } else if (severity == INFO) {
        buf.append("INFO"); //$NON-NLS-1$
      } else {
        buf.append("severity="); //$NON-NLS-1$
        buf.append(severity);
      }
      buf.append(": "); //$NON-NLS-1$
      buf.append(statusMessage);
      return buf.toString();
    }
  }

  /**
   * Applies the status to the status line of a dialog page.
   * 
   * @param page the dialog page
   * @param status the status to apply
   */
  public static void applyToStatusLine(DialogPage page, IStatus status) {
    String message = status.getMessage();
    if (message != null && message.length() == 0) {
      message = null;
    }
    switch (status.getSeverity()) {
      case IStatus.OK:
        page.setMessage(message, IMessageProvider.NONE);
        page.setErrorMessage(null);
        break;
      case IStatus.WARNING:
        page.setMessage(message, IMessageProvider.WARNING);
        page.setErrorMessage(null);
        break;
      case IStatus.INFO:
        page.setMessage(message, IMessageProvider.INFORMATION);
        page.setErrorMessage(null);
        break;
      default:
        page.setMessage(null);
        page.setErrorMessage(message);
        break;
    }
  }

  /**
   * Creates a status set to OK (no message)
   */
  public static StatusInfo createOKStatus() {
    return new StatusInfo();
  }

  /**
   * Creates a status .
   * 
   * @param severity The status severity: ERROR, WARNING, INFO and OK.
   * @param message The message of the status. Applies only for ERROR, WARNING
   *          and INFO.
   */
  public static StatusInfo createStatus(int severity, String message) {
    return new StatusInfo(severity, message);
  }

  /**
   * Compares two instances of <code>IStatus</code>. The more severe is
   * returned: An error is more severe than a warning, and a warning is more
   * severe than ok. If the two stati have the same severity, the second is
   * returned.
   * 
   * @param s1 first status
   * @param s2 second status
   * @return the more severe status
   */
  public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
    if (s1.getSeverity() > s2.getSeverity()) {
      return s1;
    } else {
      return s2;
    }
  }

  /**
   * Finds the most severe status from a array of stati. An error is more severe
   * than a warning, and a warning is more severe than ok.
   * 
   * @param status an array of stati
   * @return the most severe status
   */
  public static IStatus getMostSevere(IStatus[] status) {
    IStatus max = null;
    for (int i = 0; i < status.length; i++) {
      IStatus curr = status[i];
      if (curr.matches(IStatus.ERROR)) {
        return curr;
      }
      if (max == null || curr.getSeverity() > max.getSeverity()) {
        max = curr;
      }
    }
    return max;
  }
}