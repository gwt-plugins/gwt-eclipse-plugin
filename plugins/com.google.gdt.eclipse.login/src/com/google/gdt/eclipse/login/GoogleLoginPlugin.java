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
package com.google.gdt.eclipse.login;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class GoogleLoginPlugin extends AbstractUIPlugin {
  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.gdt.eclipse.login";

  // The shared instance
  private static GoogleLoginPlugin plugin;
  
  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static GoogleLoginPlugin getDefault() {
    return plugin;
  }

  /**
   * @param severity severity from Status class
   * @param msg the message for the log
   * @param t the exception, or null
   */
  public static void log(int severity, String msg, Throwable t) {
    plugin.getLog().log(new Status(severity, plugin.getPluginId(), msg, t));
  }

  public static void logError(String msg, Throwable t) {
    log(Status.ERROR, msg, t);
  }

  public static void logWarning(String msg) {
    log(Status.WARNING, msg, null);
  }

  public static void logWarning(String msg, Throwable t) {
    log(Status.WARNING, msg, t);
  }

  /**
   * The constructor
   */
  public GoogleLoginPlugin() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
   * )
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    GoogleLogin.getInstance().stop();
    plugin = null;
    super.stop(context);
  }
  
  public String getPluginId() {
    return PLUGIN_ID;
  }
}