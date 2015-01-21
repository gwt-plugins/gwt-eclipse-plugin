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
package com.google.appengine.eclipse.wtp.deploy;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.runtime.RuntimeUtils;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.appengine.eclipse.wtp.utils.IOUtils;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.ProcessUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class supporting execution of remote App Engine operations.
 */
public abstract class RemoteOperationJob extends WorkspaceJob {
  private static final String APPCFG_CLASS_NAME = "com.google.appengine.tools.admin.AppCfg";
  private static final String APPENGINE_TOOLS_JAR_NAME = "appengine-tools-api.jar";
  private static final IPath APPENGINE_TOOLS_JAR_PATH = new Path("lib/" + APPENGINE_TOOLS_JAR_NAME);
  // TODO(amitin): move the next constant outta here
  private static final String COMMAND_UPDATE_BACKENDS = "update backends";
  private static final String ARG_OAUTH2 = "--oauth2";
  private static final String ARG_OAUTH2_REFRESH_TOKEN = "--oauth2_refresh_token=";
  private static final String ARG_OAUTH2_CLIENT_SECRET = "--oauth2_client_secret=";
  private static final String ARG_OAUTH2_CLIENT_ID = "--oauth2_client_id=";

  protected static final ExecutorService executorService = Executors.newCachedThreadPool();

  private static void redirectOutput(final InputStream src, final OutputStream dst) {
    final PrintStream printStream = new PrintStream(dst);
    new Thread(new Runnable() {
      @Override
      public void run() {
        Scanner sc = new Scanner(src);
        try {
          while (sc.hasNextLine()) {
            printStream.println(sc.nextLine());
          }
        } catch (Throwable e) {
          // ignore
        } finally {
          sc.close();
        }
      }
    }).start();
  }

  protected GaeServer gaeServer;
  protected IPath appDirectory;
  protected String appName;
  protected OutputStream outputStream;
  protected String oauth2ClientId;
  protected String oauth2ClientSecret;
  protected String oauth2RefreshToken;

  public RemoteOperationJob(String oauth2ClientId, String oauth2ClientSecret,
      String oauth2RefreshToken, GaeServer gaeServer, OutputStream outputStream)
      throws CoreException {
    super("Executing Google App Engine Remote Operation");
    this.oauth2ClientId = oauth2ClientId;
    this.oauth2ClientSecret = oauth2ClientSecret;
    this.oauth2RefreshToken = oauth2RefreshToken;
    this.gaeServer = gaeServer;
    this.outputStream = outputStream;
    appName = gaeServer.getAppId();
    appDirectory = gaeServer.getAppDeployDirectory();
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
    IStatus status = Status.OK_STATUS;
    try {
      preExecute(monitor);
      monitor.beginTask("Deploying " + appName, IProgressMonitor.UNKNOWN);
      execute(monitor);
      postExecute(monitor);
    } finally {
      IOUtils.closeQuietly(outputStream);
    }
    return status;
  }

  protected void execute(IProgressMonitor monitor) throws CoreException {
    final List<String> args = getProcessArguments();
    final Process[] processHolder = new Process[1];
    // start waiting thread which actually runs the process and wait for it result.
    Future<Integer> future = executorService.submit(new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream();
        Process process = pb.start();
        processHolder[0] = process;
        redirectOutput(process.getErrorStream(), outputStream);
        redirectOutput(process.getInputStream(), outputStream);
        return process.waitFor();
      }
    });
    // check waiting thread and cancel execution if required
    int exitCode = 0;
    while (true) {
      try {
        exitCode = future.get(100, TimeUnit.MILLISECONDS);
        break;
      } catch (TimeoutException e) {
        // do nothing, wait again
      } catch (CancellationException e) {
        break;
      } catch (InterruptedException e) {
        break;
      } catch (ExecutionException e) {
        throw new CoreException(StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
      }
      // check for this job to be cancelled after unsuccessful waiting
      if (monitor.isCanceled()) {
        Process process = processHolder[0];
        if (process != null && !future.isDone() && !future.isCancelled()) {
          // process must be terminated, otherwise future.cancel() call has no the desired effect:
          // waiting thread has been terminated but process still runs.
          // NOTE: process.destroy() is called from different thread.
          process.destroy();
        }
        future.cancel(false);
        break;
      }
    }

    if (exitCode != 0) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "Error executing App Engine remote operation. See console output for more information.",
          AppEnginePlugin.PLUGIN_ID));
    }
  }

  protected List<String> getProcessArguments() throws CoreException {
    GaeRuntime runtime = gaeServer.getGaeRuntime();
    GaeSdk sdk = RuntimeUtils.getRuntimeSdk(runtime);

    IVMInstall vmInstall = runtime.getVMInstall();
    if (vmInstall == null) {
      vmInstall = JavaRuntime.getDefaultVMInstall();
    }
    String javaExecutable = ProcessUtilities.getJavaExecutableForVMInstall(vmInstall);
    String classPath = sdk.getInstallationPath().append(APPENGINE_TOOLS_JAR_PATH).toOSString();
    List<String> args = Lists.newArrayList();
    args.add(javaExecutable);
    args.add("-Xmx1100m");
    args.add("-cp");
    args.add(classPath);
    args.add(APPCFG_CLASS_NAME);
    args.add(ARG_OAUTH2);
    args.add(ARG_OAUTH2_CLIENT_ID + oauth2ClientId);
    args.add(ARG_OAUTH2_CLIENT_SECRET + oauth2ClientSecret);
    args.add(ARG_OAUTH2_REFRESH_TOKEN + oauth2RefreshToken);
    return args;
  }

  /**
   * Invoked right after remote operation done.
   */
  protected abstract void postExecute(IProgressMonitor monitor);

  /**
   * Invoked right before remote operation started.
   */
  protected abstract void preExecute(IProgressMonitor monitor);
}