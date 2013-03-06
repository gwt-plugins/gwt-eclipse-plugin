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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.contentassist.XmlContentAssistUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderXmlModelUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.IProposalComputer;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.xml.core.internal.document.DocumentImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.text.MessageFormat;
import java.util.Set;

/**
 * Container for static factory methods for creating various proposal computers.
 * <p>
 * Each factory method can return null if its proposals are not applicable for
 * the given request.
 */
@SuppressWarnings("restriction")
public final class ProposalComputerFactory {

  /**
   * Creates a proposal computer for widget attributes based on existing type
   * setter methods.
   */
  public static IProposalComputer newSetterAttributeProposalComputer(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {

    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
    if (node.getNodeType() != IDOMNode.ELEMENT_NODE) {
      return null;
    }

    String widgetTypeName = UiBinderXmlModelUtilities.computeQualifiedWidgetTypeName(node);
    if (widgetTypeName == null) {
      return null;
    }

    String matchString = contentAssistRequest.getMatchString();

    return new SetterAttributeProposalComputer(node, widgetTypeName,
        javaProject, matchString,
        contentAssistRequest.getReplacementBeginPosition(),
        matchString.length());
  }

  /**
   * Creates a proposal computer for autocompleting the root UiBinder element in
   * a UiBinder XML file.
   */
  public static IProposalComputer newUiBinderRootElementProposalComputer(
      ContentAssistRequest contentAssistRequest) {

    Node node = contentAssistRequest.getNode();
    if (node == null) {
      // No javadoc on getNode(), so play safe
      return null;
    }

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      if (!XmlUtilities.getRootElement(node).equals(node)) {
        // We are not the root element
        return null;
      }
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      // A completion at "<_" produces a text node
      if (node.getOwnerDocument().getDocumentElement() != null) {
        // We are not the root element
        return null;
      }
    } else {
      // Some other node type
      return null;
    }

    String newLine = ((DocumentImpl) node.getOwnerDocument()).getModel().getStructuredDocument().getLineDelimiter();

    // Come up with <ui:UiBinder xmlns:ui="...">\n_\n</ui:UiBinder> text
    String beforeCursorText = MessageFormat.format(
        "ui:{0} xmlns:ui=\"{1}\">{2}",
        UiBinderConstants.UI_BINDER_ELEMENT_NAME,
        UiBinderConstants.UI_BINDER_XML_NAMESPACE, newLine);
    String afterCursorText = MessageFormat.format("{0}</ui:{1}>", newLine,
        UiBinderConstants.UI_BINDER_ELEMENT_NAME);
    String fullText = beforeCursorText + afterCursorText;

    String matchString = contentAssistRequest.getMatchString();
    if (!fullText.startsWith(matchString)) {
      return null;
    }

    int replaceOffset = contentAssistRequest.getReplacementBeginPosition();
    return new StaticTextProposalComputer(new String[] {fullText}, matchString,
        replaceOffset, matchString.length(), replaceOffset
            + beforeCursorText.length(),
        XmlContentAssistUtilities.getImageForElement());
  }

  /**
   * Creates a proposal computer for the ui:field attribute.
   */
  public static IProposalComputer newUiFieldAttributeProposalComputer(
      ContentAssistRequest contentAssistRequest) {

    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
    if (node.getNodeType() != IDOMNode.ELEMENT_NODE) {
      return null;
    }

    if (XmlUtilities.getAttribute(node,
        UiBinderConstants.UI_BINDER_FIELD_ATTRIBUTE_NAME, true,
        UiBinderConstants.UI_BINDER_XML_NAMESPACE) != null) {
      // This element already has a ui:field attribute
      return null;
    }

    /*
     * Show the ui:field attribute on widget elements and HTML elements (without
     * a namespace)
     */
    boolean isWidget = UiBinderXmlModelUtilities.computeQualifiedWidgetTypeName(node) != null;
    boolean isLikelyHtmlElement = node.getNamespaceURI() == null;
    if (!(isWidget || isLikelyHtmlElement)) {
      return null;
    }

    return createStaticTextProposalComputerForUiAttribute(
        UiBinderConstants.UI_BINDER_FIELD_ATTRIBUTE_NAME, node,
        contentAssistRequest);
  }

  /**
   * Creates a proposal computer for ui:field attribute values (ui:field="_")
   */
  public static IProposalComputer newUiFieldProposalComputer(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {

    IDOMAttr attribute = XmlContentAssistUtilities.getAttribute(contentAssistRequest);
    if (attribute == null || attribute.getOwnerElement() == null) {
      return null;
    }

    // Ensure that we are auto-completing an ui:field attribute
    if (!attribute.equals(UiBinderXmlModelUtilities.getFieldAttribute(attribute.getOwnerElement()))) {
      return null;
    }

    IFile f = SseUtilities.resolveFile(contentAssistRequest.getDocumentRegion().getParentDocument());
    Set<IType> subtypes = UiBinderUtilities.getSubtypesFromXml(f, javaProject);
    if (subtypes.isEmpty()) {
      return null;
    }

    String attrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);

    return new UiFieldProposalComputer(
        subtypes,
        UiBinderXmlModelUtilities.computeQualifiedWidgetTypeName(attribute.getOwnerElement()),
        javaProject,
        attrValue,
        XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest),
        attrValue.length());
  }

  /**
   * Creates a proposal computer for autocompleting the java classes for the
   * <ui:import field="___" />
   */
  public static IProposalComputer newUiImportFieldProposalComputer(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject,
      String packageName) {

    IDOMAttr attribute = XmlContentAssistUtilities.getAttribute(contentAssistRequest);
    if (attribute == null || attribute.getOwnerElement() == null) {
      return null;
    }

    // Ensure we are autocompleting an 'ui:import' element attribute
    if (!UiBinderConstants.UI_BINDER_IMPORT_ELEMENT_NAME.equals(attribute.getOwnerElement().getLocalName())) {
      return null;
    }

    // Ensure we are autocompleting the 'field' attribute
    if (!attribute.equals(UiBinderXmlModelUtilities.getFieldAttribute(attribute.getOwnerElement()))) {
      return null;
    }

    String attrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);

    CodeCompleteProposalComputer ccpc = new CodeCompleteProposalComputer(
        new int[] {
            CompletionProposal.TYPE_REF, CompletionProposal.PACKAGE_REF,
            CompletionProposal.FIELD_IMPORT, CompletionProposal.FIELD_REF},
        javaProject,
        attrValue,
        XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest),
        attrValue.length(), packageName, true);

    return ccpc;
  }

  public static IProposalComputer newUiPhAttributeProposalComputer(
      ContentAssistRequest contentAssistRequest) {

    IDOMNode node = (IDOMNode) contentAssistRequest.getNode();
    if (node.getNodeType() != IDOMNode.ELEMENT_NODE) {
      return null;
    }

    if (XmlUtilities.getAttribute(node,
        UiBinderConstants.UI_BINDER_PH_ATTRIBUTE_NAME, true,
        UiBinderConstants.UI_BINDER_XML_NAMESPACE) != null) {
      // This element already has a ui:field attribute
      return null;
    }

    // Only show this on HTML elements, which should be without a namespace
    if (node.getNamespaceURI() != null) {
      return null;
    }

    return createStaticTextProposalComputerForUiAttribute(
        UiBinderConstants.UI_BINDER_PH_ATTRIBUTE_NAME, node,
        contentAssistRequest);
  }

  /**
   * Creates a proposal computer for autocompleting the UiBinder root element
   * URN import scheme. For example, <ui:UiBinder
   * xmlns:g="urn:import:com.google.gwt._" />
   */
  public static IProposalComputer newUrnImportProposalComputer(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {
    IDOMAttr attribute = XmlContentAssistUtilities.getAttribute(contentAssistRequest);
    if (attribute == null) {
      return null;
    }

    String attrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);
    if (!UiBinderXmlModelUtilities.isUrnImportAttribute(attribute)) {
      return null;
    }

    int urnImportLength = UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING.length();
    if (attrValue.length() < urnImportLength) {
      return null;
    }

    String replaceText = attrValue.substring(urnImportLength);
    int replaceOffset = XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest)
        + urnImportLength;

    return new CodeCompleteProposalComputer(
        new int[]{CompletionProposal.PACKAGE_REF}, javaProject, replaceText,
        replaceOffset, replaceText.length(), null, false);
  }

  /**
   * Creates a proposal computer for autocompleting attributes for the UiBinder
   * root element. For example, suggesting the 'urn:import:' in xmlns:g="_
   */
  public static IProposalComputer newUrnTypesProposalComputer(
      ContentAssistRequest contentAssistRequest) {
    IDOMAttr attribute = XmlContentAssistUtilities.getAttribute(contentAssistRequest);
    if (attribute == null) {
      return null;
    }

    Element element = attribute.getOwnerElement();
    String attrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);

    /*
     * Must be the root element named "UiBinder" (namespace is not checked since
     * we want to allow completion of the attribute to define this namespace.)
     */
    if (!element.getLocalName().equals(UiBinderConstants.UI_BINDER_ELEMENT_NAME)
        || element.getParentNode().getNodeType() == Node.ELEMENT_NODE
        || !attribute.getNamespaceURI().equals(
            UiBinderConstants.XMLNS_NAMESPACE)) {
      return null;
    }

    return new StaticTextProposalComputer(
        new String[] {
            UiBinderConstants.UI_BINDER_XML_NAMESPACE,
            UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING},
        attrValue,
        XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest),
        attrValue.length(), null);
  }

  /*
   * TODO: This is being used for both <ui:style> and <ui:with>, but <ui:style>
   * will likely not use this in the future (since it needs to filter on
   * CssResource)
   */
  /**
   * Creates a proposal computer for autocompleting the java classes for the
   * <ui:with ui:type="___" />
   */
  public static IProposalComputer newWithTypeProposalComputer(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {

    IDOMAttr attribute = XmlContentAssistUtilities.getAttribute(contentAssistRequest);
    if (attribute == null || attribute.getOwnerElement() == null) {
      return null;
    }

    // Ensure we are autocompleting the 'type' attribute
    if (!attribute.equals(UiBinderXmlModelUtilities.getTypeAttribute(attribute.getOwnerElement()))) {
      return null;
    }

    String attrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);

    /*
     * Even though only types are valid, we must also propose packages to get to
     * fully qualified types if the user has typed e.g. "com.".
     */
    return new CodeCompleteProposalComputer(
        new int[]{CompletionProposal.TYPE_REF, CompletionProposal.PACKAGE_REF},
        javaProject,
        attrValue,
        XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest),
        attrValue.length(), null, false);
  }

  private static IProposalComputer createStaticTextProposalComputerForUiAttribute(
      String unprefixedAttrName, Node node,
      ContentAssistRequest contentAssistRequest) {

    Node uiBinderElement = XmlUtilities.getRootElement(node);
    String fullAttrName = XmlUtilities.getName(uiBinderElement.getPrefix(),
        unprefixedAttrName);
    String proposalText = fullAttrName + "=\"\"";

    // The cursor position will be inside the quotes
    int replacementBeginPosition = contentAssistRequest.getReplacementBeginPosition();
    int cursorPosition = replacementBeginPosition + proposalText.length() - 1;
    return new StaticTextProposalComputer(new String[]{proposalText},
        contentAssistRequest.getMatchString(), replacementBeginPosition,
        contentAssistRequest.getReplacementLength(), cursorPosition,
        XmlContentAssistUtilities.getImageForAttribute());
  }

  private ProposalComputerFactory() {
  }

}
