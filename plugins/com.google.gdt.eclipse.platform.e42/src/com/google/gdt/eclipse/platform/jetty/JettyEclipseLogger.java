/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.platform.jetty;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jetty.util.log.Logger;

/**
 * This class routes the logging output from Jetty to the Eclipse log.
 */
public class JettyEclipseLogger implements Logger {

  private static ILog platformLog;

  private static final String LOGGER_NAME = "org.eclipse.jetty";

  private static final String PLUGIN_ID = "com.google.gdt.eclipse.platform.shared";

  static {
    platformLog = Platform.getLog(Platform.getBundle(PLUGIN_ID));
  }

  private boolean debug;

  private boolean info;

  public JettyEclipseLogger() {
    debug = info = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID
        + "/jetty/debug"));

    if (debug) {
      info = true;
    } else {
      info = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID
          + "/jetty/info"));
    }
  }

  public void debug(String msg, Object... args) {
    if (debug) {
      log(IStatus.OK, format(msg, args), null);
    }
  }

  public void debug(String msg, Throwable th) {
    if (debug) {
      log(IStatus.OK, msg, th);
    }
  }

  public void debug(Throwable thrown) {
    debug("", thrown);
  }

  private String format(String msg, Object... args) {
    msg = String.valueOf(msg); // Avoids NPE
    String braces = "{}";
    StringBuilder builder = new StringBuilder();
    int start = 0;
    for (Object arg : args) {
      int bracesIndex = msg.indexOf(braces, start);
      if (bracesIndex < 0) {
        builder.append(msg.substring(start));
        builder.append(" ");
        builder.append(arg);
        start = msg.length();
      } else {
        builder.append(msg.substring(start, bracesIndex));
        builder.append(String.valueOf(arg));
        start = bracesIndex + braces.length();
      }
    }
    builder.append(msg.substring(start));
    return builder.toString();
  }

  public Logger getLogger(String name) {
    return this;
  }

  public String getName() {
    return LOGGER_NAME;
  }

  public void ignore(Throwable ignored) {
  }

  public void info(String msg, Object... args) {
    if (info) {
      log(IStatus.INFO, format(msg, args), null);
    }
  }

  public void info(String msg, Throwable thrown) {
    log(IStatus.WARNING, msg, thrown);
  }

  public void info(Throwable thrown) {
    info("", thrown);
  }

  public boolean isDebugEnabled() {
    return debug;
  }

  private void log(int severity, String message, Throwable exception) {
    IStatus status = new Status(severity, PLUGIN_ID, "org.mortbay.jetty: "
        + message, exception);

    platformLog.log(status);
  }

  public void setDebugEnabled(boolean enabled) {
    debug = enabled;
  }

  public void warn(String msg, Object... args) {
    log(IStatus.WARNING, format(msg, args), null);
  }

  public void warn(String msg, Throwable th) {
    log(IStatus.WARNING, msg, th);
  }

  public void warn(Throwable thrown) {
    warn("", thrown);
  }
}
