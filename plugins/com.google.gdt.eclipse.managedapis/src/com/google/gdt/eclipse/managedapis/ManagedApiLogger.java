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
package com.google.gdt.eclipse.managedapis;


import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * Basic utility class, in this case used for the managedapis plugin.
 */
public class ManagedApiLogger {
  public static final int CANCEL = IStatus.CANCEL;
  public static final int ERROR = IStatus.ERROR;
  public static final int INFO = IStatus.INFO;
  // logging severities
  public static final int OK = IStatus.OK;
  public static final int WARNING = IStatus.WARNING;
  // reference to the Eclipse error log
  private static ILog log;
  private static boolean TRACING_ENABLED;
  
  /*
   * Get a reference to the Eclipse error log
   */
  static {
    log = Platform.getLog(Platform.getBundle(ManagedApiPlugin.PLUGIN_ID));
    TRACING_ENABLED = "true".equals(Platform.getDebugOption(ManagedApiPlugin.PLUGIN_ID + "/debug"));
  }

  public static void error(String msg) {
    log(ERROR, msg);
  }

  public static void error(Throwable e) {
    log(ERROR, e);
  }

  public static void error(Throwable e, String msg) {
    log(ERROR, e, msg);
  }

  public static void info(String msg) {
    if (TRACING_ENABLED) {
      log(INFO, msg);
    }
  }

  public static void info(Throwable e) {
    if (TRACING_ENABLED) {
      log(INFO, e);
    }
  }

  public static void info(Throwable e, String msg) {
    if (TRACING_ENABLED) {
      log(INFO, e, msg);
    }
  }

  /*
   * Prints a message to the Eclipse error log
   */
  public static void log(int severity, String msg) {
    Status s = new Status(severity, ManagedApiPlugin.PLUGIN_ID, IStatus.OK,
        msg, null);
    log.log(s);
  }

  /*
   * Prints stack trace to Eclipse error log
   */
  public static void log(int severity, Throwable e) {
    Status s = new Status(severity, ManagedApiPlugin.PLUGIN_ID, IStatus.OK,
        e.getMessage(), e);
    log.log(s);
  }

  /*
   * Prints stack trace to Eclipse error log
   */
  public static void log(int severity, Throwable e, String msg) {
    Status s = new Status(severity, ManagedApiPlugin.PLUGIN_ID, IStatus.OK, msg
        + " " + e.getMessage(), e);
    log.log(s);
  }

  public static void warn(String msg) {
    log(WARNING, msg);
  }

  public static void warn(Throwable e) {
    log(WARNING, e);
  }

  public static void warn(Throwable e, String msg) {
    log(WARNING, e, msg);
  }
  
}
