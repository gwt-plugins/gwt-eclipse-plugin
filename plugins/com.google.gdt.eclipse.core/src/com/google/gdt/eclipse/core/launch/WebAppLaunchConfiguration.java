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
package com.google.gdt.eclipse.core.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * 
 */
public class WebAppLaunchConfiguration {

  /**
   * Originally, this was defined as GdtPlugin.PLUGIN_ID + ".webapp" and lived
   * in the suite plugin. It was moved here after some non-suite plugins
   * depended on this configuration's type.
   */
  public static final String TYPE_ID = "com.google.gdt.eclipse.suite.webapp";

  public static boolean getAutoPortSelection(
      ILaunchConfiguration launchConfiguration) throws CoreException {
    return LaunchConfigurationAttributeUtilities.getBoolean(
        launchConfiguration, WebAppLaunchAttributes.AUTO_PORT_SELECTION);
  }

  public static boolean getRunServer(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return LaunchConfigurationAttributeUtilities.getBoolean(
        launchConfiguration, WebAppLaunchAttributes.RUN_SERVER);
  }

  public static String getServerPort(ILaunchConfiguration launchConfiguration)
      throws CoreException {
    return LaunchConfigurationAttributeUtilities.getString(launchConfiguration,
        WebAppLaunchAttributes.SERVER_PORT);
  }
}