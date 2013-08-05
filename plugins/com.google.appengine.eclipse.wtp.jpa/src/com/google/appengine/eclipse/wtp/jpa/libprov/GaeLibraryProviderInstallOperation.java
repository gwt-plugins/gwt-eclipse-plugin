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
package com.google.appengine.eclipse.wtp.jpa.libprov;

import com.google.appengine.eclipse.wtp.utils.ProjectUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperation;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperationConfig;

/**
 * Operation installing {@link ILibraryProvider} for Google App Engine.
 */
public final class GaeLibraryProviderInstallOperation extends LibraryProviderOperation {
  @Override
  public void execute(final LibraryProviderOperationConfig config, IProgressMonitor monitor)
      throws CoreException {
    // add datanucleus container (using workspace root job since the project is not fully
    // initialized yet).
    final IProject project = config.getFacetedProject().getProject();
    WorkspaceJob job = new WorkspaceJob("Setting up Datanucleus classpath container") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        ProjectUtils.addWebAppDependencyContainer(project, config.getProjectFacetVersion(),
            DatanucleusClasspathContainer.CONTAINER_PATH);
        return Status.OK_STATUS;
      }
    };
    job.setPriority(Job.SHORT);
    job.setRule(ResourcesPlugin.getWorkspace().getRoot());
    job.schedule();
  }
}
