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
package com.google.gwt.eclipse.core.uibinder;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.sse.css.CssExtractor;
import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelLoader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;

/**
 * Utility methods for interacting with the UiBinder XML model.
 */
@SuppressWarnings("restriction")
public final class UiBinderXmlModelUtilities {

  /**
   * Computes the fully qualified name for the Widget-derived type at the given
   * node.
   * 
   * @param widgetNode the element node that refers to a widget
   * @return the fully qualified type, or null if the element is not in the
   *         widget syntax
   */
  public static String computeQualifiedWidgetTypeName(Node widgetNode) {
    if (widgetNode.getNodeType() != Node.ELEMENT_NODE) {
      return null;
    }

    String typeName = widgetNode.getLocalName();
    // Ensure the first character is upper-case
    if (typeName.length() == 0 || Character.isLowerCase(typeName.charAt(0))) {
      return null;
    }

    String packageName = UiBinderUtilities.getPackageName(widgetNode.getNamespaceURI());

    return packageName != null ? JavaUtilities.getQualifiedTypeName(typeName,
        packageName) : null;
  }

  /**
   * Creates a {@link CssExtractor} from a <ui:style> element.
   * 
   * @param styleElement the style element that contains CSS inline, a src
   *          attribute pointing to an external CSS file, or both
   * @return a CSS extractor for the CSS, or null
   */
  public static CssExtractor createCssExtractorForStyleElement(
      IDOMElement styleElement, IJavaProject javaProject) {
    // The contents of the element is appended to the source files
    String cssBlock = "";

    String externalCssContents = getExternalCssContents(styleElement,
        javaProject);
    if (externalCssContents != null) {
      cssBlock += externalCssContents;
    }

    Node contents = styleElement.getFirstChild();
    if (contents != null) {
      cssBlock += contents.getNodeValue();
    }

    if (cssBlock.trim().length() == 0) {
      return null;
    }

    return CssExtractor.extract(cssBlock, new CssResourceAwareModelLoader());
  }

  /**
   * Gets the "field" attribute on an element according to the UiBinder rules.
   * 
   * @param element an element
   * @return an attribute node for the field attribute, or null
   */
  public static Node getFieldAttribute(Node element) {
    /*
     * If the field attribute appears on a "with", "style", "import", "data", 
     * or "image" element, it should NOT have a namespace. If it appears on a widget or
     * layout, it should have a namespace (the namespace for the typical "ui"
     * prefix).
     */
    String namespace;
    if (isWithElement(element) || isStyleElement(element)
        || isImageElement(element) || isDataElement(element) || isImportElement(element)) {
      namespace = null;
    } else {
      namespace = UiBinderConstants.UI_BINDER_XML_NAMESPACE;
    }

    return XmlUtilities.getAttribute(element,
        UiBinderConstants.UI_BINDER_FIELD_ATTRIBUTE_NAME, true, namespace);
  }

  /**
   * Gets the "src" attribute on a <ui:style>, <ui:image>, or <ui:data> element.
   * 
   * @param element an element (this method ensures the element is a style
   *          element)
   * @return an attribute node, or null
   */
  public static Node getSrcAttribute(Node element) {
    if (!isStyleElement(element) && !isImageElement(element)
        && !isDataElement(element)) {
      return null;
    }

    // the src attribute must not have a namespace
    return (IDOMAttr) XmlUtilities.getAttribute(element,
        UiBinderConstants.UI_BINDER_SRC_ATTRIBUTE_NAME, true, null);
  }

  /**
   * Gets the "type" attribute on a <ui:with> or <ui:style> element.
   * 
   * @param element an element (this method ensures the element is a "with" or
   *          "style" element)
   * @return an attribute node, or null
   */
  public static Node getTypeAttribute(Node element) {
    if (!isWithElement(element) && !isStyleElement(element)) {
      return null;
    }

    // The type attribute must not have a namespace
    return XmlUtilities.getAttribute(element,
        UiBinderConstants.UI_BINDER_TYPE_ATTRIBUTE_NAME, true, null);
  }

  /**
   * Determines whether the given node is a <ui:data> element.
   */
  public static boolean isDataElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_DATA_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Determines whether the given node is a <ui:image> element.
   */
  public static boolean isImageElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_IMAGE_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Determines whether the given node is a <ui:import> element.
   */
  public static boolean isImportElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_IMPORT_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Determines whether the given node is a <ui:style> element.
   */
  public static boolean isStyleElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_STYLE_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Determines whether the given node is a <ui:UiBinder> element.
   */
  public static boolean isUiBinderElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Determines whether the given attribute is used for importing packages via
   * the URN import scheme.
   */
  public static boolean isUrnImportAttribute(IDOMAttr node) {
    return isUiBinderElement(node.getOwnerElement())
        && UiBinderConstants.XMLNS_NAMESPACE.equals(node.getNamespaceURI())
        && node.getNodeValue() != null
        && node.getNodeValue().startsWith(
            UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING);
  }

  /**
   * Determines whether the given node is a <ui:with> element.
   */
  public static boolean isWithElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && UiBinderConstants.UI_BINDER_XML_NAMESPACE.equals(node.getNamespaceURI())
        && UiBinderConstants.UI_BINDER_WITH_ELEMENT_NAME.equalsIgnoreCase(node.getLocalName());
  }

  /**
   * Resolves an element to its corresponding java type. This deals with widget
   * types and style/with/image/data elements.
   * 
   * @param element the element to resolve
   * @param javaProject the java project used for looking up the type
   * @return the corresponding java type, or null
   */
  public static IType resolveElementToJavaType(IDOMElement element,
      IJavaProject javaProject) {
    String qualifiedTypeName = computeQualifiedWidgetTypeName(element);

    if (qualifiedTypeName == null) {
      Node typeAttr = getTypeAttribute(element);
      if (typeAttr != null) {
        qualifiedTypeName = typeAttr.getNodeValue();
      }
    }

    if (qualifiedTypeName == null) {
      if (isImageElement(element)) {
        qualifiedTypeName = UiBinderConstants.UI_BINDER_IMAGE_RESOURCE_NAME;
      } else if (isDataElement(element)) {
        qualifiedTypeName = UiBinderConstants.UI_BINDER_DATA_RESOURCE_NAME;
      } else if (isImportElement(element)) {
        Node node = getFieldAttribute(element);
        if (node == null) {
          return null; // field attribute is missing
        }
        String qualifiedConstant = node.getNodeValue();
        int index = qualifiedConstant.lastIndexOf('.');
        if (index != -1) {
          qualifiedTypeName = qualifiedConstant.substring(0, index);
        }
      }
    }

    if (qualifiedTypeName == null) {
      return null;
    }

    try {
      return javaProject.findType(qualifiedTypeName);
    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not resolve element to Java type.");
      return null;
    }
  }

  /**
   * @see #resolveUiBinderNamespacePrefix(IStructuredDocument)
   */
  public static String resolveUiBinderNamespacePrefix(IDOMModel xmlModel) {
    Element element = xmlModel.getDocument().getDocumentElement();
    if (element == null) {
      return null;
    }

    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node item = attributes.item(i);
      if (item.getNamespaceURI() == null || item.getNodeValue() == null) {
        // This attr cannot be a namespace prefix definition since those attrs
        // must be in xmlns's namespace and must have an attr value.
        continue;
      }

      if (item.getNamespaceURI().equals(UiBinderConstants.XMLNS_NAMESPACE)
          && item.getNodeValue().equals(
              UiBinderConstants.UI_BINDER_XML_NAMESPACE)) {
        return item.getLocalName();
      }
    }

    return null;
  }

  /**
   * Returns the namespace prefix bound to the UiBinder namespace.
   * 
   * @return the namespace prefix (e.g. "ui"), or null
   */
  public static String resolveUiBinderNamespacePrefix(
      IStructuredDocument xmlDocument) {
    IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
        xmlDocument);
    if (model == null || !(model instanceof IDOMModel)) {
      return null;
    }

    try {
      return resolveUiBinderNamespacePrefix((IDOMModel) model);
    } finally {
      model.releaseFromRead();
    }
  }

  /**
   * Gets the contents of an external CSS file referenced by a style element.
   */
  private static String getExternalCssContents(IDOMElement styleElement,
      IJavaProject javaProject) {
    try {
      IFile xmlFile = SseUtilities.resolveFile(styleElement.getModel());
      if (xmlFile == null) {
        return null;
      }

      IPath xmlClasspathRelativePath = ClasspathResourceUtilities.getClasspathRelativePathOfResource(
          xmlFile, javaProject);
      if (xmlClasspathRelativePath == null) {
        return null;
      }
      IPath dirClasspathRelativePath = xmlClasspathRelativePath.removeLastSegments(1);

      Node srcAttr = UiBinderXmlModelUtilities.getSrcAttribute(styleElement);
      if (srcAttr == null || srcAttr.getNodeValue() == null) {
        return null;
      }

      StringBuilder cssBlock = new StringBuilder();
      for (String cssClasspathRelativePathString : UiBinderUtilities.getPathsFromDelimitedString(srcAttr.getNodeValue())) {
        IPath cssClasspathRelativePath = dirClasspathRelativePath.append(cssClasspathRelativePathString);

        IFile cssFile = (IFile) ClasspathResourceUtilities.resolveFile(
            cssClasspathRelativePath, javaProject);
        if (cssFile == null) {
          continue;
        }

        cssBlock.append(ResourceUtils.readFileContents(cssFile.getLocation()));
      }

      return cssBlock.toString();

    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not get external CSS file contents.");
    } catch (IOException e) {
      GWTPluginLog.logWarning(e, "Could not get external CSS file contents.");
    }

    return null;
  }

  private UiBinderXmlModelUtilities() {
  }
}
