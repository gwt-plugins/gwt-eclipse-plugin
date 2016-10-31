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
 *
 * Duplicated in MainTypeProcessor to prevent circular classpath
 */
public final class GwtLaunchConfigurationProcessorUtilities {

  /**
   * GWT >= 2.0
   */
  public static final String GWT_DEV_MODE = "com.google.gwt.dev.DevMode";

  /**
   * GWT >= 2.7
   */
  public static final String GWT_CODE_SERVER = "com.google.gwt.dev.codeserver.CodeServer";

  /**
   * All
   */
  public static final String GWT_COMPILER = "com.google.gwt.dev.Compiler";

  /**
   * GWT >= 2.0
   */
  public static boolean isDevMode(ILaunchConfiguration config) throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return GWT_DEV_MODE.equals(mainTypeName);
  }

  /**
   * GWT >= 2.7
   */
  public static boolean isSuperDevModeCodeServer(ILaunchConfiguration config) throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return GWT_CODE_SERVER.equals(mainTypeName);
  }

  /**
   * All
   */
  public static boolean isCompiler(ILaunchConfiguration config) throws CoreException {
    String mainTypeName = LaunchConfigurationProcessorUtilities.getMainTypeName(config);
    return GWT_COMPILER.equals(mainTypeName);
  }

}
