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

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.refactoring.RefactoringException;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * A region updater that looks for a stable node that will be easily
 * identifiable before and after the refactoring occurs. The original node and
 * its ancestors will be searched for a node that provides a solid binding. If
 * one cannot be found, the compilation unit is used. When it is time to update
 * regions, it uses that stable node as the root of a search for an equivalent
 * node of the original node.
 * 
 * @param <N> the type of the original AST node
 */
public abstract class StableNodeBasedRegionUpdater<N extends ASTNode> extends
    RegionUpdater {

  private final N originalNode;
  private final ASTMatcher matcher;

  private final boolean isCompilationUnitTheStableNode;

  private final ASTNode originalStableNode;

  /**
   * Only set if the stable node is not the compilation unit. Do not get this
   * value directly, use {@link #getUpdatedStableBindingKey()}.
   */
  private final String originalStableBindingKey;

  public StableNodeBasedRegionUpdater(ReplaceEdit originalEdit,
      ReferenceUpdater referenceUpdater, N originalNode, ASTMatcher matcher)
      throws RefactoringException {
    super(originalEdit, referenceUpdater);

    this.originalNode = originalNode;
    this.matcher = matcher;

    // Look for a stable node
    StableBindingResolver stableBindingResolver = new StableBindingResolver(
        originalNode);
    if (stableBindingResolver.resolve()) {
      // Found one!
      isCompilationUnitTheStableNode = false;
      originalStableNode = stableBindingResolver.getStableNode();
      originalStableBindingKey = stableBindingResolver.getStableBinding().getKey();
    } else {
      // In some cases (like an import declaration), there isn't a stable
      // binding
      // node we can use, so instead we use the compilation unit
      isCompilationUnitTheStableNode = true;
      originalStableNode = originalNode.getRoot();
      originalStableBindingKey = null;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public ReplaceEdit createUpdatedEdit(CompilationUnit newCu)
      throws RefactoringException {
    // Find the equivalent node in the new AST for the stable node
    ASTNode equivalentStableNode = findEquivalentStableNode(newCu);
    // Find the equivalent node for the original node, starting the search at
    // the equivalent stable node
    EquivalentNodeFinder equivalentNodeFinder = new EquivalentNodeFinder(
        originalNode, originalStableNode, equivalentStableNode, matcher);
    N eqNode = (N) equivalentNodeFinder.find();

    if (eqNode == null) {
      GWTPluginLog.logWarning("Could not find equivalent node.");
      return null;
    }

    return createUpdatedEditForEquivalentNode(eqNode);
  }

  public N getOriginalNode() {
    return originalNode;
  }

  public ASTNode getOriginalStableNode() {
    return originalStableNode;
  }

  protected abstract ReplaceEdit createUpdatedEditForEquivalentNode(
      N equivalentNode) throws RefactoringException;

  protected ASTNode findEquivalentStableNode(CompilationUnit newCu)
      throws RefactoringException {

    if (isCompilationUnitTheStableNode) {
      // The compilation unit is the stable node
      return newCu;

    } else {
      // Find the equivalent stable node using its binding key in this AST
      // compilation unit
      ASTNode equivalentStableNode = newCu.findDeclaringNode(getUpdatedStableBindingKey());
      if (equivalentStableNode == null) {
        throw new RefactoringException(
            "Could not find the equivalent stable node in the post-refactoring AST.");
      }

      return equivalentStableNode;
    }
  }

  private String getUpdatedStableBindingKey() {
    return originalStableBindingKey != null
        ? getReferenceUpdater().getUpdatedBindingKey(originalStableBindingKey)
        : null;
  }

}
