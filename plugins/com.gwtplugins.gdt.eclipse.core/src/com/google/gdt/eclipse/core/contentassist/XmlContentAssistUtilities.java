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
package com.google.gdt.eclipse.core.contentassist;

import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.NamedNodeMap;

/**
 * Utility methods for XML-related content assist.
 * 
 * @see JavaContentAssistUtilities
 */
@SuppressWarnings("restriction")
public final class XmlContentAssistUtilities {

  /**
   * Returns the attribute from a content assist request from an attribute
   * completion.
   * 
   * @param request the content assist request, in the context of an attribute
   *          completion
   * @return the XML DOM attribute, or null
   */
  public static IDOMAttr getAttribute(ContentAssistRequest request) {
    int valueRegionStartOffset = request.getStartOffset();

    NamedNodeMap attributes = request.getNode().getAttributes();
    
    if (attributes == null) {
      return null;
    }
    
    for (int i = 0; i < attributes.getLength(); i++) {
      IDOMAttr attribute = (IDOMAttr) attributes.item(i);
      if (attribute.getValueRegionStartOffset() == valueRegionStartOffset) {
        return attribute;
      }
    }

    return null;
  }

  /**
   * Returns the offset to the attribute value fetched via
   * {@link #getAttributeValueUsingMatchString(ContentAssistRequest)} (this will
   * only work if that method returns a valid value).
   * 
   * @param contentAssistRequest the content assist request, in the context of
   *          an attribute completion
   * @return the offset to the attribute value, relative to the replacement
   *         begin position
   */
  public static int getAttributeValueOffset(
      ContentAssistRequest contentAssistRequest) {
    // Add one for the leading ' or " that is included in the match string
    return contentAssistRequest.getReplacementBeginPosition() + 1;
  }

  /**
   * Returns the attribute value of the content assist request from an attribute
   * completion.
   * <p>
   * This uses the match string to get the value, so it will work even if the
   * attribute is not well-formed and a DOM element does not exist.
   * 
   * @param contentAssistRequest the content assist request, in the context of
   *          an attribute completion
   * @return the attribute value (empty string if the value is empty)
   */
  public static String getAttributeValueUsingMatchString(
      ContentAssistRequest contentAssistRequest) {
    String currentAttrValue = contentAssistRequest.getMatchString();
    if (currentAttrValue == null || currentAttrValue.length() < 1) {
      return "";
    }

    // Take off the leading ' or "
    return currentAttrValue.substring(1);
  }

  public static Image getImageForAttribute() {
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
  }

  public static Image getImageForElement() {
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_ELEMENT);
  }

  public static Image getImageForLocalVariable() {
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_LOCAL_VARIABLE);
  }

  private XmlContentAssistUtilities() {
  }
}
