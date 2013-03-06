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
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.ArrayList;
import java.util.List;

/**
 * A completion requestor that transforms the Java {@link CompletionProposal}s
 * to {@link ICompletionProposal}s.
 */
public class ProposalGeneratingCompletionRequestor extends CompletionRequestor {

  private final boolean forceFullyQualifiedFieldNames;

  private final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

  private final int replaceOffset;

  private final int replaceLength;

  private final IJavaProject javaProject;

  private CompletionContext context;

  public ProposalGeneratingCompletionRequestor(int replaceOffset,
      int replaceLength, int[] allowedCompletionTypes,
      IJavaProject javaProject, boolean forceFullyQualifiedFieldNames) {

    this.replaceOffset = replaceOffset;
    this.replaceLength = replaceLength;
    this.javaProject = javaProject;
    this.forceFullyQualifiedFieldNames = forceFullyQualifiedFieldNames;
    // TODO: Once we drop 3.3 support, this will simple be a super(true) call
    // These correspond to the protected CompletionProposal.FIRST_KIND and
    // CompletionProposal.LAST_KIND values
    ignoreAll();

    for (int i = 0; i < allowedCompletionTypes.length; i++) {
      setIgnored(allowedCompletionTypes[i], false);
    }
  }

  @Override
  public void accept(CompletionProposal proposal) {
    ICompletionProposal createdProposal = createProposal(proposal);
    if (createdProposal != null) {
      proposals.add(createdProposal);
    }
  }

  @Override
  public void acceptContext(CompletionContext context) {
    this.context = context;
  }

  public CompletionContext getContext() {
    return context;
  }

  public List<ICompletionProposal> getProposals() {
    return proposals;
  }

  public int getReplaceLength() {
    return replaceLength;
  }

  public int getReplaceOffset() {
    return replaceOffset;
  }

  protected ICompletionProposal createProposal(CompletionProposal javaProposal) {
    String completion = String.valueOf(javaProposal.getCompletion());
    int kind = javaProposal.getKind();
    if (kind == CompletionProposal.TYPE_REF) {
      // Make sure it is fully qualified
      completion = JavaContentAssistUtilities.getFullyQualifiedTypeName(javaProposal);
    }

    if (forceFullyQualifiedFieldNames
        && (kind == CompletionProposal.FIELD_IMPORT || kind == CompletionProposal.FIELD_REF)) {
      char[] decSig = javaProposal.getDeclarationSignature();
      if (decSig != null && decSig.length > 2) {
        // declaration signatures for objects are like Ljava.lang.String;, so lop off first
        // and last chars
        completion = new String(decSig, 1, decSig.length - 2) + "."
            + new String(javaProposal.getCompletion());
        completion = completion.replace('$', '.');
      }
    }

    ICompletionProposal jdtCompletionProposal = JavaContentAssistUtilities.getJavaCompletionProposal(
        javaProposal, context, javaProject);
    return ReplacementCompletionProposal.fromExistingCompletionProposal(completion,
        replaceOffset, replaceLength, jdtCompletionProposal);
  }

  private void ignoreAll() {
    int[] ignoredKinds = new int[] {
        CompletionProposal.ANONYMOUS_CLASS_DECLARATION,
        CompletionProposal.FIELD_REF, CompletionProposal.KEYWORD,
        CompletionProposal.LABEL_REF, CompletionProposal.LOCAL_VARIABLE_REF,
        CompletionProposal.METHOD_REF, CompletionProposal.METHOD_DECLARATION,
        CompletionProposal.PACKAGE_REF, CompletionProposal.TYPE_REF,
        CompletionProposal.VARIABLE_DECLARATION,
        CompletionProposal.POTENTIAL_METHOD_DECLARATION,
        CompletionProposal.METHOD_NAME_REFERENCE,
        CompletionProposal.ANNOTATION_ATTRIBUTE_REF,
        CompletionProposal.JAVADOC_FIELD_REF,
        CompletionProposal.JAVADOC_METHOD_REF,
        CompletionProposal.JAVADOC_TYPE_REF,
        CompletionProposal.JAVADOC_VALUE_REF,
        CompletionProposal.JAVADOC_PARAM_REF,
        CompletionProposal.JAVADOC_BLOCK_TAG,
        CompletionProposal.JAVADOC_INLINE_TAG, CompletionProposal.FIELD_IMPORT,
        CompletionProposal.METHOD_IMPORT, CompletionProposal.TYPE_IMPORT};

    for (int kind : ignoredKinds) {
      setIgnored(kind, true);
    }
  }

}