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

import com.google.gwt.dev.shell.remoteui.MessageTransport;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest.AddLog.LogType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest.Initialize;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.ViewerRequest.RequestType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.ViewerResponse;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.ViewerResponse.CapabilityExchange.Capability;
import com.google.gwt.dev.shell.remoteui.RequestProcessor;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.LogSniffer;
import com.google.gwt.eclipse.oophm.model.BrowserTab;
import com.google.gwt.eclipse.oophm.model.BrowserTab.ModuleHandle;
import com.google.gwt.eclipse.oophm.model.IModelNode;
import com.google.gwt.eclipse.oophm.model.LaunchConfiguration;
import com.google.gwt.eclipse.oophm.model.Log;
import com.google.gwt.eclipse.oophm.model.LogEntry;
import com.google.gwt.eclipse.oophm.model.LogEntry.Data;
import com.google.gwt.eclipse.oophm.model.WebAppDebugModel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A server for the ViewerService. The Development Mode server implements a
 * ViewerService client, which makes requests to this server.
 */
public class ViewerServiceServer implements RequestProcessor {

  /**
   * This struct is used to store information about a logging handle. Each
   * handle corresponds to a particular log branch entry (which may be the root
   * log entry in some cases), and a module name.
   * 
   * The module name is necessary because multiple log handles can be mapped to
   * the same physical logger. This happens when multiple modules are loaded in
   * the same browser tab. In this case, the log entries that are created must
   * specify the module for which the log message was intended. This struct
   * provides that information.
   */
  private static class LogHandleInfo {
    private final ModuleHandle moduleHandle;
    private final LogEntry<IModelNode> logBranch;

    public LogHandleInfo(ModuleHandle moduleHandle,
        LogEntry<IModelNode> logBranch) {
      this.moduleHandle = moduleHandle;
      this.logBranch = logBranch;
    }
  }

  private static LogEntry.Data createLogEntryDataFromLogEntryMessage(
      ViewerRequest.LogData msgLogData) {
    String helpInfoText = null;
    String helpInfoURL = null;

    if (msgLogData.getHelpInfo() != null) {
      helpInfoText = msgLogData.getHelpInfo().getText();
      helpInfoURL = msgLogData.getHelpInfo().getUrl();
    }

    boolean needsAttention = msgLogData.hasNeedsAttention()
        ? msgLogData.getNeedsAttention() : false;

    LogEntry.Data logEntryData = new LogEntry.Data(msgLogData.getSummary(),
        msgLogData.getDetails(), msgLogData.getLevel(), helpInfoURL,
        helpInfoText, System.currentTimeMillis(), needsAttention);

    return logEntryData;
  }

  private final Object privateInstanceLock = new Object();
  private final AtomicInteger nextLoggerHandleId = new AtomicInteger(1);
  private final ConcurrentHashMap<Integer, LogHandleInfo> loggingHandleMap = new ConcurrentHashMap<Integer, LogHandleInfo>();
  private MessageTransport transport = null;
  private LaunchConfiguration launchConfiguration = null;

  public Response execute(Request request) throws Exception {
    try {
      ViewerRequest viewerRequest = request.getViewerRequest();
      RequestType requestType = viewerRequest.getRequestType();
      if (getLaunchConfiguration() == null && requestType != null
          && requestType != RequestType.INITIALIZE) {
        throw new IllegalStateException("INITIALIZE request must be the first request");
      }

      if (requestType != null) {
        switch (requestType) {
          case INITIALIZE:
            return processInitialize(request.getViewerRequest().getInitialize());

          case CAPABILITY_EXCHANGE:
            return processCapabilityExchange();

          case ADD_LOG:
            return processAddLog(request.getViewerRequest().getAddLog());

          case ADD_LOG_BRANCH:
            return processAddLogBranch(request.getViewerRequest().getAddLogBranch());

          case ADD_LOG_ENTRY:
            return processAddLogEntry(request.getViewerRequest().getAddLogEntry());

          case DISCONNECT_LOG:
            return processDisconnectLog(request.getViewerRequest().getDisconnectLog().getLogHandle());

          default: {
            break;
          }
        }
      }

      throw new IllegalArgumentException(
          "Unknown ViewerService Request: The ViewerService cannot handle requests of type "
              + requestType == null ? "(unknown)" : requestType.name());
    } catch (Exception e) {
      GWTPluginLog.logError(e, "Failed to execute request");

      // Re-throw the exception so we respond with a failure
      throw e;
    }
  }

  /**
   * NOTE: This method will probably be removed in the future. See
   * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=10
   */
  public void setTransport(MessageTransport transport) {
    synchronized (privateInstanceLock) {
      if (this.transport == null) {
        this.transport = transport;
      }
    }
  }

  private Response buildResponse(ViewerResponse.Builder viewerResponseBuilder) {
    Response.Builder responseBuilder = Response.newBuilder();
    if (viewerResponseBuilder != null) {
      responseBuilder.setViewerResponse(viewerResponseBuilder);
    }

    return responseBuilder.build();
  }

  private int createLoggingHandleAndAddToMap(ModuleHandle moduleHandle,
      Log<? extends IModelNode> newLog) {
    return createLoggingHandleAndAddToMap(moduleHandle,
        newLog.getRootLogEntry());
  }

  @SuppressWarnings("unchecked")
  private int createLoggingHandleAndAddToMap(ModuleHandle moduleHandle,
      LogEntry<? extends IModelNode> newLog) {
    int handle = nextLoggerHandleId.getAndIncrement();
    // TODO: Can we improve type safety here?
    LogHandleInfo logInfo = new LogHandleInfo(moduleHandle,
        (LogEntry<IModelNode>) newLog);
    loggingHandleMap.put(Integer.valueOf(handle), logInfo);
    return handle;
  }

  private LaunchConfiguration getLaunchConfiguration() {
    synchronized (privateInstanceLock) {
      return launchConfiguration;
    }
  }

  /**
   * NOTE: This method will probably be removed in the future. See
   * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=10
   */
  private MessageTransport getTransport() {
    synchronized (privateInstanceLock) {
      return transport;
    }
  }

  /**
   * NOTE: This method is likely to move/change in the future. See
   * http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=10
   */
  private void performDevModeServiceCapabilityExchange(
      final DevModeServiceClient client) {
    Thread t = new Thread(new Runnable() {
      public void run() {
        boolean supportsRestart = false;
        try {
          List<DevModeResponse.CapabilityExchange.Capability> supportedDevModeServerCapabilities = client.checkCapabilities();
          supportsRestart = DevModeServiceClient.checkCapability(
              supportedDevModeServerCapabilities,
              DevModeRequest.RequestType.RESTART_WEB_SERVER);

        } catch (Exception e) {
          Activator.getDefault().getLog().log(
              new Status(
                  IStatus.ERROR,
                  Activator.PLUGIN_ID,
                  "Unable to determine whether or not the web server supports restarts.",
                  e));
        }
        if (supportsRestart) {
          getLaunchConfiguration().setSupportsRestartWebServer();
        }
      }
    });

    t.setName("Dev Mode Capability Exchange");
    t.setDaemon(true);
    t.start();
  }

  private Response processAddLog(ViewerRequest.AddLog addLog) {
    LogType type = addLog.getType();
    if (type != null) {
      switch (type) {
        case MODULE: {
          return processAddModuleLog(addLog.getModuleLog());
        }

        default: {
          break;
        }
      }
    }

    throw new IllegalArgumentException(
        "Unknown Log Type: The ViewerService cannot add logs of type " + type == null
            ? "(unknown)" : type.name());
  }

  private Response processAddLogBranch(ViewerRequest.AddLogBranch addLogBranch) {
    LogHandleInfo logHandleInfo = loggingHandleMap.get(Integer.valueOf(addLogBranch.getParentLogHandle()));
    if (logHandleInfo == null) {
      throw new IllegalArgumentException("Log has not been registered");
    }

    LogEntry.Data logEntryData = createLogEntryDataFromLogEntryMessage(addLogBranch.getLogData());
    LogEntry<IModelNode> newLog = new LogEntry<IModelNode>(logEntryData,
        addLogBranch.getIndexInParent(), logHandleInfo.moduleHandle);

    // Log this event
    Data parentLogData = logHandleInfo.logBranch.getLogData();
    LogSniffer.log(
        "AddLogBranch<{0}>: idx({1,number,#}) lvl({2}) attn({3}) label({4}) plabel({5})",
        getLaunchConfiguration().getName(), addLogBranch.getIndexInParent(),
        logEntryData.getLogLevel(), logEntryData.getAttentionLevel(),
        logEntryData.getLabel(),
        (parentLogData != null) ? parentLogData.getLabel() : "");

    logHandleInfo.logBranch.addChild(newLog);

    int handle = createLoggingHandleAndAddToMap(logHandleInfo.moduleHandle,
        newLog);
    ViewerResponse.AddLogBranch.Builder addLogBranchResponseBuilder = ViewerResponse.AddLogBranch.newBuilder();
    addLogBranchResponseBuilder.setLogHandle(handle);

    ViewerResponse.Builder viewerResponseBuilder = ViewerResponse.newBuilder();
    viewerResponseBuilder.setResponseType(ViewerResponse.ResponseType.ADD_LOG_BRANCH);
    viewerResponseBuilder.setAddLogBranch(addLogBranchResponseBuilder);

    return buildResponse(viewerResponseBuilder);
  }

  private Response processAddLogEntry(ViewerRequest.AddLogEntry addLogEntry) {
    LogHandleInfo logHandleInfo = loggingHandleMap.get(Integer.valueOf(addLogEntry.getLogHandle()));
    if (logHandleInfo == null) {
      throw new IllegalArgumentException("Log for handle "
          + addLogEntry.getLogHandle() + " has not been registered");
    }

    LogEntry.Data logEntryData = createLogEntryDataFromLogEntryMessage(addLogEntry.getLogData());
    LogEntry<IModelNode> newLogEntry = new LogEntry<IModelNode>(logEntryData,
        addLogEntry.getIndexInLog(), logHandleInfo.moduleHandle);

    // Log this event
    Data parentLogData = logHandleInfo.logBranch.getLogData();
    LogSniffer.log(
        "AddLogEntry<{0}>: idx({1,number,#}) lvl({2}) attn({3}) label({4}) plabel({5})",
        getLaunchConfiguration().getName(), addLogEntry.getIndexInLog(),
        logEntryData.getLogLevel(), logEntryData.getAttentionLevel(),
        logEntryData.getLabel(),
        (parentLogData != null) ? parentLogData.getLabel() : "");

    logHandleInfo.logBranch.addChild(newLogEntry);

    // TODO: Return the proper response type (which we need to define); all
    // that really matters right now is that we ACK
    return buildResponse(null);
  }

  private Response processAddModuleLog(ViewerRequest.AddLog.ModuleLog moduleLog) {
    ModuleHandle moduleHandle = null;

    // See if we can find a browser tab which matches the given criteria
    BrowserTab tab = launchConfiguration.findBrowserTab(
        moduleLog.getUserAgent(), moduleLog.getUrl(), moduleLog.getTabKey(),
        moduleLog.getSessionKey());

    if (tab == null) {
      // We couldn't find an existing browser tab; create a new one

      byte[] iconBytes = null;
      if (moduleLog.getIcon() != null) {
        iconBytes = moduleLog.getIcon().toByteArray();
      }

      BrowserTab.Info newTabInfo = new BrowserTab.Info(moduleLog.getTabKey(),
          moduleLog.getUserAgent(), moduleLog.getUrl(),
          moduleLog.getRemoteHost(), moduleLog.getSessionKey(), iconBytes);
      tab = launchConfiguration.addBrowserTab(newTabInfo, moduleLog.getName());
      moduleHandle = tab.getModules().get(0);
    } else {
      moduleHandle = tab.addModule(moduleLog.getName(),
          moduleLog.getSessionKey());
    }

    /*
     * TODO: Consider moving this logging handle logic into the BrowserTab
     * class.
     */

    // Create a logging handle for the new module logger
    int logHandle = createLoggingHandleAndAddToMap(moduleHandle, tab.getLog());

    ViewerResponse.AddLog.Builder addLogResponseBuilder = ViewerResponse.AddLog.newBuilder();
    addLogResponseBuilder.setLogHandle(logHandle);

    ViewerResponse.Builder viewerResponseBuilder = ViewerResponse.newBuilder();
    viewerResponseBuilder.setResponseType(ViewerResponse.ResponseType.ADD_LOG);
    viewerResponseBuilder.setAddLog(addLogResponseBuilder);

    return buildResponse(viewerResponseBuilder);
  }

  /**
   * TODO: Should we somehow be asking the Development Mode View what its
   * capabilities are (instead of assuming what they are)? If we do that, we'll
   * have to change the server implementation so that it only process messages
   * for which the Dev Mode View has capabilities.
   * 
   * The capabilities reported back by this method are a basic set of
   * capabilities that must be present in order to do anything at all. As a
   * result, we don't need to query the view for its capability set at this
   * time.
   */
  private Response processCapabilityExchange() {
    ViewerResponse.CapabilityExchange.Builder capabilityExchangeBuilder = ViewerResponse.CapabilityExchange.newBuilder();

    Capability.Builder c1 = Capability.newBuilder();
    c1.setCapability(RequestType.CAPABILITY_EXCHANGE);
    capabilityExchangeBuilder.addCapabilities(c1);

    Capability.Builder c2 = Capability.newBuilder();
    c2.setCapability(RequestType.ADD_LOG);
    capabilityExchangeBuilder.addCapabilities(c2);

    Capability.Builder c3 = Capability.newBuilder();
    c3.setCapability(RequestType.ADD_LOG_BRANCH);
    capabilityExchangeBuilder.addCapabilities(c3);

    Capability.Builder c4 = Capability.newBuilder();
    c4.setCapability(RequestType.ADD_LOG_ENTRY);
    capabilityExchangeBuilder.addCapabilities(c4);

    Capability.Builder c5 = Capability.newBuilder();
    c5.setCapability(RequestType.DISCONNECT_LOG);
    capabilityExchangeBuilder.addCapabilities(c5);

    ViewerResponse.Builder viewerResponseBuilder = ViewerResponse.newBuilder();
    viewerResponseBuilder.setResponseType(ViewerResponse.ResponseType.CAPABILITY_EXCHANGE);
    viewerResponseBuilder.setCapabilityExchange(capabilityExchangeBuilder);

    return buildResponse(viewerResponseBuilder);
  }

  private Response processDisconnectLog(int logHandle) {
    /*
     * TODO: It is probably
     * no longer the case that messages for a disconnected logger can come over
     * the wire. As such, it is probably safe to remove logging handles from the
     * map on disconnection.
     */
    LogHandleInfo logHandleInfo = loggingHandleMap.get(Integer.valueOf(logHandle));
    if (logHandleInfo == null) {
      throw new IllegalArgumentException("Log has not been registered");
    }

    // Log this event
    Data logData = logHandleInfo.logBranch.getLogData();
    LogSniffer.log("DisconnectLog: label({0})",
        (logData != null) ? logData.getLabel() : "");

    IModelNode entity = logHandleInfo.logBranch.getLog().getEntity();
    if (entity instanceof BrowserTab) {
      BrowserTab tab = (BrowserTab) entity;
      if (!tab.removeModule(logHandleInfo.moduleHandle)) {
        Activator.getDefault().getLog().log(
            new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                "Request to unload module "
                    + logHandleInfo.moduleHandle.getName()
                    + ". This module has not been loaded in browser tab "
                    + tab.getName()));
      }
    }

    // TODO: Return the proper response type (which we need to define); all
    // that really matters right now is that we ACK
    return buildResponse(null);
  }

  private Response processInitialize(Initialize initialize) {
    String clientId = initialize.getClientId();
    assert (clientId != null);

    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunch[] launches = launchManager.getLaunches();
    ILaunch launch = null;
    for (int i = 0; launch == null && i < launches.length; ++i) {
      IProcess[] processes = launches[i].getProcesses();
      for (IProcess iProcess : processes) {
        String commandLine = iProcess.getAttribute(IProcess.ATTR_CMDLINE);
        if (commandLine != null && commandLine.indexOf(clientId) != -1) {
          launch = launches[i];
          break;
        }
      }
    }

    WebAppDebugModel model = WebAppDebugModel.getInstance();
    LaunchConfiguration lc = model.addOrReturnExistingLaunchConfiguration(
        launch, clientId, null);
    lc.setLaunchUrls(initialize.getStartupURLsList());
    setLaunchConfiguration(lc);
    DevModeServiceClient devModeServiceClient = new DevModeServiceClient(getTransport());
    DevModeServiceClientManager.getInstance().putClient(
        getLaunchConfiguration(), devModeServiceClient);
    performDevModeServiceCapabilityExchange(devModeServiceClient);

    return buildResponse(null);
  }

  private void setLaunchConfiguration(LaunchConfiguration lc) {
    synchronized (privateInstanceLock) {
      this.launchConfiguration = lc;
    }
  }
}
