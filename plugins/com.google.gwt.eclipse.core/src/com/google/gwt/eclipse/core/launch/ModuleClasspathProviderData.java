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
package com.google.gwt.eclipse.core.launch;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.launch.ModuleClasspathProvider.IModuleClasspathProviderIdProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Data for each extension of the com.google.gwt.eclipse.core.moduleClasspathProvider
 * extionsion point.
 */
public class ModuleClasspathProviderData implements Comparable<ModuleClasspathProviderData> {
  
  public static final int DEFAULT_PRIORITY = 10;
  
  private IModuleClasspathProviderIdProvider provider;
  private int priority;
  
  public ModuleClasspathProviderData(IConfigurationElement config) {
    priority = convertInt(config.getAttribute("priority"));
    
    try {
      provider = (IModuleClasspathProviderIdProvider) config.createExecutableExtension("providerClass");
    } catch (CoreException e) {
      GWTPluginLog.logError("Unable to create classpath provider entry", e);
    }
  }
  
  public int compareTo(ModuleClasspathProviderData other) {
    return getPriority() - other.getPriority();
  }

  /**
   * @return the priority of this classpath provider. In case of multiple classpath providers,
   * we choose the one with the highest priority.
   */
  public int getPriority() {
    return priority;
  }
  
  /**
   * @return the provider of the classpath provider id. This would be null only
   * if we were not able to load the extension.
   */
  public IModuleClasspathProviderIdProvider getProvider() {
    return provider;
  }
  
  public boolean isProviderAvailable() {
    return provider != null;
  }
  
  private int convertInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException nfe) {
      return DEFAULT_PRIORITY;
    }
  }
  
}