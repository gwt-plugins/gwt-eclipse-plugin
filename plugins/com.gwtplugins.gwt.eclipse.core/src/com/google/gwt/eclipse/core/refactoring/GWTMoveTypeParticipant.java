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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;

/**
 * Moves Java types referenced in GWT specific regions.
 */
abstract class GWTMoveTypeParticipant extends MoveParticipant {

  private GWTTypeRefactoringSupport refactoringSupport;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws OperationCanceledException {
    return new RefactoringStatus();
  }

  public final GWTTypeRefactoringSupport getRefactoringSupport() {
    if (refactoringSupport == null) {
      refactoringSupport = createRefactoringSupport();
    }
    return refactoringSupport;
  }

  protected abstract GWTTypeRefactoringSupport createRefactoringSupport();

  @Override
  protected boolean initialize(Object element) {
    if (!(element instanceof IType)) {
      return false;
    }

    Object destObj = getArguments().getDestination();
    if (!(destObj instanceof IJavaElement)) {
      return false;
    }

    IType oldType = (IType) element;
    IJavaElement dest = (IJavaElement) destObj;

    // TODO: support moving inner classes to other declaring types?

    // We only support moving top-level types to other packages, for now
    if (dest instanceof IPackageFragment && oldType.getDeclaringType() == null) {
      IPackageFragment newPckgFragment = (IPackageFragment) dest;

      // Initialize the refactoring support properties
      GWTRefactoringSupport support = getRefactoringSupport();
      support.setOldElement(oldType);

      // For some reason, getUpdateReferences() returns false when the
      // destination is the default package, even if the checkbox in the UI for
      // 'update references' is checked.
      support.setUpdateReferences(getArguments().getUpdateReferences()
          || newPckgFragment.isDefaultPackage());

      /*
       * Figure out the new type after the move. To this, we use the old
       * compilation unit and old type name, rooted under the destination
       * package fragment. It's okay to do this before the actual move takes
       * place, because the getXXX methods we're using are all handle-only
       * methods. That is, the IType we end up setting as the new element would
       * return false if we called exists() on it at this point.
       */
      ICompilationUnit oldCu = oldType.getCompilationUnit();
      ICompilationUnit newCu = newPckgFragment.getCompilationUnit(oldCu.getElementName());
      IType newType = newCu.getType(oldType.getElementName());
      support.setNewElement(newType);

      return true;
    }

    return false;
  }

}
