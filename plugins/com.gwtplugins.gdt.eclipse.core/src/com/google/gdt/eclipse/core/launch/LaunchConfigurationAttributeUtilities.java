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
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Utilities for setting and getting values using
 * {@link ILaunchConfigurationAttribute}.
 */
public class LaunchConfigurationAttributeUtilities {

  public static boolean getBoolean(ILaunchConfiguration config,
      ILaunchConfigurationAttribute attribute) throws CoreException {
    return config.getAttribute(attribute.getQualifiedName(),
        ((Boolean) attribute.getDefaultValue()));
  }

  public static String getString(ILaunchConfiguration config,
      ILaunchConfigurationAttribute attribute) throws CoreException {
    return config.getAttribute(attribute.getQualifiedName(),
        (String) attribute.getDefaultValue());
  }

  public static void set(ILaunchConfigurationWorkingCopy config,
      ILaunchConfigurationAttribute attribute, boolean value) {
    if (LaunchConfigurationAttributeUtilities.shouldClear(attribute.getDefaultValue(), value)) {
      LaunchConfigurationAttributeUtilities.clear(config, attribute);
    } else {
      config.setAttribute(attribute.getQualifiedName(), value);
    }
  }

  public static void set(ILaunchConfigurationWorkingCopy config,
      ILaunchConfigurationAttribute attribute, String value) {
    if (LaunchConfigurationAttributeUtilities.shouldClear(attribute.getDefaultValue(), value)) {
      LaunchConfigurationAttributeUtilities.clear(config, attribute);
    } else {
      config.setAttribute(attribute.getQualifiedName(), value);
    }
  }

  private static void clear(
      ILaunchConfigurationWorkingCopy workingCopy,
      ILaunchConfigurationAttribute attribute) {
    workingCopy.setAttribute(attribute.getQualifiedName(), (String) null);
  }

  private static boolean shouldClear(Object defaultValue,
      Object newValue) {
    if (defaultValue == newValue) {
      return true;
    }
  
    if (defaultValue != null) {
      return defaultValue.equals(newValue);
    }
  
    return false;
  }

}
