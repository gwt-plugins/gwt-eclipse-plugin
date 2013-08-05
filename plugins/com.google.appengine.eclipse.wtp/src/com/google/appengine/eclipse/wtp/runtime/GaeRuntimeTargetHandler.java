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
package com.google.appengine.eclipse.wtp.runtime;

import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

import java.io.File;
import java.util.List;

/**
 * Provides the Runtime Classpath containers to be added into project classpaths.
 */
public final class GaeRuntimeTargetHandler extends RuntimeClasspathProviderDelegate {

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
    return resolveClasspathContainer(runtime);
  }

  @Override
  public IClasspathEntry[] resolveClasspathContainer(IRuntime runtime) {
    GaeRuntime gaeRuntime = (GaeRuntime) runtime.loadAdapter(GaeRuntime.class,
        new NullProgressMonitor());
    GaeSdk gaeSdk = RuntimeUtils.getRuntimeSdk(gaeRuntime);
    if (gaeSdk == null) {
      // no luck
      return new IClasspathEntry[0];
    }
    try {
      synchronized (gaeSdk) {
        AppEngineBridge appEngineBridge = gaeSdk.getAppEngineBridge();
        // exclude datanucleus, it will be added by JPA facet (if installed)
        List<File> buildclasspathFiles = appEngineBridge.getBuildclasspathFiles(false);
        return GaeSdk.getClasspathEntries(buildclasspathFiles, gaeSdk.getInstallationPath());
      }
    } catch (Throwable e) {
      AppEnginePlugin.logMessage(e);
      return new IClasspathEntry[0];
    }
  }
}