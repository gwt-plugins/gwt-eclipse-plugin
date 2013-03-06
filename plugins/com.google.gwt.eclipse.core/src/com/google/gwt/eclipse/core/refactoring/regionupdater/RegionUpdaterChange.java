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
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.refactoring.ChangeUtilities;
import com.google.gwt.eclipse.core.refactoring.RefactoringException;
import com.google.gwt.eclipse.core.refactoring.TextEditUtilities;
import com.google.gwt.eclipse.core.refactoring.ChangeUtilities.EmptyChange;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/*
 * Implementation note: TextFileChange is used as the type for the
 * compilationUnitChange (here and in other classes) because
 * CompilationUnitChange is unstable across Eclipse versions, and this class
 * references a change created by the refactoring framework (so we can't use our
 * stable CompilationUnitChange from the platform plugin). Instead, we use its
 * superclass, TextFileChange, which is stable across all Eclipse versions.
 * Furthermore, the differences between these classes is not important for us
 * (CompilationUnitChange affects ICompilationUnit, which we check for.)
 */
/**
 * A refactoring change that wraps a compilation unit change and ensures, that
 * at perform-time, the text edit regions are up to date with any modifications
 * that happened between the time of the compilation unit change creation and
 * perform-time. It relies on {@link RegionUpdater} to ensure the text edits'
 * regions have been updated.
 */
public class RegionUpdaterChange extends CompositeChange {

  private TextFileChange compilationUnitChange;
  private final List<RegionUpdater> regionUpdaters;
  private final ReferenceUpdater referenceUpdater;

  /**
   * Creates a region updater change.
   * 
   * @param compilationUnitChange the compilation unit change whose text edits
   *          will be updated
   * @param regionUpdaters a list of updaters for text edits in the compilation
   *          unit change
   * @param referenceUpdater aids in finding the corresponding AST nodes for
   *          those that have been refactored (e.g. if FooService was renamed to
   *          BarService, this instance would pass that knowledge)
   */
  public RegionUpdaterChange(String name, TextFileChange compilationUnitChange,
      List<RegionUpdater> regionUpdaters, ReferenceUpdater referenceUpdater) {
    super(name);
    assert compilationUnitChange.getModifiedElement() instanceof ICompilationUnit;

    this.compilationUnitChange = compilationUnitChange;
    this.regionUpdaters = regionUpdaters;
    this.referenceUpdater = referenceUpdater;

    // Do not show this in previews
    markAsSynthetic();

    // Wrap the original compilation unit
    add(compilationUnitChange);
  }

  /*
   * This will be called right before the wrapped compilation unit change's
   * perform. We ensure its text edits are up-to-date.
   */
  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    // Get the original change's compilation unit
    ICompilationUnit cuInterface = (ICompilationUnit) compilationUnitChange.getModifiedElement();
    // Ensure it is still the right one
    ICompilationUnit updatedCuInterface = referenceUpdater.getUpdatedCompilationUnit(cuInterface);

    if (updatedCuInterface != cuInterface) {
      // The compilation unit has changed, clone the old change with the new
      // compilation unit (we cannot change the compilation unit on the existing
      // change -- API limitation)
      remove(compilationUnitChange);
      compilationUnitChange = ChangeUtilities.cloneCompilationUnitChangeWithDifferentCu(
          compilationUnitChange, updatedCuInterface);
      add(compilationUnitChange);
    }

    if (updatedCuInterface == null) {
      GWTPluginLog.logWarning(String.format(
          "The compilation unit (%s) is invalid and a corresponding valid compilation unit could not be found.",
          compilationUnitChange.getModifiedElement()));
      // We do not want to proceed with this particular change. If we return
      // null, the entire refactoring will not be undoable. Instead, return an
      // empty undo change.
      return new EmptyChange();
    }

    CompilationUnit cu = JavaASTUtils.parseCompilationUnit(updatedCuInterface);

    for (RegionUpdater regionUpdater : regionUpdaters) {
      ReplaceEdit originalEdit = regionUpdater.getOriginalEdit();
      TextEdit newEdit = null;
      try {
        newEdit = regionUpdater.createUpdatedEdit(cu);
      } catch (RefactoringException e) {
        GWTPluginLog.logError(e,
            "Could not get an updated edit region, skipping this edit.");
      }

      if (newEdit == null) {
        removeTextEditFromCuChange(originalEdit);
        continue;
      }

      if (!TextEditUtilities.replaceTextEdit(
          TextEditUtilities.getTextEditGroups(compilationUnitChange.getChangeGroups()),
          originalEdit, newEdit)) {
        GWTPluginLog.logWarning("Could not update the text edit region of a change, skipping this edit.");
        removeTextEditFromCuChange(originalEdit);
        continue;
      }
    }

    // Calls the compilation unit change's perform
    return super.perform(pm);
  }

  private void removeTextEditFromCuChange(TextEdit edit) {
    TextEditUtilities.removeTextEdit(edit, TextEditUtilities.findTextEditGroup(
        compilationUnitChange, edit), compilationUnitChange);
  }
}
