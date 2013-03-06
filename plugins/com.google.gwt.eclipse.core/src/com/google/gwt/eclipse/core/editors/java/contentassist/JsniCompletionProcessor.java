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
package com.google.gwt.eclipse.core.editors.java.contentassist;

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proposes completions for Java references in JSNI methods.
 */
public class JsniCompletionProcessor implements IContentAssistProcessor {

  private static final char[] ACTIVATION_CHARS = {'.', ':'};

  // Match a complete or partial method/field/ctor reference
  private static final Pattern MEMBER_REF_START = Pattern.compile("(\\.?)@([^:]+)::(\\w*)$");

  // Match a complete or partial package or type reference
  private static final Pattern PACKAGE_OR_TYPE_REF_START = Pattern.compile("@([\\w\\.]+)$");

  private static final String THIS_DOT = "this.";
  
  /**
   * Generate Java code suitable for generating ctor completions.
   */
  private static String getJavaCtorRefFragment(String qualifiedTypeName) {
    StringBuilder sb = new StringBuilder();
    sb.append("new ");
    sb.append(Signature.getSimpleName(qualifiedTypeName));
    sb.append("(");
    return sb.toString();
  }

  /**
   * Generate Java code suitable for generating method/field completions.
   */
  private static String getJavaMethodOrFieldRefFragment(
      String qualifiedTypeName, String memberNameFragment, boolean isStatic) {
    if (isStatic) {
      return qualifiedTypeName + "." + memberNameFragment;
    }

    return "this." + memberNameFragment;
  }

  private final ICompilationUnit cu;

  public JsniCompletionProcessor(ICompilationUnit cu) {
    this.cu = cu;
  }

  @SuppressWarnings("unchecked")
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
      int offset) {
    IJavaCompletionProposal[] proposals = null;

    try {
      // Extract the contents of the current line, up to the cursor position
      IDocument document = viewer.getDocument();
      int line = document.getLineOfOffset(offset);
      int lineStartOffset = document.getLineOffset(line);
      String jsLine = document.get(lineStartOffset, offset - lineStartOffset);

      // handle a corner case where the "this." might appear on the line before,
      // e.g. from line wrapping. Note that this fix doesn't work if there are
      // comments on the line with the "this.".
      String lineBefore = document.get(document.getLineOffset(line - 1),
          document.getLineLength(line - 1)).trim();
      if (THIS_DOT.equals(lineBefore)) {
        jsLine = THIS_DOT + jsLine.trim();
      }

      // See if there are any available package or type proposals here
      proposals = computePackageAndTypeProposals(jsLine, lineStartOffset,
          offset);

      if (proposals == null) {
        // Now check for any method/field/ctor proposals
        proposals = computeMemberProposals(jsLine, lineStartOffset, offset);
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
    }

    if (proposals != null) {
      // Use the default JDT completion comparator, which sorts the proposals
      // based on their relevance scores.
      Arrays.sort(proposals, new CompletionProposalComparator());
    }

    return proposals;
  }

  public IContextInformation[] computeContextInformation(ITextViewer viewer,
      int offset) {
    return null;
  }

  public char[] getCompletionProposalAutoActivationCharacters() {
    return ACTIVATION_CHARS;
  }

  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  public String getErrorMessage() {
    return null;
  }

  /**
   * Collects the proposals generated at the end of the specified snippet of
   * Java code, scoped to the specific type.
   */
  private IJavaCompletionProposal[] codeComplete(String qualifiedTypeName,
      String snippet, CompletionProposalCollector requestor)
      throws JavaModelException {
    IJavaCompletionProposal[] proposals = new IJavaCompletionProposal[0];

    IType type = cu.getJavaProject().findType(qualifiedTypeName);
    if (type != null) {
      // This can always be false, since the set of available completions
      // (static vs. instance) depends on the JSNI ref itself, not the modifiers
      // on the method in which it is defined.
      boolean isStatic = false;

      // Have the JDT generate completions in the context of the type, but at
      // an unspecified location (source offset -1), since the real position
      // is inside a Java comment block, which is not allowed by codeComplete.
      type.codeComplete(snippet.toCharArray(), -1, snippet.length(),
          new char[0][0], new char[0][0], new int[0], isStatic, requestor);
      proposals = requestor.getJavaCompletionProposals();
    }

    return proposals;
  }

  private IJavaCompletionProposal[] computeCtorProposals(
      String qualifiedTypeName, CompletionProposalCollector requestor)
      throws JavaModelException {
    String javaCtorFragment = getJavaCtorRefFragment(qualifiedTypeName);
    return codeComplete(qualifiedTypeName, javaCtorFragment, requestor);
  }

  private IJavaCompletionProposal[] computeMemberProposals(String js,
      int lineStartOffset, int cursorOffset) throws JavaModelException {
    Matcher matcher = MEMBER_REF_START.matcher(js);
    if (!matcher.find()) {
      // Bail if we're not inside a JSNI Java member reference
      return null;
    }

    // Extract from the match the type name and the (maybe partial) member name
    boolean isStatic = !(matcher.group(1).equals("."));
    String qualifiedTypeName = matcher.group(2);
    String memberNameFragment = matcher.group(3);
    int refOffset = matcher.start(3) + lineStartOffset;
    int refLength = cursorOffset - refOffset;

    List<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();

    // Add method and field proposals
    IJavaCompletionProposal[] methodAndFieldProposals = computeMethodAndFieldProposals(
        qualifiedTypeName, memberNameFragment, isStatic,
        JsniCompletionProposalCollector.createMemberProposalCollector(cu,
            refOffset, refLength, qualifiedTypeName));
    proposals.addAll(Arrays.asList(methodAndFieldProposals));

    // Add constructor proposals if appropriate
    if (isStatic
        && JsniCompletionProposal.JSNI_CTOR_METHOD.startsWith(memberNameFragment)) {
      IJavaCompletionProposal[] ctorProposals = computeCtorProposals(
          qualifiedTypeName,
          JsniCompletionProposalCollector.createMemberProposalCollector(cu,
              refOffset, refLength, qualifiedTypeName));
      proposals.addAll(Arrays.asList(ctorProposals));
    }

    return proposals.toArray(new IJavaCompletionProposal[0]);
  }

  private IJavaCompletionProposal[] computeMethodAndFieldProposals(
      String qualifiedTypeName, String memberNameFragment, boolean isStatic,
      CompletionProposalCollector requestor) throws JavaModelException {
    String javaMethodOrFieldFragment = getJavaMethodOrFieldRefFragment(
        qualifiedTypeName, memberNameFragment, isStatic);

    // Compute the proposals as if we were inside the type we're referencing,
    // since JSNI allows access to even private Java members (violator pattern).
    return codeComplete(qualifiedTypeName, javaMethodOrFieldFragment, requestor);
  }

  private IJavaCompletionProposal[] computePackageAndTypeProposals(String js,
      int lineStartOffset, int cursorOffset) throws JavaModelException {
    Matcher matcher = PACKAGE_OR_TYPE_REF_START.matcher(js);
    if (!matcher.find()) {
      // Bail if we're not inside a JSNI Java package/type reference
      return null;
    }

    // Extract from the match the (maybe partial) package/type reference
    int refOffset = matcher.start(1) + lineStartOffset;
    int refLength = cursorOffset - refOffset;
    String partialRef = matcher.group(1);

    CompletionProposalCollector requestor = JsniCompletionProposalCollector.createPackageAndTypeProposalCollector(
        cu, refOffset, refLength);
    IEvaluationContext evalContext = createEvaluationContext();
    evalContext.codeComplete(partialRef, partialRef.length(), requestor);
    return requestor.getJavaCompletionProposals();
  }

  private IEvaluationContext createEvaluationContext()
      throws JavaModelException {
    IJavaProject project = cu.getJavaProject();
    IEvaluationContext evalContext = project.newEvaluationContext();
    String pckgName = cu.getPackageDeclarations()[0].getElementName();
    // Scope evaluation to the containing package
    evalContext.setPackageName(pckgName);
    return evalContext;
  }

}
