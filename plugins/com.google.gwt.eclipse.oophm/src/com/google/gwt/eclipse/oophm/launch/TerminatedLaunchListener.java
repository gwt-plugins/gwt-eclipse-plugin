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
package com.google.gwt.eclipse.oophm.launch;

import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;

/**
 * Listens for terminated launch configurations. When a launch is terminated,
 * the associated model object for the launch configuration is marked as
 * terminated ({@link com.google.gwt.eclipse.oophm.model.LaunchConfiguration#setTerminated()}.
 */
public class TerminatedLaunchListener implements ILaunchesListener2 {

  public void launchesAdded(ILaunch[] launches) {
    // Ignore
  }

  public void launchesChanged(ILaunch[] launches) {
    // Ignore
  }

  public void launchesRemoved(ILaunch[] launches) {
    // Ignore
  }

  public void launchesTerminated(ILaunch[] launches) {
    for (ILaunch launch : launches) {
      WebAppDebugModel.getInstance().launchTerminated(launch);
    }
  }
}
