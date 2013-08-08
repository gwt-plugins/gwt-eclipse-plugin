/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm.util;

import com.google.gdt.eclipse.appengine.swarm.AppEngineSwarmPlugin;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Xml Manipulation Utility Class for modifying web.xml for generated APIs.
 */
public class XmlUtil {

  private static final String ACTIVITY = "activity";
  private static final String C2DM_INTENT_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
  private static final String CATEGORY = "category";
  private static final String C2DM_INTENT_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
  private static final String ACTION = "action";
  private static final String INTENT_FILTER = "intent-filter";
  private static final String C2DM_PERMISSION_SEND = "com.google.android.c2dm.permission.SEND";
  private static final String C2DM_BROADCAST_RECEIVER = "com.google.android.gcm.GCMBroadcastReceiver";
  private static final String RECEIVER2 = "receiver";
  private static final String C2DM_RECEIVER = ".GCMIntentService";
  private static final String SERVICE = "service";
  private static final String COMMA = ",";
  private static final String WEB_APP = "web-app";
  private static final String MANIFEST = "manifest";
  private static final String INIT_PARAM = "init-param";
  private static final String SERVLET = "servlet";
  private static final String SERVLET_NAME = "servlet-name";
  private static final String SERVLET_MAPPING = "servlet-mapping";
  private static final String SERVLET_CLASS = "servlet-class";
  private static final String URL_PATTERN = "url-pattern";
  private static final String SPI_URL_PATTERN = "/_ah/spi/*";
  private static final String PARAM_NAME = "param-name";
  private static final String PARAM_VALUE = "param-value";
  private static final String SERVICES = "services";
  private static final String SYSTEM_SERVICE_SERVLET = "SystemServiceServlet";
  private static final String SYSTEM_SERVICE_SERVLET_CLASS = "com.google.api.server.spi.SystemServiceServlet";
  private static final String ANDROID_MANIFEST_RELATIVE_PATH = "/AndroidManifest.xml";
  private static final String ANDROID_ATTR_NAME = "android:name";
  private static final String ANDROID_ATTR_PERMISSION = "android:permission";
  private static final String INTERNET_PERMISSION_ATTR_VALUE = "android.permission.INTERNET";
  private static final String C2D_MESSAGE_PERMISSION_ATTR_VALUE = ".permission.C2D_MESSAGE";
  private static final String WAKE_LOCK_MESSAGE_PERMISSION_ATTR_VALUE = "android.permission.WAKE_LOCK";
  private static final String C2DM_RECEIVE_PERMISSION_ATTR_VALUE = "com.google.android.c2dm.permission.RECEIVE";
  private static final String GET_ACCOUNTS_PERMISSION_ATTR_VALUE = "android.permission.GET_ACCOUNTS";
  private static final String USE_CREDENTIALS_PERMISSION_ATTR_VALUE = "android.permission.USE_CREDENTIALS";
  private static final String PERMISSION_NODE = "uses-permission";
  private static final String DEFINE_PERMISSION_NODE = "permission";
  private static final String PERMISSION_PROTCTION_ATTR_NAME = "android:protectionLevel";
  private static final String PERMISSION_PROTCTION_ATTR_VALUE = "signature";
  private static final String APPLICATION = "application";

  private IPath webXmlIPath;
  private IPath androidManifestXmlIPath;

  private org.w3c.dom.Document doc;
  Node manifestNode;

  /**
   * Adds uses-permission node to AndroidManifest, provided it does not already
   * exist.
   */
  private boolean addUsesPermissionNode(org.w3c.dom.Document doc, Node manifestNode, Node appNode,
      String attrValue) {
    if (hasUsesPermissionNode(manifestNode, attrValue)) {
      return false;
    }
    Element element = doc.createElement(PERMISSION_NODE);
    element.setAttribute(ANDROID_ATTR_NAME, attrValue);
    manifestNode.insertBefore(element, appNode);
    Node node = doc.createTextNode("\n   ");
    manifestNode.insertBefore(node, appNode);
    return true;
  }

  /**
   * Defines C2D_MESSAGE permission in AndroidManifest.
   */
  private boolean defineC2dmPermission(org.w3c.dom.Document doc, Node manifestNode, Node appNode,
      String packageName) {
    Node definePermissionNode = null;
    for (definePermissionNode = manifestNode.getFirstChild(); definePermissionNode != null; definePermissionNode = definePermissionNode.getNextSibling()) {
      if (isElementAndNamed(definePermissionNode, DEFINE_PERMISSION_NODE)) {
        Node attribute = definePermissionNode.getAttributes().getNamedItem(ANDROID_ATTR_NAME);
        if (attribute != null
            && attribute.getNodeValue().equals(packageName + C2D_MESSAGE_PERMISSION_ATTR_VALUE)) {
          return false;
        }
      }
    }

    Element element = doc.createElement(DEFINE_PERMISSION_NODE);
    element.setAttribute(ANDROID_ATTR_NAME, packageName + C2D_MESSAGE_PERMISSION_ATTR_VALUE);
    element.setAttribute(PERMISSION_PROTCTION_ATTR_NAME, PERMISSION_PROTCTION_ATTR_VALUE);
    manifestNode.insertBefore(element, appNode);

    Node node = doc.createTextNode("\n\n   ");
    manifestNode.insertBefore(node, appNode);
    return true;
  }

  public String findAndroidPackage(IProject androidProject) throws ParserConfigurationException,
      SAXException, IOException {

    doc = initializeDoc(androidProject);
    manifestNode = findManifestNode(doc);
    if (manifestNode != null && manifestNode.getAttributes() != null
        && manifestNode.getAttributes().getNamedItem("package") != null) {
      return manifestNode.getAttributes().getNamedItem("package").getNodeValue();
    }
    return null;
  }

  /**
   * Finds the application node in AndroidManifest.xml.
   */
  private Node findAppNode() {
    Node appNode = null;
    for (appNode = manifestNode.getFirstChild(); appNode != null; appNode = appNode.getNextSibling()) {
      if (isElementAndNamed(appNode, APPLICATION)) {
        break;
      }
    }
    return appNode;
  }
  
  /**
   * Finds the Manifest node in AndroidManifest.xml document.
   * 
   * @return Manifest node if found. Else null.
   */
  private Node findManifestNode(org.w3c.dom.Document doc) {
    Node manifestNode = null;
    for (manifestNode = doc.getFirstChild(); manifestNode != null; manifestNode = manifestNode.getNextSibling()) {
      if (isElementAndNamed(manifestNode, MANIFEST)) { 
        break;
      }
    }
    if (manifestNode == null) {
      AppEngineSwarmPlugin.log("Not a valid AndroidManifest.xml document");
      return null;
    }
    return manifestNode;
  }

  /**
   * Finds the WebApp node in web.xml document. Then tries to find
   * SystemServiceServlet node and returns it. If not found, returns WebApp
   * node.  The returned type is of type Element
   * 
   * @return SystemServiceServlet node if found, else WebApp node.
   */
  private Node findSystemServiceServlet(org.w3c.dom.Document doc) {
    Node webAppNode = null;
    for (webAppNode = doc.getFirstChild(); webAppNode != null; webAppNode = webAppNode.getNextSibling()) {
      if (isElementAndNamed(webAppNode, WEB_APP)) {
        break;
      }
    }
    if (webAppNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return null;
    }

    Node systemServiceServletNode = null;
    for (systemServiceServletNode = webAppNode.getFirstChild(); systemServiceServletNode != null; systemServiceServletNode = systemServiceServletNode.getNextSibling()) {
      if (isElementAndNamed(systemServiceServletNode, SERVLET)) {
        for (Node n3 = systemServiceServletNode.getFirstChild(); n3 != null; n3 = n3.getNextSibling()) {
          if (isElementAndNamed(n3, SERVLET_NAME)
              && n3.getTextContent().equals(SYSTEM_SERVICE_SERVLET)) {
            return systemServiceServletNode;
          }
        }
      }
    }
    return webAppNode;
  }

  /**
   * Checks if the manifest file already has internet permission.
   */
  private boolean hasUsesPermissionNode(Node manifestNode, String attrValue) {
    Node permissionNode = null;
    for (permissionNode = manifestNode.getFirstChild(); permissionNode != null; permissionNode = permissionNode.getNextSibling()) {
      if (isElementAndNamed(permissionNode, PERMISSION_NODE)) {
        Node attribute = permissionNode.getAttributes().getNamedItem(ANDROID_ATTR_NAME);
        if (attribute != null && attribute.getNodeValue().equals(attrValue)) {
          return true;
        }
      }
    }
    return false;
  }

  private Document initializeDoc(IProject androidProject) throws ParserConfigurationException,
      SAXException, IOException {
    androidManifestXmlIPath = androidProject.getLocation().append(ANDROID_MANIFEST_RELATIVE_PATH);
    String androidManifestXmlPath = androidManifestXmlIPath.toOSString();
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    return docBuilder.parse(androidManifestXmlPath);
  }

  /**
   * Inserts C2DM Receiver Node in AndroidManifest.xml.
   */
  private boolean insertC2dmReceiver(Node appNode, String packageName) {

    String spc = "    ";
    Element element = null;
    Element element2 = null;
    Node node = null;
    if (appNode == null) {
      AppEngineSwarmPlugin.log("Not a valid AndroidManifest.xml document");
      return false;
    }

    boolean serviceNodeFound = false;
    boolean receiverNodeFound = false;

    Node gcmNode = null;
    for (gcmNode = appNode.getFirstChild(); gcmNode != null; gcmNode = gcmNode.getNextSibling()) {
      if (isElementAndNamed(gcmNode, SERVICE)) {
        Node attribute = gcmNode.getAttributes().getNamedItem(ANDROID_ATTR_NAME);
        if (attribute != null && attribute.getNodeValue().equals(C2DM_RECEIVER)) {
          serviceNodeFound = true;
        }
      } else if (isElementAndNamed(gcmNode, RECEIVER2)) {
        Node attribute = gcmNode.getAttributes().getNamedItem(ANDROID_ATTR_NAME);
        if (attribute != null && attribute.getNodeValue().equals(C2DM_BROADCAST_RECEIVER)) {
          receiverNodeFound = true;
        }
      }

      if (serviceNodeFound && receiverNodeFound) {
        // Nothing to do
        return false;
      }
    }

    if (!serviceNodeFound) {
      node = doc.createTextNode(spc);
      appNode.appendChild(node);
      element = doc.createElement(SERVICE);
      element.setAttribute(ANDROID_ATTR_NAME, C2DM_RECEIVER);
      appNode.appendChild(element);
      node = doc.createTextNode("\n" + spc + spc);
      appNode.appendChild(node);
    }

    if (!receiverNodeFound) {
      Element receiver = doc.createElement(RECEIVER2);
      receiver.setAttribute(ANDROID_ATTR_NAME, C2DM_BROADCAST_RECEIVER);
      receiver.setAttribute(ANDROID_ATTR_PERMISSION, C2DM_PERMISSION_SEND);
      appNode.appendChild(receiver);
      node = doc.createTextNode("\n" + spc);
      appNode.appendChild(node);

      node = doc.createTextNode("\n" + spc + spc + spc);
      receiver.appendChild(node);
      element = doc.createElement(INTENT_FILTER);
      receiver.appendChild(element);

      node = doc.createTextNode("\n" + spc + spc + spc + spc);
      element.appendChild(node);
      element2 = doc.createElement(ACTION);
      element2.setAttribute(ANDROID_ATTR_NAME, C2DM_INTENT_RECEIVE);
      element.appendChild(element2);
      node = doc.createTextNode("\n" + spc + spc + spc + spc);
      element.appendChild(node);
      element2 = doc.createElement(CATEGORY);
      element2.setAttribute(ANDROID_ATTR_NAME, packageName);
      element.appendChild(element2);
      node = doc.createTextNode("\n" + spc + spc + spc);
      element.appendChild(node);

      node = doc.createTextNode("\n" + spc + spc + spc);
      receiver.appendChild(node);
      element = doc.createElement(INTENT_FILTER);
      receiver.appendChild(element);

      node = doc.createTextNode("\n" + spc + spc + spc + spc);
      element.appendChild(node);
      element2 = doc.createElement(ACTION);
      element2.setAttribute(ANDROID_ATTR_NAME, C2DM_INTENT_REGISTRATION);
      element.appendChild(element2);
      node = doc.createTextNode("\n" + spc + spc + spc + spc);
      element.appendChild(node);
      element2 = doc.createElement(CATEGORY);
      element2.setAttribute(ANDROID_ATTR_NAME, packageName);
      element.appendChild(element2);
      node = doc.createTextNode("\n" + spc + spc + spc);
      element.appendChild(node);

      node = doc.createTextNode("\n" + spc + spc);
      receiver.appendChild(node);
    }

    return (!serviceNodeFound || !receiverNodeFound);
  }

  private boolean insertRegisterActivity(Node appNode) {
    if (appNode == null) {
      AppEngineSwarmPlugin.log("Not a valid AndroidManifest.xml document");
      return false;
    }

    Node registerActivityNode = null;
    for (registerActivityNode = appNode.getFirstChild(); registerActivityNode != null; registerActivityNode = registerActivityNode.getNextSibling()) {
      if (isElementAndNamed(registerActivityNode, ACTIVITY)) {
        Node attribute = registerActivityNode.getAttributes().getNamedItem(ANDROID_ATTR_NAME);
        if (attribute != null && attribute.getNodeValue().equals(".RegisterActivity")) {
          return false;
        }
      }
    }

    Element element = null;
    Node node = null;
    String spc = "    ";
    node = doc.createTextNode("\n" + spc + spc);
    appNode.appendChild(node);
    element = doc.createElement(ACTIVITY);
    element.setAttribute(ANDROID_ATTR_NAME, ".RegisterActivity");
    element.setAttribute("android:launchMode", "singleTop");   
    appNode.appendChild(element);        
    node = doc.createTextNode("\n");
    appNode.appendChild(node);
    return true;
  }

  /**
   * Insert a SystemServiceServlet node in web.xml inside webApp node.
   * 
   * @return The inserted SystemServiceServlet node.
   */
  private Node insertSystemServiceServlet(org.w3c.dom.Document doc, Node webAppNode, String spc,
      String delimiter) {
    Node n2, n3, n4, n5;
    n5 = doc.createTextNode(spc);
    webAppNode.appendChild(n5);
    n2 = doc.createElement(SERVLET);
    webAppNode.appendChild(n2);
    n5 = doc.createTextNode(delimiter + spc);
    webAppNode.appendChild(n5);
    n3 = doc.createElement(SERVLET_MAPPING);
    webAppNode.appendChild(n3);
    n5 = doc.createTextNode(delimiter);
    webAppNode.appendChild(n5);

    n5 = doc.createTextNode("\n" + spc + spc);
    n2.appendChild(n5);
    n5 = doc.createElement(SERVLET_NAME);
    n5.setTextContent(SYSTEM_SERVICE_SERVLET);
    n2.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc + spc);
    n2.appendChild(n5);
    n5 = doc.createElement(SERVLET_CLASS);
    n5.setTextContent(SYSTEM_SERVICE_SERVLET_CLASS);
    n2.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc + spc);
    n2.appendChild(n5);
    n4 = doc.createElement(INIT_PARAM);
    n2.appendChild(n4);
    n5 = doc.createTextNode("\n" + spc);
    n2.appendChild(n5);

    n5 = doc.createTextNode("\n" + spc + spc);
    n3.appendChild(n5);
    n5 = doc.createElement(SERVLET_NAME);
    n5.setTextContent(SYSTEM_SERVICE_SERVLET);
    n3.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc + spc);
    n3.appendChild(n5);
    n5 = doc.createElement(URL_PATTERN);
    n5.setTextContent(SPI_URL_PATTERN);
    n3.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc);
    n3.appendChild(n5);

    n5 = doc.createTextNode("\n" + spc + spc + spc);
    n4.appendChild(n5);
    n5 = doc.createElement(PARAM_NAME);
    n5.setTextContent(SERVICES);
    n4.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc + spc + spc);
    n4.appendChild(n5);
    n5 = doc.createElement(PARAM_VALUE);
    n5.setTextContent("");
    n4.appendChild(n5);
    n5 = doc.createTextNode("\n" + spc + spc);
    n4.appendChild(n5);

    return n2;
  }

  /**
   * Inserts the service class as a parameter to SystemServiceServlet node.
   * 
   * @return Returns whether document has to be saved or not. If there is an
   *         unexpected error in web.xml, it will return false.
   */
  private boolean insertSystemServiceServletParam(org.w3c.dom.Document doc,
      Node systemServiceServletNode, String serviceFullName, String spc, String delimiter) {
    Node initParamNode = null;
    for (initParamNode = systemServiceServletNode.getFirstChild(); 
        initParamNode != null; 
        initParamNode = initParamNode.getNextSibling()) {
      if (isElementAndNamed(initParamNode,INIT_PARAM)) {
        break;
      }
    }
    if (initParamNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return false;
    }

    Node paramValueNode = null;
    for (paramValueNode = initParamNode.getFirstChild(); 
        paramValueNode != null; 
        paramValueNode = paramValueNode.getNextSibling()) {
      if (isElementAndNamed(paramValueNode, PARAM_VALUE)) {
        break;
      }
    }
    if (paramValueNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return false;
    }

    if (serviceFullName == null) {
      paramValueNode.setTextContent("");
      return true;
    }

    if (paramValueNode.getTextContent().indexOf(serviceFullName) != -1) {
      return false;
    }
    String paramValue = paramValueNode.getTextContent();
    if (!paramValue.equals("")) {
      paramValue += COMMA;
    }
    paramValueNode.setTextContent(paramValue + serviceFullName);
    return true;
  }

  /**
   * Checks if a node is an XML element and checks if it has a specific name
   * @param node
   * @param name
   * @return true if matching element, false if name doesn't match OR if node type isn't ELEMENT
   */
  private boolean isElementAndNamed(Node node, String name) { 
    if (node == null || name == null) {
      throw new IllegalArgumentException();
    }
    return (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name));
  }

  private void saveFile(org.w3c.dom.Document doc, IPath filePath) 
      throws TransformerFactoryConfigurationError, TransformerException, CoreException, 
      IOException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(new DOMSource(doc), new StreamResult(filePath.toFile()));
  }

  public void updateAndroidManifestXml(IProject androidProject)
      throws ParserConfigurationException, SAXException, IOException,
      TransformerFactoryConfigurationError, TransformerException, CoreException {
    org.w3c.dom.Document doc = initializeDoc(androidProject);
    manifestNode = findManifestNode(doc);
    if (manifestNode == null) {
      return;
    }
    if (addUsesPermissionNode(doc, manifestNode, findAppNode(), INTERNET_PERMISSION_ATTR_VALUE)) {
      saveFile(doc, androidManifestXmlIPath);
    }
  }

  public void updateAndroidManifestXmlForGCM(IProject androidProject)
      throws ParserConfigurationException, SAXException, IOException,
      TransformerFactoryConfigurationError, TransformerException, CoreException {

    String packageName = findAndroidPackage(androidProject);
    Node appNode = findAppNode();

    boolean shouldSave = defineC2dmPermission(doc, manifestNode, appNode, packageName);

    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode, INTERNET_PERMISSION_ATTR_VALUE);
    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode, packageName
        + C2D_MESSAGE_PERMISSION_ATTR_VALUE);
    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode,
        C2DM_RECEIVE_PERMISSION_ATTR_VALUE);
    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode,
        WAKE_LOCK_MESSAGE_PERMISSION_ATTR_VALUE);
    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode,
        GET_ACCOUNTS_PERMISSION_ATTR_VALUE);
    shouldSave |= addUsesPermissionNode(doc, manifestNode, appNode,
        USE_CREDENTIALS_PERMISSION_ATTR_VALUE);

    shouldSave |= insertC2dmReceiver(appNode, packageName);
    shouldSave |= insertRegisterActivity(appNode);

    if (shouldSave) {
      saveFile(doc, androidManifestXmlIPath);
    }
  }

  /**
   * Update the SystemServiceServlet parameter in web.xml, it doesn't make changes if nothing new is to
   * be added.  If changes are required, it will modify the file and save 
   * @param doc
   * @param systemServiceServletNode
   * @param services
   * @param spc
   * @param delimiter
   * @return
   */
  public boolean updateSystemServiceServletParam(Document doc,
      Node systemServiceServletNode, List<String> services, String spc,
      String delimiter) {
    Node initParamNode = null;
    for (initParamNode = systemServiceServletNode.getFirstChild(); initParamNode != null; initParamNode = initParamNode.getNextSibling()) {
      if (isElementAndNamed(initParamNode, INIT_PARAM)) {
        break;
      }
    }
    if (initParamNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return false;
    }

    Node paramValueNode = null;
    for (paramValueNode = initParamNode.getFirstChild(); paramValueNode != null; paramValueNode = paramValueNode.getNextSibling()) {
      if (isElementAndNamed(paramValueNode, PARAM_VALUE)) {
        break;
      }
    }
    if (paramValueNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return false;
    }

    // get all services the file currently lists,
    // put it in a treeset for sorted order, also removes duplicates
    String serviceXMLString = paramValueNode.getTextContent();
    Set<String> servicesOnFile = new TreeSet<String>();
    if (serviceXMLString != null && !serviceXMLString.trim().isEmpty()) {
      String[] servicesArray = serviceXMLString.split(",");
      for (String s : servicesArray) {
        servicesOnFile.add(s.trim());
      }
    }
    
    // find all services we need to remove
    List<String> servicesToRemove = new ArrayList<String>();
    for (String s : servicesOnFile) {
      if (!services.contains(s)) {
        servicesToRemove.add(s);
      }
    }

    // find all services we need to add
    List<String> servicesToAdd = new ArrayList<String>();
    for (String s : services) {
      if (!servicesOnFile.contains(s)) {
        servicesToAdd.add(s);
      }
    }

    // if we don't need to make any changes, then return false
    if (servicesToAdd.isEmpty() && servicesToRemove.isEmpty()) {
      return false;
    }

    // remove those marked for removal
    for (String s : servicesToRemove) {
      servicesOnFile.remove(s);
    }

    // add those marked for adding
    for (String s : servicesToAdd) {
      servicesOnFile.add(s);
    }

    // write the appropriate data to the file
    if (servicesOnFile.size() == 0) {
      paramValueNode.setTextContent("");
    } else {
      paramValueNode.setTextContent(StringUtilities.join(servicesOnFile, COMMA));
    }

    // indicate that a save is required
    return true;
  }
  
  public void updateWebXml(List<String> services, IProject project)
      throws ParserConfigurationException, SAXException, IOException,
      TransformerFactoryConfigurationError, TransformerException, CoreException {
    boolean saveRequired = false;
    boolean isGwtProject = false;

    // Web Xml indentation is different for just App Engine Projects and
    // project with Gwt nature.
    isGwtProject = GWTNature.isGWTProject(project);
    String spc = " ";
    String delimiter = "\n";

    if (isGwtProject) {
      spc = "  ";
      delimiter = "\n\n";
    }

    webXmlIPath = WebAppUtilities.getWebXmlPath(project);
    if (webXmlIPath == null || !webXmlIPath.toFile().exists()) {
      throw new CoreException(new Status(IStatus.ERROR, AppEngineSwarmPlugin.PLUGIN_ID,
          "Could not find web.xml"));
    }
    String webXmlPath = webXmlIPath.toOSString();
    
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    org.w3c.dom.Document doc = docBuilder.parse(webXmlPath);

    Node systemServiceServletNode = findSystemServiceServlet(doc);
    if (systemServiceServletNode == null) {
      AppEngineSwarmPlugin.log("Not a valid web.xml document");
      return;
    }
    Assert.isTrue(systemServiceServletNode instanceof Element);
    if (isElementAndNamed(systemServiceServletNode, WEB_APP)) {
      systemServiceServletNode = insertSystemServiceServlet(doc, systemServiceServletNode, spc,
          delimiter);
      saveRequired = true;
    }

    saveRequired = updateSystemServiceServletParam(doc, systemServiceServletNode, services,
        spc, delimiter);
    if (saveRequired) {
      saveFile(doc, webXmlIPath);
    }
  }
}
