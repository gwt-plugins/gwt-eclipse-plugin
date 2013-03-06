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

import com.google.gdt.eclipse.core.CorePlugin;

/**
 * 
 */
public enum WebAppLaunchAttributes implements ILaunchConfigurationAttribute {
  /**
   * Automatically select a server port.
   */
  AUTO_PORT_SELECTION(false),

  /**
   * Server port number to use. Default to 8888.
   */
  SERVER_PORT("8888"),

  /**
   * Run a built-in server.
   */
  RUN_SERVER(true);

  private final Object defaultValue;

  WebAppLaunchAttributes(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getQualifiedName() {
    return CorePlugin.PLUGIN_ID + "." + name();
  }

  @Override
  public String toString() {
    return getQualifiedName();
  }
}