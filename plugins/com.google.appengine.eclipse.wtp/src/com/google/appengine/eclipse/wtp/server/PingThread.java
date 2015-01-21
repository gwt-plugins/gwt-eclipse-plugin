/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.server;

import org.eclipse.wst.server.core.IServer;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Thread used to ping server to test when it is started.
 */
public final class PingThread {
  // delay before pinging starts
  private static final int PING_DELAY = 2000;

  // delay between pings
  private static final int PING_INTERVAL = 250;

  // maximum number of pings before giving up
  private int maxPings;

  private volatile boolean stop = false;
  private String url;
  private IServer server;
  private GaeServerBehaviour behaviour;

  /**
   * Create a new PingThread.
   *
   * @param server
   * @param url
   * @param maxPings the maximum number of times to try pinging, or -1 to continue forever
   * @param behaviour
   */
  public PingThread(IServer server, String url, int maxPings, GaeServerBehaviour behaviour) {
    super();
    this.server = server;
    this.url = url;
    this.maxPings = maxPings;
    this.behaviour = behaviour;
    Thread t = new Thread("App Engine Server Ping Thread") {
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
  protected void ping() {
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
          Thread.sleep(200);
          behaviour.setServerStarted();
        }
        stop = true;
      } catch (FileNotFoundException fe) {
        try {
          Thread.sleep(200);
        } catch (Throwable e) {
          // ignore
        }
        behaviour.setServerStarted();
        stop = true;
      } catch (Throwable e) {
        // pinging failed
        if (!stop) {
          try {
            Thread.sleep(PING_INTERVAL);
          } catch (InterruptedException e2) {
            // ignore
          }
        }
      }
    }
  }
}
