/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.api;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;

import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AppEngineApiActivator extends AbstractGooglePlugin {

  // The plug-in ID
  public static final String PLUGIN_ID = "com.google.gdt.eclipse.appengine.api"; //$NON-NLS-1$

  // The shared instance
  private static AppEngineApiActivator plugin;

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static AppEngineApiActivator getDefault() {
    return plugin;
  }

  /**
   * Logs a message to the plugin logger.
   *
   * @param severity severity from Status class
   * @param msg the message for the log
   * @param t the exception, or null
   */
  public static void log(int severity, String msg, Throwable t) {
    plugin.getLog().log(new Status(severity, PLUGIN_ID, msg, t));
  }

  /**
   * Logs an error message to the plugin logger.
   *
   * @param msg the message for the log
   * @param t the exception, or null
   */
  public static void logError(String msg, Exception t) {
    log(Status.ERROR, msg, t);
  }

  /**
   * The constructor
   */
  public AppEngineApiActivator() {
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

}
