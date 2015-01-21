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
package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;

/**
 * Opens default browser and directs it to http://host:port/_ah/admin
 */
public final class OpenLocalAdminConsoleHandler extends AbstractSingleServerHandler {

  private static final String LOCAL_ADMIN_CONSOLE_URL = "http://%s:%s/_ah/admin";

  @Override
  protected Object execute(ExecutionEvent event, IServer server) throws ExecutionException {
    GaeServer gaeServer = GaeServer.getGaeServer(server);
    String host = server.getHost();
    ServerPort port = gaeServer.getMainPort();
    BrowserUtilities.launchBrowserAndHandleExceptions(String.format(LOCAL_ADMIN_CONSOLE_URL, host,
        port.getPort()));
    return null;
  }
}
