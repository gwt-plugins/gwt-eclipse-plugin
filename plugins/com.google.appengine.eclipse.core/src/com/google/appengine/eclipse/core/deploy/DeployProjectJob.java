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
package com.google.appengine.eclipse.core.deploy;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.deploy.DeploymentParticipantManager;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.UnknownHostException;

/**
 * Deploys the application to Google App Engine.
 */
public class DeployProjectJob extends WorkspaceJob {

  /**
   * Dummy output stream, used for capturing the error log from App Engine if we
   * run into problems creating a temp file to write it to.
   */
  private static class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) {
      // Do nothing
    }
  }

  private static final String LOG_FILE_EXTENSION = "log";

  private static final String LOG_FILE_PREFIX = "appengine-deploy";

  private static OutputStream createFileStream(File file) {
    OutputStream stream = null;
    try {
      stream = new FileOutputStream(file, true);
    } catch (FileNotFoundException e) {
      AppEngineCorePluginLog.logError(e);
    }

    return stream;
  }

  private static File createLog() {
    File errorLog = null;
    try {
      errorLog = File.createTempFile(LOG_FILE_PREFIX, "." + LOG_FILE_EXTENSION);
    } catch (IOException e) {
      AppEngineCorePluginLog.logError(e);
    }

    return errorLog;
  }

  private final DeploymentSet deploymentSet;
  
  private final OutputStream consoleOutputStream;

  private final GaeProject gaeProject;

  private final IPath warLocation;

  private final String oauth2token;
  
  public DeployProjectJob(String oauth2token, GaeProject gaeProject, IPath warLocation,
      DeploymentSet deploymentSet, OutputStream consoleOutputStream) {
    super("Deploying " + gaeProject.getProject().getName() + " to Google");
    this.oauth2token = oauth2token;
    this.gaeProject = gaeProject;
    this.warLocation = warLocation;
    this.consoleOutputStream = consoleOutputStream;
    this.deploymentSet = deploymentSet;
  }

  @Override
  public IStatus runInWorkspace(final IProgressMonitor monitor)
      throws CoreException {

    IStatus status = Status.OK_STATUS;

    File logFile = createLog();

    // If we were able to create a log file, we'll write to it. If not, we'll
    // just send the data to our dummy output stream that ignores all incoming
    // bytes (this eliminates the need for null checks every time we wrap the
    // log stream with a PrintWriter)
    OutputStream logStream = (logFile != null ? createFileStream(logFile)
        : new NullOutputStream());

    monitor.beginTask("Deploying " + gaeProject.getProject().getName(), 115);

    try {
      /*
       * Note that we're using the same consoleOutputStream for both the output
       * of the update check, and deploy output. This should be safe, because
       * both of these operations are synchronous, so the same thread is being
       * used.
       */
      DeploymentParticipantManager.notifyAllParticipants(
          gaeProject.getJavaProject(), deploymentSet, warLocation, consoleOutputStream,
          monitor, DeploymentParticipantManager.NotificationType.PREDEPLOY);

      GaeSdk sdk = gaeProject.getSdk();
      AppEngineBridge bridge = sdk.getAppEngineBridgeForDeploy();

      AppEngineBridge.DeployOptions options = new AppEngineBridge.DeployOptions(
          "",
          "",
          oauth2token,
          warLocation.toOSString(),
          deploymentSet,
          consoleOutputStream,
          logStream,
          ProcessUtilities.computeJavaExecutableFullyQualifiedPath(gaeProject.getJavaProject()),
          ProcessUtilities.computeJavaCompilerExecutableFullyQualifiedPath(gaeProject.getJavaProject()));

      status = bridge.deploy(monitor, options);
      if (status.getSeverity() == IStatus.CANCEL || monitor.isCanceled()) {
        /*
         * If the user canceled, force the status to OK so the finally block
         * does not think there was an error (otherwise an error status is
         * return, and it has an InterruptedException which is an artifact of
         * the way GAE cancels deployment.)
         */
        status = StatusUtilities.OK_STATUS;
        throw new OperationCanceledException();
      }

    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR,
          AppEngineCorePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
    } finally {
      monitor.done();
      try {
        if (logFile != null) {
          assert (logStream != null);
          logStream.close();

          // If errors occurred, let the user know where they can find the
          // detailed error logs
          if (status.getSeverity() == IStatus.ERROR) {
            String logFileLocationMessage = "\nDebugging information may be found in "
                + logFile.getCanonicalPath();
            new PrintWriter(consoleOutputStream, true).println(logFileLocationMessage);
          }
        }

        assert (consoleOutputStream != null);
        consoleOutputStream.close();
      } catch (IOException e) {
        // Ignore IOExceptions during stream close
      }
    }

    if (status.getSeverity() == IStatus.ERROR) {
      Throwable ex = status.getException();
      String message = status.getMessage();

      if (ex instanceof ConnectException) {
        message = "Could not connect to the App Engine server";
      }

      if (ex instanceof UnknownHostException) {
        message = "Invalid host name: " + status.getMessage();
      }

      message += "\n\nSee the deployment console for more details";

      status = new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID,
          message, ex);
    } else if (status.isOK()) {
      AppEngineCorePluginLog.logInfo(gaeProject.getProject().getName()
          + " successfully deployed to Google App Engine");
      
      DeploymentParticipantManager.notifyAllParticipants(
        gaeProject.getJavaProject(), deploymentSet, warLocation, consoleOutputStream,
        monitor, DeploymentParticipantManager.NotificationType.SUCCEEDED);
    }

    return status;
  }
}
