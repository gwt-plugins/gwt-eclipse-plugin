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
package com.google.gdt.eclipse.suite.update.builders;

import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

/**
 * A compilation participant that is used to trigger an update check of the GWT Plugin's feature
 * whenever a Java build is triggered on a GWT project.
 */
public class UpdateTriggerCompilationParticipant extends CompilationParticipant {

  @Override
  public boolean isActive(IJavaProject project) {
    if (!project.exists()) {
      return false;
    }

    // Only run when preferences are set to ok, and it's a gwt project
    if (GdtPreferences.getCaptureAnalytics() && GWTNature.isGWTProject(project.getProject())) {
      runProcesses();
      return true;
    } else {
      return false;
    }
  }

  private void runProcesses() {
    checkUpdates();
    pingAnalytics();
  }

  private void checkUpdates() {
    Job job = new Job("Check Update") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          GdtExtPlugin.getFeatureUpdateManager().checkForUpdates();
        } catch (Exception e) {
          // No need to catch network issues.
        }
        System.out.println("check updates");
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  public void pingAnalytics() {
    Job job = new Job("Analytics Ping") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          GdtExtPlugin.getAnalyticsPingManager().sendCompilationPing();
        } catch (Exception e) {
          // No need to catch network issues
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

}
