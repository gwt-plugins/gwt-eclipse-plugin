/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.facet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gwt.eclipse.core.properties.ui.GWTProjectPropertyPage;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;

public final class GwtFacetUninstallDelegate implements IDelegate {

  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config, IProgressMonitor monitor)
      throws CoreException {
    // Deprecated: make a project configurator in charge
    //removeFacet();
  }

  private void removeFacet(IProject project) throws CoreException {
    GWTProjectPropertyPage projectProperty = new GWTProjectPropertyPage();
    try {
      projectProperty.removeGWTSdkForFacet(project);
    } catch (BackingStoreException e) {
      GwtWtpPlugin.logError("Could not uinstall the GWT facet.", e);
    }
  }

}
