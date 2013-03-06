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
package com.google.gwt.eclipse.core.modules;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Interface to be implemented by implementers of the
 * com.google.gwt.eclipse.core.moduleInfo extension point.
 */
public interface ModuleInfo {

  /**
   * Returns the list of startup modules for the given project and launch
   * configuration.
   * 
   * @param project the project
   * @param launchConfiguration a launch configuration associated with the
   *          project
   * 
   * @return the list of startup module names, or an empty array if this
   *         extension point cannot return an applicable list of startup module
   *         names.
   */
  String[] getStartupModules(IProject project,
      ILaunchConfiguration launchConfiguration) throws CoreException;
}
