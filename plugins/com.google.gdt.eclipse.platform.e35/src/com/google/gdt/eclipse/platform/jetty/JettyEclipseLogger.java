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
package com.google.gdt.eclipse.platform.jetty;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.mortbay.log.Logger;

/**
 * This class routes the logging output from Jetty to the Eclipse log.
 */
public class JettyEclipseLogger implements Logger {

  private static ILog platformLog;

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

  public void debug(String msg, Object arg0, Object arg1) {
    if (debug) {
      log(IStatus.OK, format(msg, arg0, arg1), null);
    }
  }

  public void debug(String msg, Throwable th) {
    if (debug) {
      log(IStatus.OK, msg, th);
    }
  }

  public Logger getLogger(String name) {
    return this;
  }

  public void info(String msg, Object arg0, Object arg1) {
    if (info) {
      log(IStatus.INFO, format(msg, arg0, arg1), null);
    }
  }

  public boolean isDebugEnabled() {
    return debug;
  }

  public void setDebugEnabled(boolean enabled) {
    debug = enabled;
  }

  public void warn(String msg, Object arg0, Object arg1) {
    log(IStatus.WARNING, format(msg, arg0, arg1), null);
  }

  public void warn(String msg, Throwable th) {
    log(IStatus.WARNING, msg, th);
  }

  private String format(String msg, Object arg0, Object arg1) {
    StringBuilder builder = new StringBuilder();

    int argPos0 = msg == null ? -1 : msg.indexOf("{}");
    int argPos1 = argPos0 < 0 ? -1 : msg.indexOf("{}", argPos0 + 2);

    if (argPos0 >= 0) {
      builder.append(msg.substring(0, argPos0));
      builder.append(String.valueOf(arg0 == null ? "null" : arg0));

      if (argPos1 >= 0) {
        builder.append(msg.substring(argPos0 + 2, argPos1));
        builder.append(String.valueOf(arg1 == null ? "null" : arg1));
        builder.append(msg.substring(argPos1 + 2));
      } else {
        builder.append(msg.substring(argPos0 + 2));
        if (arg1 != null) {
          builder.append(' ');
          builder.append(String.valueOf(arg1));
        }
      }
    } else {
      builder.append(msg);
      if (arg0 != null) {
        builder.append(' ');
        builder.append(String.valueOf(arg0));
      }
      if (arg1 != null) {
        builder.append(' ');
        builder.append(String.valueOf(arg1));
      }
    }

    return builder.toString();
  }

  private void log(int severity, String message, Throwable exception) {
    IStatus status = new Status(severity, PLUGIN_ID, "org.mortbay.jetty: " + message,
        exception);

    platformLog.log(status);
  }

}
