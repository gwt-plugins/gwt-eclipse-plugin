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

import java.util.List;

/**
 * Interface for an object that contains launch arguments, and gives callbacks
 * when the arguments change.
 */
public interface ILaunchArgumentsContainer {

  /**
   * Listener for when the arguments text fields are modified
   * (character-by-character).
   */
  interface ArgumentsListener {
    /**
     * @param args the current arguments, this list is read-only
     */
    void persistFromArguments(List<String> args, ILaunchConfigurationWorkingCopy config);
  }

  void registerProgramArgsListener(ArgumentsListener listener);
}
