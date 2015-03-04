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
package com.google.cloudsdk.eclipse.wtp.deploy;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.cloudsdk.eclipse.wtp.CloudSdkUtils;
import com.google.cloudsdk.eclipse.wtp.GCloudCommandDelegate;
import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.console.MessageConsoleUtilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for the Cloud SDK deploy action.
 */
public class CloudSdkDeployProjectHandler extends AbstractHandler {
  public static final String TITLE = "Cloud SDK Deploy";
  private static final String WAR_SRC_DIR_DEFAULT = "src/main/webapp";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    // Get initial project selection
    final IProject project = ActiveProjectFinder.getInstance().getProject();
    if (project == null) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE, "Cannot find selected project");
      return null;
    }

    final IRuntime runtime;
    try {
      runtime = CloudSdkUtils.getPrimaryRuntime(project);
    } catch (CoreException e) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE, e.getMessage());
      return null;
    }

    if (runtime == null) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE,
          "Must select a primary runtime for " + project.getName());
      return null;
    }

    boolean hasLoggedInUsers;
    try {
      hasLoggedInUsers = GCloudCommandDelegate.hasLoggedInUsers(project, runtime);
    } catch (Exception e) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE, e.getMessage());
      return null;
    }

    if (!hasLoggedInUsers) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE,
          "Please sign in to gcloud before deploying project " + project.getName());
      return null;
    }

    final IPath warLocation = getWarOutLocationOrPrompt(project);
    if (warLocation == null) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE,
          "Must select the WAR directory to deploy " + project.getName());
      return null;
    }

    final IPath sdkLocation = runtime.getLocation();
    if (sdkLocation == null) {
      CloudSdkPlugin.logAndDisplayError(null, TITLE,
          "Set the location of " + runtime.getId());
      return null;
    }

    // TODO: Add progress/cancellation action
    Job job = new Job("Running Cloud SDK Deploy Action") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          List<String> commands = new ArrayList<String>();
          commands.add(sdkLocation + GCloudCommandDelegate.GCLOUD_DIR);

          // TODO: remove the "preview" when gcloud app is public
          commands.add("preview");
          commands.add("app");
          commands.add("deploy");
          commands.add(warLocation.toString());

          MessageConsole messageConsole = MessageConsoleUtilities.getMessageConsole(
              project.getName() + " - " + TITLE, null);
          messageConsole.activate();

          IPath projectLocation = project.getLocation();

          int exitCode = ProcessUtilities.launchProcessAndWaitFor(commands,
              projectLocation.toFile(), messageConsole.newMessageStream(), null);

          if (exitCode != 0) {
            CloudSdkPlugin.logAndDisplayError(null, TITLE,
                "Cloud SDK deploy action terminated with exit code " + exitCode);
          }
        } catch (Throwable e) {
          CloudSdkPlugin.logError(e);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
    return null;
  }

  private IPath getWarOutLocationOrPrompt(final IProject project) {
    IFolder warDir = project.getFolder(WAR_SRC_DIR_DEFAULT);
    if (warDir.exists()) {
      return warDir.getLocation();
    }

    final IPath[] fileSystemPath = new IPath[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setText("WAR Directory Selection");
        dialog.setMessage("Select the WAR directory");
        dialog.setFilterPath(project.getLocation().toOSString());
        String pathString = dialog.open();
        if (pathString != null) {
          fileSystemPath[0] = new Path(pathString);
        }
      }
    });

    return fileSystemPath[0];
  }
}
