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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A simple completion proposal that does a text replacement. This differs from
 * {@link org.eclipse.jface.text.contentassist.CompletionProposal} by returning
 * a relevance and thus participating in the ranking of proposals.
 */
public class ReplacementCompletionProposal extends AbstractCompletionProposal {

  /**
   * The cursor will be positioned after the replaced text.
   */
  public static final int DEFAULT_CURSOR_POSITION = Integer.MIN_VALUE;

  /**
   * Creates a {@link ReplacementCompletionProposal} with the given text and
   * position, but attempts to copy additional information from an existing
   * {@link ICompletionProposal}.
   * 
   * @param text the replacement text
   * @param offset the offset to the text to replace
   * @param length the length of the text to replace
   * @param existingCompletionProposal the existing {@link ICompletionProposal}
   *          to copy info from
   * @return an {@link ReplacementCompletionProposal}
   */
  public static ReplacementCompletionProposal fromExistingCompletionProposal(
      String text, int offset, int length,
      ICompletionProposal existingCompletionProposal) {
    if (existingCompletionProposal != null) {
      return new ReplacementCompletionProposal(text, offset, length,
          ReplacementCompletionProposal.DEFAULT_CURSOR_POSITION,
          existingCompletionProposal.getAdditionalProposalInfo(),
          existingCompletionProposal.getDisplayString(),
          existingCompletionProposal.getImage());
    } else {
      return new ReplacementCompletionProposal(text, offset, length);
    }
  }

  private final String text;
  private final int offset;
  private final int length;
  private final int cursorPosition;

  public ReplacementCompletionProposal(String text, int offset, int length) {
    this(text, offset, length, DEFAULT_CURSOR_POSITION, null, null, null);
  }

  public ReplacementCompletionProposal(String text, int offset, int length,
      int cursorPosition) {
    this(text, offset, length, cursorPosition, null, null, null);
  }

  public ReplacementCompletionProposal(String text, int offset, int length,
      int cursorPosition, String additionalProposalInfo, String displayString,
      Image image) {
    super(additionalProposalInfo, displayString != null ? displayString : text,
        image);

    this.text = text;
    this.offset = offset;
    this.length = length;
    this.cursorPosition = cursorPosition;
  }

  @Override
  public Point getSelection(IDocument document) {
    if (cursorPosition == DEFAULT_CURSOR_POSITION) {
      if (length > 0) {
        // If the text being replaced's length is greater than 0, then returning
        // null will place the cursor at the end of the replaced text as
        // described in javadoc
        return null;
      } else {
        // If the length is 0, it keeps the cursor before the replaced text. We
        // explicitly tell it the new cursor position.
        return new Point(offset + text.length(), 0);
      }
    } else {
      return new Point(cursorPosition, 0);
    }
  }

  @Override
  public void onApply(IDocument document, ITextViewer viewer) throws BadLocationException {
    document.replace(offset, length, text);
  }

}
