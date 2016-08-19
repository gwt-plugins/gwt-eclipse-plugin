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
package com.google.gdt.eclipse.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.text.MessageFormat;

/**
 * Generates IStatus objects for errors, warnings, and info messages.
 * 
 * TODO: refactor existing dialog and wizard validation to use these instead of
 * plugin-specific IStatus factories.
 */
public class StatusUtilities {

  public static final Status OK_STATUS = new Status(IStatus.OK,
      CorePlugin.PLUGIN_ID, "");

  public static IStatus getMostImportantStatusWithMessage(IStatus... status) {
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
  
  public static IStatus newErrorStatus(Exception ex, String pluginId) {
    return new Status(IStatus.ERROR, pluginId, ex.getLocalizedMessage(), ex);
  }

  public static IStatus newErrorStatus(String message, String pluginId) {
    return new Status(IStatus.ERROR, pluginId, message);
  }

  public static IStatus newErrorStatus(String message, String pluginId,
      Object... args) {
    return newErrorStatus(MessageFormat.format(message, args), pluginId);
  }

  public static IStatus newInfoStatus(String message, String pluginId) {
    return new Status(IStatus.INFO, pluginId, message);
  }

  public static IStatus newInfoStatus(String message, String pluginId, Object... args) {
    return newInfoStatus(MessageFormat.format(message, args), pluginId);
  }

  public static IStatus newOkStatus(String message, String pluginId) {
    return new Status(IStatus.OK, pluginId, message);
  }

  public static IStatus newOkStatus(String message, String pluginId,
      Object... args) {
    return newOkStatus(MessageFormat.format(message, args), pluginId);
  }

  public static IStatus newWarningStatus(String message, String pluginId) {
    return new Status(IStatus.WARNING, pluginId, message);
  }

  public static IStatus newWarningStatus(String message, String pluginId,
      Object... args) {
    return newWarningStatus(MessageFormat.format(message, args), pluginId);
  }
}
