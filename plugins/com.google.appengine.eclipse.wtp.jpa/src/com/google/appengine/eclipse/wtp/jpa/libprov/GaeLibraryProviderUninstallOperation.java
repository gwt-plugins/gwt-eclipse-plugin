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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.common.project.facet.core.ClasspathHelper;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperation;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderOperationConfig;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * This {@link LibraryProviderOperation} is for uninstalling library provider.
 * 
 * Called for example, for primary runtime changing.
 */
public final class GaeLibraryProviderUninstallOperation extends LibraryProviderOperation {
  @Override
  public void execute(LibraryProviderOperationConfig config, IProgressMonitor monitor)
      throws CoreException {
    IProject project = config.getFacetedProject().getProject();
    IProjectFacetVersion projectFacetVersion = config.getProjectFacetVersion();
    ClasspathHelper.removeClasspathEntries(project, projectFacetVersion);
  }
}
