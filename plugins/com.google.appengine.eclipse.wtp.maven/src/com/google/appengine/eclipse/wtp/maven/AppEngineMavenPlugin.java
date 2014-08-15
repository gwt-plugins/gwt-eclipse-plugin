/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.maven;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Google App Engine WTP Maven support plug-in life-cycle.
 */
// TODO(nhcohen): Rename this class to something that does not invite confusion with the
// maven-plugin of the same name.
public final class AppEngineMavenPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "com.google.appengine.eclipse.wtp.maven";
  private static AppEngineMavenPlugin instance;

  @Override
  public void start(BundleContext context) throws Exception {
    logInfo("entering AppEngineMavenPlugin.start(BundleContext)");
    super.start(context);
    instance = this;
  }

  public static AppEngineMavenPlugin getInstance() {
    return instance;
  }
  
  // Static logging methods:
  
  public static void log(int severity, String message, Throwable t) {
    if (instance == null) {
      // We are running a test, or the plugin has been stopped.
      logToStderr(severity, message, t);
    } else {
       instance.getLog().log(new Status(severity, PLUGIN_ID, message, t));
    }
  }

  private static void logToStderr(int severity, String message, Throwable t) {
    switch(severity) {
      case IStatus.OK:
        System.err.print("OK: ");
        break;
      case IStatus.INFO:
        System.err.print("INFO: ");
        break;
      case IStatus.WARNING:
        System.err.print("WARNING: ");
        break;
      case IStatus.ERROR:
        System.err.print("ERROR: ");
        break;
      case IStatus.CANCEL:
        System.err.print("CANCEL: ");
        break;
      default:
        break;
    }
    System.err.println(message);
    if (t != null) {
      t.printStackTrace(System.err);
    }
  }
  
  public static void logError(String message, Throwable t) {
    log(IStatus.ERROR, message, t);
  }
  
  public static void logInfo(String message) {
    log(IStatus.INFO, message, null);
  }

}
