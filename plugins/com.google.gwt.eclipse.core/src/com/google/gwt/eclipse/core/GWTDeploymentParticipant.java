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
package com.google.gwt.eclipse.core;

import com.google.gdt.eclipse.core.console.TerminateProcessAction;
import com.google.gdt.eclipse.core.deploy.DeploymentParticipant;
import com.google.gdt.eclipse.core.deploy.DeploymentSet;
import com.google.gdt.eclipse.core.projects.ProjectChangeTimestampTracker;
import com.google.gwt.eclipse.core.compile.GWTCompileRunner;
import com.google.gwt.eclipse.core.compile.GWTCompileSettings;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.properties.GWTProjectProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Performs a GWT compile before deployment.
 */
public class GWTDeploymentParticipant implements DeploymentParticipant {

  private static final QualifiedName PREVIOUS_DEPLOYMENT_BUILD_PROJECT_CHANGE_STAMP_KEY = new QualifiedName(
      GWTDeploymentParticipant.class.getName(),
      "previousDeploymentBuildProjectChangeStamp");

  public void deploySucceeded(IJavaProject javaProject,
      DeploymentSet deploymentSet, IPath warLocation,
      OutputStream consoleOutputStream, IProgressMonitor monitor) {
    // ignored
  }

  public void predeploy(IJavaProject javaProject, DeploymentSet deploymentSet,
      IPath warLocation, OutputStream consoleOutputStream,
      IProgressMonitor monitor) throws CoreException {
    if (!javaProject.getProject().hasNature(GWTNature.NATURE_ID)) {
      // Only compile if the project has the GWT nature
      return;
    }

    try {
      IProject project = javaProject.getProject();
      long curStamp = ProjectChangeTimestampTracker.getProjectTimestamp(project);
      if (curStamp != ProjectChangeTimestampTracker.getTimestampFromKey(
          project, PREVIOUS_DEPLOYMENT_BUILD_PROJECT_CHANGE_STAMP_KEY)) {
        GWTCompileSettings settings = GWTProjectProperties.getGwtCompileSettings(project);
        settings.setEntryPointModules(GWTProjectProperties.getEntryPointModules(project));

        // Watch the monitor for cancellations, and destroy the process
        TerminateProcessAction processTerminator = new TerminateProcessAction();
        GWTCompileRunner.compileWithCancellationSupport(javaProject,
            warLocation, settings, consoleOutputStream, processTerminator,
            monitor, processTerminator);
      } else {
        PrintWriter printWriter = new PrintWriter(consoleOutputStream);
        printWriter.println("Skipping GWT compilation since no relevant changes have occurred since the last deploy.");
        printWriter.flush();
      }

      project.setPersistentProperty(
          PREVIOUS_DEPLOYMENT_BUILD_PROJECT_CHANGE_STAMP_KEY,
          String.valueOf(curStamp));

    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    } catch (InterruptedException e) {
      throw new CoreException(new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          e.getLocalizedMessage(), e));
    }
  }
}
