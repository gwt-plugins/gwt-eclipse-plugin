/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Interface implemented by clients that want to perform a custom action at the "buildForLaunch"
 * stage of launching a Web Application Launch Configuration.
 */
public interface IBuildForLaunchCallback {

  /**
   * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#buildForLaunch(org.eclipse.debug.
   *      core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
   */
  void buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
      throws CoreException;

}
