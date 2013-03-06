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

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;

import java.text.MessageFormat;

/**
 * Common logger class that is associated with a plugin.
 */
public class Logger {
  private final ILog log;

  public Logger(Plugin plugin) {
    this.log = plugin.getLog();
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logError(String message) {
    LogUtils.logError(log, null, message);
  }

  /**
   * Log the specified error.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logError(String message, Object... args) {
    LogUtils.logError(log, null, message, args);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception.
   */
  public void logError(Throwable exception) {
    LogUtils.logError(log, exception, "Unexpected Exception");
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not
   *          applicable.
   * @param message a human-readable message, localized to the current locale.
   */
  public void logError(Throwable exception, String message) {
    LogUtils.log(log, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified error.
   * 
   * @param exception a low-level exception, or <code>null</code> if not
   *          applicable.
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logError(Throwable exception, String message, Object... args) {
    message = MessageFormat.format(message, args);
    LogUtils.log(log, IStatus.ERROR, IStatus.OK, message, exception);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logInfo(String message) {
    LogUtils.logInfo(log, message, new Object[0]);
  }

  /**
   * Log the specified information.
   * 
   * @param message a human-readable message, localized to the current locale.
   * @param args message arguments.
   */
  public void logInfo(String message, Object... args) {
    message = MessageFormat.format(message, args);
    LogUtils.log(log, IStatus.INFO, IStatus.OK, message, null);
  }

  /**
   * Log the specified warning.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logWarning(String message) {
    LogUtils.log(log, IStatus.WARNING, IStatus.OK, message, null);
  }

  /**
   * Log the specified warning.
   * 
   * @param message a human-readable message, localized to the current locale.
   */
  public void logWarning(Throwable e, String message) {
    LogUtils.log(log, IStatus.WARNING, IStatus.OK, message, e);
  }
}
