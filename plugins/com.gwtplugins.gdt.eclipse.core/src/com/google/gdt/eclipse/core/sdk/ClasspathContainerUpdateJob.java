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
package com.google.gdt.eclipse.core.sdk;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.LaunchingMessages;

/**
 * Job used to update any {@link IClasspathEntry#CPE_CONTAINER} entries whose
 * comparison ID matches the expected comparison ID.
 */
@SuppressWarnings("restriction")
public class ClasspathContainerUpdateJob extends Job {
  private final String expectedComparisonId;
  private final ClasspathContainerInitializer classpathContainerInitializer;

  public ClasspathContainerUpdateJob(String jobName, String expectedContainerId) {
    super(jobName);
    assert (expectedContainerId != null);
    assert (expectedContainerId.trim().length() != 0);

    this.expectedComparisonId = expectedContainerId;
    classpathContainerInitializer = JavaCore.getClasspathContainerInitializer(expectedContainerId);
    assert (classpathContainerInitializer != null);
  }

  @Override
  protected IStatus run(IProgressMonitor jobMonitor) {
    try {
      IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
        public void run(IProgressMonitor runnableMonitor) throws CoreException {
          IJavaProject[] projects = JavaCore.create(
              ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
          runnableMonitor.beginTask(LaunchingMessages.LaunchingPlugin_0,
              projects.length + 1);
          rebindContainers(runnableMonitor, projects);
          runnableMonitor.done();
        }

        /**
         * Rebind all of the classpath containers whose comparison ID matches
         * the expected ID.
         */
        private void rebindContainers(IProgressMonitor runnableMonitor,
            IJavaProject[] projects) throws CoreException {
          for (IJavaProject project : projects) {
            // Update the progress monitor
            runnableMonitor.worked(1);
            IClasspathEntry[] rawClasspathEntries = project.getRawClasspath();
            for (IClasspathEntry rawClasspathEntry : rawClasspathEntries) {
              if (rawClasspathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
                continue;
              }

              IPath path = rawClasspathEntry.getPath();
              if (path == null || path.segmentCount() == 0) {
                continue;
              }

              Object actualComparisonId = classpathContainerInitializer.getComparisonID(
                  path, project);
              if (!actualComparisonId.equals(expectedComparisonId)) {
                continue;
              }

              classpathContainerInitializer.initialize(path, project);
            }
          }
        }
      };
      JavaCore.run(runnable, null, jobMonitor);
      return Status.OK_STATUS;
    } catch (CoreException e) {
      return e.getStatus();
    }
  }
}
