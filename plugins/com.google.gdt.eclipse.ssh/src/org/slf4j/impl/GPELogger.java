/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package org.slf4j.impl;

import com.google.gdt.eclipse.core.Logger;
import com.google.gdt.eclipse.ssh.SSHPlugin;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * This is an implementation of slf4j logger to redirect to eclipse plugin
 * logging
 */
public class GPELogger extends MarkerIgnoringBase {

  private static final long serialVersionUID = 1L;

  private final Logger proxyLogger;

  public GPELogger(String name) {
    // TODO(appu) find a good way to use name
    proxyLogger = new Logger(SSHPlugin.getInstance());
  }

  public void debug(String message) {
    proxyLogger.logInfo(message);
  }

  public void debug(String message, Object arg) {
    proxyLogger.logInfo(MessageFormatter.format(message, arg).getMessage());
  }

  public void debug(String message, Object arg1, Object arg2) {
    proxyLogger.logInfo(MessageFormatter.format(message, arg1, arg2).getMessage());
  }

  public void debug(String message, Object[] argArray) {
    proxyLogger.logInfo(MessageFormatter.format(message, argArray).getMessage());
  }

  public void debug(String message, Throwable t) {
    proxyLogger.logInfo(message + " " + t.getMessage());
  }

  public void error(String message) {
    proxyLogger.logError(message);
  }

  public void error(String message, Object arg1) {
    proxyLogger.logError(MessageFormatter.format(message, arg1).getMessage());
  }

  public void error(String message, Object arg1, Object arg2) {
    proxyLogger.logError(MessageFormatter.format(message, arg1, arg2).getMessage());
  }

  public void error(String message, Object[] argArray) {
    proxyLogger.logError(MessageFormatter.format(message, argArray).getMessage());
  }

  public void error(String message, Throwable t) {
    proxyLogger.logError(t, message);
  }

  public void info(String message) {
    proxyLogger.logInfo(message);
  }

  public void info(String message, Object arg1) {
    proxyLogger.logInfo(MessageFormatter.format(message, arg1).getMessage());
  }

  public void info(String message, Object arg1, Object arg2) {
    proxyLogger.logInfo(MessageFormatter.format(message, arg1, arg2).getMessage());
  }

  public void info(String message, Object[] argArray) {
    proxyLogger.logInfo(MessageFormatter.format(message, argArray).getMessage());
  }

  public void info(String message, Throwable arg1) {
    proxyLogger.logInfo(message + " " + arg1.getMessage());
  }

  public boolean isDebugEnabled() {
    // TODO(appu) might want to get the log level from the gdtplugin somehow
    return true;
  }

  public boolean isErrorEnabled() {
    return true;
  }

  public boolean isInfoEnabled() {
    return true;
  }

  public boolean isTraceEnabled() {
    // lets just say trace isn't supported
    return false;
  }

  public boolean isWarnEnabled() {
    return true;
  }

  public void trace(String message) {
    // ignore
  }

  public void trace(String message, Object arg1) {
    // ignore
  }

  public void trace(String message, Object arg1, Object arg2) {
    // ignore
  }

  public void trace(String message, Object[] arg1) {
    // ignore
  }

  public void trace(String message, Throwable arg1) {
    // ignore
  }

  public void warn(String message) {
    proxyLogger.logWarning(message);
  }

  public void warn(String message, Object arg) {
    proxyLogger.logWarning(MessageFormatter.format(message, arg).getMessage());
  }

  public void warn(String message, Object arg1, Object arg2) {
    proxyLogger.logWarning(MessageFormatter.format(message, arg1, arg2).getMessage());
  }

  public void warn(String message, Object[] argArray) {
    proxyLogger.logWarning(MessageFormatter.format(message, argArray).getMessage());
  }

  public void warn(String message, Throwable t) {
    proxyLogger.logWarning(t, message);
  }
}
