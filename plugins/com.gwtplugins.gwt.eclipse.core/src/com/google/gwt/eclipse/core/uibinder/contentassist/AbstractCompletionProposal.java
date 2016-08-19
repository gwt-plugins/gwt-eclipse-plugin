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
package com.google.gwt.eclipse.core.uibinder.contentassist;

import com.google.gdt.eclipse.core.SseUtilities;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;

/**
 * Abstract class that contains some common behavior across all of our
 * completion proposal implementations.
 */
@SuppressWarnings("restriction")
public abstract class AbstractCompletionProposal implements
    ICompletionProposal, IJavaCompletionProposal, ICompletionProposalExtension4 {

  /**
   * A relevance higher than all Eclipse-generated proposals but still allows
   * for ranking between {@link AbstractCompletionProposal}s.
   */
  public static final int DEFAULT_RELEVANCE = Integer.MAX_VALUE / 2;

  private int relevance = DEFAULT_RELEVANCE;

  private final String additionalProposalInfo;

  private final String displayString;

  private final Image image;

  public AbstractCompletionProposal(String additionalProposalInfo,
      String displayString, Image image) {
    this.displayString = displayString;
    this.image = image;
    this.additionalProposalInfo = additionalProposalInfo;
  }

  public void apply(IDocument document) {
    /*
     * IDocument.replace throws an annoying BadLocationException that's
     * typically ignored. We catch and ignore here so each client does not deal
     * with the try-catch ugliness.
     */
    try {
      /*
       * Originally, we implemented ICompletionProposalExtension2 to be given
       * the ITextViewer. However, this also forces us to implement the validate
       * method, which isn't trivial. Because of this, we get the active
       * ITextViewer from global state.
       */
      onApply(document, getActiveViewer(document));
    } catch (BadLocationException e) {
      // Ignore (like CompletionProposal does)
    }
  }

  public String getAdditionalProposalInfo() {
    return additionalProposalInfo;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return displayString;
  }

  public Image getImage() {
    return image;
  }

  public int getRelevance() {
    /*
     * Unfortunately, the IRelevanceCompletionProposal does not have a defined
     * upper bound for this value like IJavaCompletionProposal does. We
     * implement the IJavaCompletionProposal since it is the public API. We
     * break contract by exceeding the IJavaCompletionProposal's range since we
     * eventually get compared to IRelevanceCompletionProposals which have high
     * ranking (> 100) and we need to be ranked higher.
     */
    return relevance;
  }

  public Point getSelection(IDocument document) {
    /*
     * Default behavior is to place the cursor at the end of inserted/replaced
     * text
     */
    return null;
  }

  public boolean isAutoInsertable() {
    return true;
  }

  /**
   * Called when it is time to apply the autocompletion.
   * 
   * @param document the document
   * @param viewer the possibly-null text viewer
   * @throws BadLocationException
   */
  public abstract void onApply(IDocument document, ITextViewer viewer)
      throws BadLocationException;

  public void setRelevance(int relevance) {
    this.relevance = relevance;
  }

  private StructuredTextViewer getActiveViewer(IDocument document) {
    StructuredTextViewer viewer = SseUtilities.getActiveTextViewer();
    return viewer.getDocument().equals(document) ? viewer : null;
  }

}
