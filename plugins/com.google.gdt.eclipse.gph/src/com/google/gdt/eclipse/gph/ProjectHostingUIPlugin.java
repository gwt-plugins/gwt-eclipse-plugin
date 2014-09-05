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
  /**
   * The id of the Project Hosting UI plug-in (value <code>"com.google.gdt.eclipse.gph"</code>).
   */
  public static final String PLUGIN_ID = ProjectHostingUIPlugin.class.getPackage().getName();

  private static final String ICONS_PATH = "icons/"; //$NON-NLS-1$

  private static ProjectHostingUIPlugin PLUGIN;

  /**
   * Creates and caches an image from the given ImageDescriptor.
   */
  public static Image createImage(String key, ImageDescriptor imageDescriptor) {
    ImageRegistry registry = getPlugin().getImageRegistry();

    if (registry.get(key) == null) {
      registry.put(key, imageDescriptor.createImage());
    }

    return registry.get(key);
  }

  /**
   * Creates a new error status object from the given message.
   */
  public static IStatus createErrorStatus(String message) {
    return createErrorStatus(message, null);
  }

  /**
   * Creates a new error status object from the given message and exception.
   */
  public static IStatus createErrorStatus(String message, Throwable exception) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
  }

  /**
   * Creates a new error status object from the given exception.
   */
  public static IStatus createErrorStatus(Throwable exception) {
    return createErrorStatus(exception.getMessage(), exception);
  }

  /**
   * Returns the bundle context for this plug-in.
   */
  public static BundleContext getBundleContext() {
    return getPlugin().getBundle().getBundleContext();
  }

  /**
   * Returns an {@link Image} given a relative path within this plug-in, or {@code null} if there is
   * no image.
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
   * Returns an {@link ImageDescriptor} given a relative path within this plug-in's
   * {@link #ICONS_PATH} folder, or {@code null} if there is no image.
   */
  public static ImageDescriptor getImageDescriptor(String relativePath) {
    return imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH + relativePath);
  }

  public static ProjectHostingUIPlugin getPlugin() {
    return PLUGIN;
  }

  /**
   * Returns a Software Configuration Management provider for the given SCM type.
   *
   * @param scmType the SCM type (legal values include {@code "hg"} and {@code "svn"}
   * @returns an associated SCM provider, or {@code null} if none is registered
   */
  public static ScmProvider getScmProvider(String scmType) {
    List<ScmProvider> providers = ScmProviderRegistry.getRegistry().getScmProviders(scmType);

    if (providers.isEmpty()) {
      return null;
    }

    // If there is more than one provider is registered for an SCM type,
    // the first will be selected. The providers are sorted by install status
    // and then by desired provider (priority).
    return providers.get(0);
  }

  /**
   * Logs the given {@link Throwable} in the error log.
   */
  public static void logError(Throwable t) {
    try {
      logStatus(createErrorStatus(t));
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Logs the given {@link IStatus} in the error log.
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
