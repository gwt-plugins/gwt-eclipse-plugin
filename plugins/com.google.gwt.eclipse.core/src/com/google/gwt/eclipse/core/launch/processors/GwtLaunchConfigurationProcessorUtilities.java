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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Utility methods for GWT-related launch configuration processors.
 */
public final class GwtLaunchConfigurationProcessorUtilities {

  public static final String GWT_SHELL_MAIN_TYPE = "com.google.gwt.dev.GWTShell";

  public static final String HOSTED_MODE_MAIN_TYPE = "com.google.gwt.dev.HostedMode";

  public static final String DEV_MODE_MAIN_TYPE = "com.google.gwt.dev.DevMode";

  public static boolean isDevMode(ILaunchConfiguration config)
      throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return DEV_MODE_MAIN_TYPE.equals(mainTypeName);
  }

  public static boolean isGwtShell(ILaunchConfiguration config)
      throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return GWT_SHELL_MAIN_TYPE.equals(mainTypeName);
  }

  public static boolean isHostedMode(ILaunchConfiguration config)
      throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return HOSTED_MODE_MAIN_TYPE.equals(mainTypeName);
  }

  private GwtLaunchConfigurationProcessorUtilities() {
  }
}
