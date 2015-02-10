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
package com.google.gdt.eclipse.suite.update.usage;

import com.google.gdt.eclipse.drive.notifications.DrivePluginListener;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;

/**
 * Reports the import of an Apps Script project from Drive.
 */
public class AppsScriptImportReporter implements DrivePluginListener {

  /**
   * Reports the import from Drive of an Apps Script project with a specified Drive file ID.
   *
   * @param driveFileId the specified Drive file ID
   */
  @Override
  public void onImportFromDrive(String driveFileId) {
    GdtExtPlugin.getAnalyticsPingManager().sendAppsScriptImportPing(driveFileId);
  }
}
