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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gwt.eclipse.core.uibinder.UiBinderException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.List;

/**
 * A proposal computer that uses an evaluation context's code completion to
 * generate proposals.
 */
public class CodeCompleteProposalComputer extends AbstractJavaProposalComputer {

  public static void completeCodeUsingAlphabet(IEvaluationContext evalContext,
      ProposalGeneratingCompletionRequestor requestor)
      throws JavaModelException {
    for (char letter = 'a'; letter <= 'z'; letter++) {
      evalContext.codeComplete(String.valueOf(letter), 1, requestor);
      if (requestor.getProposals().size() > 0) {
        /*
         * Once we've hit a letter that returns at least one valid proposal,
         * bail out.
         */
        return;
      }
    }
  }

  private final int[] allowedCompletionTypes;
  private final String packageName;

  private final boolean forceFullyQualifiedFieldNames;

  /**
   * @param allowedCompletionTypes an array of allowed completion types (see
   *          constants in {@link org.eclipse.jdt.core.CompletionProposal})
   */
  public CodeCompleteProposalComputer(int[] allowedCompletionTypes,
      IJavaProject javaProject, String enteredText, int replaceOffset,
      int replaceLength, String packageName,
      boolean forceFullyQualifiedFieldNames) {
    super(javaProject, enteredText, replaceOffset, replaceLength);
    this.allowedCompletionTypes = allowedCompletionTypes;
    this.packageName = packageName;
    this.forceFullyQualifiedFieldNames = forceFullyQualifiedFieldNames;
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {
    try {
      IEvaluationContext evalContext = createEvaluationContext();

      // for <ui:import field='____'> autocomplete
      if (packageName != null) {
        evalContext.setPackageName(packageName);
      }

      ProposalGeneratingCompletionRequestor requestor = new ProposalGeneratingCompletionRequestor(
          getReplaceOffset(), getReplaceLength(), allowedCompletionTypes,
          getJavaProject(), forceFullyQualifiedFieldNames);

      if (getEnteredText().trim().length() == 0) {
        completeCodeUsingAlphabet(evalContext, requestor);
      } else {
        evalContext.codeComplete(getEnteredText(), getEnteredText().length(),
            requestor);
      }
      proposals.addAll(requestor.getProposals());
    } catch (JavaModelException e) {
      throw new UiBinderException(e);
    }
  }

}
