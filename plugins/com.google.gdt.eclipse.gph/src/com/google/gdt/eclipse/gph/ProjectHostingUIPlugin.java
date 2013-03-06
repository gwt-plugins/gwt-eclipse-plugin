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
package com.google.gdt.eclipse.gph;

import com.google.gdt.eclipse.gph.providers.ScmProvider;
import com.google.gdt.eclipse.gph.providers.ScmProviderRegistry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.List;

/**
 * Project hosting UI plugin.
 */
public class ProjectHostingUIPlugin extends AbstractUIPlugin {

  private static final String ICONS_PATH = "icons/"; //$NON-NLS-1$

  /**
   * The id of the Project Hosting UI plug-in (value
   * <code>"com.google.gdt.eclipse.gph"</code>).
   */
  public static final String PLUGIN_ID = ProjectHostingUIPlugin.class.getPackage().getName();

  private static ProjectHostingUIPlugin PLUGIN;

  /**
   * Create and cache an image from the given ImageDescriptor.
   * 
   * @param key
   * @param imageDescriptor
   * @return
   */
  public static Image createImage(String key, ImageDescriptor imageDescriptor) {
    ImageRegistry registry = getPlugin().getImageRegistry();

    if (registry.get(key) == null) {
      registry.put(key, imageDescriptor.createImage());
    }

    return registry.get(key);
  }

  /**
   * Create and return a new Status object given a message.
   * 
   * @param message the message
   * @return a status object
   */
  public static IStatus createStatus(String message) {
    return createStatus(message, null);
  }

  /**
   * Create and return a new Status object given a message and an exception.
   * 
   * @param message the message
   * @param exception an exception
   * @return a status object
   */
  public static IStatus createStatus(String message, Throwable exception) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
  }

  /**
   * Create and return a new Status object given an exception.
   * 
   * @param exception an exception
   * @return a status object
   */
  public static IStatus createStatus(Throwable exception) {
    return createStatus(exception.toString(), exception);
  }

  /**
   * Returns the bundle context for this plugin.
   * 
   * @return the bundle context
   */
  public static BundleContext getBundleContext() {
    return getPlugin().getBundle().getBundleContext();
  }

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

  public static ProjectHostingUIPlugin getPlugin() {
    return PLUGIN;
  }

  /**
   * Get a SCM provider for the given scm type.
   * 
   * @param scmType the SCM type (legal values include <code>"hg"</code> and
   *          <code>"svn"</code>)
   * @param onlyInstalledProviders only return providers that are fully
   *          installed
   * @return an associated SCM provider, or <code>null</code> if none is
   *         registered
   */
  public static ScmProvider getScmProvider(String scmType) {
    List<ScmProvider> providers = ScmProviderRegistry.getRegistry().getScmProviders(
        scmType);

    if (providers.isEmpty()) {
      return null;
    }

    // If there is more than one provider is registered for an SCM type,
    // the first will be selected. The providers are sorted by install status
    // and then by desired provider (priority).
    return providers.get(0);
  }

  /**
   * Log the given throwable in this bundle's error log.
   * 
   * @param t the throwable to log
   * @see ILog#log(IStatus)
   */
  public static void logError(Throwable t) {
    try {
      logStatus(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Log the given status object to this plugin's log.
   * 
   * @param status
   */
  public static void logStatus(IStatus status) {
    getPlugin().getLog().log(status);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    PLUGIN = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    PLUGIN = null;

    super.stop(context);
  }

}
