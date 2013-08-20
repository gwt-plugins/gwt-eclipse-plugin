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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.ClasspathHelper;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperation;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperationConfig;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import java.util.Collections;

/**
 * Operation installing {@link ILibraryProvider} for Google App Engine.
 */
public final class GaeLibraryProviderInstallOperation extends LibraryProviderOperation {
  @Override
  public void execute(final LibraryProviderOperationConfig config, IProgressMonitor monitor)
      throws CoreException {
    // add datanucleus container
    final IProject project = config.getFacetedProject().getProject();
    final IClasspathEntry containerEntry = JavaCore.newContainerEntry(DatanucleusClasspathContainer.CONTAINER_PATH);
    final IProjectFacetVersion fv = config.getProjectFacetVersion();
    ClasspathHelper.addClasspathEntries(project, fv, Collections.singletonList(containerEntry));
    // This container must be included into web dependencies (see project properties->Deployment
    // Assembly), but faceted project is not fully initialized at this moment, which leads to
    // incorrect deployment target dir set (should be WEB-INF/lib). The workaround is to add the
    // container to dependencies later using workspace job (which runs after project creation is
    // done).
    WorkspaceJob job = new WorkspaceJob("Setting up Datanucleus classpath container") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IVirtualComponent virtualComponent = ComponentCore.createComponent(project);
        IPath runtimePath = ClasspathDependencyUtil.getDefaultRuntimePath(virtualComponent,
            containerEntry);
        ClasspathHelper.removeClasspathEntries(project, fv);
        IClasspathEntry newEntry = ClasspathDependencyUtil.modifyDependencyPath(containerEntry,
            runtimePath);
        ClasspathHelper.addClasspathEntries(project, fv, Collections.singletonList(newEntry));
        return Status.OK_STATUS;
      }
    };

    job.setPriority(Job.SHORT);
    job.setRule(ResourcesPlugin.getWorkspace().getRoot());
    job.schedule();
  }
}
