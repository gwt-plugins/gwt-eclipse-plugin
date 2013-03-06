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
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;

import java.util.List;

/**
 * A proposal computer that proposes static text (which must start with the
 * user's entered text).
 */
public class StaticTextProposalComputer extends AbstractProposalComputer {

  private final String[] textProposals;

  /**
   * The image that will be shown for all proposals.
   */
  private final Image image;

  private final boolean isCursorPositionValid;
  private final int cursorPosition;

  public StaticTextProposalComputer(String[] proposals, String enteredText,
      int replaceOffset, int replaceLength, Image image) {
    super(enteredText, replaceOffset, replaceLength);
    this.textProposals = proposals;
    this.cursorPosition = 0;
    isCursorPositionValid = false;
    this.image = image;
  }

  public StaticTextProposalComputer(String[] proposals, String enteredText,
      int replaceOffset, int replaceLength, int cursorPosition, Image image) {
    super(enteredText, replaceOffset, replaceLength);
    this.textProposals = proposals;
    this.cursorPosition = cursorPosition;
    isCursorPositionValid = true;
    this.image = image;
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {
    for (String proposal : textProposals) {
      if (proposal.startsWith(getEnteredText())) {
        proposals.add(createProposal(proposal));
      }
    }
  }

  private ICompletionProposal createProposal(String text) {
    if (isCursorPositionValid) {
      return new ReplacementCompletionProposal(text, getReplaceOffset(),
          getReplaceLength(), cursorPosition, null, text, image);
    } else {
      return new ReplacementCompletionProposal(text, getReplaceOffset(),
          getReplaceLength(),
          ReplacementCompletionProposal.DEFAULT_CURSOR_POSITION, null, text,
          image);
    }
  }
}
