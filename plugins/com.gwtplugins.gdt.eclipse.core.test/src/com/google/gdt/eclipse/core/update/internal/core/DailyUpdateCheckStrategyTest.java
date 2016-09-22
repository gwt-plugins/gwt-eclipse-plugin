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

import junit.framework.TestCase;

import java.util.Date;

/**
 * Tests for {@link DailyUpdateCheckStrategy}.
 */
public class DailyUpdateCheckStrategyTest extends TestCase {
  // 1000 millis/sec
  // 60 secs/min
  // 60 mins/hr
  // 24 hrs/day
  private static final int MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

  public void testIsSameDay() {
    long now = new Date().getTime();
    // Today
    assertTrue(DailyUpdateCheckStrategy.isSameDay(now, now));

    // Tomorrow
    assertFalse(DailyUpdateCheckStrategy.isSameDay(now, now + MILLIS_PER_DAY));

    // Yesterday
    assertFalse(DailyUpdateCheckStrategy.isSameDay(now, now - MILLIS_PER_DAY));
  }
}
