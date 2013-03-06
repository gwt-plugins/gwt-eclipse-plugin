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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Abstract class for building refactoring changes from a contribution ID. This
 * class allows clients to get the intermediate objects along the path to build
 * a change (for example, the {@link Refactoring}) and allows subclasses to
 * properly configure objects (for example, the Descriptor).
 * TODO: determine correct Descriptor.
 * 
 * @param <D> the type of {@link RefactoringDescriptor} that the subclass
 *          requires to build its change
 */
public abstract class ChangeBuilder<D extends RefactoringDescriptor> {

  private final Class<D> descriptorType;
  private final String contributionId;
  private Refactoring refactoring;
  private Change change;
  private final IWorkspace workspace;

  /**
   * Constructs the builder.
   * 
   * @param descriptorType the class of the expected descriptor created from the
   *          contribution ID
   * @param contributionId the contribution ID used to create the change
   */
  public ChangeBuilder(Class<D> descriptorType, String contributionId,
      IWorkspace workspace) {
    this.descriptorType = descriptorType;
    this.contributionId = contributionId;
    this.workspace = workspace;
  }

  /**
   * Creates a change (or returns a previously created change).
   */
  public Change createChange() throws RefactoringException {
    if (change != null) {
      return change;
    }

    if (refactoring == null) {
      refactoring = createRefactoring();
    }

    change = ChangeUtilities.createChange(workspace,
        new NullProgressMonitor(), refactoring, RefactoringStatus.FATAL);

    return change;
  }

  /**
   * Creates a refactoring (or returns a previously created refactoring).
   */
  public Refactoring createRefactoring() throws RefactoringException {
    if (refactoring != null) {
      return refactoring;
    }

    RefactoringStatus status = new RefactoringStatus();
    D descriptor = createDescriptor();
    try {
      refactoring = descriptor.createRefactoring(status);
    } catch (CoreException e) {
      throw new RefactoringException(e);
    }
    if (refactoring == null) {
      throw new RefactoringException(
          String.format(
              "The refactoring descriptor (%s) was unable to create a refactoring.",
              descriptor.getClass().getSimpleName()));
    }
    if (status.hasError()) {
      throw new RefactoringException(
          status.getMessageMatchingSeverity(RefactoringStatus.ERROR));
    }

    return refactoring;
  }

  /**
   * Override this to properly configure the descriptor.
   */
  protected abstract void configureDescriptor(D descriptor);

  /**
   * Validates the given descriptor. The base implementation ensures it is the
   * proper type.
   */
  protected void validateDescriptor(RefactoringDescriptor descriptor)
      throws RefactoringException {
    if (!descriptorType.isInstance(descriptor)) {
      throw new RefactoringException(
          String.format(
              "The refactoring descriptor (%s) is an unexpected type (expecting %s).",
              descriptor.getClass().getSimpleName(),
              descriptorType.getSimpleName()));
    }
  }

  @SuppressWarnings("unchecked")
  private D createDescriptor() throws RefactoringException {
    RefactoringDescriptor refactoringDescriptor = RefactoringUtilities.createDescriptor(contributionId);
    validateDescriptor(refactoringDescriptor);
    D descriptor = (D) refactoringDescriptor;
    configureDescriptor(descriptor);
    return descriptor;
  }

}
