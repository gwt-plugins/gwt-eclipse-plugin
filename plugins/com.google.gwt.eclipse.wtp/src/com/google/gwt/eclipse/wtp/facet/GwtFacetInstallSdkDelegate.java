/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.wtp.facet;

import com.google.gwt.eclipse.core.properties.ui.GWTProjectPropertyPage;
import com.google.gwt.eclipse.core.runtime.GWTJarsRuntime;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public final class GwtFacetInstallSdkDelegate implements IDelegate, IGwtFacetConstants {

  private IProject project;
  private GWTJarsRuntime runtime;

  @Override
  public void execute(final IProject project, final IProjectFacetVersion version,
      final Object config, final IProgressMonitor monitor) throws CoreException {
    this.project = project;

    IDataModel dataModel = (IDataModel) config;

    runtime = (GWTJarsRuntime) dataModel.getProperty(GWT_SDK);

    GwtWtpPlugin.logMessage("GwtFacetInstallSdkDelegate: selected runtime version="
        + runtime.getVersion());

    // Standard project GWT facet install
    if (!isMavenProject(dataModel)) {
      String message = "GwtFacetInstallSdkDelegate: installing standard classpath container.";
      GwtWtpPlugin.logMessage(message);
      installGwtFacet();
    } else {
      String message =
          "GwtFacetInstallSdkDelegate: Maven detected, skipping standard GWT classpath container.";
      GwtWtpPlugin.logMessage(message);
    }
  }

  /**
   * Configure a standard GWT classpath container for Facet.
   */
  private void installGwtFacet() {

    GWTProjectPropertyPage projectProperty = new GWTProjectPropertyPage();


    // TODO extract to this class, having problems with exporting/importing UpdateType?
    projectProperty.createGwtSdkContainerForFacet(project, runtime);


  }

  private static boolean isMavenProject(IDataModel model) {
    if (!model.isProperty(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME)) {
      return false;
    }
    if (!model.isPropertySet(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME)) {
      return false;
    }
    return model.getBooleanProperty(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME);
  }

}
