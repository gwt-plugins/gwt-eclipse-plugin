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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.platform.jetty.JettyServer;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

/**
 * Helper to set up the server which listens for viewSource HTTP requests.
 */
public class SourceViewerServer extends JettyServer {

  private static final String VIEW_SOURCE_SERVLET_PATH = "/viewSource";

  public static final SourceViewerServer INSTANCE = new SourceViewerServer();

  public SourceViewerServer() {
    super(SourceViewerServer.class.getClassLoader());

    int port = GWTPreferences.getSourceViewerServerPort();
    setPort(port);
    addServlet(VIEW_SOURCE_SERVLET_PATH, new ViewSourceServlet());
  }

  public String getViewSourceUrl() {
    return "http://localhost:" + getPort() + VIEW_SOURCE_SERVLET_PATH;
  }

}
