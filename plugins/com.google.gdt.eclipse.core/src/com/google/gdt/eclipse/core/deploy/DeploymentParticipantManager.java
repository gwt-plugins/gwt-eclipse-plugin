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
package com.google.gdt.eclipse.core.deploy;

import com.google.gdt.eclipse.core.CorePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;

import java.io.OutputStream;

/**
 * Manages the set of {@link DeploymentParticipant}s.
 */
public class DeploymentParticipantManager {

  /**
   * The stages of the deployment process
   */
  public enum NotificationType {
    PREDEPLOY, SUCCEEDED
  }

  /**
   * Notifies all {@link DeploymentParticipant}s that the project is about to be
   * deployed or has succeeded in deploying.
   * 
   * TODO: This really belongs in the GAE plugin. We are placing it here until
   * we rationalize the potential cyclic dependency.
   * 
   * @param javaProject
   * @param monitor
   * @throws CoreException
   */
  public static void notifyAllParticipants(IJavaProject javaProject, DeploymentSet deploymentSet, IPath warLocation,
      OutputStream consoleOutputStream, IProgressMonitor monitor,
      NotificationType type) throws CoreException {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(
        CorePlugin.PLUGIN_ID, "deploymentParticipant");
    IExtension[] extensions = extensionPoint.getExtensions();
    for (IExtension extension : extensions) {
      IConfigurationElement[] configurationElements = extension.getConfigurationElements();
      for (IConfigurationElement configurationElement : configurationElements) {
        Object createExecutableExtension = configurationElement.createExecutableExtension("class");
        assert (createExecutableExtension instanceof DeploymentParticipant);
        DeploymentParticipant participant = (DeploymentParticipant) createExecutableExtension;

        switch (type) {
          case PREDEPLOY:
            participant.predeploy(javaProject, deploymentSet, warLocation, consoleOutputStream,
                monitor);
            break;
          case SUCCEEDED:
            participant.deploySucceeded(javaProject, deploymentSet, warLocation,
                consoleOutputStream, monitor);
            break;
        }
      }
    }
  }
}
