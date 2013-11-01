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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.gdt.eclipse.drive.test.MockEclipsePreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.osgi.service.prefs.BackingStoreException;


/**
 * Unit test for {@link PreferencesVersionManager}.
 */
@RunWith(JUnit4.class)
public class PreferencesVersionManagerTest {
  
  private static final ImmutableMap<String, String> PREFERENCES_A_VERSION_0 =
      ImmutableMap.<String, String>builder()
          .put("driveFileId", "random string 01")
          .put("driveImportName.name%20with%20spaces%2Egs", "name with spaces")
          .put("driveImportName.savedPage%2Ehtml", "savedPage")
          .put("driveImportName.savedScript%2Egs", "savedScript")
          .put("driveImportName.unsavedPage%2Ehtml", "unsavedPage")
          .put("driveImportName.unsavedScript%2Egs", "unsavedScript")
          .put("driveImportName.x%3Dy%2Egs", "x=y")
          .put("driveScriptId.name%20with%20spaces%2Egs", "random string 02")
          .put("driveScriptId.savedPage%2Ehtml", "random string 03")
          .put("driveScriptId.savedScript%2Egs", "random string 04")
          .put("driveScriptId.unsavedPage%2Ehtml", "random string 05")
          .put("driveScriptId.unsavedScript%2Egs", "random string 06")
          .put("driveScriptId.x%3Dy%2Egs", "random string 07")
          .put("driveScriptType.name%20with%20spaces%2Egs", "server_js")
          .put("driveScriptType.savedPage%2Ehtml", "html")
          .put("driveScriptType.savedScript%2Egs", "server_js")
          .put("driveScriptType.unsavedPage%2Ehtml", "html")
          .put("driveScriptType.unsavedScript%2Egs", "server_js")
          .put("driveScriptType.x%3Dy%2Egs", "server_js")
          .put("driveVersionFingerprint", "random string 08")
          .put("eclipse.preferences.version", "1")
          .put("unsaved.name%20with%20spaces%2Egs", "true")
          .put("unsaved.unsavedPage%2Ehtml", "true")
          .put("unsaved.unsavedScript%2Egs", "true")
          .put("unsaved.x%3Dy%2Egs", "true")
          .build();
  
  private static final ImmutableMap<String, String> PREFERENCES_A_VERSION_1 =
      ImmutableMap.<String, String>builder()
          .put("appsScriptProjectPreferencesVersion", "1")
          .put("driveFileId", "random string 01")
          .put("driveImportName.name with spaces.gs", "name with spaces")
          .put("driveImportName.savedPage.html", "savedPage")
          .put("driveImportName.savedScript.gs", "savedScript")
          .put("driveImportName.unsavedPage.html", "unsavedPage")
          .put("driveImportName.unsavedScript.gs", "unsavedScript")
          .put("driveImportName.x=y.gs", "x=y")
          .put("driveScriptId.name with spaces.gs", "random string 02")
          .put("driveScriptId.savedPage.html", "random string 03")
          .put("driveScriptId.savedScript.gs", "random string 04")
          .put("driveScriptId.unsavedPage.html", "random string 05")
          .put("driveScriptId.unsavedScript.gs", "random string 06")
          .put("driveScriptId.x=y.gs", "random string 07")
          .put("driveScriptType.name with spaces.gs", "server_js")
          .put("driveScriptType.savedPage.html", "html")
          .put("driveScriptType.savedScript.gs", "server_js")
          .put("driveScriptType.unsavedPage.html", "html")
          .put("driveScriptType.unsavedScript.gs", "server_js")
          .put("driveScriptType.x=y.gs", "server_js")
          .put("driveVersionFingerprint", "random string 08")
          .put("eclipse.preferences.version", "1")
          .put("unsaved.name with spaces.gs", "true")
          .put("unsaved.unsavedPage.html", "true")
          .put("unsaved.unsavedScript.gs", "true")
          .put("unsaved.x=y.gs", "true")
          .build();
  
  private static final ImmutableMap<String, String> PREFERENCES_B_VERSION_1 =
      ImmutableMap.<String, String>builder()
          .put("appsScriptProjectPreferencesVersion", "1")
          .put("driveFileId", "random string 09")
          .put("driveImportName.name with spaces.gs", "name with spaces")
          .put("driveImportName.savedPage.html", "savedPage")
          .put("driveImportName.savedScript.gs", "savedScript")
          .put("driveImportName.unsavedPage.html", "unsavedPage")
          .put("driveImportName.unsavedScript.gs", "unsavedScript")
          .put("driveImportName.x=y.gs", "x=y")
          .put("driveImportName.コード.gs", "コード")
          .put("driveScriptId.name with spaces.gs", "random string 10")
          .put("driveScriptId.savedPage.html", "random string 11")
          .put("driveScriptId.savedScript.gs", "random string 12")
          .put("driveScriptId.unsavedPage.html", "random string 13")
          .put("driveScriptId.unsavedScript.gs", "random string 14")
          .put("driveScriptId.x=y.gs", "random string 15")
          .put("driveScriptId.コード.gs", "random string 16")
          .put("driveScriptType.name with spaces.gs", "server_js")
          .put("driveScriptType.savedPage.html", "html")
          .put("driveScriptType.savedScript.gs", "server_js")
          .put("driveScriptType.unsavedPage.html", "html")
          .put("driveScriptType.unsavedScript.gs", "server_js")
          .put("driveScriptType.x=y.gs", "server_js")
          .put("driveScriptType.コード.gs", "server_js")
          .put("driveVersionFingerprint", "random string 17")
          .put("eclipse.preferences.version", "1")
          .put("unsaved.name with spaces.gs", "true")
          .put("unsaved.unsavedPage.html", "true")
          .put("unsaved.unsavedScript.gs", "true")
          .put("unsaved.x=y.gs", "true")
          .put("unsaved.コード.gs", "true")
          .build();
  
  @Test
  public void testEnsureCurrentVersion_version0Input() throws BackingStoreException {
    MockEclipsePreferences prefs = new MockEclipsePreferences(PREFERENCES_A_VERSION_0, "/A");
    PreferencesVersionManager.ensureCurrentVersion(prefs);
    assertEquals(PREFERENCES_A_VERSION_1, prefs.getStringMap());
  }
  
  @Test
  public void testEnsureCurrentVersion_version1Input() throws BackingStoreException {
    MockEclipsePreferences prefs1 = new MockEclipsePreferences(PREFERENCES_A_VERSION_1, "/A");
    PreferencesVersionManager.ensureCurrentVersion(prefs1);
    assertEquals(PREFERENCES_A_VERSION_1, prefs1.getStringMap());
    MockEclipsePreferences prefs2 = new MockEclipsePreferences(PREFERENCES_B_VERSION_1, "/B");
    PreferencesVersionManager.ensureCurrentVersion(prefs2);
    assertEquals(PREFERENCES_B_VERSION_1, prefs2.getStringMap());
  }

}
