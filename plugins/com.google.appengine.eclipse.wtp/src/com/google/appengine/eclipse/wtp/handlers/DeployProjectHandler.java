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

import com.google.appengine.eclipse.wtp.deploy.DeployJob;
import com.google.appengine.eclipse.wtp.server.ui.ServerSelectionDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.ui.ServerUIUtil;

/**
 * Do deploy to remote server having just a project. Attempts to find appropriate server. If no
 * server found, ask user to create new. If more than one, ask user to select one.
 */
public final class DeployProjectHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = Display.getDefault().getActiveShell();
    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveWorkbenchWindow(
        event).getActivePage().getSelection();
    // this handler is not enabled for empty selection
    IProject project = (IProject) selection.getFirstElement();
    IModule module = ServerUtil.getModule(project);
    IServer[] servers = ServerUtil.getServersByModule(module, new NullProgressMonitor());
    if (servers.length == 0) {
      // not configured, create and add this module to it.
      boolean created = ServerUIUtil.showNewServerWizard(shell, null, null, null);
      if (created) {
        // find just created server for module
        servers = ServerUtil.getServersByModule(module, new NullProgressMonitor());
        if (servers.length != 0) {
          // should be only one
          deployUsingServer(servers[0]);
        }
        // do nothing if the user didn't create server or didn't add a module (project) to it.
      }
    } else if (servers.length == 1) {
      // single server found
      deployUsingServer(servers[0]);
    } else {
      // user do selection
      ServerSelectionDialog dialog = new ServerSelectionDialog(servers, shell);
      if (Window.OK == dialog.open()) {
        IServer server = dialog.getSelectedServer();
        deployUsingServer(server);
      }
    }
    return null;
  }

  /**
   * Attempts to deploy using given server.
   */
  private void deployUsingServer(IServer server) throws ExecutionException {
    ParameterizedRemoteOperationHandler handler = new ParameterizedRemoteOperationHandler();
    handler.jobClassName = DeployJob.class.getName();
    handler.doExecute(server);
  }
}
