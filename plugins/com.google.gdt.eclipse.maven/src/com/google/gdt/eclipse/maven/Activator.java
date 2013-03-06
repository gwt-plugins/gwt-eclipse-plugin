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
package com.google.gdt.eclipse.maven;

import com.google.gdt.eclipse.maven.builders.EnsureEnhancerBuilderLast;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator class that controls the plugin lifecycle.
 */
public class Activator extends AbstractUIPlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.gdt.eclipse.maven";

  // The shared instance
  private static Activator plugin;

  private static IResourceChangeListener ensureEnhancerBuilderLastListener;

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return plugin;
  }

  /**
   * The constructor
   */
  public Activator() {
  }

  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    ensureEnhancerBuilderLastListener = new EnsureEnhancerBuilderLast();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(
        ensureEnhancerBuilderLastListener, IResourceChangeEvent.PRE_BUILD);
  }

  public void stop(BundleContext context) throws Exception {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(
        ensureEnhancerBuilderLastListener);
    ensureEnhancerBuilderLastListener = null;
    plugin = null;
    super.stop(context);
  }

}
