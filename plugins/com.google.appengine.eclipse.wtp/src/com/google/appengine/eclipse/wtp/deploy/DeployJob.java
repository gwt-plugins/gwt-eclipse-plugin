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
package com.google.appengine.eclipse.wtp.deploy;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.runtime.RuntimeUtils;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.appengine.eclipse.wtp.utils.IOUtils;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;

/**
 * Does deploy to Google server.
 */
public final class DeployJob extends WorkspaceJob {

  private String oauth2Token;
  private GaeServer gaeServer;
  private IPath deployDirectory;
  private String appName;
  private OutputStream outputStream;

  public DeployJob(String oauth2Token, GaeServer gaeServer, OutputStream outputStream)
      throws CoreException {
    super("Deploying " + gaeServer.getAppId() + " to Google");
    this.oauth2Token = oauth2Token;
    this.gaeServer = gaeServer;
    this.outputStream = outputStream;
    appName = gaeServer.getAppId();
    deployDirectory = gaeServer.getAppDeployDirectory();
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Deploying " + appName, IProgressMonitor.UNKNOWN);
    IStatus status = Status.OK_STATUS;
    try {
      // sync server before deploy
      try {
        monitor.beginTask("Re-deploying server", IProgressMonitor.UNKNOWN);
        gaeServer.getServer().publish(IServer.PUBLISH_FULL, monitor);
      } finally {
        monitor.done();
      }
      //
      GaeSdk sdk = RuntimeUtils.getRuntimeSdk(gaeServer.getGaeRuntime());
      AppEngineBridge bridge = sdk.getAppEngineBridgeForDeploy();

      // TODO: deployment set
      DeploymentSet deploymentSet = new DeploymentSet(true, Lists.<String> newArrayList());

      String javaExecutable = ProcessUtilities.getJavaExecutableForVMInstall(gaeServer.getGaeRuntime().getVMInstall());
      String javaCompilerExecutable = ProcessUtilities.computeJavaCompilerExecutablePathFromJavaExecutablePath(javaExecutable);
      AppEngineBridge.DeployOptions options = new AppEngineBridge.DeployOptions("", "",
          oauth2Token, deployDirectory.toOSString(), deploymentSet, outputStream, outputStream,
          javaExecutable, javaCompilerExecutable);

      status = bridge.deploy(monitor, options);
      if (status.getSeverity() == IStatus.CANCEL || monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

    } catch (Throwable e) {
      throw new CoreException(new Status(IStatus.ERROR, AppEnginePlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } finally {
      monitor.done();
      IOUtils.closeQuietly(outputStream);
    }
    // check status
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
      status = new Status(IStatus.ERROR, AppEngineCorePlugin.PLUGIN_ID, message, ex);
    } else if (status.isOK()) {
      AppEnginePlugin.logMessage(appName + " successfully deployed to Google App Engine");
    }
    // done
    return status;
  }

}
