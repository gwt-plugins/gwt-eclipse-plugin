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
package com.google.appengine.eclipse.wtp.handlers;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.deploy.DeployJob;
import com.google.appengine.eclipse.wtp.properties.ui.DeployPropertiesPage;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gdt.eclipse.core.console.CustomMessageConsole;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.console.TerminateJobAction;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.server.core.IServer;

import java.io.OutputStream;

/**
 * Handler for deploy to remote AppEngine server.
 */
public final class DeployHandler extends AbstractSingleServerHandler {

  @Override
  protected Object execute(ExecutionEvent event, IServer server) throws ExecutionException {
    doDeploy(server);
    return null;
  }

  /**
   * Performs deploy operation to AppEngine server.
   */
  private void doDeploy(IServer server) throws ExecutionException {
    // prompt for log on
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      boolean signedIn = GoogleLogin.getInstance().logIn(
          "Deploying to Google App Engine requires authentication.");
      if (!signedIn) {
        // user canceled signing in
        return;
      }
    }
    // get server
    GaeServer gaeServer = GaeServer.getGaeServer(server);
    if (gaeServer == null) {
      return;
    }
    try {
      // check for appid
      String appId = gaeServer.getAppId();
      IProject project = gaeServer.getProject();
      if (project == null) {
        // show error message
        MessageDialog.openError(Display.getDefault().getActiveShell(),
            "Error while deploying to AppEngine", "Invalid project.");
        return;
      }
      if (appId == null || appId.trim().length() == 0) {
        PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(
            Display.getDefault().getActiveShell(), project, DeployPropertiesPage.ID,
            new String[] {DeployPropertiesPage.ID}, null);
        if (Window.OK != page.open()) {
          return;
        }
        // check again, user tries to cheat. ;)
        appId = gaeServer.getAppId();
        if (appId == null || appId.trim().length() == 0) {
          MessageDialog.openError(Display.getDefault().getActiveShell(),
              "Error while deploying to AppEngine", "Please enter Application ID.");
          return;
        }
      }
      //
      CustomMessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
          gaeServer.getAppId() + " - Deploy to App Engine", null);
      OutputStream newMessageStream = messageConsole.newMessageStream();

      String oauth2Token = GoogleLogin.getInstance().fetchOAuth2Token();
      DeployJob deployJob = new DeployJob(oauth2Token, gaeServer, newMessageStream);

      final TerminateJobAction terminateJobAction = new TerminateJobAction(deployJob);
      messageConsole.setTerminateAction(terminateJobAction);
      messageConsole.activate();

      deployJob.addJobChangeListener(new JobChangeAdapter() {
        @Override
        public void done(IJobChangeEvent event) {
          terminateJobAction.setEnabled(false);
        }
      });

      PlatformUI.getWorkbench().getProgressService().showInDialog(
          Display.getDefault().getActiveShell(), deployJob);
      deployJob.schedule();
    } catch (Throwable e) {
      handleException(e);
      return;
    }
  }

  /**
   * Logs error and displays a message to the user.
   */
  private void handleException(Throwable e) {
    AppEnginePlugin.logMessage(e);
    showDeployError(e.getMessage());
  }

  /**
   * Displays a message to the user.
   */
  private void showDeployError(String msg) {
    MessageDialog.openError(Display.getDefault().getActiveShell(),
        "Error while deploying to AppEngine", "An error occured while deploying to AppEngine"
            + (msg == null ? "." : ": " + msg) + "\nSee the error log for more details");
  }
}
