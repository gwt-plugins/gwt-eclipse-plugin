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
package com.google.gdt.eclipse.gph.egit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class for the com.google.gdt.eclipse.gph.egit plugin.
 */
public class EGitCheckoutProviderPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = EGitCheckoutProviderPlugin.class.getPackage().getName();

  private final static String ICONS_PATH = "icons/";

  private static EGitCheckoutProviderPlugin plugin;

  /**
   * Get an image given a path relative to this PLUGIN.
   * 
   * @param path
   * @return an image
   */
  public static Image getImage(String path) {
    if (getPlugin().getImageRegistry().get(path) != null) {
      return getPlugin().getImageRegistry().get(path);
    }

    ImageDescriptor descriptor = getImageDescriptor(path);

    if (descriptor != null) {
      getPlugin().getImageRegistry().put(path, descriptor);

      return getPlugin().getImageRegistry().get(path);
    }

    return null;
  }

  /**
   * Get the workbench image with the given path relative to {@link #ICONS_PATH}
   * .
   * 
   * @param relativePath relative path to the image
   * @return an image descriptor, or <code>null</code> if none could be found
   */
  public static ImageDescriptor getImageDescriptor(String relativePath) {
    return imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH + relativePath);
  }

  /**
   * @return the singleton instance of this plugin
   */
  public static EGitCheckoutProviderPlugin getPlugin() {
    return plugin;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    plugin = null;
  }

  /**
   * @return a new Status object
   */
  public static IStatus createStatus(int severity, String message,
      Throwable exception) {
    return new Status(severity, PLUGIN_ID, message, exception);
  }

  /**
   * Log the given message and exception to the Eclipse log.
   * 
   * @param message
   * @param e
   */
  public static void logError(String message, Throwable e) {
    getPlugin().getLog().log(createStatus(IStatus.ERROR, message, e));
  }

}
