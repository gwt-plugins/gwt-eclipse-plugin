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
package com.google.appengine.eclipse.wtp.runtime;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.server.GaeServer;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.IStartup;

/**
 * At server core startup, checks and adds default GaeRuntime if needed. Does nothing if any runtime
 * with this type already exists. Otherwise creates new GaeRuntime even if there is no GAE SDK
 * registered. In this case the created runtime must be validated later.
 */
@SuppressWarnings("restriction")
public final class GaeRuntimeConfigurator implements IStartup {
  @Override
  public void startup() {
    // HACK: access GdtPreferences forces it container plug-in to start
    // which will implicitly invoke auto-register GAE SDK
    GdtPreferences.getInstallationId();
    GaeSdk sdk = GaePreferences.getDefaultSdk();
    createRuntime(sdk);
  }

  /**
   * Finds a runtime of given type. If none found, creates new.
   */
  private IRuntime createRuntime(GaeSdk sdk) {
    try {
      IServerType serverType = ServerCore.findServerType(GaeServer.SERVER_TYPE_ID);
      IRuntime[] runtimes = ServerCore.getRuntimes();
      for (IRuntime runtime : runtimes) {
        if (runtime != null && serverType.getRuntimeType().equals(runtime.getRuntimeType())) {
          return runtime;
        }
      }
      // not found, create new
      IRuntimeWorkingCopy runtimeWorkingCopy = serverType.getRuntimeType().createRuntime(null, null);
      GaeRuntime runtime = (GaeRuntime) runtimeWorkingCopy.loadAdapter(GaeRuntime.class,
          new NullProgressMonitor());
      if (sdk != null) {
        // have sdk, initialize
        String location = sdk.getInstallationPath().toOSString();
        runtime.setGaeSdk(sdk);
        runtimeWorkingCopy.setLocation(new Path(location));
      }
      return runtimeWorkingCopy.save(true, null);
    } catch (CoreException e) {
      AppEnginePlugin.logMessage(e);
    }
    return null;
  }

}
