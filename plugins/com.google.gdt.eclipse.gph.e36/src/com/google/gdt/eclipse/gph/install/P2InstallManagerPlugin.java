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
package com.google.gdt.eclipse.gph.install;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Project hosting install manager plugin. This plugin is only active in Eclipse
 * 3.6 and above.
 */
public class P2InstallManagerPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = P2InstallManagerPlugin.class.getPackage().getName();

  private static P2InstallManagerPlugin PLUGIN;

  /**
   * Create and return a new Status object given an exception.
   * 
   * @param exception an exception
   * @return a status object
   */
  public static IStatus createStatus(Throwable exception) {
    return new Status(IStatus.ERROR, PLUGIN_ID, exception.toString(), exception);
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
   * @return the plugin singleton
   */
  public static P2InstallManagerPlugin getPlugin() {
    return PLUGIN;
  }

  /**
   * Log the given throwable in this bundle's error log.
   * 
   * @param t the throwable to log
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
