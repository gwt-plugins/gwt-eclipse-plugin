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
package com.google.gwt.eclipse.oophm;

import com.google.gdt.eclipse.core.PluginProperties;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

import java.text.MessageFormat;
import java.util.LinkedList;

/**
 * A circular buffer to temporarily store debug/trace information.
 * 
 * Added to investigate
 * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=13
 */
public class LogSniffer extends AbstractHandler {
  /**
   * Valid user commands.
   */
  private enum Commands {
    CMD_DUMP, CMD_CLEAR
  }

  private static final String DEFAULT_LOG_SIZE = "256";
  private static final LinkedList<String> logBuffer = new LinkedList<String>();
  private static final int logSize;
  private static final Object logLock = new Object();

  private static final String CMD_PARAM_ID = "com.google.gwt.eclipse.oophm.logSniffer.param1";

  static {
    PluginProperties props = new PluginProperties(Activator.class);
    logSize = Integer.valueOf(props.getProperty("logSnifferSize",
        DEFAULT_LOG_SIZE));
  }

  /**
   * Stores the message in the circular buffer.
   */
  public static void log(String msg, Object... args) {
    StringBuffer sb = new StringBuffer();

    sb.append(MessageFormat.format("[{0,number,#}] ",
        System.currentTimeMillis()));
    sb.append(MessageFormat.format(msg, args));
    
    doLog(sb.toString());
  }

  private static void doLog(String msg) {
    synchronized (logLock) {
      logBuffer.add(msg);
      if (logBuffer.size() > logSize) {
        logBuffer.removeFirst();
      }
    }
  }

  /**
   * Handles user commands.
   */
  public Object execute(ExecutionEvent event) {
    switch (Commands.valueOf((String) event.getParameters().get(CMD_PARAM_ID))) {
      case CMD_DUMP:
        dumpLog();
        break;
      case CMD_CLEAR:
        clearLog();
        break;
    }

    return null;
  }

  /**
   * Clears the current buffer.
   */
  private void clearLog() {
    synchronized (logLock) {
      logBuffer.clear();
    }
  }

  /**
   * Writes out the current buffer.
   */
  private void dumpLog() {
    synchronized (logLock) {
      for (String logEntry : logBuffer) {
        GWTPluginLog.logError(logEntry);
      }
    }
  }
}
