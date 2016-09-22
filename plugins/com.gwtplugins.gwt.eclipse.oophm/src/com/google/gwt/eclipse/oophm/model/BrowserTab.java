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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a browser tab associated with a launch configuration.
 */
public class BrowserTab implements IModelNode, INeedsAttention {

  /**
   * Holds information about the browser tab.
   */
  public static class Info {

    private final byte[] browserIconBytes;
    private final String remoteHost;
    private final String initialSessionKey;
    private final String tabKey;
    private final String url;
    private final String userAgentTag;

    /**
     * Construct a new instance.
     * 
     * @param tabKey stable browser tab identifier
     * @param userAgentTag short-form user agent identifier, suitable for use in a
     *          label for this connection
     * @param url URL of top-level window
     * @param remoteHost The browser socket endpoint, in host:port format,
     *          that is communicating with the Development Mode server.
     * @param initialSessionKey the session key for the first session that was
     *          loaded in this tab
     * @param browserIconBytes icon to use for the user agent (fits inside
     *          24x24) or null if unavailable
     */
    public Info(String tabKey, String userAgentTag, String url,
        String remoteHost, String initialSessionKey, byte[] browserIconBytes) {
      this.tabKey = returnEmptyStrIfNull(tabKey);
      this.userAgentTag = generateUniqueNameIfNullOrEmpty(userAgentTag,
          "Browser");
      this.remoteHost = returnEmptyStrIfNull(remoteHost);
      this.url = generateUniqueNameIfNullOrEmpty(url, "URL");
      this.initialSessionKey = returnEmptyStrIfNull(initialSessionKey);
      this.browserIconBytes = browserIconBytes;
    }

    /**
     * Return icon to use for the user agent (fits inside 24x24) or null if
     * unavailable
     */
    public byte[] getBrowserIconBytes() {
      return browserIconBytes;
    }

    /**
     * Return the browser socket endpoint, in host:port format, that is being
     * used to communicate with the Development Mode server, or "Unknown".
     * 
     * @return the remote host as a string formatted as host:port
     */
    public String getRemoteHost() {
      return remoteHost;
    }

    /**
     * Return a stable browser tab identifier for this tab, or the empty string.
     */
    public String getTabKey() {
      return tabKey;
    }

    /**
     * Return the top-level URL for the tab.
     */
    public String getUrl() {
      return url;
    }

    /**
     * Return a short-form user agent identifier suitable for use in a label.
     */
    public String getUserAgentTag() {
      return userAgentTag;
    }

    /**
     * Return the key identifying the first session that was loaded in this tab.
     * Note that after the tab is refreshed, this value will not reflect the
     * session for the current session. This method is only useful when tabs do
     * not have an identifier, and there is only one session per browser tab.
     * Otherwise, it is best to look at the session key for a given module.
     */
    String getInitialSessionKey() {
      return initialSessionKey;
    }
  }

  /**
   * A reference to a module instance in a browser tab. A module instance is
   * identified by its name and its session key.
   */
  public static class ModuleHandle {

    private final String moduleName;
    private final String sessionKey;

    public ModuleHandle(String moduleName, String sessionKey) {
      this.moduleName = moduleName;
      this.sessionKey = sessionKey;
    }

    public String getName() {
      return moduleName;
    }

    public String getSessionKey() {
      return sessionKey;
    }
  }

  private static final AtomicInteger nextIdForUnspecifiedValue = new AtomicInteger(
      1);

  /**
   * Computes a tab name prefix for the {@link Info} instance. This name may be
   * augmented by the caller to guarantee uniqueness within the
   * {@link LaunchConfiguration}.
   */
  static String computeNamePrefix(Info info) {
    String path;
    try {
      URL url = new URL(info.getUrl());
      path = url.getPath();
      if (path.startsWith("/") && path.length() > 1) {
        path = path.substring(1);
      }
    } catch (MalformedURLException e) {
      path = info.getUrl();
    }

    return path + " - " + info.getUserAgentTag();
  }

  private static String generateUniqueNameIfNullOrEmpty(String str,
      String prefix) {
    if (str == null || str.length() == 0) {
      return prefix + " " + nextIdForUnspecifiedValue.getAndIncrement();
    }

    return str;
  }

  private static String returnEmptyStrIfNull(String str) {
    if (str == null) {
      return "";
    }

    return str;
  }

  private final int id;
  private boolean isTerminated = false;
  private String needsAttentionLevel = null;

  private final LaunchConfiguration launchConfiguration;
  private final Log<BrowserTab> log = new Log<BrowserTab>(this);
  private final List<ModuleHandle> modules = new ArrayList<ModuleHandle>();
  private final String name;
  private final Object privateInstanceLock = new Object();

  private final Info tabInfo;

  /**
   * Create a new browser tab instance.
   * 
   * @param launchConfiguration The launch configuration associated with this
   *          browser tab instance
   * @param info information about the browser tab and the modules that it's
   *          loaded
   * @param name the name of this tab
   * @param moduleName initial module name
   */
  BrowserTab(LaunchConfiguration launchConfiguration, Info info, String name,
      String moduleName) {
    id = launchConfiguration.getModel().getModelNodeNextId();
    this.launchConfiguration = launchConfiguration;
    this.tabInfo = info;
    this.name = name;
    addModule(moduleName, info.getInitialSessionKey());
  }

  /**
   * Add a module that was loaded in this browser tab. If the module name is
   * null or the empty string, a name will be generated. It is legal to add a
   * module with the same name and session key more than once.
   * 
   * If this browser tab was marked as terminated, then it will be reset to the
   * unterminated state. An event will be fired to all listeners on the model.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   * 
   * @param moduleName the name of the module
   * @param sessionKey the session in which this module was loaded
   * @return a handle to the loaded module
   */
  public ModuleHandle addModule(String moduleName, String sessionKey) {
    String sanitizedModuleName = generateUniqueNameIfNullOrEmpty(moduleName,
        "Module");
    ModuleHandle moduleHandle = new ModuleHandle(sanitizedModuleName,
        sessionKey);
    synchronized (privateInstanceLock) {
      modules.add(moduleHandle);
    }

    setTerminated(false);
    return moduleHandle;
  }

  /**
   * Mark all log entries for this element as being undisclosed, and clears the
   * attention level of this node.
   * 
   * Fires removal events and a needs attention event to all listeners of this
   * log.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   */
  public void clearLog() {
    getLog().undiscloseAllLogEntries();
    setNeedsAttentionLevel(null);
  }

  public List<IModelNode> getChildren() {
    return Collections.emptyList();
  }

  public int getId() {
    return id;
  }

  /**
   * Return detailed information about this browser tab.
   */
  public Info getInfo() {
    return tabInfo;
  }

  /**
   * Returns the launch configuration associated with this browser tab.
   */
  public LaunchConfiguration getLaunchConfiguration() {
    return launchConfiguration;
  }

  /**
   * Return the log associated with this browser tab.
   */
  public Log<BrowserTab> getLog() {
    return log;
  }

  /**
   * Return a list of the names of the modules that were loaded in this browser
   * tab.
   */
  public List<ModuleHandle> getModules() {
    synchronized (privateInstanceLock) {
      return new ArrayList<ModuleHandle>(modules);
    }
  }

  /**
   * Return the name of the browser tab.
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
   * Returns whether or not the browser tab has been terminated.
   */
  public boolean isTerminated() {
    synchronized (privateInstanceLock) {
      return isTerminated;
    }
  }

  /**
   * Removes a module that is no longer loaded in this browser tab. If, after
   * the removal of the module from the list of loaded modules, this browser tab
   * has no loaded modules, it will be marked as terminated, and an event will
   * be reported to all listeners on the model.
   * 
   * There should only be a single module loaded in this tab that matches the
   * given module handle.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   * 
   * @param moduleHandle a handle to the module that was unloaded. This handle
   *          must have been one that was returned by a call to
   *          {@link #addModule(String, String)} on this browser tab instance
   * @return true if the module existed in the list of loaded modules and was
   *         removed, false otherwise
   */
  public boolean removeModule(ModuleHandle moduleHandle) {
    boolean noLoadedModules = false;
    boolean wasModuleRemoved = false;
    synchronized (privateInstanceLock) {
      if (modules.remove(moduleHandle)) {
        wasModuleRemoved = true;
        noLoadedModules = (modules.size() == 0);
      }
    }

    if (noLoadedModules) {
      setTerminated(true);
    }

    return wasModuleRemoved;
  }

  public void setNeedsAttentionLevel(String needsAttentionLevel) {
    synchronized (privateInstanceLock) {
      // We shouldn't be raising the attn level if terminated, but we should
      // still allow setTerminated() to clear it (needsAttentionLevel == null).
      if (!AttentionLevelUtils.isNewAttnLevelMoreImportantThanOldAttnLevel(
          this.needsAttentionLevel, needsAttentionLevel)
          || (isTerminated() && needsAttentionLevel != null)) {
        return;
      }
      this.needsAttentionLevel = needsAttentionLevel;
    }

    final WebAppDebugModelEvent<BrowserTab> browserTabNeedsAttentionEvent = new WebAppDebugModelEvent<BrowserTab>(
        this);
    fireBrowserTabNeedsAttention(browserTabNeedsAttentionEvent);
  }

  /**
   * Flag this browser tab as terminated. Removes all other terminated tabs
   * (except the most-recently terminated tab) with a name matching that of this
   * tab. Clears the attention level of this node.
   * 
   * Fires a termination event, possibly an attention event, and possibly
   * removal events to all listeners on the {@link WebAppDebugModel}.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   */
  public void setTerminated() {
    setTerminated(true);
  }

  private void fireBrowserTabNeedsAttention(
      WebAppDebugModelEvent<BrowserTab> browserTabNeedsAttentionEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getLaunchConfiguration().getModel().getWebAppDebugModelListeners()) {
      webAppDebugModelListener.browserTabNeedsAttention(browserTabNeedsAttentionEvent);
    }
  }

  private void fireBrowserTabTerminated(
      WebAppDebugModelEvent<BrowserTab> browserTabTerminatedEvent) {
    for (IWebAppDebugModelListener webAppDebugModelListener : getLaunchConfiguration().getModel().getWebAppDebugModelListeners()) {
      webAppDebugModelListener.browserTabTerminated(browserTabTerminatedEvent);
    }
  }

  /**
   * Set the termination state of this browser tab. If <code>terminated</code>
   * is true, and this browser tab is not already terminated, then all other
   * terminated tabs (except the most-recently terminated tab) with a name
   * matching that of this tab with be removed from the model. The attention
   * level of this node will also be cleared. A termination event, possibly an
   * attention event, and possibly removal events to all listeners on the
   * {@link WebAppDebugModel} will be fired.
   * 
   * If <code>terminated</code> is false, and this browser tab is marked as
   * terminated, then a termination state change event will be reported to all
   * listeners on the model.
   * 
   * NOTE: This method fires events. If you're invoking this method from other
   * model classes, make sure that no locks are being held.
   */
  private void setTerminated(boolean terminated) {
    synchronized (privateInstanceLock) {
      if (isTerminated == terminated) {
        return;
      }
      this.isTerminated = terminated;
    }

    final WebAppDebugModelEvent<BrowserTab> browserTabTerminatedEvent = new WebAppDebugModelEvent<BrowserTab>(
        this);
    fireBrowserTabTerminated(browserTabTerminatedEvent);

    if (terminated) {
      setNeedsAttentionLevel(null);

      /*
       * On a browser refresh, we're informed about the new session far quicker
       * than the termination of the existing session. As a result, we miss the
       * opportunity to clean up previous terminated sessions when the new
       * session is added, because at that point, the previous session has not
       * been marked as terminated as yet.
       * 
       * To combat this problem, we'll perform a cleanup of terminated launches
       * whenever we receive a termination notification, in addition to whenever
       * we're informed that a new session has started.
       */
      launchConfiguration.removeAllAssociatedTerminatedTabsExceptMostRecent(this);
    }
  }
}
