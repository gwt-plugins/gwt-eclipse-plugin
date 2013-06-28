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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Removes Google App Engine related data from project.
 */
public final class GaeEarFacetUninstallDelegate implements IDelegate {
  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config,
      IProgressMonitor monitor) throws CoreException {
    // remove "appengine-application.xml" descriptor
    IVirtualComponent component = ComponentCore.createComponent(project, false);
    IVirtualFolder earroot = component.getRootFolder();
    IFile appEngineXmlFile = earroot.getUnderlyingFolder().getFile(
        new Path("META-INF/appengine-application.xml"));
    if (appEngineXmlFile.exists()) {
      appEngineXmlFile.delete(true, true, monitor);
    }
  }
}
