/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/

package com.google.gdt.eclipse.appengine.swarm.wizards;

import com.google.appengine.eclipse.core.properties.ui.GaeProjectChangeExtension;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.appengine.swarm.util.ConnectedProjectHandler;
import com.google.gdt.eclipse.appengine.swarm.wizards.helpers.SwarmServiceCreator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * When AppId of App Engine project gets changed, this gets called and it
 * regenerates the Web API libraries.
 * 
 * TODO(rdayal): This rebuild can occur even during the user-generated action of
 * generating an App Engine backend. We need to disable this behavior during the
 * generation process.
 */
public class HandleGaeProjectChange implements GaeProjectChangeExtension {

  /**
   * TODO(rdayal): Clarify the contract of this method. It seems that it is only
   * called whenever the user explicitly changes their App ID via the
   * Preferences Dialog. Note that it regenerates libraries whether or not this
   * project is connected to another Android project. Perhaps we should be
   * checking this before performing a regenerate? Or at least checking to see
   * if libraries have been generated in this project's directory?
   */
  public void gaeAppIdChanged(IProject appEngineProject) {
    SwarmServiceCreator serviceCreator = createSwarmServiceCreator(appEngineProject);
    try {
      serviceCreator.create(true, new NullProgressMonitor());
    } catch (Exception e) {
      AppEngineSwarmPlugin.log(e);
    }
  }

  /**
   * TODO(rdayal): Clarify the contract of this method.
   * 
   * It regenerates libraries only if appengine-web.xml changes and this is a
   * Connected Android project. What about the case where the project is not
   * connected, but the user has generated libaries locally?
   */
  public void gaeProjectRebuilt(IProject appEngineProject, boolean appEngineWebXmlChanged) {
    SwarmServiceCreator serviceCreator = createSwarmServiceCreator(appEngineProject);
    try {
      if (appEngineWebXmlChanged) {
        /*
         * This project is associated with another project that is consuming its generated client
         * libraries, so let's re-generate them.
         */
        if (ConnectedProjectHandler.getConnectedProject(appEngineProject) != null) {
          serviceCreator.create(true, new NullProgressMonitor());
          return;
        }
      }

      // Otherwise, just update the services, but don't regenerate the libraries
      serviceCreator.create(false, new NullProgressMonitor());
    } catch (Exception e) {
      AppEngineSwarmPlugin.log(e);
    }
  }

  /**
   * @return configured {@link SwarmServiceCreator} instance.
   */
  private SwarmServiceCreator createSwarmServiceCreator(IProject appEngineProject) {
    SwarmServiceCreator serviceCreator = new SwarmServiceCreator();
    GaeProject gaeProject = GaeProject.create(appEngineProject);
    serviceCreator.setAppId(gaeProject.getAppId());
    serviceCreator.setProject(appEngineProject);
    serviceCreator.setGaeSdkPath(gaeProject.getSdk().getInstallationPath());
    return serviceCreator;
  }
}
