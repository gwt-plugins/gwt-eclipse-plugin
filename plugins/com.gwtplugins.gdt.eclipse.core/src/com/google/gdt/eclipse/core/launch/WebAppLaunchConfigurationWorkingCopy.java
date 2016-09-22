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

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * 
 */
public class WebAppLaunchConfigurationWorkingCopy extends
    WebAppLaunchConfiguration {

  public static void setAutoPortSelection(
      ILaunchConfigurationWorkingCopy workingCopy, boolean value) {
    LaunchConfigurationAttributeUtilities.set(workingCopy,
        WebAppLaunchAttributes.AUTO_PORT_SELECTION, value);
  }

  public static void setRunServer(ILaunchConfigurationWorkingCopy workingCopy,
      boolean value) {
    LaunchConfigurationAttributeUtilities.set(workingCopy,
        WebAppLaunchAttributes.RUN_SERVER, value);
  }

  public static void setServerPort(ILaunchConfigurationWorkingCopy workingCopy,
      String serverPort) {
    LaunchConfigurationAttributeUtilities.set(workingCopy,
        WebAppLaunchAttributes.SERVER_PORT, serverPort);
  }

}