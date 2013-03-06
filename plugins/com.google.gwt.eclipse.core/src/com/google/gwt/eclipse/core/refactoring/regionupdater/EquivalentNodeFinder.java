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
package com.google.gwt.eclipse.core.refactoring.regionupdater;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.List;

/**
 * Finds a node that is "equivalent" to a node in another AST. In addition to
 * the rules of the given AST matcher, equivalence is measured by the subtree of
 * the two nodes matching, each of its ancestor's subtree matching, and finally
 * maintaining it and each of its ancestors maintaining the same order.
 */
public class EquivalentNodeFinder {

  private static final int NOT_FROM_LIST = -2;

  private final ASTNode originalNode;

  private final ASTNode ancestorNode;

  private final ASTNode newAncestorNode;

  private final ASTMatcher matcher;

  public EquivalentNodeFinder(ASTNode node, ASTNode ancestorNode,
      ASTNode newAncestorNode, ASTMatcher matcher) {
    this.originalNode = node;
    this.ancestorNode = ancestorNode;
    this.newAncestorNode = newAncestorNode;
    this.matcher = matcher;
  }

  /**
   * Finds the equivalent AST node, or null if one could not be found.
   */
  public ASTNode find() {
    final ASTNode foundNode[] = new ASTNode[1];
    newAncestorNode.accept(new ASTVisitor(true) {
      @Override
      public void preVisit(ASTNode visitedNode) {
        if (foundNode[0] != null) {
          // Already found a result, do not search further
          return;
        }

        if (!treesMatch(originalNode, visitedNode, ancestorNode,
            newAncestorNode, matcher)) {
          // Keep searching
          return;
        }

        foundNode[0] = visitedNode;

        // We are done
      }
    });

    return foundNode[0];
  }

  @SuppressWarnings("unchecked")
  private int getIndex(ASTNode node) {
    StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
    if (locationInParent != null && locationInParent.isChildListProperty()) {
      List<ASTNode> parentsChildren = (List) node.getParent().getStructuralProperty(
          locationInParent);
      if (parentsChildren != null) {
        return parentsChildren.indexOf(node);
      }
    }

    // The node is not contained within a list-based property on the parent
    return NOT_FROM_LIST;
  }

  private boolean indexMatches(ASTNode node, ASTNode otherNode) {
    return getIndex(node) == getIndex(otherNode);
  }

  private boolean treesMatch(ASTNode node, ASTNode otherNode,
      ASTNode topmostAncestorNode, ASTNode topmostNewAncestorNode,
      ASTMatcher matcher) {

    while (true) {
      if ((node == topmostAncestorNode) != (otherNode == topmostNewAncestorNode)) {
        // One has reached the end and the other has not
        return false;
      }

      if (node == topmostAncestorNode) {
        // They have both reached an end, and everything went smoothly
        return true;
      }

      if (!node.subtreeMatch(matcher, otherNode)) {
        // Subtrees do not match
        return false;
      }

      if (!indexMatches(node, otherNode)) {
        // The index of each does not match
        return false;
      }

      node = node.getParent();
      otherNode = otherNode.getParent();
    }
  }
}
