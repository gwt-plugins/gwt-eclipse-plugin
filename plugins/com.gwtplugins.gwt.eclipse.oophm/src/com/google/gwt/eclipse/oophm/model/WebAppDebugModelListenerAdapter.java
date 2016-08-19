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
 * Default implementation of {@link IWebAppDebugModelListener}.
 */
public class WebAppDebugModelListenerAdapter implements
    IWebAppDebugModelListener {

  public void browserTabCreated(WebAppDebugModelEvent<BrowserTab> e) {
  }

  public void browserTabNeedsAttention(WebAppDebugModelEvent<BrowserTab> e) {
  }

  public void browserTabRemoved(WebAppDebugModelEvent<BrowserTab> e) {
  }

  public void browserTabTerminated(WebAppDebugModelEvent<BrowserTab> e) {
  }

  public void launchConfigurationLaunched(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationLaunchUrlsChanged(WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationRemoved(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationRestartWebServerStatusChanged(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void launchConfigurationTerminated(
      WebAppDebugModelEvent<LaunchConfiguration> e) {
  }

  public void serverCreated(WebAppDebugModelEvent<Server> e) {
  }

  public void serverNeedsAttention(WebAppDebugModelEvent<Server> e) {
  }

  public void serverTerminated(WebAppDebugModelEvent<Server> e) {
  }

}
