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

import com.google.gdt.eclipse.core.contentassist.JavaContentAssistUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.List;

/**
 * A proposal computer that generates proposals for EL expressions that refer to
 * methods/fields inside java types.
 * <p>
 * Some examples:
 * <ul>
 * <li>{someIdentifier.method.method.field.methodThatReturnsString}. The methods
 * are no arg.
 * <li>{someIdentifier.fieldOfStringType}
 * </ul>
 * (someIdentifier is defined by a ui:field attribute in the UiBinder XML.)
 */
public class JavaElExpressionProposalComputer extends
    AbstractJavaProposalComputer {

  /**
   * This takes field, method, and type references and offers proposals based on
   * the rules of the expression language supported by UiBinder.
   */
  private class JavaElExpressionCompletionRequestor extends
      ProposalGeneratingCompletionRequestor {

    /**
     * The fully qualified type name of {@link #javaType}.
     */
    private final String qualifiedTypeName;

    public JavaElExpressionCompletionRequestor(int... allowedCompletionTypes) {
      super(JavaElExpressionProposalComputer.this.getReplaceOffset(),
          JavaElExpressionProposalComputer.this.getReplaceLength(),
          allowedCompletionTypes, getJavaProject(), false);

      qualifiedTypeName = javaType.getFullyQualifiedName('.');
    }

    @Override
    protected ICompletionProposal createProposal(CompletionProposal javaProposal) {
      char[] signature = javaProposal.getSignature();
      String replacementText = null;
      int relevanceAdjustment = 0;

      if (javaProposal.getKind() != CompletionProposal.METHOD_REF) {
        return null;
      }

      if (Signature.getParameterCount(signature) != 0) {
        // Only zero-arg methods are allowed
        return null;
      }

      String returnType = String.valueOf(Signature.getReturnType(signature));
      if (Signature.SIG_VOID.equals(returnType)) {
        // Methods with void return type are not allowed
        return null;
      }

      relevanceAdjustment += getRelevanceAdjustmentForMyTypeAndDeclarationType(
          returnType, javaProposal.getDeclarationSignature());
      replacementText = String.valueOf(javaProposal.getName());

      IJavaCompletionProposal jdtCompletionProposal = JavaContentAssistUtilities.getJavaCompletionProposal(
          javaProposal, getContext(), getJavaProject());
      ReplacementCompletionProposal proposal = ReplacementCompletionProposal.fromExistingCompletionProposal(
          replacementText, getReplaceOffset(), getReplaceLength(),
          jdtCompletionProposal);

      if (relevanceAdjustment != 0) {
        proposal.setRelevance(proposal.getRelevance() + relevanceAdjustment);
      }

      return proposal;
    }

    private int getRelevanceAdjustmentForMyTypeAndDeclarationType(
        String typeSignature, char[] declarationTypeSignature) {
      int relevanceAdjustment = 0;

      if (Signature.toString(typeSignature).equals("java.lang.String")) {
        // Boost String return types, since strings are the typical final return
        // type for the field ref
        relevanceAdjustment++;
      }

      String qualifiedDeclaringType = String.valueOf(Signature.toCharArray(declarationTypeSignature));
      if (qualifiedDeclaringType.equals(qualifiedTypeName)) {
        // Prefer stuff from our type rather than our supertypes
        relevanceAdjustment++;
      }

      return relevanceAdjustment;
    }
  }

  public static JavaElExpressionProposalComputer newUsingJavaType(
      IType javaType, String followingText, int followingTextOffset) {

    // This is the type referenced by the first fragment of the EL expression
    IType snippetReceivingType = javaType;
    String snippet = followingText;
    int snippetOffset = followingTextOffset;

    int lastPeriodIndex = followingText.lastIndexOf('.');
    if (lastPeriodIndex != -1) {
      String textUntilLastPeriod = followingText.substring(0, lastPeriodIndex);
      // Find the right-most resolvable type, and update our snippet to be the
      // the text following the fragment that corresponds to that type
      snippetReceivingType = UiBinderUtilities.resolveJavaElExpression(
          javaType, textUntilLastPeriod, null);
      if (snippetReceivingType == null) {
        return null;
      }

      snippet = followingText.substring(lastPeriodIndex + 1);
      snippetOffset = followingTextOffset + lastPeriodIndex + 1;
    }

    return new JavaElExpressionProposalComputer(javaType.getJavaProject(),
        snippet, snippetOffset, snippet.length(), snippetReceivingType);
  }

  private final IType javaType;

  public JavaElExpressionProposalComputer(IJavaProject javaProject,
      String enteredText, int replaceOffset, int replaceLength, IType javaType) {
    super(javaProject, enteredText, replaceOffset, replaceLength);
    this.javaType = javaType;
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {
    IEvaluationContext evalContext = createEvaluationContext();

    try {
      // Get proposals for instance methods and static methods by treating the
      // Java type as an instance.
      // Since the type does not necessarily have to have a zero-arg
      // constructor, we use a local variable in the completion request below
      JavaElExpressionCompletionRequestor requestor = new JavaElExpressionCompletionRequestor(
          CompletionProposal.METHOD_REF);
      String snippet = javaType.getFullyQualifiedName('.') + " tmpVar; tmpVar."
          + getEnteredText();
      evalContext.codeComplete(snippet, snippet.length(), requestor);
      proposals.addAll(requestor.getProposals());

    } catch (JavaModelException e) {
      throw new UiBinderException(e);
    }
  }

  @Override
  public IEvaluationContext createEvaluationContext() {
    IEvaluationContext evalContext = super.createEvaluationContext();
    evalContext.setPackageName(javaType.getPackageFragment().getElementName());
    return evalContext;
  }

}
