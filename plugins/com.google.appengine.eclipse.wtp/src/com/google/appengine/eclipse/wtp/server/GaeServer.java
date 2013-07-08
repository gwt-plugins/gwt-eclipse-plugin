/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.server;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.runtime.RuntimeUtils;
import com.google.appengine.eclipse.wtp.utils.ModuleUtils;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.IEnterpriseApplication;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * a {@link ServerDelegate} for Google App Engine.
 */
public final class GaeServer extends ServerDelegate implements IURLProvider {
  private static final String ATTR_GENERIC_SERVER_MODULES = "gae-server-modules-list";
  /**
   * Property which specifies the directory where web applications are published.
   */
  public static final String PROPERTY_DEPLOY_DIR = "deploy-directory";
  public static final String PROPERTY_SERVERPORT = "server-port-number";
  public static final String PROPERTY_AUTORELOAD_TIME = "server-autoreload-time";
  public static final String DEFAULT_DEPLOYDIR = "wtpwebapps";
  public static final String DEFAULT_SERVER_PORT = "8888";
  public static final String DEFAULT_AUTORELOAD_TIME = "5";

  /**
   * @return a {@link GaeServer} instance which associated with gives {@link IServer} instance.
   */
  public static GaeServer getGaeServer(IServer server) {
    GaeServer gaeServer = (GaeServer) server.getAdapter(GaeServer.class);
    if (gaeServer == null) {
      gaeServer = (GaeServer) server.loadAdapter(GaeServer.class, new NullProgressMonitor());
    }
    return gaeServer;
  }

  /**
   * @return a {@link GaeServer} instance which associated with gives {@link IServerWorkingCopy}
   *         instance.
   */
  public static GaeServer getGaeServer(IServerWorkingCopy server) {
    GaeServer gaeServer = (GaeServer) server.getAdapter(GaeServer.class);
    if (gaeServer == null) {
      gaeServer = (GaeServer) server.loadAdapter(GaeServer.class, new NullProgressMonitor());
    }
    return gaeServer;
  }

  private List<PropertyChangeListener> propChangeListeners;

  /**
   * Add a property change listener to this server.
   *
   * @param listener java.beans.PropertyChangeListener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (propChangeListeners == null) {
      propChangeListeners = new ArrayList<PropertyChangeListener>();
    }
    propChangeListeners.add(listener);
  }

  /**
   * Returns {@link Status#OK_STATUS} if the given project is supported by this server, otherwise
   * returns error status.
   */
  @Override
  public IStatus canModifyModules(IModule[] add, IModule[] remove) {
    if (add != null) {
      IModule[] currentModules = getServer().getModules();
      if (currentModules.length > 0 && remove == null) {
        return new Status(
            IStatus.ERROR,
            AppEnginePlugin.PLUGIN_ID,
            0,
            "This server instance already runs \"" + currentModules[0].getName() + "\" application",
            null);
      }
      for (IModule module : add) {
        if (!ModuleUtils.isModuleType(module, ModuleUtils.MODULETYPE_JST_WEB)
            && !ModuleUtils.isModuleType(module, ModuleUtils.MODULETYPE_JST_EAR)) {
          return new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID, 0, "Unsupported module", null);
        }

        if (getServer().getRuntime() == null || getGaeRuntime() == null) {
          return new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID, 0, "Runtime is missing", null);
        }

        if (module.getProject() != null) {
          IStatus status = FacetUtil.verifyFacets(module.getProject(), getServer());
          if (status != null && !status.isOK()) {
            return status;
          }
        }
      }
    }

    return Status.OK_STATUS;
  }

  /**
   * Fire a property change event.
   *
   * @param propertyName a property name
   * @param oldValue the old value
   * @param newValue the new value
   */
  public void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
    if (propChangeListeners == null) {
      return;
    }

    PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
    try {
      Iterator<PropertyChangeListener> iterator = propChangeListeners.iterator();
      while (iterator.hasNext()) {
        try {
          PropertyChangeListener listener = iterator.next();
          listener.propertyChange(event);
        } catch (Exception e) {
          AppEnginePlugin.logMessage("Error firing property change event", e); //$NON-NLS-1$
        }
      }
    } catch (Exception e) {
      AppEnginePlugin.logMessage("Error in property event", e); //$NON-NLS-1$
    }
  }

  /**
   * Gets the directory to which application should be deployed for this server.
   */
  public IPath getAppDeployDirectory() throws CoreException {
    String deployDir = getDeployDirectory();
    IPath deployPath = new Path(deployDir);
    if (!deployPath.isAbsolute()) {
      IPath base = getRuntimeBaseDirectory();
      deployPath = base.append(deployPath);
    }
    //
    return deployPath.append(getRootModule().getName());
  }

  /**
   * @return the application ID currently associated with this server.
   */
  public String getAppId() throws CoreException {
    IProject project = getProject();
    return ProjectUtils.getAppId(project);
  }

  /**
   * @return the autoreload timeout as it set in properties.
   */
  public String getAutoReloadTime() {
    return getServerInstanceProperties().get(PROPERTY_AUTORELOAD_TIME);
  }

  @Override
  public IModule[] getChildModules(IModule[] module) {
    if (module[0] != null && module[0].getModuleType() != null) {
      if (module.length == 1) {
        if (ModuleUtils.isModuleType(module[0], ModuleUtils.MODULETYPE_JST_EAR)) {
          IEnterpriseApplication enterpriseApplication = (IEnterpriseApplication) module[0].loadAdapter(
              IEnterpriseApplication.class, null);
          if (enterpriseApplication != null) {
            IModule[] earModules = enterpriseApplication.getModules();
            if (earModules != null) {
              return earModules;
            }
          }
        } else if (ModuleUtils.isModuleType(module[0], ModuleUtils.MODULETYPE_JST_WEB)) {
          IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, null);
          if (webModule != null) {
            IModule[] modules = webModule.getModules();
            if (modules != null) {
              return modules;
            }
          }
        }
      }
    }
    return new IModule[0];
  }

  /**
   * @return the {@link GaeRuntime} instance or <code>null</code> if missing.
   */
  public GaeRuntime getGaeRuntime() {
    if (getServer().getRuntime() == null) {
      return null;
    }
    return (GaeRuntime) getServer().getRuntime().loadAdapter(GaeRuntime.class, null);
  }

  /**
   * @return the port at which the dev server is bound at.
   */
  public ServerPort getMainPort() {
    ServerPort[] serverPorts = getServerPorts();
    if (serverPorts == null || serverPorts.length == 0) {
      return null;
    }
    return serverPorts[0];
  }

  @Override
  public URL getModuleRootURL(IModule module) {
    try {
      if (module == null || module.loadAdapter(IWebModule.class, null) == null) {
        return null;
      }
      String host = getServer().getHost();
      String url = "http://" + host;
      ServerPort serverPort = getMainPort();
      if (serverPort == null) {
        return null;
      }
      int port = ServerUtil.getMonitoredPort(getServer(), serverPort.getPort(), "web");
      if (port != 80) {
        url += ":" + port;
      }
      if (!url.endsWith("/")) {
        url += "/";
      }
      return new URL(url);
    } catch (Throwable e) {
      AppEnginePlugin.logMessage(e);
      return null;
    }
  }

  /**
   * @return the project associated with root module.
   */
  public IProject getProject() throws CoreException {
    IModule module = getRootModule();
    if (module == null) {
      return null;
    }
    return module.getProject();
  }

  @Override
  public IModule[] getRootModules(IModule module) throws CoreException {
    IStatus status = canModifyModules(new IModule[] {module}, null);
    if (status != null && !status.isOK()) {
      throw new CoreException(status);
    }
    IModule[] parents = doGetParentModules(module);
    if (parents.length > 0) {
      return parents;
    }
    return new IModule[] {module};
  }

  /**
   * Gets the base directory where the server instance runs. This path can vary depending on the
   * configuration. Null may be returned if a runtime hasn't been specified for the server.
   */
  public IPath getRuntimeBaseDirectory() {
    GaeServerBehaviour sb = (GaeServerBehaviour) getServer().loadAdapter(GaeServerBehaviour.class,
        null);
    if (sb == null) {
      return null;
    }
    return sb.getRuntimeBaseDirectory();
  }

  /**
   * Returns the server properties.
   */
  @SuppressWarnings("unchecked")
  public Map<String, String> getServerInstanceProperties() {
    return getAttribute(GaeRuntime.SERVER_INSTANCE_PROPERTIES, Maps.<String, String> newHashMap());
  }

  @Override
  public ServerPort[] getServerPorts() {
    try {
      String serverPortString = getServerPort();
      if (serverPortString == null || serverPortString.trim().length() == 0) {
        serverPortString = DEFAULT_SERVER_PORT;
      }
      int serverPort = Integer.parseInt(serverPortString);
      return new ServerPort[] {new ServerPort("server", "Dev Server Port", serverPort, "HTTP")};
    } catch (Throwable e) {
      return new ServerPort[0];
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor)
      throws CoreException {
    List<String> modules = this.getAttribute(ATTR_GENERIC_SERVER_MODULES, (List<String>) null);

    if (add != null && add.length > 0) {
      if (add.length > 1) {
        throw new CoreException(new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID, 0,
            "This server instance cannot run more than one application", null));
      }
      if (modules == null) {
        modules = Lists.newArrayList();
      }
      for (int i = 0; i < add.length; i++) {
        if (!modules.contains(add[i].getId())) {
          modules.add(add[i].getId());
        }
      }
    }
    if (remove != null && remove.length > 0 && modules != null) {
      for (int i = 0; i < remove.length; i++) {
        modules.remove(remove[i].getId());
      }
    }
    if (modules != null) {
      setAttribute(ATTR_GENERIC_SERVER_MODULES, modules);
    }

  }

  /**
   * Remove a property change listener from this server.
   *
   * @param listener java.beans.PropertyChangeListener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (propChangeListeners != null) {
      propChangeListeners.remove(listener);
    }
  }

  @Override
  public void setDefaults(IProgressMonitor monitor) {
    setServerPort(DEFAULT_SERVER_PORT);
    setAutoreloadTime(DEFAULT_AUTORELOAD_TIME);
  }

  public void setServerInstanceProperties(Map<String, String> properties) {
    setAttribute(GaeRuntime.SERVER_INSTANCE_PROPERTIES, properties);
  }

  /**
   * Checks if the properties set for this server is valid.
   */
  public IStatus validate() {
    String serverPort = getServerPort();
    if (serverPort == null || serverPort.trim().length() == 0) {
      return StatusUtilities.newErrorStatus("Server port value cannot be empty",
          AppEnginePlugin.PLUGIN_ID);
    }
    int port;
    try {
      port = Integer.parseInt(serverPort);
    } catch (Throwable e) {
      return StatusUtilities.newErrorStatus("Invalid server port: " + serverPort,
          AppEnginePlugin.PLUGIN_ID);
    }
    if (port < 0 || port > 65535) {
      return StatusUtilities.newErrorStatus("TCP port value is out of range: " + serverPort,
          AppEnginePlugin.PLUGIN_ID);
    }
    String autoreloadTime = getAutoReloadTime();
    if (autoreloadTime == null || autoreloadTime.trim().length() == 0) {
      return StatusUtilities.newErrorStatus("Auto-reload delay value cannot be empty",
          AppEnginePlugin.PLUGIN_ID);
    }
    try {
      Integer.parseInt(autoreloadTime);
    } catch (Throwable e) {
      return StatusUtilities.newErrorStatus("Invalid autoreload delay time: " + autoreloadTime,
          AppEnginePlugin.PLUGIN_ID);
    }
    return StatusUtilities.newOkStatus("", AppEnginePlugin.PLUGIN_ID);
  }

  /**
   * Check that this server has EAR supporting runtime set. If the server has no modules then return
   * {@link Status#OK_STATUS}.
   */
  public IStatus validateEarSupported(GaeRuntime gaeRuntime) {
    IStatus runtimeStatus = Status.OK_STATUS;
    GaeSdk sdk = RuntimeUtils.getRuntimeSdkNoFallback(gaeRuntime);
    if (sdk != null) {
      try {
        IProject project = getProject();
        if (project == null) {
          return runtimeStatus;
        }
        if (FacetedProjectFramework.hasProjectFacet(project, "jst.ear")) {
          if (!sdk.getCapabilities().contains(GaeSdkCapability.EAR)) {
            runtimeStatus = StatusUtilities.newErrorStatus(
                "This App Engine SDK doesn't support EAR projects, use "
                    + GaeSdkCapability.EAR.minVersion + " or later.", AppEnginePlugin.PLUGIN_ID);
          }
        }
      } catch (CoreException e) {
        // ignore exceptions here, as there could be no modules yet associated with this server.
      }
    }
    return runtimeStatus;
  }

  /**
   * @return root module representing the GAE application.
   */
  protected IModule getRootModule() throws CoreException {
    IModule[] modules = getServer().getModules();
    if (modules == null || modules.length == 0) {
      throw new CoreException(new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID,
          "No modules found"));
    }
    if (modules == null || modules.length > 1) {
      throw new CoreException(new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID,
          "More than one module found"));
    }
    return modules[0];
  }

  private IModule[] doGetParentModules(IModule module) {
    IModule[] earModules = ServerUtil.getModules(ModuleUtils.MODULETYPE_JST_EAR); //$NON-NLS-1$
    ArrayList<IModule> list = new ArrayList<IModule>();
    for (IModule earModule : earModules) {
      IEnterpriseApplication earApp = (IEnterpriseApplication) earModule.loadAdapter(
          IEnterpriseApplication.class, null);
      for (IModule childModule : earApp.getModules()) {
        if (childModule.equals(module)) {
          list.add(earModule);
        }
      }
    }
    return list.toArray(new IModule[list.size()]);
  }

  /**
   * Gets the directory to which web application is to be deployed. If relative, it is relative to
   * the runtime base directory for the server.
   */
  private String getDeployDirectory() {
    return getAttribute(PROPERTY_DEPLOY_DIR, "");
  }

  /**
   * @return the main server port as it set in properties.
   */
  private String getServerPort() {
    return getServerInstanceProperties().get(PROPERTY_SERVERPORT);
  }

  /**
   * Set the time after which server will reload app upon resource change. 0 means never reload.
   */
  private void setAutoreloadTime(String seconds) {
    getServerInstanceProperties().put(PROPERTY_AUTORELOAD_TIME, seconds);
  }

  /**
   * Set the server port property value.
   */
  private void setServerPort(String portString) {
    getServerInstanceProperties().put(PROPERTY_SERVERPORT, portString);
  }
}
