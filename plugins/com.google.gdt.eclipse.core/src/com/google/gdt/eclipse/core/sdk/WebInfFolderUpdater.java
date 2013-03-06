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

import com.google.gdt.eclipse.core.CorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.java.ClasspathChangedListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;

/**
 * Handles updating the WEB-INF/lib folder when the classpath changes.
 */
public abstract class WebInfFolderUpdater {

  private static final String UPDATING_SERVER_LIBRARIES_MESSAGE = "Updating %1$s with jars from %2$s";

  private final ClasspathChangedListener classpathChangedListener = new ClasspathChangedListener() {
    @Override
    protected void classpathChanged(final IJavaProject javaProject) {
      IProject project = javaProject.getProject();
      if (WebAppUtilities.hasManagedWarOut(project)) {
        final Sdk sdk = getSdk(javaProject);
        if (sdk == null) {
          // Nothing to do
          return;
        }

        String jobName = String.format(UPDATING_SERVER_LIBRARIES_MESSAGE,
            WebAppUtilities.getWebInfLib(project).getFullPath().toString(),
            sdk.getDescription());
        Job job = new WorkspaceJob(jobName) {
          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor)
              throws CoreException {
            try {
              getUpdateCommand(javaProject, sdk).execute();
              return Status.OK_STATUS;
            } catch (FileNotFoundException e) {
              return StatusUtilities.newErrorStatus(e, CorePlugin.PLUGIN_ID);
            } catch (BackingStoreException e) {
              return StatusUtilities.newErrorStatus(e, CorePlugin.PLUGIN_ID);
            }
          }
        };

        // Lock on the project
        job.setRule(project);
        job.schedule();
      }
    }
  };

  /**
   * Starts listening for changes to update the WEB-INF/lib folder.
   */
  public void start() {
    JavaCore.addElementChangedListener(classpathChangedListener);
  }

  /**
   * Stops listening for changes.
   */
  public void stop() {
    JavaCore.removeElementChangedListener(classpathChangedListener);
  }

  /**
   * @return an applicable {@link Sdk} for the project, or null
   */
  protected abstract Sdk getSdk(IJavaProject javaProject);

  /**
   * @param sdk an {@link Sdk} returned by {@link #getSdk(IJavaProject)}
   */
  protected abstract UpdateWebInfFolderCommand getUpdateCommand(
      IJavaProject javaProject, Sdk sdk);
}
