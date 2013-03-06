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
package com.google.gdt.eclipse.platform.jetty;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import javax.servlet.Servlet;

/**
 * Helper to set up the server which listens for viewSource HTTP requests.
 */
public class JettyServer implements IJettyServer {

  static {
    initLogging();
  }

  private static void initLogging() {
    // This plugin is not in the classpath of the plugins that use Jetty, so
    // Jetty will not be able to use a Class.forName() to find our Eclipse
    // logger.
    // System.setProperty("org.mortbay.log.class",
    // JettyEclipseLogger.class.getName());

    org.mortbay.log.Log.setLog(new JettyEclipseLogger());
  }

  private int port;

  private Server server;

  private final ServletHandler servletHandler = new ServletHandler();

  public JettyServer(ClassLoader classLoader) {
    // This implementation does not require the class loader
  }

  public void addServlet(String path, Servlet servlet) {
    servletHandler.addServletWithMapping(new ServletHolder(servlet), path);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void start() throws Exception {

    if (server != null) {
      return;
    }

    server = new Server(port);
    Context root = new Context(server, "/");
    root.setServletHandler(servletHandler);
    server.start();
  }

  public void stop() throws Exception {
    if (server == null) {
      return;
    }

    server.stop();
    server = null;
  }

}
