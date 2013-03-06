/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.deploy.ui;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.deploy.DeployProjectJob;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.console.CustomMessageConsole;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;
import com.google.gdt.eclipse.core.console.TerminateJobAction;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.io.OutputStream;

/**
 * Deploys a GAE project.
 */
public class DeployProjectHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) {

    if (!GoogleLogin.getInstance().isLoggedIn()) {
      boolean signedIn = GoogleLogin.getInstance()
          .logIn("Deploying to Google App Engine requires authentication.");
      if (!signedIn) {
        // user canceled signing in
        return null;
      }
    }

    // Check for dirty editors and prompt to save
    if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
      return null;
    }
    //

    // Get initial project selection
    IProject project = ActiveProjectFinder.getInstance().getProject();

    // Gather deployment parameters
    DeployProjectDialog dlg = new DeployProjectDialog(
        project, Display.getDefault().getActiveShell());
    if (dlg.open() != Window.OK) {
      return null;
    }

    // The project may have changed; update the selectedProject to reflect this.
    project = dlg.getProject();

    DeploymentSet deploymentSet = dlg.getDeploymentSet();

    // Make sure the project is ready for deployment
    GaeProject gaeProject = GaeProject.create(project);
    if (!shouldDeploy(gaeProject)) {
      return null;
    }

    IPath warLocation = WebAppUtilities.getWarOutLocationOrPrompt(project);

    if (warLocation == null) {
      // User canceled the dialog
      return null;
    }

    // Start the deploy job
    CustomMessageConsole messageConsole = MessageConsoleUtilities
      .getMessageConsole(project.getName() + " - Deploy to App Engine", null);

    String oauth2Token;
    try {
      oauth2Token = GoogleLogin.getInstance().fetchOAuth2Token();
    } catch (Exception e) {
      AppEngineCorePluginLog.logError(e, "Error while deploying to AppEngine.");
      MessageDialog.openError(Display.getDefault().getActiveShell(),
          "Error while deploying to AppEngine",
              "An error occured while deploying to AppEngine. "
              + "See the error log for more details");
      return null;
    }

    OutputStream newMessageStream = messageConsole.newMessageStream();
    Job deployJob = new DeployProjectJob(
        oauth2Token, gaeProject, warLocation, deploymentSet, newMessageStream);

    final TerminateJobAction terminateJobAction = new TerminateJobAction(
        deployJob);
    messageConsole.setTerminateAction(terminateJobAction);
    messageConsole.activate();

    deployJob.addJobChangeListener(new JobChangeAdapter() {
        @Override
      public void done(IJobChangeEvent event) {
        terminateJobAction.setEnabled(false);
      }
    });

    PlatformUI.getWorkbench().getProgressService()
        .showInDialog(Display.getDefault().getActiveShell(), deployJob);
    deployJob.schedule();

    return null;
  }

  private boolean shouldDeploy(GaeProject gaeProject) {
    IStatus status = gaeProject.getDeployableStatus();
    if (status.getSeverity() == IStatus.ERROR) {
      MessageDialog.openError(Display.getDefault().getActiveShell(),
          "Google App Engine", status.getMessage());
      return false;
    } else if (status.getSeverity() == IStatus.WARNING) {
      String message = status.getMessage()
          + "\n\nDo you want to continue and deploy anyway?";
      return MessageDialog.openQuestion(
          Display.getDefault().getActiveShell(), "Google App Engine", message);
    }

    return true;
  }

}