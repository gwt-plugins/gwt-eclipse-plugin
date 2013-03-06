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

import com.google.gwt.eclipse.oophm.devmode.DevModeServiceClientManager;

import org.eclipse.debug.core.ILaunch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The model for the OOPHM data. At the top level, the model consists of a list
 * of {@link LaunchConfiguration}.
 * 
 * This class is thread-safe, except for the initializeModel() method.
 * TODO: initializeModel() method gone, determine impact on thread safety.
 */
public class WebAppDebugModel implements IModelNode {

  private static WebAppDebugModel INSTANCE = new WebAppDebugModel();

  /**
   * Get the global model instance.
   */
  public static WebAppDebugModel getInstance() {
    return INSTANCE;
  }

  private final int id;
  private final List<LaunchConfiguration> launchConfigurations = new ArrayList<LaunchConfiguration>();
  private final AtomicInteger nextModelNodeId = new AtomicInteger();

  private final Object privateInstanceLock = new Object();

  private final List<IWebAppDebugModelListener> webAppDebugModelListeners = new ArrayList<IWebAppDebugModelListener>();

  WebAppDebugModel() {
    // Not instantiable
    id = getModelNodeNextId();
  }

  /**
   * Adds the launch configuration to the model, or returns the existing launch
   * configuration. Fires an event to all listeners on the model if the launch
   * configuration was added.
   * 
   * @param clientId optional if newLaunch is non-null
   * @param factory if non-null, this will be used to instantiate a
   *          {@link LaunchConfiguration} to be added
   */
  public LaunchConfiguration addOrReturnExistingLaunchConfiguration(
      ILaunch newLaunch, String clientId, ILaunchConfigurationFactory factory) {
    LaunchConfiguration launchConfiguration;

    synchronized (privateInstanceLock) {
      for (LaunchConfiguration lc : launchConfigurations) {
        if (lc.getLaunch().equals(newLaunch)) {
          return lc;
        }
      }

      String name = LaunchConfiguration.computeNamePrefix(newLaunch, clientId);
      if (factory != null) {
        launchConfiguration = factory.newLaunchConfiguration(newLaunch, name,
            this);
      } else {
        launchConfiguration = new LaunchConfiguration(newLaunch, name, this);
      }
      launchConfigurations.add(launchConfiguration);
    }

    // Only fire events when we're not holding any locks. Otherwise, deadlock
    // may happen.
    WebAppDebugModelEvent<LaunchConfiguration> launchedEvent = new WebAppDebugModelEvent<LaunchConfiguration>(
        launchConfiguration);
    fireLaunchConfigurationLaunched(launchedEvent);

    // Clean up terminated launch configurations with the same name as the added
    // launch configuration
    removeAllAssociatedTerminatedLaunchConfigsExceptMostRecent(launchConfiguration);

    return launchConfiguration;
  }

  /**
   * Add a listener for changes to the model.
   */
  public void addWebAppDebugModelListener(IWebAppDebugModelListener listener) {
    synchronized (privateInstanceLock) {
      webAppDebugModelListeners.add(listener);
    }
  }

  public List<? extends IModelNode> getChildren() {
    return getLaunchConfigurations();
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the most-recently created launch configuration that has not been
   * terminated as yet, or <code>null</code> if no such launch configuration can
   * be found.
   */
  public LaunchConfiguration getLatestActiveLaunchConfiguration() {
    synchronized (privateInstanceLock) {
      for (int i = launchConfigurations.size() - 1; i > -1; i--) {
        LaunchConfiguration lc = launchConfigurations.get(i);
        if (!lc.isTerminated()) {
          return lc;
        }
      }
      return null;
    }
  }

  /**
   * Get the launch configurations.
   */
  public List<LaunchConfiguration> getLaunchConfigurations() {
    synchronized (privateInstanceLock) {
      return new ArrayList<LaunchConfiguration>(launchConfigurations);
    }
  }

  public String getName() {
    // The WebAppDebugModel doesn't have a name; return the empty string
    return "";
  }

  public String getNeedsAttentionLevel() {
    return null;
  }

  public IModelNode getParent() {
    return null;
  }

  /**
   * Returns the terminated launch configurations that are part of the model.
   */
  public List<LaunchConfiguration> getTerminatedLaunchConfigurations() {
    List<LaunchConfiguration> launchConfigurations = getLaunchConfigurations();
    Iterator<LaunchConfiguration> iter = launchConfigurations.iterator();
    while (iter.hasNext()) {
      LaunchConfiguration next = iter.next();
      if (!next.isTerminated()) {
        iter.remove();
      }
    }

    return launchConfigurations;
  }

  /**
   * Returns a list of the {@link IWebAppDebugModelListener} instances that are
   * registered with the model.
   */
  public List<IWebAppDebugModelListener> getWebAppDebugModelListeners() {
    synchronized (privateInstanceLock) {
      return new ArrayList<IWebAppDebugModelListener>(webAppDebugModelListeners);
    }
  }

  public boolean isTerminated() {
    return false;
  }

  /**
   * Given a {@link ILaunch} of a terminated launch, find the associated launch
   * configuration in the model, and mark it as terminated. Fires an event to
   * all listeners on the {@link WebAppDebugModel}.
   * 
   * This method does nothing if no such launch configuration can be found.
   */
  public void launchTerminated(ILaunch launch) {
    LaunchConfiguration launchConfiguration = null;
    synchronized (privateInstanceLock) {
      for (LaunchConfiguration lc : launchConfigurations) {
        if (lc.getLaunch() == launch) {
          launchConfiguration = lc;
          break;
        }
      }
    }

    if (launchConfiguration != null) {
      launchConfiguration.setTerminated();
    }
  }

  /**
   * Remove the launch configuration. Fires an event for the launch
   * configuration that was removed.
   * 
   * @return true if the launch configuration was removed successfully
   */
  public boolean removeLaunchConfiguration(LaunchConfiguration lc) {
    boolean wasRemoved = false;
    synchronized (privateInstanceLock) {
      // TODO: Mark launch configuration as removed, and force the removal of
      // all of its browser tabs and servers
      wasRemoved = launchConfigurations.remove(lc);
    }

    if (wasRemoved) {
      DevModeServiceClientManager.getInstance().removeClient(lc);

      // Only fire events when we're not holding any locks. Otherwise, deadlock
      // may happen.
      WebAppDebugModelEvent<LaunchConfiguration> removedEvent = new WebAppDebugModelEvent<LaunchConfiguration>(
          lc);
      fireLaunchConfigurationRemoved(removedEvent);
    }
    return wasRemoved;
  }

  /**
   * Removes all launches that have been terminated from the model. Fires an
   * event for each launch configuration that was removed.
   * 
   * {@link #removeLaunchConfiguration(LaunchConfiguration)}
   */
  public void removeTerminatedLaunchesFromModel() {
    List<LaunchConfiguration> terminatedLaunchConfigs = new ArrayList<LaunchConfiguration>();
    synchronized (privateInstanceLock) {
      for (LaunchConfiguration lc : launchConfigurations) {
        if (lc.isTerminated()) {
          terminatedLaunchConfigs.add(lc);
        }
      }
    }

    for (LaunchConfiguration lc : terminatedLaunchConfigs) {
      removeLaunchConfiguration(lc);
    }
  }

  /**
   * Remove a listener registered to receive notifications about model changes.
   * 
   * @return true if the listener was removed successfully
   */
  public boolean removeWebAppDebugModelListener(
      IWebAppDebugModelListener listener) {
    synchronized (privateInstanceLock) {
      return webAppDebugModelListeners.remove(listener);
    }
  }

  int getModelNodeNextId() {
    return nextModelNodeId.getAndIncrement();
  }

  private void fireLaunchConfigurationLaunched(
      WebAppDebugModelEvent<LaunchConfiguration> launchedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getWebAppDebugModelListeners()) {
      webAppDebugModelListener.launchConfigurationLaunched(launchedEvent);
    }
  }

  private void fireLaunchConfigurationRemoved(
      WebAppDebugModelEvent<LaunchConfiguration> removedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getWebAppDebugModelListeners()) {
      webAppDebugModelListener.launchConfigurationRemoved(removedEvent);
    }
  }

  private void removeAllAssociatedTerminatedLaunchConfigsExceptMostRecent(
      LaunchConfiguration addedLaunchConfiguration) {
    List<LaunchConfiguration> terminatedAssociatedLaunchConfigs = new ArrayList<LaunchConfiguration>();
    synchronized (privateInstanceLock) {
      for (LaunchConfiguration lc : launchConfigurations) {
        if (lc.isTerminated()
            && lc.getName().equals(addedLaunchConfiguration.getName())) {
          terminatedAssociatedLaunchConfigs.add(lc);
        }
      }
    }

    int numTerminatedLaunchesToRemove = terminatedAssociatedLaunchConfigs.size() - 1;
    for (int i = 0; i < numTerminatedLaunchesToRemove; i++) {
      removeLaunchConfiguration(terminatedAssociatedLaunchConfigs.get(i));
    }
  }
}
