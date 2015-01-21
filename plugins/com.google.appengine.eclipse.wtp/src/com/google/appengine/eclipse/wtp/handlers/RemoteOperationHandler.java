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

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.properties.ui.DeployEarPropertiesPage;
import com.google.appengine.eclipse.wtp.properties.ui.DeployWebPropertiesPage;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.gdt.eclipse.core.console.CustomMessageConsole;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.console.TerminateJobAction;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.wst.server.core.IServer;

import java.io.OutputStream;

/**
 * Base class preparing for remote Google App Engine operation.
 */
public abstract class RemoteOperationHandler extends AbstractSingleServerHandler {

  protected OutputStream newMessageStream;
  protected String clientId;
  protected String clientSecret;
  protected String refreshToken;
  protected GaeServer gaeServer;

  /**
   * Create a job instance to be scheduled.
   */
  protected abstract Job createJob() throws CoreException;

  /**
   * Performs remote operation to AppEngine server.
   */
  protected void doExecute(IServer server) {
    // prompt for log on
    if (!GoogleLogin.getInstance().isLoggedIn()) {
      boolean signedIn = GoogleLogin.getInstance().logIn(
          "Google App Engine requires authentication.");
      if (!signedIn) {
        // user canceled signing in
        return;
      }
    }
    // check for dirty editors and prompt to save
    if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
      return;
    }
    // get server
    gaeServer = GaeServer.getGaeServer(server);
    if (gaeServer == null) {
      return;
    }
    try {
      // check for appid
      String appId = gaeServer.getAppId().trim();
      IProject project = gaeServer.getProject();
      if (project == null) {
        // show error message
        MessageDialog.openError(Display.getDefault().getActiveShell(), "App Engine error",
            "Invalid project.");
        return;
      }
      boolean isEarProject = JavaEEProjectUtilities.isEARProject(project);
      String appVersion = ProjectUtils.getAppVersion(project).trim();
      if (appId.length() == 0 || (!isEarProject && (appVersion.length() == 0))) {
        String pageId = isEarProject ? DeployEarPropertiesPage.ID : DeployWebPropertiesPage.ID;
        PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(
            Display.getDefault().getActiveShell(), project, pageId, new String[] {pageId}, null);
        if (Window.OK != page.open()) {
          return;
        }
        // check again, user tries to cheat. ;)
        appId = gaeServer.getAppId();
        if (appId == null || appId.trim().length() == 0) {
          MessageDialog.openError(Display.getDefault().getActiveShell(), "App Engine error",
              "Please enter Application ID.");
          return;
        }
      }
      //
      CustomMessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
          gaeServer.getAppId() + " - Google App Engine Operation", null);
      newMessageStream = messageConsole.newMessageStream();

      clientId = GoogleLogin.getInstance().fetchOAuth2ClientId();
      clientSecret = GoogleLogin.getInstance().fetchOAuth2ClientSecret();
      refreshToken = GoogleLogin.getInstance().fetchOAuth2RefreshToken();

      Job job = createJob();

      final TerminateJobAction terminateJobAction = new TerminateJobAction(job);
      messageConsole.setTerminateAction(terminateJobAction);
      messageConsole.activate();

      job.addJobChangeListener(new JobChangeAdapter() {
        @Override
        public void done(IJobChangeEvent event) {
          terminateJobAction.setEnabled(false);
        }
      });

      PlatformUI.getWorkbench().getProgressService().showInDialog(
          Display.getDefault().getActiveShell(), job);
      job.schedule();
    } catch (Throwable e) {
      handleException(e);
      return;
    }
  }

  @Override
  protected Object execute(ExecutionEvent event, IServer server) throws ExecutionException {
    doExecute(server);
    return null;
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
    MessageDialog.openError(Display.getDefault().getActiveShell(), "App Engine error",
        "An error occured during App Engine operation" + (msg == null ? "." : ": " + msg)
            + "\nSee the error log for more details");
  }
}