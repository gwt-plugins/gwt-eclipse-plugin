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

/**
 * A set of package-private constants used in this package and its unit tests.
 */
interface PreferencesConstants {
  static final int CURRENT_PREFERENCES_VERSION = 1;
  
  static final String PREFERENCES_VERSION_KEY = "appsScriptProjectPreferencesVersion";
  static final String DRIVE_FILE_ID_KEY = "driveFileId";
  static final String DRIVE_IMPORT_NAME_KEY_PREFIX = "driveImportName.";
  static final String DRIVE_SCRIPT_ID_KEY_PREFIX = "driveScriptId.";
  static final String DRIVE_TYPE_KEY_PREFIX = "driveScriptType.";
  static final String UNSAVED_KEY_PREFIX = "unsaved.";
  static final String DRIVE_VERSION_FINGERPRINT_KEY = "driveVersionFingerprint";
}
