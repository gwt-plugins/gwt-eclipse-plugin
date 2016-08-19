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

import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchConfiguration;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.model.BrowserTab.Info;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * A launch configuration with associated {@link BrowserTab} connections and a
 * {@link Server} connection.
 * 
 * This class is thread-safe.
 */
public class LaunchConfiguration implements IModelNode {

  /**
   * Compute a name prefix the {@link ILaunch}. The caller may augment this
   * prefix to make the name unique within the {@link WebAppDebugModel}.
   * 
   * @param clientId optional if launch is non-null
   */
  static String computeNamePrefix(ILaunch launch, String clientId) {
    return launch != null ? launch.getLaunchConfiguration().getName()
        : clientId;
  }

  private final List<BrowserTab> browserTabs = new ArrayList<BrowserTab>();
  private final int id;
  private boolean isServerReloading = false;
  private boolean isTerminated = false;
  private final ILaunch launch;
  private final String launchTypeId;
  private List<String> launchUrls;
  private WebAppDebugModel model;
  private final String name;

  private final Object privateInstanceLock = new Object();

  private Server server = null;

  private boolean supportsRestartWebServer = false;

  /**
   * Create a new instance.
   * 
   * @param name The name of the launch configuration.
   * @param model The model associated with this launch configuration
   */
  LaunchConfiguration(ILaunch launch, String name, WebAppDebugModel model) {
    id = model.getModelNodeNextId();
    this.launch = launch;
    this.name = name;
    this.model = model;

    // We're caching the type ID as
    // ModelLabelProvider.getLaunchConfigurationImage() may request it even
    // after the actual launch configuration has been deleted.
    String typeId = WebAppLaunchConfiguration.TYPE_ID;
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
    if (launchConfiguration != null) {
      try {
        typeId = launchConfiguration.getType().getIdentifier();
      } catch (CoreException e) {
        GWTPluginLog.logError(e,
            "Could not determine the launch configuration type");
      }
    }

    this.launchTypeId = typeId;
  }

  /**
   * Associate a browser tab with this launch configuration. Also removes any
   * other browser tabs with the same name that have been terminated, except for
   * the most-recently terminated one.
   * 
   * Fires an event for browser tab creation and possibly multiple events for
   * browser tab removals to all listeners on the {@link WebAppDebugModel}.
   * 
   * If the launch configuration is already terminated, then the browser tab
   * will not be associated with the launch configuration.
   */
  public BrowserTab addBrowserTab(Info info, String moduleName) {
    BrowserTab browserTab = null;

    synchronized (privateInstanceLock) {
      String tabName = BrowserTab.computeNamePrefix(info);

      if (isTerminated) {
        Activator.getDefault().getLog().log(
            new Status(IStatus.INFO, Activator.PLUGIN_ID, "Browser tab " + tabName
                + " could not be added to launch configuration " + getName()
                + " because the launch configuration is already terminated."));
        return null;
      }

      browserTab = new BrowserTab(this, info, tabName, moduleName);

      // Add to the LaunchConfiguration
      browserTabs.add(browserTab);
    }

    // Only fire events when we're not holding any locks. Otherwise, we can
    // cause deadlock.
    final WebAppDebugModelEvent<BrowserTab> createdEvent = new WebAppDebugModelEvent<BrowserTab>(
        browserTab);
    fireBrowserTabCreated(createdEvent);

    // Clean up terminated browser tabs with the same name
    removeAllAssociatedTerminatedTabsExceptMostRecent(browserTab);

    return browserTab;
  }

  /**
   * Find a browser tab that matches the given criteria.
   * 
   * If the tabKey is either null or the empty string, then matching is done
   * only be url, userAgent, and sessionKey.
   * 
   * If tabKey is set to a value, then matching is done by url, userAgent, and
   * tabKey.
   * 
   * @param url URL of top-level window
   * @param tabKey stable browser tab identifier
   * @param sessionKey the session key
   * @param userAgent short-form user agent identifier
   * @return a browser tab instance matching the criteria, or null
   */
  public BrowserTab findBrowserTab(String userAgent, String url, String tabKey,
      String sessionKey) {
    synchronized (privateInstanceLock) {
      for (BrowserTab tab : browserTabs) {
        Info info = tab.getInfo();
        boolean curTabMatches = false;
        if (info.getUserAgentTag().equals(userAgent)
            && info.getUrl().equals(url)) {
          if (tabKey != null && tabKey.length() > 0) {
            curTabMatches = info.getTabKey().equals(tabKey);
          } else {
            curTabMatches = info.getInitialSessionKey().equals(sessionKey);
          }
        }

        if (curTabMatches) {
          return tab;
        }
      }
    }

    return null;
  }

  /**
   * Returns a list of the browser tabs associated with this launch
   * configuration.
   */
  public final List<BrowserTab> getBrowserTabs() {
    synchronized (privateInstanceLock) {
      return new ArrayList<BrowserTab>(browserTabs);
    }
  }

  public List<IModelNode> getChildren() {
    ArrayList<IModelNode> children = new ArrayList<IModelNode>(getBrowserTabs());
    Server s = getServer();
    if (s != null) {
      children.add(s);
    }
    return children;
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the most-recently created browser tab that has not been terminated
   * as yet, or <code>null</code> if no such tab can be found.
   */
  public BrowserTab getLatestActiveBrowserTab() {
    synchronized (privateInstanceLock) {
      for (int i = browserTabs.size() - 1; i > -1; i--) {
        BrowserTab tab = browserTabs.get(i);
        if (!tab.isTerminated()) {
          return tab;
        }
      }
      return null;
    }
  }

  public ILaunch getLaunch() {
    return launch;
  }

  public String getLaunchTypeId() {
    return launchTypeId;
  }

  /**
   * Returns the list of launch URLs for this launch configuration, or null if
   * they have not been set yet.
   */
  public List<String> getLaunchUrls() {
    synchronized (privateInstanceLock) {
      return launchUrls != null ? new ArrayList<String>(launchUrls) : null;
    }
  }

  /**
   * Returns the model that contains this launch configuration.
   */
  public WebAppDebugModel getModel() {
    return model;
  }

  /**
   * Returns the name of this launch configuration.
   */
  public String getName() {
    return name;
  }

  public String getNeedsAttentionLevel() {
    TreeLogger.Type maxNeedsAttentionLevel = null;

    synchronized (privateInstanceLock) {
      List<IModelNode> allChildren = new ArrayList<IModelNode>();
      allChildren.addAll(browserTabs);
      if (server != null) {
        allChildren.add(server);
      }

      for (IModelNode child : allChildren) {
        TreeLogger.Type childAttentionLevel = null;

        if (child.getNeedsAttentionLevel() != null) {
          childAttentionLevel = LogEntry.toTreeLoggerType(child.getNeedsAttentionLevel());
        }

        if (childAttentionLevel == null) {
          continue;
        }

        if (maxNeedsAttentionLevel == null
            || maxNeedsAttentionLevel.isLowerPriorityThan(childAttentionLevel)) {
          maxNeedsAttentionLevel = childAttentionLevel;
        }
      }
    }

    if (maxNeedsAttentionLevel == null) {
      return null;
    }

    return maxNeedsAttentionLevel.getLabel();
  }

  public WebAppDebugModel getParent() {
    return getModel();
  }

  /**
   * Returns the server associated with this launch configuration.
   */
  public final Server getServer() {
    synchronized (privateInstanceLock) {
      return server;
    }
  }

  /**
   * Returns <code>true</code> if this launch configuration has a web server.
   */
  public boolean hasWebServer() {
    ILaunchConfiguration config = getLaunch().getLaunchConfiguration();
    if (config == null) {
      GWTPluginLog.logError("Checking if the launch is running a web server, but the ILaunch does not have an ILaunchConfiguration.");
      // Return true for the common case
      return true;
    }

    try {
      List<String> commands = LaunchConfigurationProcessorUtilities.parseProgramArgs(config);
      // No web server if the -noserver flag was specified
      return !commands.contains("-noserver");

    } catch (CoreException e) {
      GWTPluginLog.logError(e,
          "Could not check if the launch is running a web server.");
      return true;
    }
  }

  public boolean isServerReloading() {
    synchronized (privateInstanceLock) {
      return isServerReloading;
    }
  }

  /**
   * @return true if the launch is in the serving state, false if it is still
   *         loading
   */
  public boolean isServing() {
    synchronized (privateInstanceLock) {
      return launchUrls != null;
    }
  }

  /**
   * Returns whether or not this launch configuration has terminated.
   */
  public boolean isTerminated() {
    synchronized (privateInstanceLock) {
      return isTerminated;
    }
  }

  public void setLaunchUrls(List<String> launchUrls) {
    synchronized (privateInstanceLock) {
      this.launchUrls = launchUrls;
    }
    
    fireLaunchConfigurationLaunchUrlsChanged(new WebAppDebugModelEvent<LaunchConfiguration>(this));
  }

  /**
   * Sets the server associated with this launch configuration. Fires an event
   * to all listeners on the {@link WebAppDebugModel}. If the launch
   * configuration is already terminated, then the server will not be associated
   * with the launch configuration.
   * 
   * The server cannot be set more than once; there can only be one server
   * associated with a launch configuration over it's lifetime.
   */
  public void setServer(Server newServer) {
    assert (newServer != null);

    boolean wasServerAdded = false;
    synchronized (privateInstanceLock) {
      if (this.server == null) {
        if (!isTerminated) {
          this.server = newServer;
          wasServerAdded = true;
        }
      } else {
        throw new UnsupportedOperationException(
            "Cannot set the server more than once.");
      }
    }

    // Only fire events when we're not holding any locks. Otherwise, we can
    // cause deadlock.
    if (wasServerAdded) {
      final WebAppDebugModelEvent<Server> createEvent = new WebAppDebugModelEvent<Server>(
          server);
      fireServerCreated(createEvent);
    } else {
      // The launch configuration must have been terminated
      // TODO: Create a logger for the OOPHM plugin
      Activator.getDefault().getLog().log(
          new Status(IStatus.INFO, Activator.PLUGIN_ID, "Server "
              + server.getName()
              + " could not be added to launch configuration " + getName()
              + " because the launch configuration is already terminated."));
    }
  }

  public void setServerReloading(boolean serverReloading) {
    synchronized(privateInstanceLock) {
      this.isServerReloading = serverReloading;
    }
    fireLaunchConfigurationRestartWebServerStatusChanged(new WebAppDebugModelEvent<LaunchConfiguration>(this));
  }
  
  public void setSupportsRestartWebServer() {
    synchronized (privateInstanceLock) {
      if (supportsRestartWebServer) {
        return;
      }
      supportsRestartWebServer = true;
    }
    final WebAppDebugModelEvent<LaunchConfiguration> restartWebServerStatusChanged = new WebAppDebugModelEvent<LaunchConfiguration>(
        this);
    fireLaunchConfigurationRestartWebServerStatusChanged(restartWebServerStatusChanged);
  }

  /**
   * Flag this launch configuration as terminated. Fires an event to all
   * listeners on the {@link WebAppDebugModel}.
   * 
   * This will also flag all browser tabs and the server as terminated as well
   * (which will cause events to be fired to listeners on the
   * {@link WebAppDebugModel}).
   */
  public void setTerminated() {
    /*
     * While we check/set the value of isTerminated while protected by a lock,
     * we set the browser tabs and server to terminated outside of the block.
     * This has the effect of allowing the possibility that the model can be
     * queried where a launch configuration has been marked as terminated,
     * whereas all of its children are not necessarily terminated.
     * 
     * In theory, we should not be violating this invariant, but in practice, it
     * does not really matter, because events are fired in the correct order.
     * That is, the browser termination and server termination events will be
     * fired before the launch configuration termination event. Since the view
     * updates are event driven, there is no real reason as to why the view
     * would care to query and see if the parent launch configuration is
     * terminated whenever it receives a server or browser tab termination
     * event. If there happens to be such a case, we may have to revisit this
     * implementation.
     */
    synchronized (privateInstanceLock) {
      if (isTerminated) {
        return;
      }
      isTerminated = true;
    }

    for (BrowserTab tab : getBrowserTabs()) {
      tab.setTerminated();
    }

    Server s = getServer();
    if (s != null) {
      s.setTerminated();
    }

    // Only fire events when we're not holding any locks. Otherwise, we can
    // cause deadlock.
    final WebAppDebugModelEvent<LaunchConfiguration> terminateEvent = new WebAppDebugModelEvent<LaunchConfiguration>(
        this);
    fireLaunchConfigurationTerminated(terminateEvent);
  }

  public boolean supportsRestartWebServer() {
    return supportsRestartWebServer;
  }

  /**
   * Removes all terminated browser tabs that have name matching the given
   * browser tab, except for the most-recently terminated related browser tab.
   * Fires an event to all listeners on the {@link WebAppDebugModel}.
   */
  void removeAllAssociatedTerminatedTabsExceptMostRecent(BrowserTab browserTab) {
    List<BrowserTab> terminatedAssociatedBrowserTabs = new ArrayList<BrowserTab>();
    synchronized (privateInstanceLock) {
      for (BrowserTab tab : browserTabs) {
        if (tab.isTerminated() && tab.getName().equals(browserTab.getName())) {
          terminatedAssociatedBrowserTabs.add(tab);
        }
      }
    }

    int numTerminatedTabsToRemove = terminatedAssociatedBrowserTabs.size() - 1;
    for (int i = 0; i < numTerminatedTabsToRemove; i++) {
      removeBrowserTab(terminatedAssociatedBrowserTabs.get(i));
    }
  }

  private void fireBrowserTabCreated(
      WebAppDebugModelEvent<BrowserTab> tabCreatedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener.browserTabCreated(tabCreatedEvent);
    }
  }

  private void fireBrowserTabRemoved(
      WebAppDebugModelEvent<BrowserTab> removedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener.browserTabRemoved(removedEvent);
    }
  }

  private void fireLaunchConfigurationLaunchUrlsChanged(
      WebAppDebugModelEvent<LaunchConfiguration> launchConfigurationLaunchUrlsChangedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener
          .launchConfigurationLaunchUrlsChanged(launchConfigurationLaunchUrlsChangedEvent);
    }
  }

  private void fireLaunchConfigurationRestartWebServerStatusChanged(
      WebAppDebugModelEvent<LaunchConfiguration> restartWebServerStatusChangedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener.launchConfigurationRestartWebServerStatusChanged(restartWebServerStatusChangedEvent);
    }
  }

  private void fireLaunchConfigurationTerminated(
      WebAppDebugModelEvent<LaunchConfiguration> launchConfigurationTerminatedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener.launchConfigurationTerminated(launchConfigurationTerminatedEvent);
    }
  }

  private void fireServerCreated(
      WebAppDebugModelEvent<Server> serverCreatedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : model.getWebAppDebugModelListeners()) {
      webAppDebugModelListener.serverCreated(serverCreatedEvent);
    }
  }

  /**
   * Remove the browser tab associated with this launch configuration. Fires an
   * event for the browser tab that was removed.
   * 
   * 
   * @return true if the browser tab was removed successfully
   */
  private boolean removeBrowserTab(BrowserTab browserTab) {
    boolean wasRemoved = false;
    synchronized (privateInstanceLock) {
      wasRemoved = browserTabs.remove(browserTab);
    }

    if (wasRemoved) {
      // Only fire events when we're not holding any locks. Otherwise, deadlock
      // may happen.
      WebAppDebugModelEvent<BrowserTab> removedEvent = new WebAppDebugModelEvent<BrowserTab>(
          browserTab);
      fireBrowserTabRemoved(removedEvent);
    }
    return wasRemoved;
  }
}
