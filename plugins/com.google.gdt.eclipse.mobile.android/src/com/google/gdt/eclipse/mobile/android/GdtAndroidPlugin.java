/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.mobile.android;

import com.google.gdt.eclipse.core.AbstractGooglePlugin;
import com.google.gdt.eclipse.core.Logger;

import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.BundleContext;

/**
 * TODO: Doc me
 */
public class GdtAndroidPlugin extends AbstractGooglePlugin {

  public static final String PLUGIN_ID = GdtAndroidPlugin.class.getPackage().getName();

  private static Logger logger;

  private static GdtAndroidPlugin plugin;

  public static GdtAndroidPlugin getDefault() {
    return plugin;
  }

  public static Logger getLogger() {
    return logger;
  }

  public static void log(Throwable e) {
    getLogger().logError(e);
  }

  public GdtAndroidPlugin() {
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    logger = new Logger(this);     
  }

  @Override
  protected void initializeImageRegistry(ImageRegistry reg) {
    super.initializeImageRegistry(reg);

    reg.put(GdtAndroidImages.NEW_PROJECT_WIZARD_ICON,
        imageDescriptorFromPath("icons/app_engine_droid_64.png")); //$NON-NLS-1$
    reg.put(GdtAndroidImages.MAIN_LAUNCHTAB_ICON,
        imageDescriptorFromPath("icons/mainLaunchTab.png")); //$NON-NLS-1$
  }

}
