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
package com.google.gdt.eclipse.suite.resources;

import com.google.gdt.eclipse.suite.GdtPlugin;

/**
 * Contains the key names for GWT plug-in image registry.
 */
public final class GdtImages {

  public static final String GAE_ICON = GdtPlugin.PLUGIN_ID + ".images.gaeIcon";

  public static final String GDT_ICON = GdtPlugin.PLUGIN_ID + ".images.gdtIcon";

  public static final String GDT_NEW_PROJECT_ICON = GdtPlugin.PLUGIN_ID
      + ".images.newGdtProjectIcon";

  public static final String GDT_NEW_PROJECT_LARGE = GdtPlugin.PLUGIN_ID
      + ".images.newGdtProjectLarge";

  private GdtImages() {
    // not instantiable.
  }
}