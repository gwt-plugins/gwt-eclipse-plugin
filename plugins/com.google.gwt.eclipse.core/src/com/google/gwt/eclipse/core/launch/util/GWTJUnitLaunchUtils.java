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
package com.google.gwt.eclipse.core.launch.util;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.GWTLaunchConstants;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * JUnit test launch utilities.
 */
public class GWTJUnitLaunchUtils {
  public static void setDefaults(ILaunchConfigurationWorkingCopy wc) {

    try {
      wc.setAttribute(
          IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER,
          JavaRuntime.getJavaProject(wc) == null
              ? null
              : ModuleClasspathProvider.computeProviderId(JavaRuntime.getJavaProject(
                  wc).getProject()));
    } catch (CoreException ce) {
      GWTPluginLog.logError(ce);
    }

    wc.setAttribute(GWTLaunchConstants.ATTR_LOG_LEVEL,
        GWTLaunchConstants.Defaults.LOG_LEVEL);

    wc.setAttribute(GWTLaunchConstants.ATTR_OBFUSCATION,
        GWTLaunchConstants.Defaults.OBFUSCATION);

    wc.setAttribute(GWTLaunchConstants.ATTR_WEB_MODE,
        GWTLaunchConstants.Defaults.WEB_MODE);

    wc.setAttribute(GWTLaunchConstants.ATTR_NOT_HEADLESS,
        GWTLaunchConstants.Defaults.NOT_HEADLESS);

    // Override the default output directory for web mode tests
    wc.setAttribute(GWTLaunchConstants.ATTR_OUT_DIR,
        GWTLaunchConstants.Defaults.OUT_DIR_TEST);
  }

  private GWTJUnitLaunchUtils() {
    // Not instantiable
  }

}
