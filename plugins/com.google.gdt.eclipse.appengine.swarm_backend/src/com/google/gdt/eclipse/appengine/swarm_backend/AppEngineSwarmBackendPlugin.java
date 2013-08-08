/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm_backend;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.Logger;
import com.google.gdt.eclipse.core.projects.ProjectUtilities;
import com.google.gdt.eclipse.suite.GdtPlugin;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

/**
 * Swarm Plugin Class.
 */
public class AppEngineSwarmBackendPlugin extends AbstractGooglePlugin {

  public static final String PLUGIN_ID = AppEngineSwarmBackendPlugin.class.getPackage().getName();

  // singleton instance
  private static AppEngineSwarmBackendPlugin plugin;
  private static Logger logger;

  public static AppEngineSwarmBackendPlugin getDefault() {
    return plugin;
  }

  public static Logger getLogger() {
    return logger;
  }

  public static void log(String string) {
    getLogger().logError(string);
  }

  public static void log(Throwable e) {
    getLogger().logError(e);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    /*
     * Force load of the com.google.gdt.eclipse.suite plugin, so that
     * ProjectUtilities.webAppCreatorFactory is initialized properly.
     * 
     * TODO (rdayal): This is ugly, but it would be difficult to refactor
     * ProjectUtilities, due to its usage in test code. Nonetheless, we should
     * do this refactoring.
     * 
     * The best approach would be to write a different project creation method
     * for test code that does not have a dependency on the
     * com.google.gdt.eclipse.suite plugin. That, or another test plugin should
     * be created that is used for integration testing. This plugin would depend
     * on the suite plugin.
     */
    @SuppressWarnings("unused")
    GdtPlugin gdtPlugin = GdtPlugin.getDefault();
    assert (ProjectUtilities.getWebAppProjectCreatorFactory() != null);

    plugin = this;
    logger = new Logger(this);
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);
    reg.put(AppEngineSwarmBackendImages.GENERATE_BACKEND_DIALOG_IMAGE,
        imageDescriptorFromPath("icons/app_engine_droid_64.png"));
  }
}
