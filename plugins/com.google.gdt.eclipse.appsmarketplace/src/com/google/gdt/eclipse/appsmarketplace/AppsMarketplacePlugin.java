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
package com.google.gdt.eclipse.appsmarketplace;

import com.google.gdt.eclipse.appsmarketplace.resources.AppsMarketplaceImages;
import com.google.gdt.eclipse.core.AbstractGooglePlugin;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.io.IOException;

/**
 * The activator class controls the plug-in life cycle.
 *
 */
public class AppsMarketplacePlugin extends AbstractGooglePlugin {

  // The plug-in ID
  public static final String PLUGIN_ID =
      AppsMarketplacePlugin.class.getPackage().getName();

  public static final String APPS_MARKETPLACE_JAR_NAME = "apps-marketplace.jar";

  // The shared instance
  private static AppsMarketplacePlugin plugin;

  public static Shell getActiveWorkbenchShell() {
    IWorkbenchWindow window = getActiveWorkbenchWindow();
    if (window != null) {
      return window.getShell();
    }
    return null;
  }

  public static IWorkbenchWindow getActiveWorkbenchWindow() {
    return getDefault().getWorkbench().getActiveWorkbenchWindow();
  }

  /**
   * Returns the shared instance.
   *
   * @return the shared instance
   */
  public static AppsMarketplacePlugin getDefault() {
    return plugin;
  }

  public static String getVersion() {
    return (String) getDefault().getBundle().getHeaders().get(
        Constants.BUNDLE_VERSION);
  }

  private static void extractAppsMarketplaceJar() throws IOException {
    extractJar(AppsMarketplacePlugin.getDefault(),
        "lib/" + APPS_MARKETPLACE_JAR_NAME, APPS_MARKETPLACE_JAR_NAME);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;

    extractAppsMarketplaceJar();
  }

  @Override
  public void stop(BundleContext context) throws Exception {

    plugin = null;
    super.stop(context);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    reg.put(AppsMarketplaceImages.APPS_MARKETPLACE_LIST_LARGE,
        imageDescriptorFromPath("icons/list-on-marketplace_75x66.png"));
    reg.put(AppsMarketplaceImages.APPS_MARKETPLACE_LIST_SMALL,
        imageDescriptorFromPath("icons/list-on-marketplace_16x16.png"));

  }

}
