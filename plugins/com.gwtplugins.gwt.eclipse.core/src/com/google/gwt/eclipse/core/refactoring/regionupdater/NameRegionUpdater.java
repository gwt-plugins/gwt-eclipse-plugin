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

import com.google.gwt.eclipse.core.refactoring.RefactoringException;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * A region updater which updates name nodes. Most type/method refactorings
 * create text changes which affect only the name nodes. For example, a method
 * rename would affect the {@link Name} child node of a
 * {@link org.eclipse.jdt.core.dom.MethodDeclaration} node, and also the
 * {@link Name} child node of {@link org.eclipse.jdt.core.dom.MethodInvocation}
 * nodes for the callers of the renamed method.
 */
public class NameRegionUpdater extends StableNodeBasedRegionUpdater<Name> {

  public NameRegionUpdater(ReplaceEdit originalEdit,
      ReferenceUpdater referenceUpdater, Name originalNode, ASTMatcher matcher)
      throws RefactoringException {
    super(originalEdit, referenceUpdater, originalNode, matcher);
  }

  @Override
  protected ReplaceEdit createUpdatedEditForEquivalentNode(Name equivalentNode)
      throws RefactoringException {
    return new ReplaceEdit(equivalentNode.getStartPosition(),
        equivalentNode.getLength(), getOriginalEdit().getText());
  }

}
