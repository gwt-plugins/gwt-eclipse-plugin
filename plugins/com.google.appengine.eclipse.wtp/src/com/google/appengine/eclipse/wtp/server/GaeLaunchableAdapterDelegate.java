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
package com.google.appengine.eclipse.wtp.server;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.util.HttpLaunchable;
import org.eclipse.wst.server.core.util.WebResource;

import java.net.URL;

/**
 * {@link LaunchableAdapterDelegate} for opening Browser upon server launch.
 */
public final class GaeLaunchableAdapterDelegate extends LaunchableAdapterDelegate {

  @Override
  public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) throws CoreException {
    if (server.getAdapter(GaeServer.class) == null) {
      return null;
    }
    if (!(moduleArtifact instanceof Servlet) && !(moduleArtifact instanceof WebResource)) {
      return null;
    }
    if (moduleArtifact.getModule().loadAdapter(IWebModule.class, null) == null) {
      return null;
    }

    URL url = ((IURLProvider) server.loadAdapter(IURLProvider.class, null)).getModuleRootURL(moduleArtifact.getModule());
    return new HttpLaunchable(url);
  }
}
