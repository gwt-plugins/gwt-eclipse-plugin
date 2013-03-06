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

import com.google.gdt.eclipse.core.contentassist.XmlContentAssistUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderXmlModelUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.ElExpressionFirstFragmentComputer;
import com.google.gwt.eclipse.core.uibinder.contentassist.IProposalComputer;
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;
import com.google.gwt.eclipse.core.uibinder.contentassist.ElExpressionFirstFragmentComputer.ElExpressionFirstFragment;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A proposal computer that generates proposals when the user is within an EL
 * expression. For example, the user is entering:
 * 
 * <pre>&lt;div class=&quot;{_</pre>
 * 
 * or
 * 
 * <pre>&lt;div class=&quot;{style._</pre>
 */
@SuppressWarnings("restriction")
public class ElExpressionProposalComputer implements IProposalComputer {

  public static ElExpressionProposalComputer newUsingContentAssistRequest(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {

    String currentAttrValue = XmlContentAssistUtilities.getAttributeValueUsingMatchString(contentAssistRequest);
    int currentAttrValueOffset = XmlContentAssistUtilities.getAttributeValueOffset(contentAssistRequest);

    int elExpressionOffsetInAttrValue = UiBinderUtilities.getOpenElExpressionContentsOffset(currentAttrValue);
    if (elExpressionOffsetInAttrValue == -1) {
      // Not in EL expression, this computer is useless
      return null;
    }

    return new ElExpressionProposalComputer(
        currentAttrValue.substring(elExpressionOffsetInAttrValue),
        currentAttrValueOffset + elExpressionOffsetInAttrValue,
        contentAssistRequest.getNode().getOwnerDocument(), javaProject);
  }

  private final Document document;
  private final String elExpressionContents;
  private final int elExpressionContentsOffsetInDocument;
  private final IJavaProject javaProject;

  public ElExpressionProposalComputer(String elContents,
      int elContentsOffsetInDocument, Document document,
      IJavaProject javaProject) {
    this.document = document;
    this.elExpressionContents = elContents;
    this.elExpressionContentsOffsetInDocument = elContentsOffsetInDocument;
    this.javaProject = javaProject;
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {

    Set<ElExpressionFirstFragment> matchedFirstFragments = findMatchingFirstFragments();
    if (matchedFirstFragments.size() == 0) {
      // No matching first fragments
      return;
    }

    if (elExpressionContents.indexOf('.') == -1) {
      // Completing the first fragment
      computeProposalsForFirstFragment(proposals, matchedFirstFragments);

    } else {
      // Completing subsequent domain-specific fragments
      computeProposalsForDomainSpecificFragments(proposals,
          matchedFirstFragments);
    }
  }

  private void computeProposalsForDomainSpecificFragments(
      List<ICompletionProposal> proposals,
      Set<ElExpressionFirstFragment> matchedFirstFragments)
      throws UiBinderException {

    for (ElExpressionFirstFragment firstFragment : matchedFirstFragments) {
      IDOMElement node = (IDOMElement) firstFragment.getNode();
      IType javaType;

      if (UiBinderXmlModelUtilities.isStyleElement(node)) {
        // This is a <ui:style> element
        computeProposalsForStyleElement(proposals, node);
      } else if ((javaType = UiBinderXmlModelUtilities.resolveElementToJavaType(
          node, javaProject)) != null) {
        // This is either a <ui:with> element or sth like <g:Button>
        computeProposalsForJavaType(proposals, javaType);
      }
    }
  }

  private void computeProposalsForFirstFragment(
      List<ICompletionProposal> proposals,
      Set<ElExpressionFirstFragment> matchedFirstFragments) {

    for (ElExpressionFirstFragment matchedFirstFragment : matchedFirstFragments) {
      proposals.add(new ReplacementCompletionProposal(
          matchedFirstFragment.getValue(),
          elExpressionContentsOffsetInDocument, elExpressionContents.length()));
    }
  }

  private void computeProposalsForJavaType(List<ICompletionProposal> proposals,
      IType javaType) throws UiBinderException {
    JavaElExpressionProposalComputer proposalComputer = JavaElExpressionProposalComputer.newUsingJavaType(
        javaType, getAllButFirstFragment(), getAllButFirstFragmentOffset());
    if (proposalComputer != null) {
      proposalComputer.computeProposals(proposals);
    }
  }

  private void computeProposalsForStyleElement(
      List<ICompletionProposal> proposals, Node node) throws UiBinderException {
    CssSelectorProposalComputer computer = CssSelectorProposalComputer.newUsingUiBinderStyleElement(
        (IDOMElement) node, getAllButFirstFragment(),
        getAllButFirstFragmentOffset(), javaProject);
    if (computer == null) {
      return;
    }

    computer.computeProposals(proposals);
  }

  private Set<ElExpressionFirstFragment> findMatchingFirstFragments() {
    Set<ElExpressionFirstFragment> possibleFirstFragments = ElExpressionFirstFragmentComputer.compute(
        document, null, javaProject, null).getFirstFragments();
    
    String enteredFirstFragment = getFirstFragment();
    Iterator<ElExpressionFirstFragment> it = possibleFirstFragments.iterator();
    while (it.hasNext()) {
      ElExpressionFirstFragment possibleFirstFragment = it.next();
      if (!possibleFirstFragment.getValue().startsWith(enteredFirstFragment)) {
        it.remove();
      }
    }

    return possibleFirstFragments;
  }

  private String getAllButFirstFragment() {
    // +1 for the fragment separator (.)
    return elExpressionContents.substring(getFirstFragment().length() + 1);
  }

  private int getAllButFirstFragmentOffset() {
    return elExpressionContentsOffsetInDocument
        + (elExpressionContents.length() - getAllButFirstFragment().length());
  }

  private String getFirstFragment() {
    int periodPos = elExpressionContents.indexOf('.');
    return periodPos == -1 ? elExpressionContents
        : elExpressionContents.substring(0, periodPos);
  }
}
