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

/**
 * An interface for listening to changes in the {@link WebAppDebugModel}.
 */
public interface IWebAppDebugModelListener {

  /**
   * Called whenever a new browser tab is created for a launch configuration.
   */
  void browserTabCreated(WebAppDebugModelEvent<BrowserTab> e);

  /**
   * Called whenever the browser's attention level has changed.
   */
  void browserTabNeedsAttention(WebAppDebugModelEvent<BrowserTab> e);

  /**
   * Called whenever browser tab is removed from a launch configuration.
   */
  void browserTabRemoved(WebAppDebugModelEvent<BrowserTab> e);

  /**
   * Called whenever the termination state of a browser tab has changed.
   */
  void browserTabTerminated(WebAppDebugModelEvent<BrowserTab> e);

  /**
   * Called whenever a new launch configuration is started.
   */
  void launchConfigurationLaunched(WebAppDebugModelEvent<LaunchConfiguration> e);

  /**
   * Called whenever a launch configuration's launch URLs have changed.
   */
  void launchConfigurationLaunchUrlsChanged(WebAppDebugModelEvent<LaunchConfiguration> e);

  /**
   * Called whenever a launch configuration is removed.
   */
  void launchConfigurationRemoved(WebAppDebugModelEvent<LaunchConfiguration> e);

  void launchConfigurationRestartWebServerStatusChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e);

  /**
   * Called whenever a launch configuration has been terminated.
   */
  void launchConfigurationTerminated(
      WebAppDebugModelEvent<LaunchConfiguration> e);

  /**
   * Called whenever a server is started for a launch configuration.
   */
  void serverCreated(WebAppDebugModelEvent<Server> e);

  /**
   * Called whenever the server's attention level has changed.
   */
  void serverNeedsAttention(WebAppDebugModelEvent<Server> e);

  /**
   * Called whenever a server associated with a launch configuration is
   * terminated.
   */
  void serverTerminated(WebAppDebugModelEvent<Server> e);
}
