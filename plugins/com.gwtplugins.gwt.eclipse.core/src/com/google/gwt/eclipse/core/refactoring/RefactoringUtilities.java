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

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

/**
 * Utility methods dealing with refactoring. See {@link ChangeUtilities} for
 * refactoring change-related utility methods.
 */
public final class RefactoringUtilities {

  /**
   * Creates a {@link RefactoringDescriptor} from a
   * {@link RefactoringContribution} of the given ID.
   * 
   * @return a non-null {@link RefactoringDescriptor}
   * @throws RefactoringException if there was a problem creating the descriptor
   */
  public static RefactoringDescriptor createDescriptor(String contributionId)
      throws RefactoringException {
    RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(contributionId);
    if (contribution == null) {
      throw new RefactoringException(
          String.format("The refactoring contribution (%s) is not available.",
              contributionId));
    }

    RefactoringDescriptor refactoringDescriptor = contribution.createDescriptor();
    if (refactoringDescriptor == null) {
      throw new RefactoringException(
          String.format(
              "A descriptor could not be created from the refactoring contribution (%s).",
              contribution.getClass().getSimpleName()));
    }

    return refactoringDescriptor;
  }

  /* Non-instantiable */
  private RefactoringUtilities() {
  }
}
