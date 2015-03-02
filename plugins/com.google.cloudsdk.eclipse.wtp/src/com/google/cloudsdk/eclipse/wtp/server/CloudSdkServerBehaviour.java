/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp.server;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.common.io.ByteStreams;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

/**
 * A {@link ServerBehaviourDelegate} for Google Cloud SDK.
 */
public class CloudSdkServerBehaviour extends ServerBehaviourDelegate {
  private PingThread pingThread;
  private IDebugEventSetListener processListener;

  @Override
  public IStatus canStart(String launchMode) {
    // Check that the port is not in use before can start
    CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
    int port = server.getApiPort();
    if (!isPortAvailable(port)) {
      String message = server.getServer().getName() + " has admin port set to " + port
          + " which is already is in use.\nTo use port " + port
          + " stop the processes that are using it";
      CloudSdkPlugin.logError(message);
      return new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID, message);
    }
    return Status.OK_STATUS;
  }

  @Override
  public void stop(boolean force) {
    terminate();
  }

  /**
   * Notifies that the server started.
   */
  public void setServerStarted() {
    setServerState(IServer.STATE_STARTED);
  }

  /**
   * Finalizes preparations to launch server.
   *
   * @param launchMode the mode in which a server is running
   */
  protected void setupLaunch(String launchMode) {
    setServerRestartState(false);
    setServerState(IServer.STATE_STARTING);
    setMode(launchMode);
    String adminHost = CloudSdkServer.getCloudSdkServer(getServer()).getApiHost();

    // ping server to check for startup
    try {
      String url = "http://" + adminHost;
      pingThread = new PingThread(getServer(), url, -1, this);
      pingThread.start();
    } catch (Throwable e) {
      CloudSdkPlugin.logError("Can't ping for GAE Server startup.", e);
    }
  }

  /**
   * Terminates the Cloud SDK server instance, stops the ping thread and removes debug listener.
   */
  protected void terminate() {
    int serverState = getServer().getServerState();
    if ((serverState == IServer.STATE_STOPPED) || (serverState == IServer.STATE_STOPPING)) {
      return;
    }

    try {
      setServerState(IServer.STATE_STOPPING);
      stopDevAppServer(); // sends a "quit" message to port
      ILaunch launch = getServer().getLaunch();
      if (launch != null) {
        launch.terminate();
      }

      if (pingThread != null) {
        pingThread.stop();
        pingThread = null;
      }

      if (processListener != null) {
        DebugPlugin.getDefault().removeDebugEventListener(processListener);
        processListener = null;
      }

      // Terminate the remote debugger
      CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
      ILaunchConfigurationWorkingCopy remoteDebugLaunchConfig = server.getRemoteDebugLaunchConfig();
      if (remoteDebugLaunchConfig != null) {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        for (ILaunch aLaunch : manager.getLaunches()) {
          if (aLaunch.getLaunchConfiguration().equals(remoteDebugLaunchConfig)) {
            try {
              aLaunch.terminate();
              manager.removeLaunch(aLaunch);
              break;
            } catch (DebugException e) {
              CloudSdkPlugin.logError(e);
            }
          }
        }
      }

      setServerState(IServer.STATE_STOPPED);
    } catch (Throwable e) {
      CloudSdkPlugin.logError("Error terminating the Cloud SDK server", e);
    }
  }

  /**
   * Sets up process listener to be able to handle debug events for {@code newProcess).
   *
   * @param newProcess the process to be monitored
   */
  protected void addProcessListener(final IProcess newProcess) {
    if (processListener != null || newProcess == null) {
      return;
    }

    processListener = new IDebugEventSetListener() {
      @Override
      public void handleDebugEvents(DebugEvent[] events) {
        if (events != null) {
          int size = events.length;
          for (int i = 0; i < size; i++) {
            if (newProcess != null && newProcess.equals(events[i].getSource())
                && events[i].getKind() == DebugEvent.TERMINATE) {
              terminate();
            }
          }
        }
      }
    };
    DebugPlugin.getDefault().addDebugEventListener(processListener);
  }

  private boolean isPortAvailable(int port) {
    Socket socket = null;
    try {
      socket = new Socket("localhost", port);
      return false;
    } catch (IOException e) {
      return true;
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          // ignore close exceptions
        }
      }
    }
  }

  private void stopDevAppServer() {
    CloudSdkServer server = CloudSdkServer.getCloudSdkServer(getServer());
    HttpURLConnection connection = null;
    boolean serverStopped = false;
    try {
      String ad = server.getHostName();
      int port = server.getAdminPort();

      URL url = new URL("http", ad, port, "/quit");
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setRequestMethod("GET");
      ByteStreams.toByteArray(connection.getInputStream());
      serverStopped = true;
    } catch (IOException e) {
      CloudSdkPlugin.logError("Error stopping the dev app server", e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    if (serverStopped) {
      try {
        // TODO: confirm appropriate delay time
        Thread.sleep(4000);
      } catch (InterruptedException e) {
        // ignore
      }
    }

  }
}
