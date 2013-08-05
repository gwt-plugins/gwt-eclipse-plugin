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

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.wtp.facet.IGaeFacetConstants;
import com.google.appengine.eclipse.wtp.runtime.GaeRuntime;
import com.google.gdt.eclipse.core.DynamicWebProjectUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.core.ClasspathHelper;
import org.eclipse.jst.j2ee.classpathdep.ClasspathDependencyUtil;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponent;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntimeComponentType;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

/**
 * Utility methods to work with {@link IProject} representing GAE WTP project.
 */
@SuppressWarnings("restriction")
public final class ProjectUtils {
  /**
   * Creates and adds into the project a new classpath container which is modified to be deployment
   * dependency.
   */
  public static void addWebAppDependencyContainer(IProject project, IProjectFacetVersion fv,
      IPath containerPath) throws CoreException {
    IClasspathEntry containerEntry = JavaCore.newContainerEntry(containerPath);
    IVirtualComponent virtualComponent = ComponentCore.createComponent(project);
    IPath runtimePath = ClasspathDependencyUtil.getDefaultRuntimePath(virtualComponent,
        containerEntry);
    IClasspathEntry newEntry = ClasspathDependencyUtil.modifyDependencyPath(containerEntry,
        runtimePath);
    ClasspathHelper.addClasspathEntries(project, fv, Collections.singletonList(newEntry));
  }

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
   * Reads and returns AppID associated with given project or empty string if not set or DD file is
   * not found.
   */
  public static String getAppId(IProject project) throws CoreException {
    IFile ddFile = JavaEEProjectUtilities.isEARProject(project)
        ? getAppEngineApplicationXml(project) : getAppEngineWebXml(project);
    if (ddFile == null) {
      return "";
    }
    return getSingleElementValue(ddFile, "application");
  }

  /**
   * Reads and returns App Version associated with given project or empty string if not set or DD
   * file is not found.
   */
  public static String getAppVersion(IProject project) throws CoreException {
    IFile appEngineXmlFile = getAppEngineWebXml(project);
    if (appEngineXmlFile == null) {
      return "";
    }
    return getSingleElementValue(appEngineXmlFile, "version");
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
      return getGaeSdkLocation(primaryRuntime);
    }
    return null;
  }

  /**
   * Searches for {@link GaeSdk} location.
   */
  public static IPath getGaeSdkLocation(IRuntime primaryRuntime) {
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
   * Reads and returns Module ID associated with given project or empty string if not set or DD file
   * is not found.
   */
  public static String getModuleId(IProject project) throws CoreException {
    IFile appEngineXmlFile = getAppEngineWebXml(project);
    if (appEngineXmlFile == null) {
      return "";
    }
    return getSingleElementValue(appEngineXmlFile, "module");
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
   * Returns <code>true</code> if the project has the runtime which supports EAR, otherwise returns
   * <code>false</code>. If GAE SDK is not found, returns <code>false</code>.
   */
  public static boolean isEarSupported(IProject project) throws CoreException {
    IPath sdkLocation = ProjectUtils.getGaeSdkLocation(project);
    if (sdkLocation != null) {
      SdkSet<GaeSdk> sdks = GaePreferences.getSdkManager().getSdks();
      GaeSdk sdk = SdkUtils.findSdkForInstallationPath(sdks, sdkLocation);
      if (sdk != null) {
        return sdk.getCapabilities().contains(GaeSdkCapability.EAR);
      }
    }
    return false;
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
   * appengine-application.xml).
   */
  public static void setAppId(IProject project, final String appId, boolean forceSave)
      throws IOException, CoreException {
    setDeploymentDescriptorSingleValue(project, "application", appId, forceSave);
  }

  /**
   * Sets given App Version into appengine-web.xml.
   */
  public static void setAppVersion(IProject project, final String appVersion, boolean forceSave)
      throws IOException, CoreException {
    IFile appEngineWebXml = getAppEngineWebXml(project);
    if (appEngineWebXml == null) {
      throw new FileNotFoundException("Could not find appengine-web.xml in project "
          + project.getName());
    }
    setSingleElementValue(appEngineWebXml, "version", appVersion, forceSave);
  }

  /**
   * Sets given Module ID into deployment appengine-web.xml.
   */
  public static void setModuleId(IProject project, final String moduleId, boolean forceSave)
      throws IOException, CoreException {
    IFile appEngineWebXml = getAppEngineWebXml(project);
    if (appEngineWebXml == null) {
      throw new FileNotFoundException("Could not find appengine-web.xml in project "
          + project.getName());
    }
    setSingleElementValue(appEngineWebXml, "module", moduleId, forceSave);
  }

  /**
   * Reads a value of tag which should be single document element.
   */
  private static String getSingleElementValue(IFile ddXml, final String tagName) {
    final String[] textHolder = new String[] {""};
    try {
      new XmlUtilities.ReadOperation(ddXml) {
        @Override
        protected void read(IDOMDocument document) {
          NodeList nodes = document.getDocumentElement().getElementsByTagName(tagName);
          if (nodes.getLength() == 1) {
            textHolder[0] = XmlUtilities.getElementText((Element) nodes.item(0));
          }
        }
      }.run();
      return textHolder[0];
    } catch (Throwable e) {
      // ignore errors
      return "";
    }
  }

  /**
   * Searches for appropriate deployment descriptor and set a value for given tag.
   */
  private static void setDeploymentDescriptorSingleValue(IProject project, String tagName,
      String tagValue, boolean forceSave) throws CoreException, FileNotFoundException, IOException {
    boolean isEar = JavaEEProjectUtilities.isEARProject(project);
    IFile ddFile = isEar ? getAppEngineApplicationXml(project) : getAppEngineWebXml(project);
    if (ddFile == null) {
      throw new FileNotFoundException("Could not find "
          + (isEar ? "appengien-application.xml" : "appengine-web.xml") + " in project "
          + project.getName());
    }
    setSingleElementValue(ddFile, tagName, tagValue, forceSave);
  }

  /**
   * Set a value for a tag which should be single document element. Attempts to create element if
   * none found.
   */
  private static void setSingleElementValue(IFile ddXml, final String tagName,
      final String tagValue, boolean forceSave) throws IOException, CoreException {
    new XmlUtilities.EditOperation(ddXml) {
      @Override
      protected void edit(IDOMDocument document) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
          Element element = document.createElement(tagName);
          document.getDocumentElement().appendChild(element);
          XmlUtilities.setElementText(document, element, tagValue);
        } else if (nodes.getLength() == 1) {
          XmlUtilities.setElementText(document, (Element) nodes.item(0), tagValue);
        }
      }
    }.run(forceSave);
  }

  /**
   * Non-instantiable.
   */
  private ProjectUtils() {
  }
}
