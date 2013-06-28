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
package com.google.appengine.eclipse.wtp.utils;

import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Utility methods to work with {@link IProject} representing GAE WTP project.
 */
@SuppressWarnings("restriction")
public final class ProjectUtils {
  /**
   * @return IFile representing "appengine-application.xml" file or <code>null</code> if file cannot
   *         be found.
   */
  public static IFile getAppEngineApplicationXml(IProject project) throws CoreException {
    if (!JavaEEProjectUtilities.isEARProject(project)) {
      return null;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null && component.exists()) {
      IPath path = component.getRootFolder().getWorkspaceRelativePath();
      if (project.getFullPath().isPrefixOf(path)) {
        IPath earContentFolder = path.removeFirstSegments(project.getFullPath().segmentCount());
        if (earContentFolder != null) {
          IFile file = project.getFile(earContentFolder.append(J2EEConstants.META_INF).append(
              "appengine-application.xml"));
          return file.exists() ? file : null;
        }
      }
    }
    return null;
  }

  /**
   * @return IFile representing "appengine-web.xml" file or <code>null</code> if file cannot be
   *         found.
   */
  public static IFile getAppEngineWebXml(IProject project) throws CoreException {
    if (!DynamicWebProjectUtilities.isDynamicWebProject(project)) {
      return null;
    }
    IPath webContentFolder = DynamicWebProjectUtilities.getWebContentFolder(project);
    if (webContentFolder != null) {
      IFile file = project.getFile(webContentFolder.append(J2EEConstants.WEB_INF).append(
          "appengine-web.xml"));
      return file.exists() ? file : null;
    }
    return null;
  }

  /**
   * Reads and returns AppID associated with given project or <code>null</code> if not set.
   */
  public static String getAppId(IProject project) throws CoreException {
    IFile ddFile = JavaEEProjectUtilities.isEARProject(project)
        ? getAppEngineApplicationXml(project) : getAppEngineWebXml(project);
    if (ddFile == null) {
      return null;
    }
    return GaeProject.getAppId(ddFile);
  }

  /**
   * Reads and returns App Version associated with given project or <code>null</code> if not set.
   */
  public static String getAppVersion(IProject project) throws CoreException {
    IFile appEngineXmlFile = getAppEngineWebXml(project);
    if (appEngineXmlFile == null) {
      return null;
    }
    return GaeProject.getAppVersion(appEngineXmlFile);
  }

  /**
   * @return {@link GaeSdk} location if given project is faceted project and runs against
   *         GaeRuntime. Otherwise returns <code>null</code>.
   */
  public static IPath getGaeSdkLocation(IProject project) throws CoreException {
    if (FacetedProjectFramework.isFacetedProject(project)) {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      IRuntime primaryRuntime = facetedProject.getPrimaryRuntime();
      if (primaryRuntime == null) {
        return null;
      }
      return getSdkPath(primaryRuntime);
    }
    return null;
  }

  /**
   * @return IProject instance associated with model.
   */
  public static IProject getProject(IDataModel model) {
    String projectName = model.getStringProperty(IFacetDataModelProperties.FACET_PROJECT_NAME);
    if (projectName != null && projectName.length() > 0) {
      return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    }
    return null;
  }

  /**
   * Searches for {@link GaeSdk} location.
   */
  public static IPath getSdkPath(IRuntime primaryRuntime) {
    for (IRuntimeComponent component : primaryRuntime.getRuntimeComponents()) {
      IRuntimeComponentType type = component.getRuntimeComponentType();
      if (GaeRuntime.GAE_RUNTIME_ID.equals(type.getId())) {
        String location = component.getProperty("location");
        return location == null ? null : new Path(location);
      }
    }
    return null;
  }

  /**
   * @return <code>true</code> if this project is faceted project and has AppEngine facet installed.
   */
  public static boolean isGaeProject(IJavaProject javaProject) throws CoreException {
    return isGaeProject(javaProject.getProject());
  }

  /**
   * @return <code>true</code> if this project is faceted project and has AppEngine facet installed.
   */
  public static boolean isGaeProject(IProject project) throws CoreException {
    if (FacetedProjectFramework.isFacetedProject(project)) {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      IProjectFacet gaeFacet = ProjectFacetsManager.getProjectFacet(IGaeFacetConstants.GAE_FACET_ID);
      return facetedProject.hasProjectFacet(gaeFacet);
    }
    return false;
  }

  /**
   * Sets given App ID into deployment descriptor file (appengine-web.xml or
   * appengine-application.xml)
   */
  public static void setAppId(IProject project, final String appId, boolean forceSave)
      throws IOException, CoreException {
    boolean isEar = JavaEEProjectUtilities.isEARProject(project);
    IFile ddFile = isEar ? getAppEngineApplicationXml(project) : getAppEngineWebXml(project);
    if (ddFile == null) {
      throw new FileNotFoundException("Could not find "
          + (isEar ? "appengien-application.xml" : "appengine-web.xml") + " in project "
          + project.getName());
    }
    GaeProject.setAppId(ddFile, appId, forceSave);
  }

  /**
   * Sets given App Version into appengine-web.xml
   */
  public static void setAppVersion(IProject project, final String appId, boolean forceSave)
      throws IOException, CoreException {
    IFile appEngineWebXml = getAppEngineWebXml(project);
    if (appEngineWebXml == null) {
      throw new FileNotFoundException("Could not find appengine-web.xml in project "
          + project.getName());
    }
    GaeProject.setAppVersion(appEngineWebXml, appId, forceSave);
  }

  /**
   * Non-instantiable.
   */
  private ProjectUtils() {
  }
}
