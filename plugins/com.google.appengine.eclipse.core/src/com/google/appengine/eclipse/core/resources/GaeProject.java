/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.core.resources;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GAE-natured project.
 */
@SuppressWarnings("restriction")
public class GaeProject {

  private class ReadAppEngineXmlOperation extends XmlUtilities.ReadOperation {

    private final String elementTagName;

    private String elementText = "";

    public ReadAppEngineXmlOperation(String elementTagName) {
      super(getAppEngineWebXml());
      this.elementTagName = elementTagName;
    }

    public String getElementText() {
      return elementText;
    }

    @Override
    protected void read(IDOMDocument document) {
      if (this.getXmlFile() != null) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(
            elementTagName);
        if (nodes.getLength() == 1) {
          elementText = XmlUtilities.getElementText((Element) nodes.item(0));
        }
      }
    }
  }

  private class ReadBackendsXmlOperation extends XmlUtilities.ReadOperation {

    private static final String BACKENDS_TAG_NAME = "backend";
    private static final String BACKEND_NAME_ATTR_NAME = "name";

    private List<String> backendNames;

    public ReadBackendsXmlOperation() {
      super(getBackendsXml());
      backendNames = new ArrayList<String>(4);
    }

    public List<String> getBackendNames() {
      return backendNames;
    }

    @Override
    protected void read(IDOMDocument document) {
      if (this.getXmlFile() != null) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(
            BACKENDS_TAG_NAME);
        for (int i = 0; i < nodes.getLength(); i++) {
          Node n = nodes.item(i);
          Node backendName = n.getAttributes().getNamedItem(
              BACKEND_NAME_ATTR_NAME);
          if (backendName != null) {
            backendNames.add(backendName.getNodeValue());
          }
        }
      }
    }
  }

  /**
   * Returns the GaeProject for the specified IProject.
   * 
   * @param project the project
   * @return a GaeProject instance, or <code>null</code> if the project does not
   *         have the GAE nature
   */
  public static GaeProject create(IProject project) {
    assert (project != null);
    if (GaeNature.isGaeProject(project)) {
      return new GaeProject(JavaCore.create(project));
    }
    return null;
  }

  private final IJavaProject javaProject;

  protected GaeProject(IJavaProject project) {
    this.javaProject = project;
  }

  public IFile getAppEngineWebXml() {
    return getXmlFile("appengine-web.xml");
  }

  public String getAppId() {
    if (getAppEngineWebXml() == null) {
      return "";
    }

    try {
      ReadAppEngineXmlOperation op = new ReadAppEngineXmlOperation(
          "application");
      op.run();
      return op.getElementText();
    } catch (Exception e) {
      AppEngineCorePluginLog.logError(e);
      return "";
    }
  }

  public String getAppVersion() {
    if (getAppEngineWebXml() == null) {
      return "";
    }

    try {
      ReadAppEngineXmlOperation op = new ReadAppEngineXmlOperation("version");
      op.run();
      return op.getElementText();
    } catch (Exception e) {
      AppEngineCorePluginLog.logError(e);
      return "";
    }
  }

  public List<String> getBackendNames() {
    if (getBackendsXml() == null) {
      return Collections.emptyList();
    }

    try {
      ReadBackendsXmlOperation op = new ReadBackendsXmlOperation();
      op.run();
      return op.getBackendNames();
    } catch (Exception e) {
      AppEngineCorePluginLog.logError(e);
      return Collections.emptyList();
    }
  }

  public IFile getBackendsXml() {
    return getXmlFile("backends.xml");
  }

  public IStatus getDeployableStatus() {
    try {
      if (!WebAppUtilities.isWebApp(getProject())) {
        return StatusUtilities.newErrorStatus(
            "The project {0} is not configured as a web application (set in Project Properties > Google > Web Application)",
            AppEngineCorePlugin.PLUGIN_ID);
      }

      if (IMarker.SEVERITY_ERROR == getProject().findMaxProblemSeverity(
          IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
        Object[] args = {getProject().getName()};
        return StatusUtilities.newWarningStatus("The project {0} has errors.",
            AppEngineCorePlugin.PLUGIN_ID, args);
      }

      // Everything looks ok
      return StatusUtilities.OK_STATUS;

    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
      return StatusUtilities.newErrorStatus(e.getMessage(),
          AppEngineCorePlugin.PLUGIN_ID);
    }
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  public String getName() {
    return getProject().getName();
  }

  public IProject getProject() {
    return javaProject.getProject();
  }

  public GaeSdk getSdk() {
    try {
      return GaeSdk.findSdkFor(javaProject);

    } catch (JavaModelException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return null;
  }

  public void setAppId(String appId) throws IOException, CoreException {
    setAppId(appId, false);
  }

  public void setAppId(final String appId, boolean forceSave)
      throws IOException, CoreException {
    IFile appEngineWebXml = getAppEngineWebXml();
    if (appEngineWebXml == null) {
      throw new FileNotFoundException(
          "Could not find appengine-web.xml in project " + getName());
    }

    new XmlUtilities.EditOperation(appEngineWebXml) {
      @Override
      protected void edit(IDOMDocument document) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(
            "application");
        if (nodes.getLength() == 1) {
          XmlUtilities.setElementText(document, (Element) nodes.item(0), appId);
        }
      }
    }.run(forceSave);
  }

  public void setAppVersion(String version) throws IOException, CoreException {
    setAppVersion(version, false);
  }

  public void setAppVersion(final String version, boolean forceSave)
      throws IOException, CoreException {
    IFile appEngineWebXml = getAppEngineWebXml();
    if (appEngineWebXml == null) {
      throw new FileNotFoundException(
          "Could not find appengine-web.xml in project " + getName());
    }

    new XmlUtilities.EditOperation(appEngineWebXml) {
      @Override
      protected void edit(IDOMDocument document) {
        NodeList nodes = document.getDocumentElement().getElementsByTagName(
            "version");
        if (nodes.getLength() == 1) {
          XmlUtilities.setElementText(document, (Element) nodes.item(0),
              version);
        }
      }
    }.run(forceSave);
  }

  private IFile getXmlFile(String name) {
    try {
      IProject project = javaProject.getProject();
      WebAppUtilities.verifyIsWebApp(project);

      IFolder webInfFolder = WebAppUtilities.getWebInfSrc(project);
      IFile xmlFile = webInfFolder.getFile(name);
      if (xmlFile.exists()) {
        return xmlFile;
      }

    } catch (CoreException e) {
      AppEngineCorePluginLog.logError(e);
    }

    return null;
  }

}
