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
package com.google.appengine.eclipse.wtp.maven;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.server.GaeServer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import javax.annotation.Nullable;

/**
 * Provides a method to ensure that a {@link GaeRuntime} with an appropriate GAE SDK is registered
 * with {@code org.eclipse.wst.server.core}.
 */
public class GaeRuntimeManager {

  private static final IRuntimeType GAE_RUNTIME_TYPE =
      ServerCore.findServerType(GaeServer.SERVER_TYPE_ID).getRuntimeType();

  // The %s in this template is meant to be replaced by a version number.
  private static final String GAE_RUNTIME_NAME_TEMPLATE = "Google App Engine %s";

  /**
   * Finds the first {@link GaeRuntime} registered with {@code org.eclipse.wst.server.core} whose
   * GAE SDK has the same version as a specified GAE SDK; or, if such a {@code GaeRuntime} is not
   * already registered, creates and registers a new one and sets its SDK to the specified GAE SDK.
   *
   * @param sdk the specified GAE SDK
   * @param monitor a progress monitor for the execution of this method
   * @return the newly created {@link GaeRuntime}
   * @throws CoreException
   *     if an error is encountered creating or saving the working copy of the runtime
   */
  public static GaeRuntime ensureGaeRuntimeWithSdk(GaeSdk sdk, IProgressMonitor monitor)
      throws CoreException {
    String sdkVersion = sdk.getVersion();
    IRuntime existingRuntime = getGaeRuntime(sdkVersion);
    String runtimeId = existingRuntime == null ? null : existingRuntime.getId();
    // The call on createRuntime below creates a new runtime if runtimeId==null, and reuses the
    // existing one otherwise.
    IRuntimeWorkingCopy runtimeWorkingCopy = GAE_RUNTIME_TYPE.createRuntime(runtimeId, monitor);
    GaeRuntime runtime = (GaeRuntime) runtimeWorkingCopy.loadAdapter(GaeRuntime.class, monitor);
    if (sdk != null) {
      String location = sdk.getInstallationPath().toOSString();
      runtime.setGaeSdk(sdk);
      runtimeWorkingCopy.setLocation(new Path(location));
      runtimeWorkingCopy.setName(String.format(GAE_RUNTIME_NAME_TEMPLATE, sdkVersion));
    }
    runtimeWorkingCopy.save(true, monitor);
    return runtime;
  }

  /**
   * Obtains the registered GAE {@link IRuntime} if any, whose GAE SDK has a specified version.
   *
   * @param sdkVersion the specified version
   * @return
   *     a GAE runtime registered with {@link org.eclipse.wst.server.core.ServerCore} and having
   *     the specified version, or {@code null} if there is none
   */
  @Nullable
  private static IRuntime getGaeRuntime(String sdkVersion) {
    IRuntime[] runtimes = ServerCore.getRuntimes();
    for (IRuntime runtime : runtimes) {
      if (runtime != null && runtime.getRuntimeType().equals(GAE_RUNTIME_TYPE)) {
        GaeRuntime gaeRuntime = (GaeRuntime) runtime.getAdapter(GaeRuntime.class);
        if (gaeRuntime == null) {
          return null;
        }
        String version = gaeRuntime.getGaeSdkVersion();
        if (version != null && version.equals(sdkVersion)) {
          return runtime;
        }
      }
    }
    return null;
  }

}
