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
package com.google.gdt.eclipse.appsmarketplace.resources;

import com.google.api.client.http.GenericUrl;
import com.google.common.base.Strings;
import com.google.gdt.eclipse.appsmarketplace.AppsMarketplacePlugin;
import com.google.gdt.eclipse.appsmarketplace.properties.AppsMarketplaceProjectProperties;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Google Apps Marketplace project and related operations.
 */
@SuppressWarnings("restriction")
public class AppsMarketplaceProject {
  class NameEditOperation extends XmlUtilities.EditOperation {
    private String appName;

    public NameEditOperation(IFile xmlFile, String appName) {
      super(xmlFile);
      this.appName = appName;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gdt.eclipse.core.XmlUtilities.EditOperation#edit(org.eclipse
     * .wst.xml.core.internal.provisional.document.IDOMDocument)
     */
    @Override
    protected void edit(IDOMDocument document) {
      // <Name>Next Meeting App</Name>
      NodeList nameNodes = document.getDocumentElement().getElementsByTagName(
          "Name");
      updateName(document, nameNodes);
      // <Extension id="navLink" type="link">
      // <Name>hiranmoy_demo4</Name>
      // <Url>http://www.google.com/openid?hd=${DOMAIN_NAME}</Url>
      // <Scope ref="calendarAPI"/>
      NodeList extNodeList = document.getDocumentElement().getElementsByTagName(
          "Extension");
      for (int i = 0; i < extNodeList.getLength(); ++i) {
        Element extensionNode = (Element) extNodeList.item(i);
        String id = extensionNode.getAttribute("id");
        if ("navLink".equals(id)) {
          NodeList nameNodeList = extensionNode.getElementsByTagName("Name");
          updateName(document, nameNodeList);
        }
      }
    }

    private void updateName(IDOMDocument document, NodeList nameNodes) {
      for (int i = 0; i < nameNodes.getLength(); ++i) {
        Node nameNode = nameNodes.item(i);
        NodeList children = nameNode.getChildNodes();
        if (children.getLength() == 0) {
          nameNode.appendChild(document.createTextNode(appName));
        } else {
          for (int k = 0; k < children.getLength(); ++k) {
            Node nodeToChange = children.item(k);
            nodeToChange.setNodeValue(appName);
          }
        }
      }
    }
  }

  class UrlEditOperation extends XmlUtilities.EditOperation {
    private final String dummyDomain = "www.example.com";
    private final String dummyAppId = "10000";
    private GenericUrl newBaseAppUrl;

    public UrlEditOperation(IFile xmlFile, String newAppUrl) {
      super(xmlFile);
      this.newBaseAppUrl = createNewBaseUrl(newAppUrl);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gdt.eclipse.core.XmlUtilities.EditOperation#edit(org.eclipse
     * .wst.xml.core.internal.provisional.document.IDOMDocument)
     */
    @Override
    protected void edit(IDOMDocument document) {
      // <Extension id="realm"><Url></Url></Extension>
      NodeList extNodes = document.getDocumentElement().getElementsByTagName(
          "Extension");
      for (int i = 0; i < extNodes.getLength(); ++i) {
        Element extensionNode = (Element) extNodes.item(i);
        String id = extensionNode.getAttribute("id");
        if ("navLink".equals(id) || "realm".equals(id)) {
          NodeList urlNodes = extensionNode.getElementsByTagName("Url");
          updateUrl(document, urlNodes);
        }
      }
    }

    /**
     * Extracts the base url from newAppUrl. E.g: http://abc.com:1234/abc/ will
     * return http://abc.com:1234/
     */
    private GenericUrl createNewBaseUrl(String newAppUrl) {
      newAppUrl = replaceMarketplaceToken(newAppUrl);
      GenericUrl newParsedUrl = new GenericUrl(newAppUrl);
      GenericUrl newBaseUrl = new GenericUrl();
      // Copy only the base part of URL
      newBaseUrl.setScheme(newParsedUrl.getScheme());
      newBaseUrl.setHost(newParsedUrl.getHost());
      newBaseUrl.setPort(newParsedUrl.getPort());
      return newBaseUrl;
    }

    /**
     * Creates a new url by appending the new base url to the old url.
     */
    private String createNewUrlValue(String oldUrl) {
      GenericUrl oldParsedUrl;
      if (StringUtilities.isEmpty(oldUrl)) {
        oldParsedUrl = new GenericUrl();
      } else {
        oldUrl = replaceMarketplaceToken(oldUrl);
        try {
          oldParsedUrl = new GenericUrl(oldUrl);
        } catch (Exception e) {
          // Catch all Exceptions resulting from mal-formatted URLs.
          return "";
        }
      }
      oldParsedUrl.setScheme(newBaseAppUrl.getScheme());
      oldParsedUrl.setHost(newBaseAppUrl.getHost());
      oldParsedUrl.setPort(newBaseAppUrl.getPort());
      return placeMarketplaceToken(oldParsedUrl.toString());
    }

    /**
     * Replaces special tokens ${DOMAIN_NAME} and ${APP_ID} in url with dummy
     * strings. Url parsers cannot parse these tokens.
     */
    private String placeMarketplaceToken(String url) {
      return url.replace(dummyDomain, "${DOMAIN_NAME}").replace(
          dummyAppId, "${APP_ID}");
    }

    /**
     * Replaces dummy strings in url with special tokens ${DOMAIN_NAME} and
     * ${APP_ID}.
     */
    private String replaceMarketplaceToken(String url) {
      return url.replace("${DOMAIN_NAME}", dummyDomain).replace(
          "${APP_ID}", dummyAppId);
    }

    private void updateUrl(IDOMDocument document, NodeList urlNodes) {
      for (int j = 0; j < urlNodes.getLength(); ++j) {
        Node urlNode = urlNodes.item(j);
        NodeList children = urlNode.getChildNodes();
        if (children.getLength() == 0) {
          urlNode.appendChild(
              document.createTextNode(newBaseAppUrl.toString()));
        } else {
          for (int k = 0; k < children.getLength(); ++k) {
            Node nodeToChange = children.item(k);
            // Update the base url for old url
            String oldUrl = Strings.emptyToNull(nodeToChange.getNodeValue());
            String newValue = createNewUrlValue(oldUrl);
            if (!StringUtilities.isEmpty(newValue)) {
              nodeToChange.setNodeValue(newValue);
            }
          }
        }
      }
    }
  }

  class UrlReadOperation extends XmlUtilities.ReadOperation {
    private String appUrl;

    public UrlReadOperation(IFile xmlFile) {
      super(xmlFile);
    }

    public String getAppUrl() {
      return appUrl;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gdt.eclipse.core.XmlUtilities.ReadOperation#read(org.eclipse
     * .wst.xml.core.internal.provisional.document.IDOMDocument)
     */
    @Override
    protected void read(IDOMDocument document) {
      // <Extension id="realm"><Url></Url></Extension>
      NodeList extNodeList = document.getDocumentElement().getElementsByTagName(
          "Extension");
      for (int i = 0; i < extNodeList.getLength(); ++i) {
        Element extensionNode = (Element) extNodeList.item(i);
        String id = extensionNode.getAttribute("id");
        if ("realm".equals(id)) {
          NodeList urlNodes = extensionNode.getElementsByTagName("Url");
          for (int j = 0; j < urlNodes.getLength(); ++j) {
            Node urlNode = urlNodes.item(j);
            NodeList children = urlNode.getChildNodes();
            for (int k = 0; k < children.getLength(); ++k) {
              Node urlValue = children.item(k);
              appUrl = urlValue.getNodeValue();
            }
          }
        }
      }
    }
  }

  private static final String APPS_MARKETPLACE_SUPPORT =
      "appsMarketplaceSupport";

  public static void addAppsMarketplaceSupport(
      IProject project, IFolder warFolder) throws CoreException {
    assert (!isAppsMarketplaceEnabled(project));
    enableAppsMarketplace(project);
    AppsMarketplaceProjectResources.addAppsMarketplaceMetadata(
        project.getName().toString(), warFolder, null);
  }

  /**
   * Returns the AppsMarketplaceProject for the specified IProject.
   *
   * @param project the project
   * @return a AppsMarketplaceProject instance, or <code>null</code> if the
   *         project does not have the Apps Marketplace enabled.
   */
  public static AppsMarketplaceProject create(IProject project) {
    assert (project != null);
    if (isAppsMarketplaceEnabled(project)) {
      return new AppsMarketplaceProject(JavaCore.create(project));
    }
    return null;
  }

  public static void disableAppsMarketplace(IProject project)
      throws CoreException {
    project.setPersistentProperty(new QualifiedName(
        AppsMarketplacePlugin.PLUGIN_ID, APPS_MARKETPLACE_SUPPORT), "false");
  }

  public static void enableAppsMarketplace(IProject project)
      throws CoreException {
    project.setPersistentProperty(new QualifiedName(
        AppsMarketplacePlugin.PLUGIN_ID, APPS_MARKETPLACE_SUPPORT), "true");
  }

  public static boolean isAppsMarketplaceEnabled(IProject project) {
    try {
      return "true".equals(project.getPersistentProperty(new QualifiedName(
          AppsMarketplacePlugin.PLUGIN_ID, APPS_MARKETPLACE_SUPPORT)));
    } catch (CoreException e) {
      return false;
    }
  }

  public static void removeAppsMarketplaceSupport(
      IProject project, IFolder warFolder) throws CoreException {
    assert (isAppsMarketplaceEnabled(project));
    disableAppsMarketplace(project);
    AppsMarketplaceProjectResources.removeAppsMarketplaceMetaData(
        project, warFolder, null);
  }

  private final IJavaProject javaProject;

  protected AppsMarketplaceProject(IJavaProject project) {
    this.javaProject = project;
  }

  public String getAppUrl() throws CoreException {
    IFile appXml = getManifestXml("application-manifest.xml");
    if (appXml == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find application-manifest.xml in project."
                  + getName()));
    }
    return getAppUrl(appXml);
  }

  public String getAppUrl(IFile appXml) throws CoreException {
    UrlReadOperation urlReadOperation;
    try {
      urlReadOperation = new UrlReadOperation(appXml);
      urlReadOperation.run();
    } catch (IOException ioe) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not set app url in application-manifest.xml of project "
                  + getName() + "Try refreshing workspace."));
    }
    return urlReadOperation.getAppUrl();
  }

  public String getAppUrl(String appManifest) throws CoreException {
    IFile temp = javaProject.getProject().getFile("temp.xml");
    temp.create(new ByteArrayInputStream(appManifest.getBytes()), true, null);
    String appUrl = getAppUrl(temp);
    temp.delete(true, null);
    return appUrl;
  }

  public IJavaProject getJavaProject() {
    return javaProject;
  }

  /**
   * xmlFileName is either application-manifest.xml, listing-manifest.xml or
   * web.xml..
   *
   */
  public IFile getManifestXml(final String filename) {

    IProject project = javaProject.getProject();
    IFolder webInfFolder;
    try {
      WebAppUtilities.verifyIsWebApp(project);
      webInfFolder = WebAppUtilities.getWebInfSrc(project);
    } catch (CoreException e) {
      // It is a not Google web app
      String warInfFolder =
          AppsMarketplaceProjectProperties.getAppListingWarDirectory(project)
          + "/WEB-INF";
      webInfFolder = (IFolder) project.findMember(warInfFolder);
    }

    IFile xmlFile = webInfFolder.getFile(filename);
    if (xmlFile.exists()) {
      return xmlFile;
    }
    return null;
  }

  public String getName() {
    return getProject().getName();
  }

  public IProject getProject() {
    return javaProject.getProject();
  }

  public void setAppName(final String appName) throws CoreException {
    IFile appXml = getManifestXml("application-manifest.xml");
    if (appXml == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find application-manifest.xml in project."
                  + getName()));
    }
    if (StringUtilities.isEmpty(appName)) {
      return;
    }
    try {
      new NameEditOperation(appXml, appName).run(true);
    } catch (IOException ioe) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not set app name in application-manifest.xml of project "
                  + getName() + "Try refreshing workspace."));
    }
  }

  public void setAppUrl(String appUrl) throws CoreException {
    setAppUrl(appUrl, true);
  }

  public void setAppUrl(String appUrl, boolean forceSave) throws CoreException {
    IFile appXml = getManifestXml("application-manifest.xml");
    if (appXml == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find application-manifest.xml in project."
                  + getName()));
    }
    if (StringUtilities.isEmpty(appUrl)) {
      return;
    }
    try {
      new UrlEditOperation(appXml, appUrl).run(forceSave);
    } catch (IOException ioe) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not set app url in application-manifest.xml of project "
                  + getName() + "Try refreshing workspace."));
    }
  }

  public void setOAuthParams(
      final String consumerKey, final String consumerSecret)
          throws CoreException {
    setOAuthParams(consumerKey, consumerSecret, true);
  }

  public void setOAuthParams(
      final String consumerKey, final String consumerSecret, boolean forceSave)
          throws CoreException {
    IFile webXml = getManifestXml("web.xml");
    if (webXml == null) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not find web.xml in project " + getName()));
    }
    try {
      new XmlUtilities.EditOperation(webXml) {
        @Override
        protected void edit(IDOMDocument document) {
          // <init-param>
          // <param-name>consumer_key</param-name>
          // <param-value></param-value>
          // </init-param>
          NodeList initParamNodes =
              document.getDocumentElement().getElementsByTagName("init-param");
          for (int i = 0; i < initParamNodes.getLength(); ++i) {
            Node initParamNode = initParamNodes.item(i);
            NodeList paramNodes = initParamNode.getChildNodes();
            for (int j = 0; j < paramNodes.getLength(); ++j) {
              Node paramNode = paramNodes.item(j);
              if ("param-name".equals(paramNode.getNodeName())) {
                // TODO: make sure this works (no npe) even if
                // web.xml
                // is messed up.
                String paramNameStr = paramNode.getChildNodes().item(
                    0).getNodeValue();
                if ("consumer_key".equals(paramNameStr)) {
                  // Look for following param-value.
                  for (int k = j + 1; k < paramNodes.getLength(); ++k) {
                    Node paramNode2 = paramNodes.item(k);
                    if ("param-value".equals(paramNode2.getNodeName())) {
                      NodeList children = paramNode2.getChildNodes();
                      if (children.getLength() == 0) {
                        paramNode2.appendChild(
                            document.createTextNode(consumerKey));
                      } else {
                        Node nodeToChange = children.item(0);
                        nodeToChange.setNodeValue(consumerKey);
                      }
                    }
                  }
                } else if ("consumer_secret".equals(paramNameStr)) {
                  // Look for following param-value.
                  for (int k = j + 1; k < paramNodes.getLength(); ++k) {
                    Node paramNode2 = paramNodes.item(k);
                    if ("param-value".equals(paramNode2.getNodeName())) {
                      NodeList children = paramNode2.getChildNodes();
                      if (children.getLength() == 0) {
                        paramNode2.appendChild(
                            document.createTextNode(consumerSecret));
                      } else {
                        Node nodeToChange = children.item(0);
                        nodeToChange.setNodeValue(consumerSecret);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }.run(forceSave);
    } catch (IOException ioe) {
      throw new CoreException(
          new Status(Status.ERROR, AppsMarketplacePlugin.PLUGIN_ID,
              "Could not set oauth consumer key and password in web.xml of project "
                  + getName() + ". Try refreshing the workspace."));
    }
  }
}
