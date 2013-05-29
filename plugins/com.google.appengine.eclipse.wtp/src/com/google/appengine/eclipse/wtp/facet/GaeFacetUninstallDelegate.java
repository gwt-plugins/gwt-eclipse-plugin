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
package com.google.appengine.eclipse.wtp.facet;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.building.ProjectChangeNotifier;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Facet delegate for uninstalling GAE facet.
 */
public final class GaeFacetUninstallDelegate implements IDelegate {
  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config,
      IProgressMonitor monitor) throws CoreException {
    // remove notifying builder
    BuilderUtilities.removeBuilderFromProject(project, ProjectChangeNotifier.BUILDER_ID);
    BuilderUtilities.removeBuilderFromProject(project, AppEnginePlugin.PLUGIN_ID
        + ".projectValidator");
  }
}
