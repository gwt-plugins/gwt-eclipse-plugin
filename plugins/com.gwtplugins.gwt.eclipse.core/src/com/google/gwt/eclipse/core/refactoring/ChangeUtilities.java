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
package com.google.gwt.eclipse.core.refactoring;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for refactoring {@link Change Changes}.
 */
public class ChangeUtilities {

  /**
   * Callback interface for
   * {@link ChangeUtilities#acceptOnChange(Change, ChangeVisitor)}.
   */
  public interface ChangeVisitor {
    void visit(Change change);
  }

  /**
   * A change that does nothing. One use of this is be returned as an undo
   * change by {@link Change#perform(IProgressMonitor)} when the client does
   * nothing but still needs to return success (if null is returned, the entire
   * refactoring is undoable).
   */
  public static class EmptyChange extends Change {

    @Override
    public Object getModifiedElement() {
      return null;
    }

    @Override
    public String getName() {
      return "Empty change";
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
      // No undo change
      return null;
    }
  }

  /**
   * Creates replacement changes for
   * {@link ChangeUtilities#replaceChange(Change, ReplacementChangeFactory)}.
   */
  public interface ReplacementChangeFactory {
    Change createChange(Change originalChange);
  }

  /**
   * Visits the given change and all its descendents.
   */
  public static void acceptOnChange(Change change, ChangeVisitor visitor) {
    if (change instanceof CompositeChange) {
      CompositeChange compositeChange = (CompositeChange) change;
      for (Change curChange : compositeChange.getChildren()) {
        acceptOnChange(curChange, visitor);
      }
    }

    visitor.visit(change);
  }

  /**
   * Destructively clones a {@link CompilationUnitChange} where the cloned
   * change will have a different compilation unit. This does not update text
   * regions or anything more than setting the change properties and moving text
   * edits from the old to new change.
   * 
   * @param originalChange the original change, this change's internal state
   *          will likely become invalid (its text edits will be moved to the
   *          new change)
   * @param cu the compilation unit to be used for the new
   *          {@link CompilationUnitChange}
   * @return the cloned {@link CompilationUnitChange}
   */
  public static CompilationUnitChange cloneCompilationUnitChangeWithDifferentCu(
      TextFileChange originalChange, ICompilationUnit cu) {
    CompilationUnitChange newChange = new CompilationUnitChange(
        originalChange.getName(), cu);

    newChange.setEdit(originalChange.getEdit());
    newChange.setEnabledShallow(originalChange.isEnabled());
    newChange.setKeepPreviewEdits(originalChange.getKeepPreviewEdits());
    newChange.setSaveMode(originalChange.getSaveMode());
    newChange.setTextType(originalChange.getTextType());

    // Copy the changes over
    TextEditUtilities.moveTextEditGroupsIntoChange(
        originalChange.getChangeGroups(), newChange);

    return newChange;
  }

  /**
   * Creates a {@link Change} from the given {@link Refactoring}.
   * 
   * @param errorLevel the error level for the condition checking which should
   *          cause the change creation to fail
   * 
   * @return a non-null {@link Change}
   * @throws RefactoringException if there was a problem creating the change
   */
  public static Change createChange(IWorkspace workspace, IProgressMonitor pm,
      Refactoring refactoring, int errorLevel) throws RefactoringException {
    CreateChangeOperation createChangeOperation = ChangeUtilities.createCreateChangeOperation(
        refactoring, errorLevel);
    try {
      workspace.run(createChangeOperation, pm);

      RefactoringStatus status = createChangeOperation.getConditionCheckingStatus();
      if (status.getSeverity() >= errorLevel) {
        // Could not create the change, throw an exception with the failure
        // status message
        throw new RefactoringException(
            status.getMessageMatchingSeverity(status.getSeverity()));
      }
    } catch (CoreException e) {
      throw new RefactoringException(e);
    }

    return createChangeOperation.getChange();
  }

  /**
   * Inserts a change at the specified index.
   * 
   * @param change the change to insert
   * @param insertIndex the index to insert at (if >= the number of children, it
   *          will be added to the end)
   * @param parentChange the new parent of the change
   */
  public static void insertChange(Change change, int insertIndex,
      CompositeChange parentChange) {
    Change[] changes = parentChange.getChildren();

    if (insertIndex >= changes.length) {
      parentChange.add(change);
    } else {
      // CompositeChange.clear does not clear the parent field on the removed
      // changes, but CompositeChange.remove does
      for (Change curChange : changes) {
        parentChange.remove(curChange);
      }

      for (int i = 0; i < changes.length; i++) {
        if (i == insertIndex) {
          parentChange.add(change);
        }
        parentChange.add(changes[i]);
      }
    }
  }

  /**
   * Attempts to merge all the text changes rooted at <code>rootChange</code>
   * into the refactoring's shared text change for each file (via
   * {@link RefactoringParticipant#getTextChange(Object)}). Any text changes
   * that are merged will be removed from their parents.
   * 
   * 
   * @param participant the participant which will be used to get the shared
   *          text change for each file
   * @param rootChange the root of the change tree that will be searched for
   *          text changes to merge into the shared text changes
   * @return true if the entire tree was merged and the root change is no longer
   *         valid
   */
  public static boolean mergeParticipantTextChanges(
      final RefactoringParticipant participant, Change rootChange) {
    final List<Change> dupChanges = new ArrayList<Change>();
    ChangeUtilities.acceptOnChange(rootChange, new ChangeVisitor() {
      public void visit(Change change) {
        if (change instanceof TextEditBasedChange) {
          TextEditBasedChange textChange = (TextEditBasedChange) change;
          TextChange existingTextChange = participant.getTextChange(textChange.getModifiedElement());
          if (existingTextChange == null) {
            return;
          }

          // Merge all changes from text change into the existing
          TextEditUtilities.moveTextEditGroupsIntoChange(
              textChange.getChangeGroups(), existingTextChange);

          dupChanges.add(textChange);
        }
      }
    });

    for (Change dupChange : dupChanges) {
      ChangeUtilities.removeChange(dupChange,
          (CompositeChange) dupChange.getParent());
    }

    return dupChanges.contains(rootChange);
  }

  /**
   * Performs the given change.
   * 
   * @return The undo change as produced by the refactoring's change.
   */
  public static Change performChange(IWorkspace workspace, IProgressMonitor pm,
      Change change) throws CoreException {
    PerformChangeOperation performChangeOperation = new PerformChangeOperation(
        change);
    try {
      workspace.run(performChangeOperation, pm);
    } finally {
      if (!performChangeOperation.changeExecuted()) {
        change.dispose();
      }
    }

    return performChangeOperation.getUndoChange();
  }

  /**
   * Removes a change and returns its old index, or -1 if it was not found.
   */
  public static int removeChange(Change change, CompositeChange parentChange) {
    Change[] changes = parentChange.getChildren();
    for (int index = 0; index < changes.length; index++) {
      if (changes[index] == change) {
        parentChange.remove(change);
        return index;
      }
    }

    return -1;
  }

  /**
   * Replaces a change with another change created by the given factory.
   * 
   * @param originalChange the original change that will be replaced in its
   *          hierarchy (no children will be copied)
   * @param replacementChangeFactory the factory that creates the replacement
   *          change
   * @return true if a replacement occurred
   */
  public static boolean replaceChange(Change originalChange,
      ReplacementChangeFactory replacementChangeFactory) {
    // The old change must not have a parent when we wrap it
    Change parentChange = originalChange.getParent();
    if (parentChange == null || !(parentChange instanceof CompositeChange)) {
      return false;
    }

    int oldIndex = ChangeUtilities.removeChange(originalChange,
        (CompositeChange) parentChange);
    if (oldIndex == -1) {
      return false;
    }

    Change newChange = replacementChangeFactory.createChange(originalChange);
    if (newChange == null) {
      return false;
    }

    ChangeUtilities.insertChange(newChange, oldIndex,
        (CompositeChange) parentChange);

    return true;
  }

  private static CreateChangeOperation createCreateChangeOperation(
      Refactoring refactoring, int errorLevel) {
    CheckConditionsOperation checkConditionsOperation = new CheckConditionsOperation(
        refactoring, CheckConditionsOperation.ALL_CONDITIONS);
    return new CreateChangeOperation(checkConditionsOperation, errorLevel);
  }
}
