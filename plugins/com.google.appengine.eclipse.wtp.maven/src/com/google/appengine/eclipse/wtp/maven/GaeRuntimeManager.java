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
package com.google.appengine.eclipse.wtp.maven;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.appengine.eclipse.wtp.server.GaeServer;

/**
 * Provides a method to ensure that a {@link GaeRuntime} with an appropriate GAE SDK is registered
 * with {@code org.eclipse.wst.server.core}.
 */
public class GaeRuntimeManager {
  
  private static final IRuntimeType GAE_RUNTIME_TYPE =
      ServerCore.findServerType(GaeServer.SERVER_TYPE_ID).getRuntimeType();

  /**
   * Finds the first {@link GaeRuntime} registered with {@code org.eclipse.wst.server.core}, or
   * creates and registers a new {@code GaeRuntime} if one is not already registered, then sets the
   * SDK of that {@code GaeRuntime} to a specified {@link GaeSdk}.
   * 
   * @param sdk the specified SDK
   * @param monitor a progress monitor for the execution of this method
   * @return the newly created {@link GaeRuntime}
   * @throws CoreException
   *     if an error is encountered creating or saving the working copy of the runtime
   */
  public static GaeRuntime ensureGaeRuntimeWithSdk(GaeSdk sdk, IProgressMonitor monitor)
      throws CoreException {
    IRuntime existingRuntime = getGaeRuntime();
    String runtimeId = existingRuntime == null ? null : existingRuntime.getId();
    // The call on createRuntime below creates a new runtime if runtimeId==null, and reuses the
    // existing one otherwise.
    IRuntimeWorkingCopy runtimeWorkingCopy = GAE_RUNTIME_TYPE.createRuntime(runtimeId, monitor);
    GaeRuntime runtime = (GaeRuntime) runtimeWorkingCopy.loadAdapter(GaeRuntime.class, monitor);
    if (sdk != null) {
      String location = sdk.getInstallationPath().toOSString();
      runtime.setGaeSdk(sdk);
      runtimeWorkingCopy.setLocation(new Path(location));
    }
    runtimeWorkingCopy.save(true, monitor);
    return runtime;
  }
  
  /**
   * @return
   *     the GAE runtime registered with {@link org.eclipse.wst.server.core.ServerCore},
   *     or {@code null} if there is none
   */
  @Nullable
  private static IRuntime getGaeRuntime() {
    IRuntime[] runtimes = ServerCore.getRuntimes();
    for (IRuntime runtime : runtimes) {
      if (runtime != null && runtime.getRuntimeType().equals(GAE_RUNTIME_TYPE)) {
        return runtime;
      }
    }
    return null;
  }
  
}
