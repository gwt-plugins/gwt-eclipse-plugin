/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.webtools;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AppEngineWtpPlugin extends AbstractGooglePlugin {
  public static final String PLUGIN_ID = AppEngineWtpPlugin.class.getPackage().getName();

  private static AppEngineWtpPlugin plugin;
  private static Logger logger;

  /**
   * Returns the shared instance.
   */
  public static AppEngineWtpPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the plug-in's {@link Logger}.
   */
  public static Logger getLogger() {
    return logger;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    logger = new Logger(this);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    plugin = null;
    logger = null;
    super.stop(context);
  }

  /**
   * Creates a new error status object.
   */
  public static IStatus createErrorStatus(String message, Throwable t) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, t);
  }
}
