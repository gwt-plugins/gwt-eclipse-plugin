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
package com.google.gwt.eclipse.oophm.model;

import org.eclipse.debug.core.ILaunch;

import java.util.Collections;
import java.util.List;

/**
 * LaunchConfiguration subclass that has a static URL.
 */
public class SpeedTracerLaunchConfiguration extends LaunchConfiguration {

  public SpeedTracerLaunchConfiguration(ILaunch launch, String url,
      String name,
      WebAppDebugModel model) {
    super(launch, name, model);
    super.setLaunchUrls(Collections.singletonList(url));
  }

  @Override
  public boolean isServing() {
    // The server is serving as soon as it is started, so always return true
    return true;
  }

  @Override
  public void setLaunchUrls(List<String> launchUrls) {
    // This type of launch configuration sets the URLs on creation
  }

}
