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

import java.util.Collections;
import java.util.List;

/**
 * A server that is associated with a {@link LaunchConfiguration}.
 * 
 */
public class Server implements IModelNode, INeedsAttention {

  private final int id;

  private boolean isTerminated = false;
  private final LaunchConfiguration launchConfiguration;
  private final Log<Server> log = new Log<Server>(this);
  private final String name;
  private final Object privateInstanceLock = new Object();
  private final byte[] serverIconData;

  private String needsAttentionLevel = null;

  /**
   * Create a new Server instance.
   * 
   * @param launchConfiguration the launch configuration associated with this
   *          server
   * @param name the name of the server
   * @param serverIconData byte array containing an icon (fitting into 24x24) to
   *          use for the server, or null if unavailable
   */
  public Server(LaunchConfiguration launchConfiguration, String name,
      byte[] serverIconData) {
    id = launchConfiguration.getModel().getModelNodeNextId();
    this.launchConfiguration = launchConfiguration;
    this.name = name;
    this.serverIconData = serverIconData;
  }

  public List<IModelNode> getChildren() {
    return Collections.emptyList();
  }

  public int getId() {
    return id;
  }

  /**
   * Get the launch configuration associated with this server.
   */
  public LaunchConfiguration getLaunchConfiguration() {
    return launchConfiguration;
  }

  /**
   * Get the log associated with this server.
   */
  public Log<Server> getLog() {
    return log;
  }

  /**
   * Get the name of the server.
   */
  public String getName() {
    return name;
  }

  public String getNeedsAttentionLevel() {
    synchronized (privateInstanceLock) {
      return needsAttentionLevel;
    }
  }

  public LaunchConfiguration getParent() {
    return getLaunchConfiguration();
  }

  /**
   * Return the byte array containing the server icon data, or null if
   * unavailable.
   */
  public byte[] getServerIconData() {
    return serverIconData;
  }

  /**
   * Returns whether or not the server has been terminated.
   */
  public boolean isTerminated() {
    synchronized (privateInstanceLock) {
      return isTerminated;
    }
  }

  public void setNeedsAttentionLevel(String needsAttentionLevel) {
    synchronized (privateInstanceLock) {
      if (!AttentionLevelUtils.isNewAttnLevelMoreImportantThanOldAttnLevel(
          this.needsAttentionLevel, needsAttentionLevel)) {
        return;
      }
      this.needsAttentionLevel = needsAttentionLevel;
    }

    final WebAppDebugModelEvent<Server> serverNeedsAttentionEvent = new WebAppDebugModelEvent<Server>(
        this);
    fireServerNeedsAttention(serverNeedsAttentionEvent);
  }

  /**
   * Flag this server as terminated, and clears the attention level of this
   * node. Fires a termination event and possible an attention changed event to
   * all listeners on the {@link WebAppDebugModel}.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   */
  public void setTerminated() {
    synchronized (privateInstanceLock) {
      if (isTerminated) {
        return;
      }
      isTerminated = true;
    }

    setNeedsAttentionLevel(null);

    final WebAppDebugModelEvent<Server> serverTerminatedEvent = new WebAppDebugModelEvent<Server>(
        this);
    fireServerTerminated(serverTerminatedEvent);
  }

  private void fireServerNeedsAttention(
      WebAppDebugModelEvent<Server> serverNeedsAttentionEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getLaunchConfiguration().getModel().getWebAppDebugModelListeners()) {
      webAppDebugModelListener.serverNeedsAttention(serverNeedsAttentionEvent);
    }
  }

  private void fireServerTerminated(
      WebAppDebugModelEvent<Server> serverTerminatedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getLaunchConfiguration().getModel().getWebAppDebugModelListeners()) {
      webAppDebugModelListener.serverTerminated(serverTerminatedEvent);
    }
  }
}
