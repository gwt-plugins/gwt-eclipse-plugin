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
package com.google.gwt.eclipse.oophm.devmode;

import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages instances of {@link DevModeServiceClient}.
 * 
 * NOTE: This class is likely to change significantly in the near future. See
 * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=10
 */
public class DevModeServiceClientManager {

  private static final DevModeServiceClientManager INSTANCE = new DevModeServiceClientManager();

  public static DevModeServiceClientManager getInstance() {
    return INSTANCE;
  }

  private final Map<LaunchConfiguration, DevModeServiceClient> launchConfigurationToDevModeClient = new HashMap<LaunchConfiguration, DevModeServiceClient>();

  private final Object mapLock = new Object();

  private DevModeServiceClientManager() {
    // Not instantiable
  }

  /**
   * Get a client for the given launch configuration. Returns <code>null</code>
   * if there is no client associated with the launch configuration.
   */
  public DevModeServiceClient getClient(LaunchConfiguration lc) {
    assert (lc != null);
    synchronized (mapLock) {
      return launchConfigurationToDevModeClient.get(lc);
    }
  }

  /**
   * Associate the given client with the given launch configuration. The launch
   * configuration must not already be associated with another client.
   */
  public void putClient(LaunchConfiguration lc, DevModeServiceClient client) {
    assert (lc != null);
    assert (client != null);

    synchronized (mapLock) {
      assert (!launchConfigurationToDevModeClient.containsKey(lc));
      launchConfigurationToDevModeClient.put(lc, client);
    }
  }

  /**
   * Remove the association between the launch configuration and the given
   * launch configuration.
   * 
   * @return true if the association was removed, false otherwise
   */
  public boolean removeClient(LaunchConfiguration lc) {
    assert (lc != null);
    synchronized (mapLock) {
      return (launchConfigurationToDevModeClient.remove(lc) != null);
    }
  }
}
