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
package com.google.gdt.eclipse.appsmarketplace.resources;

import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;

/**
 * Contains the key names for plug-in image registry.
 */
public final class AppsMarketplaceImages {

  public static final String APPS_MARKETPLACE_LIST_LARGE =
      AppsMarketplacePlugin.PLUGIN_ID + ".images.deployLarge";

  public static final String APPS_MARKETPLACE_LIST_SMALL =
      AppsMarketplacePlugin.PLUGIN_ID + ".images.deploySmall";

  private AppsMarketplaceImages() {
    // Not instantiable
  }
}
