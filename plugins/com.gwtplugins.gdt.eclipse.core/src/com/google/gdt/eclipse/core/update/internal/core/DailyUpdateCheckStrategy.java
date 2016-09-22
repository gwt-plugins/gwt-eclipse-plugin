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
package com.google.gdt.eclipse.core.update.internal.core;

import java.util.Date;

/**
 * Strategy that provides one feature update check per day.
 * 
 * TODO: It is somewhat odd that this class has no context to match it with the
 * feature for which it is doing an update check. Consider restructuring this
 * class.
 */
public class DailyUpdateCheckStrategy implements UpdateCheckStrategy {
  /**
   * Returns <code>true</code> if <code>now</code> and
   * <code>lastUpdate</code> are the same day.
   * 
   * @param nowMillis the current time
   * @param lastUpdateMillis date of the last update check
   * 
   * @return <code>true</code> if <code>now</code> and
   *         <code>lastUpdate</code>
   */
  @SuppressWarnings("deprecation")
  static boolean isSameDay(long nowMillis, long lastUpdateMillis) {
    if (lastUpdateMillis != 0) {
      Date now = new Date(nowMillis);
      Date lastUpdate = new Date(lastUpdateMillis);

      if (now.getMonth() == lastUpdate.getMonth()
          && now.getDate() == lastUpdate.getDate()
          && now.getYear() == lastUpdate.getYear()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns <code>true</code> if no update checks have been performed or if
   * it the last update check was not today.
   * 
   * @return <code>true</code> if an update check should be performed.
   */
  public boolean shouldCheckForUpdates(long lastUpdateTimeMillis) {
    long nowMillis = System.currentTimeMillis();

    boolean shouldCheckForUpdates = !isSameDay(nowMillis, lastUpdateTimeMillis);
    return shouldCheckForUpdates;
  }
}
