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

import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery.Data;
import com.google.gdt.eclipse.drive.DrivePlugin;

import java.util.List;

/**
 * When notified of an Drive plugin event, broadcasts that event to all listeners that other
 * plugin projects have registered through the {@link #EXTENSION_POINT_NAME} extension point.
 */
public class Broadcaster implements DrivePluginListener {
  
  public static final String EXTENSION_POINT_NAME = "listener";

  @Override
  public void onImportFromDrive(String driveFileId) {
    for (DrivePluginListener listener : getExtensionPointListeners()) {
      listener.onImportFromDrive(driveFileId);
    }    
  }
  
  private static List<DrivePluginListener> getExtensionPointListeners() {
    ExtensionQuery<DrivePluginListener> extensionQuery =
        new ExtensionQuery<DrivePluginListener>(
            DrivePlugin.PLUGIN_ID, EXTENSION_POINT_NAME, "class");
    List<Data<DrivePluginListener>> listenerDataObjects = extensionQuery.getData();
    List<DrivePluginListener> result =
        Lists.newArrayListWithCapacity(listenerDataObjects.size());
    for (Data<DrivePluginListener> datum : listenerDataObjects) {
      result.add(datum.getExtensionPointData());
    }
    return result;
  }
}
