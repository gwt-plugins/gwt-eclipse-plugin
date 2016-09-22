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
package com.google.gwt.eclipse.oophm.model;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Utility methods that deal with attention levels (which correspond to one of
 * {@link com.google.gwt.core.ext.TreeLogger.Type} values).
 * 
 * TODO: Move some of the attention level logic from the LaunchConfiguration
 * class into this class.
 */
public class AttentionLevelUtils {

  /**
   * Determine whether or not the new attention level is more important than the
   * old attention level. The level typically corresponds to one of the
   * {@link com.google.gwt.core.ext.TreeLogger.Type} values.
   * 
   * The new level will be deemed to be more important if:
   * 
   * 1) <code>newAttentionLevel</code> is non-null and
   * <code>oldAttentionLevel</code> is null
   * 
   * 2) <code>newAttentionLevel</code> is non-null and
   * <code>oldAttentionLevel</code> is a lower priority than
   * <code>newAttentionLevel</code>.
   * 
   * 3) <code>newAttentionLevel</code> is null and the
   * <code>oldAttentionLevel</code> is non-null
   * 
   * @param oldAttentionLevel the old attention level
   * @param newAttentionLevel the new attention level
   * 
   * @return true if <code>newAttentionLevel</code> is more important than
   *         <code>oldAttentionLevel</code>, false otherwise
   */
  public static boolean isNewAttnLevelMoreImportantThanOldAttnLevel(
      String oldAttentionLevel, String newAttentionLevel) {

    if (oldAttentionLevel == newAttentionLevel) {
      // The two attention levels are equal; the new attention level is
      // not more important than the old attention level
      return false;
    }

    // If the two attention levels are non-null, check to see if
    // they're identical, or if the new level is a lower priority
    // than the old level. In either case, the new level is not
    // more important than the old level.
    if (oldAttentionLevel != null && newAttentionLevel != null) {

      if (oldAttentionLevel.equals(newAttentionLevel)) {
        // The attention levels are identical
        return false;
      }

      TreeLogger.Type oldLevel = LogEntry.toTreeLoggerType(oldAttentionLevel);
      TreeLogger.Type newLevel = LogEntry.toTreeLoggerType(newAttentionLevel);

      if (oldLevel == null || newLevel == null) {
        // Can't decipher the TreeLogger levels for either the old or new
        // attention levels; assume that the new level is not more important
        // the old level
        return false;
      }

      if (newLevel.isLowerPriorityThan(oldLevel)) {
        // The new level is at a lower level than the current level
        return false;
      }
    }

    /*
     * If we've reached this point, it is either the case that newAttentionLevel
     * is higher priority than oldAttentionLevel, or only one of
     * newAttentionLevel or oldAttentionLevel is null. In any of these cases,
     * the new level is more important than the old level.
     */
    return true;
  }
}
