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

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.platform.jdt.dom.NodeFinder;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.refactoring.ChangeUtilities;
import com.google.gwt.eclipse.core.refactoring.RefactoringException;
import com.google.gwt.eclipse.core.refactoring.TextEditUtilities;
import com.google.gwt.eclipse.core.refactoring.ChangeUtilities.ReplacementChangeFactory;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A change visitor that finds all compilation unit changes and wraps each with
 * a {@link RegionUpdaterChange} that will update the compilation unit change's
 * text edit regions at perform-time.
 */
public class RegionUpdaterChangeWeavingVisitor implements
    ChangeUtilities.ChangeVisitor {

  private final ASTMatcher astMatcher;
  private final ReferenceUpdater referenceUpdater;

  /**
   * Creates the visitor.
   * 
   * @param astMatcher a relaxed matcher that is knowledgeable about the
   *          refactoring (for example, it matches two nodes even if the names
   *          don't match -- given the new node has the new name and the old
   *          node has the old name)
   * @param referenceUpdater a reference updater that can update references from
   *          the old matcher to the new
   */
  public RegionUpdaterChangeWeavingVisitor(ASTMatcher astMatcher,
      ReferenceUpdater referenceUpdater) {
    this.astMatcher = astMatcher;
    this.referenceUpdater = referenceUpdater;
  }

  public void visit(Change change) {
    if (!(change instanceof TextFileChange)) {
      return;
    }

    // Ideally we would cast to CompilationUnitChange, but we settle on
    // TextFileChange since it is public API in E3.3 and E3.4
    final TextFileChange textChange = (TextFileChange) change;
    Object modifiedElement = textChange.getModifiedElement();
    if (!(modifiedElement instanceof ICompilationUnit)) {
      return;
    }

    // Get an AST compilation unit
    final CompilationUnit compilationUnit = JavaASTUtils.parseCompilationUnit((ICompilationUnit) modifiedElement);

    final List<RegionUpdater> regionUpdaters = new ArrayList<RegionUpdater>();

    for (final TextEditBasedChangeGroup changeGroup : textChange.getChangeGroups()) {
      for (TextEdit textEdit : changeGroup.getTextEdits()) {
        textEdit.accept(new TextEditVisitor() {

          @Override
          public boolean visit(ReplaceEdit edit) {
            // Find the node for this text edit
            ASTNode node = NodeFinder.perform(compilationUnit,
                edit.getOffset(), edit.getLength());
            try {
              // Create a region updater for this text edit
              regionUpdaters.add(RegionUpdaterFactory.newRegionUpdater(edit,
                  node, referenceUpdater, astMatcher));
              // Keep searching
              return true;

            } catch (RefactoringException e) {
              // We will not be able to update the region of this text edit,
              // play safe and remove it
              GWTPluginLog.logWarning("Could not create a region updater for a text edit: "
                  + e.getMessage());
              removeTextEdit(textChange, changeGroup, edit);
              // Don't visit children of this edit
              return false;
            }
          }

          @Override
          public boolean visitNode(TextEdit edit) {
            // This will get called for unhandled edits. We remove these edits
            // from the change since they are unhandled.
            removeTextEdit(textChange, changeGroup, edit);

            // Don't visit its children
            return false;
          }

          private void removeTextEdit(final TextFileChange textChange,
              final TextEditBasedChangeGroup changeGroup, TextEdit edit) {
            if (!TextEditUtilities.removeTextEdit(edit,
                changeGroup.getTextEditGroup(), textChange)) {
              GWTPluginLog.logWarning("Could not remove a text edit from a change.");
            }
          }
        });
      }
    }

    // This change has meaningful text edits, wrap the change so
    // its text edits' regions gets updated at perform-time
    if (regionUpdaters.size() > 0) {
      wrapAndReplaceChange(change, regionUpdaters);
    }
  }

  private void wrapAndReplaceChange(Change change,
      final List<RegionUpdater> regionUpdaters) {
    ChangeUtilities.replaceChange(change, new ReplacementChangeFactory() {
      public Change createChange(Change originalChange) {
        Change wrappedChange = new RegionUpdaterChange("",
            (TextFileChange) originalChange, regionUpdaters, referenceUpdater);
        return wrappedChange;
      }
    });
  }

}
