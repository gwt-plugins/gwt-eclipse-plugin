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
package com.google.gdt.eclipse.drive.images;

import com.google.common.collect.ImmutableList;
import com.google.gdt.eclipse.drive.DrivePlugin;

import java.util.Collection;

/**
 * Defines string constants used as keys into the image registry.
 */
public class ImageKeys {

  private ImageKeys() {
    // prevent instantiation
  }
  
  private static final String PREFIX = DrivePlugin.PLUGIN_ID + ".images.";
  
  public static final String FOLDER_ICON = PREFIX + "folderIcon";
  public static final String APPS_SCRIPT_PROJECT_ICON = PREFIX + "appsScriptProjectIcon";
  public static final String UNSAVED_ICON = PREFIX + "unsavedIcon";
  
  public static final Collection<String> ALL_KEYS =
      ImmutableList.of(FOLDER_ICON, APPS_SCRIPT_PROJECT_ICON, UNSAVED_ICON);

}
