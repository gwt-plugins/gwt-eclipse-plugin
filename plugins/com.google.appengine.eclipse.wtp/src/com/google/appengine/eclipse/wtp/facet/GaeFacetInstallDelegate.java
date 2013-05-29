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

import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.building.ProjectChangeNotifier;
import com.google.appengine.eclipse.wtp.facet.ops.AppEngineXmlCreateOperation;
import com.google.appengine.eclipse.wtp.facet.ops.GaeFileCreateOperation;
import com.google.appengine.eclipse.wtp.facet.ops.SampleServletCreateOperation;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.StringUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Google App Engine Facet delegate for INSTALL action.
 */
public final class GaeFacetInstallDelegate implements IDelegate {

  @Override
  public void execute(IProject project, IProjectFacetVersion fv, Object config,
      IProgressMonitor monitor) throws CoreException {
    IDataModel model = (IDataModel) config;
    List<IDataModelOperation> operations = Lists.newArrayList();
    // add custom builders
    BuilderUtilities.addBuilderToProject(project, ProjectChangeNotifier.BUILDER_ID);
    BuilderUtilities.addBuilderToProject(project, AppEnginePlugin.PLUGIN_ID + ".projectValidator");
    // do create project contents
    if (JavaEEProjectUtilities.isDynamicWebProject(project)) {
      // add "appengine-web.xml" file
      operations.add(new AppEngineXmlCreateOperation(model));
      // add "logging.properties"
      IPath sdkLocation = getSdkLocation(model);
      if (sdkLocation != null) {
        final File loggingPropertiesFile = sdkLocation.append("config/user/logging.properties").toFile();
        if (loggingPropertiesFile.exists()) {
          operations.add(new GaeFileCreateOperation(model, new Path("WEB-INF/logging.properties")) {
            @Override
            protected InputStream getResourceContentsAsStream() throws CoreException {
              try {
                return new FileInputStream(loggingPropertiesFile);
              } catch (FileNotFoundException e) {
                throw new CoreException(
                    StatusUtilities.newErrorStatus(e, AppEnginePlugin.PLUGIN_ID));
              }
            }
          });
        }
      }
      boolean generateSample = model.getBooleanProperty(IGaeFacetConstants.GAE_PROPERTY_CREATE_SAMPLE);
      if (generateSample) {
        // add "favicon.ico"
        operations.add(new GaeFileCreateOperation(model, new Path("favicon.ico")) {
          @Override
          protected InputStream getResourceContentsAsStream() throws CoreException {
            return GaeProjectResources.createFavicon();
          }
        });
        // add sample servlet
        final String projectName = project.getName();
        String servletClassName = generateServletClassName(projectName);
        final String servletPath = generateServletPath(projectName);
        operations.add(new SampleServletCreateOperation(model, servletClassName, servletPath));
        // add index.html
        operations.add(new GaeFileCreateOperation(model, new Path("index.html")) {
          @Override
          protected InputStream getResourceContentsAsStream() throws CoreException {
            String servletName = generateServletName(projectName);
            return GaeProjectResources.createWelcomePageSource(servletName, servletPath);
          }
        });
      }
    }
    try {
      for (IDataModelOperation operation : operations) {
        IStatus status = operation.execute(monitor, null);
        if (status != null && !status.isOK()) {
          throw new CoreException(status);
        }
      }
    } catch (ExecutionException e) {
      // TODO: perform undo?
      AppEnginePlugin.logMessage(e);
    }
  }

  /**
   * Generates servlet class name basing on project name.
   */
  private String generateServletClassName(String projectName) {
    return generateServletName(projectName) + "Servlet";
  }

  /**
   * Generates servlet class name basing on project name.
   */
  private String generateServletName(String projectName) {
    return StringUtilities.capitalize(sanitizeProjectName(projectName));
  }

  /**
   * Generates path basing on project name.
   */
  private String generateServletPath(String projectName) {
    return generateServletName(projectName).toLowerCase();
  }

  /**
   * Searches runtime components and retrieves runtime location (GAE SDK location). Returns
   * <code>null</code>, if cannot be found.
   */
  private IPath getSdkLocation(IDataModel model) {
    IFacetedProjectWorkingCopy fpwc = (IFacetedProjectWorkingCopy) model.getProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
    IRuntime primaryRuntime = fpwc.getPrimaryRuntime();
    for (IRuntimeComponent component : primaryRuntime.getRuntimeComponents()) {
      IRuntimeComponentType type = component.getRuntimeComponentType();
      if (GaeRuntime.GAE_RUNTIME_ID.equals(type.getId())) {
        String location = component.getProperty("location");
        if (location == null) {
          return null;
        }
        return new Path(location);
      }
    }
    return null;
  }

  /**
   * Does some project name validations. Borrowed from WebAppProjectCreator.
   */
  private String sanitizeProjectName(String projectName) {
    assert projectName != null && projectName.length() > 0;
    String sanitized = null;
    // Replace first character if it's invalid
    char firstChar = projectName.charAt(0);
    if (Character.isJavaIdentifierStart(firstChar)) {
      sanitized = String.valueOf(firstChar);
    } else {
      sanitized = "_";
    }
    // Replace remaining invalid characters
    for (int i = 1; i < projectName.length(); i++) {
      char ch = projectName.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        sanitized += String.valueOf(ch);
      } else {
        sanitized += "_";
      }
    }
    return sanitized;
  }
}
