/*******************************************************************************
 * Copyright 2009 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.update.usage;

import com.google.gdt.eclipse.core.deploy.DeploymentParticipant;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import java.io.OutputStream;

/**
 * Deployment participant to track Gae deployment usage statistics.
 */
public class GaeUsageTrackingDeploymentParticipant implements DeploymentParticipant {
  @Override
  public void deploySucceeded(IJavaProject javaProject, DeploymentSet deploymentSet,
      IPath warLocation, OutputStream consoleOutputStream, IProgressMonitor monitor) {

    if (deploymentSet.getDeployFrontend()) {
      GdtExtPlugin.getAnalyticsPingManager().sendGaeDeployPing(javaProject.getProject());
    }

    if (deploymentSet.getBackendNames().size() > 0) {
      GdtExtPlugin.getAnalyticsPingManager().sendGaeBackendDeployPing(javaProject.getProject());
    }
  }

  @Override
  public void predeploy(IJavaProject javaProject, DeploymentSet deploymentSet, IPath warLocation,
      OutputStream consoleOutputStream, IProgressMonitor monitor) {
    // ignored
  }
}
