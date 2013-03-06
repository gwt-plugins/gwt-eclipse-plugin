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
package com.google.appengine.eclipse.core.sdk;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * A classpath container for the App Engine SDK.
 */
public class GaeSdkContainer extends SdkClasspathContainer<GaeSdk> {
  public static final String CONTAINER_ID = AppEngineCorePlugin.PLUGIN_ID
      + ".GAE_CONTAINER";
  public static final Path CONTAINER_PATH = new Path(CONTAINER_ID);

  public static Path getDefaultSdkContainerPath() {
    return CONTAINER_PATH;
  }

  public static boolean isPathForGaeSdkContainer(IPath path) {
    return CONTAINER_PATH.isPrefixOf(path);
  }

  public GaeSdkContainer(IPath path, GaeSdk sdk,
      IClasspathEntry[] classpathEntries, String description) {
    super(path, sdk, classpathEntries, description);
  }  
}
