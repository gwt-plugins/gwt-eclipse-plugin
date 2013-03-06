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

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameCompilationUnitProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

/**
 * Renames Java elements referenced in GWT specific regions.
 */
@SuppressWarnings("restriction")
abstract class GWTRenameParticipant extends RenameParticipant {

  private GWTRefactoringSupport refactoringSupport;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws OperationCanceledException {
    return new RefactoringStatus();
  }

  public final GWTRefactoringSupport getRefactoringSupport() {
    if (refactoringSupport == null) {
      refactoringSupport = createRefactoringSupport();
    }
    return refactoringSupport;
  }

  protected abstract GWTRefactoringSupport createRefactoringSupport();

  /**
   * If we're renaming types, the initialize(Object) method will expect a
   * RenameTypeProcessor, so if we happened to be invoked by the
   * RenameCompilationUnitProcessor (e.g., the user did an Alt-Shift-R directly
   * on the .java file in the Package Explorer), we need to return the
   * processor's child RenameTypeProcessor instead of the original processor.
   */
  protected JavaRenameProcessor getRenameProcessor() {
    JavaRenameProcessor processor = (JavaRenameProcessor) getProcessor();
    if (processor instanceof RenameCompilationUnitProcessor) {
      return ((RenameCompilationUnitProcessor) processor).getRenameTypeProcessor();
    }

    return processor;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IType || element instanceof IMethod
        || element instanceof IField) {
      GWTRefactoringSupport support = getRefactoringSupport();

      IMember oldMember = (IMember) element;
      support.setOldElement(oldMember);

      try {
        /*
         * We can't trust our RenameArgument's getNewName() to always return the
         * correct new name. When the user sets the new name in the Rename
         * refactoring dialog, clicks Next to go to the Preview page, then
         * clicks Back and changes the new name, getNewName() returns the
         * original name, not the updated one. This appears to be a bug in the
         * JDT, which affects other built-in rename participants such as
         * BreakpointRenameTypeParticipant. It does not affect the core JDT
         * refactorings, though, since they are executed directly from
         * JavaRenameRefactoring and not as loaded rename participants.
         * 
         * The workaround here is to instead go directly to the refactoring
         * processor and query it for the new element, which will have the right
         * name.
         * 
         * TODO: file this as a JDT bug?
         */
        JavaRenameProcessor processor = getRenameProcessor();
        IJavaElement newElement = (IJavaElement) processor.getNewElement();
        IType declaringType = oldMember.getDeclaringType();
        String newElementName = newElement.getElementName();
        /*
         * Compute the new method by using the declaring type of the old method.
         * Otherwise when a RenameVirtualMethodProcessor instance is passed in,
         * we will end up looking at the topmost declaration of the method
         * instead of the one we actually want because
         * RenameVirtualMethodProcessor.getNewElement() actually points to the
         * topmost declaration.
         */
        if (element instanceof IField) {
          newElement = declaringType.getField(newElementName);
        } else if (element instanceof IMethod) {
          IMethod method = (IMethod) newElement;
          newElement = declaringType.getMethod(newElementName,
              method.getParameterTypes());
        } else {
          assert (element instanceof IType);
        }

        support.setNewElement(newElement);
        support.setUpdateReferences(getArguments().getUpdateReferences());

        return true;
      } catch (CoreException e) {
        GWTPluginLog.logError(e);
      }
    }

    return false;
  }

}