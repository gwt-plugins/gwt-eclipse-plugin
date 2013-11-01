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
package com.google.gdt.eclipse.drive.notifications;

/**
 * An interface for listeners in other plugin projects to specify actions to be performed upon
 * certain events in this plugin.
 */
public interface DrivePluginListener {
  /**
   * Responds to a notification that a plugin has been imported from Drive into Eclipse.
   * 
   * @param driveFileId the Drive file ID for the imported plugin project
   */
  void onImportFromDrive(String driveFileId);
}
