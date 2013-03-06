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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Factory for creating region updaters.
 */
public class RegionUpdaterFactory {

  /**
   * Creates a new region updater for the given text edit contained within the
   * given node.
   * 
   * @param originalEdit the text edit
   * @param node the most-specific node that contains the text edit
   * @param referenceUpdater an reference updater knowledgeable about the
   *          refactoring that is taking place
   * @param matcher an AST matcher knowledgeable about refactoring that is
   *          taking place
   * @return a region updater instance for the given text edit
   * @throws RefactoringException if there was an error creating a region
   *           updater
   */
  public static RegionUpdater newRegionUpdater(ReplaceEdit originalEdit,
      ASTNode node, ReferenceUpdater referenceUpdater, ASTMatcher matcher)
      throws RefactoringException {

    if (node instanceof Name) {
      return new NameRegionUpdater(originalEdit, referenceUpdater, (Name) node,
          matcher);

    } else if (node instanceof TextElement) {
      return new TextElementRegionUpdater(originalEdit, referenceUpdater,
          (TextElement) node, matcher);
    }

    throw new RefactoringException("This AST node type is not supported");
  }

}
