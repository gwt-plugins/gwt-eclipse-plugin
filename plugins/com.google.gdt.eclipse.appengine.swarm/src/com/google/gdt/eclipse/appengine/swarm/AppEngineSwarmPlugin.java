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
package com.google.gdt.eclipse.appengine.swarm;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.Logger;
import com.google.gson.Gson;

import org.osgi.framework.BundleContext;

/**
 * Swarm Plugin Class.
 */
@SuppressWarnings("restriction")
public class AppEngineSwarmPlugin extends AbstractGooglePlugin {

  public static final String
      PLUGIN_ID = AppEngineSwarmPlugin.class.getPackage().getName();

  // singleton instance
  private static AppEngineSwarmPlugin plugin;
  private static Logger logger;

  public static final Gson GSON_CODEC = new Gson();

  public static AppEngineSwarmPlugin getDefault() {
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
    plugin = this;
    logger = new Logger(this);
  }
}
