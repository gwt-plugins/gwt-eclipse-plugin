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

import com.google.gwt.eclipse.core.clientbundle.ClientBundleResourceDependencyIndex;
import com.google.gwt.eclipse.core.search.JavaRefIndex;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;

/**
 * Clears JavaRefIndex entries for deleted compilation units.
 */
public class GWTDeleteCompilationUnitParticipant extends DeleteParticipant {

  class ClearJavaRefIndexEntries extends Change {

    @Override
    public Object getModifiedElement() {
      return null;
    }

    @Override
    public String getName() {
      return GWTDeleteCompilationUnitParticipant.this.getName();
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
      // Clear the index entries for this compilation unit
      JavaRefIndex.getInstance().clear(deletedCu.getPath());
      ClientBundleResourceDependencyIndex.getInstance().remove(deletedCu);

      /*
       * Don't need to provide an undo change since Eclipse doesn't provide an
       * undo for deleting a file (and even if it did, we wouldn't need to
       * provide an undo change since the undeleted file would be re-indexed by
       * the JavaCompilationParticipant automatically).
       */
      return null;
    }
  }

  private ICompilationUnit deletedCu;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return new ClearJavaRefIndexEntries();
  }

  @Override
  public String getName() {
    return "Delete Java reference index entries for "
        + deletedCu.getElementName();
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof ICompilationUnit) {
      deletedCu = (ICompilationUnit) element;
      return true;
    }

    return false;
  }

}
