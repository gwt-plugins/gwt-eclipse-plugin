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

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Thread used to ping server to test when it is started.
 */
public final class PingThread {
  private static final String NAME = "Cloud SDK Server Ping Thread";

  // delay before pinging starts
  private static final int PING_DELAY = 2000;

  // delay between pings
  private static final int PING_INTERVAL = 250;

  // maximum number of pings before giving up
  private int maxPings;

  private volatile boolean stop = false;
  private String url;
  private IServer server;
  private CloudSdkServerBehaviour behaviour;

  /**
   * Create a new PingThread.
   *
   * @param server the server to be monitored
   * @param url the url to ping
   * @param maxPings the maximum number of times to try pinging, or -1 to continue forever
   * @param behaviour the {@link ServerBehaviourDelegate} of {@code server}
   */
  public PingThread(IServer server, String url, int maxPings, CloudSdkServerBehaviour behaviour) {
    this.server = server;
    this.url = url;
    this.maxPings = maxPings;
    this.behaviour = behaviour;
  }

  /**
   * Starts a thread that pings the url to be monitored.
   */
  public void start() {
    Thread t = new Thread(NAME) {
      @Override
      public void run() {
        ping();
      }
    };
    t.setDaemon(true);
    t.start();
  }

  /**
   * Tell the pinging to stop.
   */
  public void stop() {
    stop = true;
  }

  /**
   * Ping the server until it is started. Then set the server state to STATE_STARTED.
   */
  private void ping() {
    int count = 0;
    try {
      Thread.sleep(PING_DELAY);
    } catch (Throwable e) {
      // ignore
    }
    while (!stop) {
      try {
        if (count == maxPings) {
          try {
            server.stop(false);
          } catch (Throwable e) {
            // ignore
          }
          stop = true;
          break;
        }
        count++;

        URL pingUrl = new URL(url);
        URLConnection conn = pingUrl.openConnection();
        ((HttpURLConnection) conn).getResponseCode();

        // ping worked - server is up
        if (!stop) {
          behaviour.setServerStarted();
        }
        stop = true;
      } catch (Throwable e) {
        // pinging failed
        try {
          Thread.sleep(PING_INTERVAL);
        } catch (InterruptedException e2) {
          // ignore
        }
      }
    }
  }
}
