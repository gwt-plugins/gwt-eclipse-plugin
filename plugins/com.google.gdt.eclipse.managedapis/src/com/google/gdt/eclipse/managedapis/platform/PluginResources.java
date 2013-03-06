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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.managedapis.BaseResources;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.Resources;

import org.eclipse.swt.widgets.Display;

import java.net.URL;

/**
 * Provide consistent access to system-mediated resources and constants for the
 * Google API Import Wizard.
 */
public class PluginResources extends BaseResources implements Resources {
  public PluginResources(Display display) {
    super(display);
  }

  @Override
  public URL getURLForLocalPath(String path) {
    URL infoIconUrl = ManagedApiPlugin.getDefault().getBundle().getResource(
        path);
    return infoIconUrl;
  }
}
