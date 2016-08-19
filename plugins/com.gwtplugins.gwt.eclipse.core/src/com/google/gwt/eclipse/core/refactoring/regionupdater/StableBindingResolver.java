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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * Finds the closest stable node with a binding and resolves that binding. This
 * searches the node itself followed by the ancestors for a stable node.
 */
public class StableBindingResolver {

  private final ASTNode node;

  private ASTNode stableNode;
  private IBinding stableBinding;

  public StableBindingResolver(ASTNode node) {
    this.node = node;
  }

  public IBinding getStableBinding() {
    return stableBinding;
  }

  public ASTNode getStableNode() {
    return stableNode;
  }

  /**
   * Finds the stable node and resolves the binding (or does nothing if already
   * resolved).
   * 
   * @return true if a stable node with a binding was found
   */
  public boolean resolve() {
    if (stableNode != null) {
      return true;
    }

    ASTNode curNode = node;
    
    while (curNode != null) {

      // These AST node types have bindings that later can be used to retrieve
      // an instance of these AST node types. Some other
      // AST nodes can return bindings (e.g. MethodInvocation), but that binding
      // is for the called method, so it ends up resolving to a
      // MethodDeclaration.
      if (curNode instanceof AbstractTypeDeclaration) {
        stableBinding = ((AbstractTypeDeclaration) curNode).resolveBinding();
      } else if (node instanceof MethodDeclaration) {
        stableBinding = ((MethodDeclaration) node).resolveBinding();
      }

      if (stableBinding != null) {
        stableNode = curNode;
        return true;
      }

      curNode = curNode.getParent();
    }

    // Did not find a stable node
    return false;
  }

}
