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
package com.google.gdt.eclipse.swtbot;

import java.lang.reflect.Field;

/**
 * Used to set the general SWTBot timeout. This compatibility layer exists to
 * deal with the API differences between SWTBot 2.0.0.204 and SWTBot 2.0.0.358.
 * Once we upgrade our Eclipse 3.3/3.4 test environment ot use 2.0.0.358, we can
 * get rid of this class.
 * 
 * Note that this is a "static" class, so it is not thread-safe. The pattern of
 * usage is to call {@link #setTimeout(long)} to set the general SWTBot timeout,
 * and then call {@link #resetTimeout()} to reset the SWTBot timeout back to
 * it's original value. You should never call {@link #setTimeout(long)} more
 * than once without a call to {@link #resetTimeout()} in between.
 */
public class SwtBotTimeoutManager {

  private static final int TYPICAL_TIMEOUT = 30000;

  private static final String KEY_SWTBOT_TIMEOUT = "org.eclipse.swtbot.search.timeout";

  private static String SWTBOT_PREFS_TIMEOUT_FIELD_NAME = "TIMEOUT";

  private static long UNSET_TIMEOUT_VALUE = -1;

  private static String oldTimeoutSysProp = null;
  private static long oldTimeoutSwtPrefs = UNSET_TIMEOUT_VALUE;

  /**
   * Reset the timeout value back to what it was before
   * {@link #setTimeout(long)} was called.
   */
  public static void resetTimeout() {

    // Code for 2.0.0.204
    if (oldTimeoutSysProp != null) {
      System.setProperty(KEY_SWTBOT_TIMEOUT, oldTimeoutSysProp);
    } else {
      System.clearProperty(KEY_SWTBOT_TIMEOUT);
    }

    // Code for 2.0.0.358
    if (oldTimeoutSwtPrefs != UNSET_TIMEOUT_VALUE) {
      setSwtBotPrefsTimeoutFieldValue(oldTimeoutSwtPrefs);
    }
  }

  /**
   * Set the SWTBot timeout to a value we've found suitable for our set of tests
   * running on our test machines.
   */
  public static void setTimeout() {
    setTimeout(TYPICAL_TIMEOUT);
  }

  /**
   * Set the SWTBot timeout value.
   * 
   * @param timeout the timeout value, in milliseconds
   */
  public static void setTimeout(long timeout) {
    // Code for 2.0.0.204
    oldTimeoutSysProp = System.getProperty(KEY_SWTBOT_TIMEOUT, null);
    System.setProperty(KEY_SWTBOT_TIMEOUT, String.valueOf(timeout));

    // Code for 2.0.0.358
    oldTimeoutSwtPrefs = getSwtBotPrefsTimeoutFieldValue();
    setSwtBotPrefsTimeoutFieldValue(timeout);
  }

  private static long getSwtBotPrefsTimeoutFieldValue() {
    try {
      Class<?> swtBotPrefsClass = Class.forName("org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences");
      Field timeoutField = swtBotPrefsClass.getDeclaredField(SWTBOT_PREFS_TIMEOUT_FIELD_NAME);
      return timeoutField.getLong(null);
    } catch (ClassNotFoundException e) {
      // Ignore
    } catch (SecurityException e) {
      // Ignore
    } catch (NoSuchFieldException e) {
      // Ignore
    } catch (IllegalArgumentException e) {
      // Ignore
    } catch (IllegalAccessException e) {
      // Ignore
    }
    return UNSET_TIMEOUT_VALUE;
  }

  private static void setSwtBotPrefsTimeoutFieldValue(long timeout) {
    if (timeout == UNSET_TIMEOUT_VALUE) {
      return;
    }

    try {
      Class<?> swtBotPrefsClass = Class.forName("org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences");
      Field timeoutField = swtBotPrefsClass.getDeclaredField(SWTBOT_PREFS_TIMEOUT_FIELD_NAME);
      timeoutField.setLong(null, timeout);
    } catch (ClassNotFoundException e) {
      // Ignore
    } catch (SecurityException e) {
      // Ignore
    } catch (NoSuchFieldException e) {
      // Ignore
    } catch (IllegalArgumentException e) {
      // Ignore
    } catch (IllegalAccessException e) {
      // Ignore
    }
  }
}
