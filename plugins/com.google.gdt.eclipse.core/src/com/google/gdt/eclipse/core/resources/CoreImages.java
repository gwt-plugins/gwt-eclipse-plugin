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
package com.google.gdt.eclipse.core.resources;

import com.google.gdt.eclipse.core.CorePlugin;

/**
 * Contains the key names for Google Core plug-in image registry.
 */
public final class CoreImages {

  public static final String INVALID_SDK_ICON = CorePlugin.PLUGIN_ID
      + ".images.invalidSdk";

  public static final String LIBRARY_ICON = CorePlugin.PLUGIN_ID
      + ".images.library";

  public static final String ERROR_OVERLAY = CorePlugin.PLUGIN_ID
      + ".images.errorOverlay";

  public static final String TERMINATE_ICON = CorePlugin.PLUGIN_ID
      + ".images.terminate";

  private CoreImages() {
    // not instantiable.
  }
}