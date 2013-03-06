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
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * A region updater for text elements (e.g. comments).
 */
public class TextElementRegionUpdater extends
    StableNodeBasedRegionUpdater<TextElement> {

  public TextElementRegionUpdater(ReplaceEdit originalEdit,
      ReferenceUpdater referenceUpdater, TextElement originalNode,
      ASTMatcher matcher) throws RefactoringException {
    super(originalEdit, referenceUpdater, originalNode, matcher);
  }

  @Override
  protected ReplaceEdit createUpdatedEditForEquivalentNode(
      TextElement equivalentNode) throws RefactoringException {
    // Adjust the offset based on the difference between start positions of
    // equivalent nodes
    int newOffset = getOriginalEdit().getOffset()
        + equivalentNode.getStartPosition()
        - getOriginalNode().getStartPosition();

    return new ReplaceEdit(newOffset, getOriginalEdit().getLength(),
        getOriginalEdit().getText());
  }
}
