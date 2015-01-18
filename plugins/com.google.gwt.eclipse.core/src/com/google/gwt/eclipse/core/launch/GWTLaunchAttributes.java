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

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationAttribute;
import com.google.gwt.eclipse.core.GWTPlugin;

import java.util.Collections;

/**
 *
 */
public enum GWTLaunchAttributes implements ILaunchConfigurationAttribute {

  CODE_SERVER_PORT("9997"),

  CODE_SERVER_PORT_AUTO(false),

  /**
   * Set of entry point modules to pass to GWT.
   */
  ENTRY_POINT_MODULES(Collections.emptyList()),

  /**
   * log level defaults to "info".
   */
  LOG_LEVEL("INFO"),

  OUTPUT_STYLE("OBFUSCATED"),

  /**
   * SDK container path, only necessary when the launch configuration is using a
   * classpath other than the default for the project.
   */
  SDK_CONTAINER_PATH(""),

  SDM_CODE_SERVER_PORT("9876"),

  SUPERDEVMODE_ENABLED(false),

  SUPERDEVMODE_PORT("9876"),

  /**
   * URL to launch.
   */
  URL(""),

  /**
   * Super Dev Mode Code Server War Directory. Resource and compile output will land here.
   */
  CODE_SERVER_LAUNCHER_DIR("");

  // TODO: these should be defined inside their own enum
  public static final String[] LOG_LEVELS = {
    "ERROR", "WARN", "INFO", "TRACE", "DEBUG", "SPAM", "ALL"};

  // TODO: these should be defined inside their own enum
  public static final String[] OUTPUT_STYLES = {
    "OBFUSCATED", "PRETTY", "DETAILED"};

  private final Object defaultValue;

  GWTLaunchAttributes(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public Object getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String getQualifiedName() {
    return GWTPlugin.PLUGIN_ID + "." + name();
  }

  @Override
  public String toString() {
    return getQualifiedName();
  }

}
