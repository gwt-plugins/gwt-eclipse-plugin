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
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
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

  private static final String CLOUD_CONSOLE_URL = "http://cloud.google.com/console";

  private static final String GOOGLEPLEX = "googleplex.com";

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

  private boolean launchAppInBrowser;

  private final DeploymentSet deploymentSet;

  private final OutputStream consoleOutputStream;

  private final GaeProject gaeProject;

  private final IPath warLocation;

  private final String oauth2token;

  public DeployProjectJob(String oauth2token, GaeProject gaeProject, IPath warLocation,
      DeploymentSet deploymentSet, OutputStream consoleOutputStream, boolean launchBrowser) {
    super("Deploying " + gaeProject.getProject().getName() + " to Google");
    this.oauth2token = oauth2token;
    this.gaeProject = gaeProject;
    this.warLocation = warLocation;
    this.consoleOutputStream = consoleOutputStream;
    this.deploymentSet = deploymentSet;
    this.launchAppInBrowser = launchBrowser;
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
    @SuppressWarnings("resource") // NullOutputStream.close() is a no-op.
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
          ProcessUtilities.computeJavaCompilerExecutableFullyQualifiedPath(
              gaeProject.getJavaProject()));

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
      String message = customizedMessage(status.getMessage(), ex);
      status = new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID, message, ex);
    } else if (status.isOK()) {
      AppEngineCorePluginLog.logInfo(gaeProject.getProject().getName()
          + " successfully deployed to Google App Engine");
      
      DeploymentParticipantManager.notifyAllParticipants(
        gaeProject.getJavaProject(), deploymentSet, warLocation, consoleOutputStream,
        monitor, DeploymentParticipantManager.NotificationType.SUCCEEDED);

      // Launch app after successful deploy
      if (launchAppInBrowser) {
        launchProjectInBrowser();
      }
    }

    return status;
  }

  private String customizedMessage(String statusMessage, Throwable ex) {
    if (statusMessage.contains("This application does not exist")) {
      return "The App Id you selected, " + gaeProject.getAppId() + ", does not exist. Go to "
          + CLOUD_CONSOLE_URL + " to view existing App Ids or create a new App Id.\n";
    } else {
      String prefix =
          ex instanceof ConnectException ? "Could not connect to the App Engine server"
              : ex instanceof UnknownHostException ? "Invalid host name: " + statusMessage
                  : statusMessage;
      return prefix + "\n\nSee the deployment console for more details";
    }
  }

  private void launchProjectInBrowser() {
    String urlString = "";
    String appId = gaeProject.getAppId();
    String version = gaeProject.getAppVersion().trim();

    assert (version != null);
    assert (appId != null);
    
    if (version.length() > 0) {
      version += "-dot-";
    }
    
    if (appId.contains(":")) {
      // Domain application; app ID follows domainName:appId format
      String[] splitApp = appId.split(":");

      if (splitApp.length != 2) {
        AppEngineCorePluginLog.logError("Error retrieving domain and app id during the launch of"
            + " the application in a browser.");
        return;
      }
      String domain = splitApp[0];

      // Update for googleplex
      if (domain.equals("google.com")) {
        domain = GOOGLEPLEX;
      }

      appId = splitApp[1];
      urlString = "http://" + version + appId + "." + domain;
    } else {
      // Regular application
      urlString = "http://" + version + appId + ".appspot.com";
    }

    // Open URI in Desktop default browser if possible, or else open in Eclipse default browser
    if (!BrowserUtilities.launchDesktopDefaultBrowserAndHandleExceptions(urlString)) {
      BrowserUtilities.launchBrowserAndHandleExceptions(urlString);
    }
  }
}
