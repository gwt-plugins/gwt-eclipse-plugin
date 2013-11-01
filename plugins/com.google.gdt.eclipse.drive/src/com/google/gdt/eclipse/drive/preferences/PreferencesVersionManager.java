/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.preferences;

import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.CURRENT_PREFERENCES_VERSION;
import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.DRIVE_IMPORT_NAME_KEY_PREFIX;
import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.DRIVE_SCRIPT_ID_KEY_PREFIX;
import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.DRIVE_TYPE_KEY_PREFIX;
import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.PREFERENCES_VERSION_KEY;
import static com.google.gdt.eclipse.drive.preferences.PreferencesConstants.UNSAVED_KEY_PREFIX;

import com.google.common.collect.ImmutableList;
import com.google.gdt.eclipse.drive.DrivePlugin;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import java.util.List;

/**
 * Provides a method to ensure that Apps Script project preferences are recorded in accordance with
 * the latest version of the project-preferences representation, rewriting them in the current
 * representation if not.
 * 
 * <p>The representation of project preferences has evolved as follows:
 * <ul>
 *   <li><strong>Version 0 to version 1:</strong>
 *     <ul>
 *       <li>The {@link PREFERENCES_VERSION_KEY} property was added. (Thus, the absence of this
 *           property marks project preferences as being at version 0.)
 *       <li>Percent escaping of file names in property-name suffixes is eliminated. Percent
 *           escaping does not work for Unicode characters beyond '\u00ff'. The implementation of
 *           the {@link IEclipsePreferences} interface handles all necessary escaping and unescaping
 *           when writing to or reading from the the file where it stores preferences.
 *     </ul>
 * </ul>
 */
class PreferencesVersionManager {
  
  /**
   * A transformation applied to {@link IEclipsePreferences}, to upgrade it from a particular
   * version of Apps Script project preferences to the immediately succeeding version.
   */
  private interface Upgrader {
    /**
     * Applies the transformation associated with this {@code Upgrader} to specified
     * {@link IEclipsePreferences}.
     * 
     * @param prefs the specified {@code IEclipsePreferences}
     * @throws BackingStoreException if there is an error reading or writing preferences
     */
    void upgrade(IEclipsePreferences prefs) throws BackingStoreException;
  }
  
  private static final List<String> PREFIXES_FOR_KEYS_WITH_FILE_NAMES =
      ImmutableList.of(
          DRIVE_IMPORT_NAME_KEY_PREFIX, DRIVE_SCRIPT_ID_KEY_PREFIX, DRIVE_TYPE_KEY_PREFIX,
          UNSAVED_KEY_PREFIX);
  
  private static final Upgrader FROM_0_TO_1 =
      new Upgrader() {
        /**
         * Upgrades specified preferences from version 0 to version 1 by eliminating any percent
         * escaping from the file names in property-name suffixes.
         * 
         * @param prefs the specified prefs
         * @throws BackingStoreException if there is an error reading keys from the preferences
         */
        @Override public void upgrade(IEclipsePreferences prefs) throws BackingStoreException {
          for (String oldKey : prefs.keys()) {
            for (String prefix : PREFIXES_FOR_KEYS_WITH_FILE_NAMES) {
              if (oldKey.startsWith(prefix)) {
                String percentEscapedFileName = oldKey.substring(prefix.length());
                String fileNameAsJavaString = undoPercentEscaping(percentEscapedFileName);
                String newKey = prefix + fileNameAsJavaString;
                prefs.put(newKey, prefs.get(oldKey, null));
                prefs.remove(oldKey);
              }
            }
          }
        }
      };
      
  // ALL_VERSION_UPGRADERS.get(i) is an upgrader from version i to version (i+1).
  private static final ImmutableList<Upgrader> ALL_VERSION_UPGRADERS =
      ImmutableList.of(FROM_0_TO_1);
  
  /**
   * Ensures that specified Apps Script project preferences are recorded in accordance with
   * the latest version of the project-preferences representation. The version of the recorded
   * representation is determined from the value associated with {@link PREFERENCES_VERSION_KEY}
   * property (with the absence of such a key indicating version 0). For each obsolete version,
   * there is an {@link Upgrader} that transforms preferences from that version to the next version.
   * All necessary {@code Upgrader}s are applied in sequence to transform the specified preferences
   * from the version in which they were recorded to the current version.
   * 
   * @param prefs the specified Apps Script project preferences
   */
  public static void ensureCurrentVersion(IEclipsePreferences prefs) {
    try {
      int currentVersion = prefs.getInt(PREFERENCES_VERSION_KEY, 0); // not defined means version 0
      // When preferences version is up to date, the following loop is executed 0 times:
      for (int oldVersion = currentVersion; oldVersion < CURRENT_PREFERENCES_VERSION; oldVersion++) {
        int newVersion = oldVersion + 1;
        DrivePlugin.logInfo(
            "Upgrading preferences for " + prefs.absolutePath() + " from version " + currentVersion
                + " to version " + newVersion);
        ALL_VERSION_UPGRADERS.get(oldVersion).upgrade(prefs);
        prefs.putInt(PREFERENCES_VERSION_KEY, newVersion);
      }
    } catch (BackingStoreException e) {
      DrivePlugin.logError("Error trying to updated preferences version", e);
    }
  }

  /**
   * Undoes percent escaping, as performed by
   * {@link com.google.api.client.util.escape.PercentEscaper}.
   * 
   * @param percentEscapedString a string that has been percent-escaped
   * @return
   *     a string in which each occurrence of the pattern %hh, where hh denotes two hex digits,
   *     is replaced by the character at Unicode code point 0xhh
   */
  private static String undoPercentEscaping(String percentEscapedString) {
    StringBuilder resultBuilder = new StringBuilder();
    int length = percentEscapedString.length();
    int i = 0;
    while (i < length) {
      char nextChar = percentEscapedString.charAt(i);
      if (nextChar == '%' && i + 2 < length) {
        String hexNumeral = 
            new String(
                new char[]{percentEscapedString.charAt(i + 1), percentEscapedString.charAt(i + 2)});
        try {
          resultBuilder.append((char) Integer.parseInt(hexNumeral, 16));
        } catch (NumberFormatException e) {
          resultBuilder.append(percentEscapedString.substring(i, i + 3));
        }
        i += 3;
      } else {
        resultBuilder.append(nextChar);
        i++;
      }
    }
    return resultBuilder.toString();
  }

}
