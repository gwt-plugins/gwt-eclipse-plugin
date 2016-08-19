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
package com.google.gwt.eclipse.core.uibinder.sse.css;

import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelLoader;
import com.google.gwt.eclipse.core.uibinder.text.IndentationFixingCompletionProposal;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces content assist proposals for inlined CSS (CSS within a document of
 * another type.)
 * <p>
 * The WST framework has a notion of "embedded CSS" which is constrained to CSS
 * within HTML (it contains hard-coded checks such as the parent element must be
 * "<style>"). To prevent confusion, the GPE embedded CSS is termed
 * "inlined CSS".
 */
@SuppressWarnings("restriction")
public class InlinedCssContentAssistProcessor implements
    IContentAssistProcessor {

  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
      int offsetInDoc) {

    IDocument doc = viewer.getDocument();
    IStructuredDocument structuredDoc = (IStructuredDocument) doc;
    ITypedRegion partition = SseUtilities.getPartition(structuredDoc,
        offsetInDoc);
    if (partition == null) {
      GWTPluginLog.logWarning("Could not generate CSS proposals due to problem getting partition of offset.");
      return null;
    }

    int offsetInExtractedDoc = offsetInDoc - partition.getOffset();
    CssExtractor extractor = CssExtractor.extract(doc, partition.getOffset(),
        partition.getLength(), new CssResourceAwareModelLoader());
    if (extractor == null) {
      GWTPluginLog.logWarning("Could not extract CSS document to generate CSS proposals.");
      return null;
    }

    IndexedRegion indexedNode = extractor.getCssModel().getIndexedRegion(
        offsetInExtractedDoc);
    if (indexedNode == null) {
      indexedNode = (IndexedRegion) extractor.getCssDocument();
    }

    ICompletionProposal[] proposals;
    try {
      proposals = CssProposalArrangerCaller.getProposals(offsetInExtractedDoc,
          (ICSSNode) indexedNode, partition.getOffset(), (char) 0);

      List<ICompletionProposal> newProposals = new ArrayList<ICompletionProposal>();
      for (ICompletionProposal proposal : proposals) {
        newProposals.add(new IndentationFixingCompletionProposal(proposal));
      }

      return newProposals.toArray(new ICompletionProposal[newProposals.size()]);

    } catch (Throwable e) {
      GWTPluginLog.logWarning(e,
          "Could not generate CSS proposals due to failed call to CSS proposal arranger.");
      return null;
    }
  }

  /*
   * Derived from CSSContentAssistProcessor's implementation.
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer,
      int offset) {
    return null;
  }

  /*
   * Derived from CSSContentAssistProcessor's implementation.
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return null;
  }

  /*
   * Derived from CSSContentAssistProcessor's implementation.
   */
  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  /*
   * Derived from CSSContentAssistProcessor's implementation.
   */
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  /*
   * Derived from CSSContentAssistProcessor's implementation.
   */
  public String getErrorMessage() {
    return null;
  }

}
