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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocumentRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods related to reading and writing XML files.
 */
@SuppressWarnings("restriction")
public final class XmlUtilities {

  /**
   * Visits the text regions within a DOM region within a DOM node. For example
   * values, see
   * {@link org.eclipse.wst.xml.core.internal.regions.DOMRegionContext}.
   */
  public interface DomTextRegionVisitor {
    /**
     * Visits the given text region.
     * 
     * @return true to continue visiting, false to prevent further visits
     */
    boolean visitDomTextRegion(IDOMNode node,
        IStructuredDocumentRegion domRegion,
        ITextRegion textRegion);
  }

  /**
   * Wraps writes to XML files belonging to an Eclipse project.
   */
  public abstract static class EditOperation {

    private final IFile file;

    public EditOperation(IFile xmlFile) {
      this.file = xmlFile;
    }

    public final void run() throws IOException, CoreException {
      run(false);
    }

    public final void run(boolean forceSave) throws IOException, CoreException {
      IDOMModel model = null;
      try {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        model = (IDOMModel) modelManager.getModelForEdit(file);
        if (model == null) {
          throw new CoreException(new Status(IStatus.ERROR,
              CorePlugin.PLUGIN_ID, "No DOM model for file: "
                  + file.getFullPath().toString()));
        }
        model.aboutToChangeModel();

        // Now do something to the model
        edit(model.getDocument());

        // Notify listeners that we've changed the model
        model.changedModel();

        // Save the model if necessary
        if (model.isDirty()) {
          if (forceSave || !model.isSharedForEdit()) {
            model.save();
          }
        }
      } finally {
        if (model != null) {
          model.releaseFromEdit();
        }
      }
    }

    protected abstract void edit(IDOMDocument document);

    protected IFile getXmlFile() {
      return file;
    }
  }

  /**
   * Visitor for {@link XmlUtilities#visitNodes(Node, NodeVisitor)}.
   */
  public interface NodeVisitor {

    /**
     * Visits the given node.
     * 
     * @param node the node to visit
     * @return true to continue visiting this node's children
     */
    boolean visitNode(Node node);
  }

  /**
   * Wraps reads of XML files belonging to an Eclipse project.
   */
  public abstract static class ReadOperation {

    private final IFile file;

    public ReadOperation(IFile xmlFile) {
      this.file = xmlFile;
    }

    public final void run() throws CoreException, IOException {
      IDOMModel model = null;
      try {
        IModelManager modelManager = StructuredModelManager.getModelManager();
        model = (IDOMModel) modelManager.getModelForRead(file);
        if (model == null) {
          throw new CoreException(new Status(IStatus.ERROR,
              CorePlugin.PLUGIN_ID, "No DOM model for file: "
                  + file.getFullPath().toString()));
        }

        read(model.getDocument());
      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }
    }

    protected IFile getXmlFile() {
      return file;
    }

    protected abstract void read(IDOMDocument document);
  }

  /**
   * Gets the attribute from an element.
   * 
   * @param node the node to search
   * @param attributeName the attribute name (without a namespace prefix)
   * @param requireNamespace whether to match the namespace
   * @param namespaceUri if <code>requireNamespace</code> is true, only
   *          attributes with this namespace will be considered (if null, the
   *          attribute must not have a namespace)
   * @return the attribute, or null
   */
  public static Node getAttribute(Node node, String attributeName, boolean requireNamespace, String namespaceUri) {
    NamedNodeMap attributes = node.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attribute = attributes.item(i);
      if (requireNamespace
          && !JavaUtilities.equalsWithNullCheck(namespaceUri,
              attribute.getNamespaceURI())) {
        continue;
      }

      if (attribute.getLocalName().equals(attributeName)) {
        return attribute;
      }
    }

    return null;
  }

  /**
   * Gets the value of some attribute.
   * 
   * @see XmlUtilities#getAttribute(Node, String, boolean, String)
   */
  public static String getAttributeValue(Node node, String attributeName,
      boolean requireNamespace, String namespaceUri) {
    Node attribute = getAttribute(node, attributeName, requireNamespace,
        namespaceUri);
    return attribute != null ? attribute.getNodeValue() : null;
  }

  /**
   * Gets a region for the attribute's value (without the quotes).
   */
  public static IRegion getAttributeValueRegion(IDOMAttr attribute) {
    String attrValue = attribute.getValueRegionText();
    if (attrValue == null) {
      return null;
    }
    
    int offset = attribute.getValueRegionStartOffset();
    int length = attrValue.length();

    // Strip off the quotes
    if (isXmlQuote(attrValue.charAt(0))) {
      offset++;
      length--;
    }
    if (isXmlQuote(attrValue.charAt(attrValue.length() - 1))) {
      length--;
    }

    return new Region(offset, length);
  }

  /**
   * Finds the text regions (absolute positions within document) for the
   * element's start and end tags.
   * 
   * @param element the element whose tag regions are being found
   * @param includePrefix whether to include the namespace prefix in the regions
   * @return a non-null list of regions, with the start tag's region before the
   *         end tag's region (if there are two distinct tags)
   */
  public static List<IRegion> getElementTagRegions(IDOMElement element,
      final boolean includePrefix) {
    final List<IRegion> regions = new ArrayList<IRegion>();
    final IStructuredDocument doc = element.getStructuredDocument();
    DomTextRegionVisitor visitor = new DomTextRegionVisitor() {
      public boolean visitDomTextRegion(IDOMNode node,
          IStructuredDocumentRegion domRegion, ITextRegion textRegion) {
        try {
          if (DOMRegionContext.XML_TAG_NAME.equals(textRegion.getType())) {
            // DOM region is relative to document, text region is relative to
            // the DOM
            int nameOffset = domRegion.getStartOffset() + textRegion.getStart();
            int nameLength = textRegion.getTextLength();

            if (!includePrefix) {
              // Lose the namespace prefix
              int unprefixedOffset = XmlUtilities.getUnprefixedOffset(doc.get(
                  nameOffset, nameLength));
              nameOffset += unprefixedOffset;
              nameLength -= unprefixedOffset;
            }

            regions.add(new Region(nameOffset, nameLength));

            return false;
          }
        } catch (BadLocationException e) {
          // Ignore
        }

        return true;
      }
    };

    XmlUtilities.visitDomTextRegions(element,
        element.getFirstStructuredDocumentRegion(), visitor);
    XmlUtilities.visitDomTextRegions(element,
        element.getLastStructuredDocumentRegion(), visitor);

    return regions;
  }

  public static String getElementText(Element element) {
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE) {
        return ((Text) child).getNodeValue();
      }
    }
    return "";
  }

  /**
   * Forms the name of a node from the prefix and unprefixed name.
   * 
   * @param prefix optional prefix
   * @param unprefixedName the node name (without a prefix)
   * @return if a prefix is non-null, the result will be prefix:name, otherwise
   *         name
   */
  public static String getName(String prefix, String unprefixedName) {
    return prefix != null ? prefix + ":" + unprefixedName : unprefixedName;
  }

  /**
   * Returns the prefix of the given name, or null if there is no prefix.
   */
  public static String getPrefix(String name) {
    int colonLocation = name.indexOf(':');
    return colonLocation == -1 ? null : name.substring(0, colonLocation);
  }

  /**
   * Returns the root element.
   * 
   * @param node an XML node
   * @return the root element of the tree containing the given element
   */
  public static Node getRootElement(Node node) {
    return node.getOwnerDocument().getDocumentElement();
  }

  /**
   * Returns the unprefixed name.
   * 
   * @param name the name, with or without a prefix
   * @return the unprefixed name
   */
  public static String getUnprefixed(String name) {
    int colonLocation = name.indexOf(':');
    return colonLocation == -1 ? name : name.substring(colonLocation + 1);
  }

  /**
   * Returns the offset of the name without a prefix.
   * 
   * @param name the name, with or without a prefix
   * @return the offset of the name without a prefix
   */
  public static int getUnprefixedOffset(String name) {
    int colonIndex = name.indexOf(':');
    return colonIndex == -1 ? 0 : colonIndex + 1; 
  }

  public static void setElementText(IDOMDocument document, Element element,
      String text) {
    Node oldText = element.getFirstChild();
    Text newText = document.createTextNode(text);
    if (oldText != null) {
      element.replaceChild(newText, oldText);
    } else {
      element.appendChild(newText);
    }
  }

  /**
   * Invokes the visitor for each DOM text region on the given node. This will
   * not traverse into the text regions of descendant nodes.
   * 
   * @return false if there was an error trying to read the DOM text regions
   */
  public static boolean visitDomTextRegions(IDOMNode node,
      DomTextRegionVisitor visitor) {
    IStructuredDocumentRegion region = node.getFirstStructuredDocumentRegion();

    return visitDomTextRegions(node, region, visitor);
  }

  public static boolean visitDomTextRegions(IDOMNode node,
      IStructuredDocumentRegion region, DomTextRegionVisitor visitor) {
    while (region != null) {
      if (!(region instanceof BasicStructuredDocumentRegion)) {
        return false;
      }

      BasicStructuredDocumentRegion basicRegion = (BasicStructuredDocumentRegion) region;
      ITextRegionList regions = basicRegion.getRegions();
      for (int i = 0; i < regions.size(); i++) {
        if (!visitor.visitDomTextRegion(node, region, regions.get(i))) {
          return true;
        }
      }

      region = region.getNext();
    }

    return true;
  }

  /**
   * Visits all the nodes rooted at and including <code>node</code>. All nodes
   * includes both elements and attributes.
   * 
   * @param node the node where the visit will begin (if null, method returns
   *          right away)
   */
  public static void visitNodes(Node node, NodeVisitor nodeVisitor) {
    if (node == null) {
      return;
    }

    if (!nodeVisitor.visitNode(node)) {
      return;
    }
    
    NamedNodeMap attributes = node.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        visitNodes(attributes.item(i), nodeVisitor);
      }
    }
    Node childNode = node.getFirstChild();
    while (childNode != null) {
      visitNodes(childNode, nodeVisitor);
      childNode = childNode.getNextSibling();
    }
  }

  /**
   * Returns whether the character can be used as a quote in XML (e.g. for
   * attribute values).
   */
  private static boolean isXmlQuote(char c) {
    return c == '\'' || c == '"';
  }

  private XmlUtilities() {
    // Not instantiable
  }
}
