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
package com.google.gwt.eclipse.core.uibinder.sse.css;

import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNodeList;

/**
 * Utility methods for interacting with a CSS model.
 */
@SuppressWarnings("restriction")
public final class CssModelUtilities {

  /**
   * Visitor for CSS nodes.
   */
  public interface CssNodeVisitor {
    void visitCssNode(ICSSNode cssNode);
  }

  /**
   * Visits all the nodes rooted at the given node.
   */
  public static void visitCssNodes(ICSSNode cssNode, CssNodeVisitor visitor) {
    visitor.visitCssNode(cssNode);

    ICSSNodeList childNodes = cssNode.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      visitCssNodes(childNodes.item(i), visitor);
    }
  }

  private CssModelUtilities() {
  }
}
