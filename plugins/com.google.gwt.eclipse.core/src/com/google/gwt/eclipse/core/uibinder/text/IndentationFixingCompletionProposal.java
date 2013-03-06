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
package com.google.gwt.eclipse.core.uibinder.text;

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.text.CompletionProposalTextCapturer.CapturedText;
import com.google.gwt.eclipse.core.uibinder.text.CompletionProposalTextCapturer.CapturingFailedException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A wrapping completion proposal that fixes the indentation of another
 * completion proposal. The other completion proposal is assumed to have no base
 * indentation (that is, at least one of its lines should start at column 0.)
 * <p>
 * At {@link #apply(IDocument)}-time, this class will attempt to calculate the
 * indentation on the first line, and prefix each subsequent line of the
 * completion proposal with that indentation. If an error occurs during the
 * calculation, the original proposal will be applied without any modifications.
 */
public class IndentationFixingCompletionProposal implements ICompletionProposal {

  private static final int CURSOR_POSITION_NOT_SET = -1;

  private final ICompletionProposal originalProposal;

  /**
   * Only valid after {@link #apply(IDocument)} has been called.
   */
  private int cursorPosition = CURSOR_POSITION_NOT_SET;

  public IndentationFixingCompletionProposal(
      ICompletionProposal originalProposal) {
    this.originalProposal = originalProposal;
  }

  public void apply(IDocument document) {
    try {
      List<CapturedText> capturedTextList = CompletionProposalTextCapturer.capture(originalProposal);
      if (capturedTextList.size() != 1) {
        throw new CapturingFailedException(String.format(
            "Expecting 1 captured text, got %1$d", capturedTextList.size()));
      }
      CapturedText capturedText = capturedTextList.get(0);

      int line = document.getLineOfOffset(capturedText.getOffset());
      String lineDelimiter = document.getLineDelimiter(line);
      if (lineDelimiter == null) {
        // This is the last line of the document, and it doesn't need
        // indentation fixing
        originalProposal.apply(document);
        return;
      }

      String lineText = document.get(document.getLineOffset(line),
          document.getLineLength(line) - lineDelimiter.length());

      int firstCharOffsetInLine = StringUtilities.findNonwhitespaceCharacter(
          lineText, 0);
      String indentationString = lineText.substring(0, firstCharOffsetInLine);
      String lineDelimiterAndIndentationString = lineDelimiter
          + indentationString;
      String newProposalText = capturedText.getText().replaceAll(
          Pattern.quote(lineDelimiter), lineDelimiterAndIndentationString);
      if (newProposalText.endsWith(lineDelimiterAndIndentationString)) {
        newProposalText = newProposalText.substring(0,
            newProposalText.lastIndexOf(indentationString));
      }

      document.replace(capturedText.getOffset(), capturedText.getLength(),
          newProposalText);

      Point originalSelection = originalProposal.getSelection(document);
      if (originalSelection != null) {
        int lineOfCursor = new Document(capturedText.getText()).getLineOfOffset(originalSelection.x
            - capturedText.getOffset());

        // We don't want to add indentation to the first line, which works since
        // that would lineOfCursor = 0
        cursorPosition = originalSelection.x + lineOfCursor
            * indentationString.length();
      }
    } catch (BadLocationException e) {
      GWTPluginLog.logWarning(
          e,
          "The indentation of the completion proposal could not be fixed, applying anyway.");
      originalProposal.apply(document);
    } catch (CapturingFailedException e) {
      GWTPluginLog.logWarning(
          e,
          "The indentation of the completion proposal could not be fixed, applying anyway.");
      originalProposal.apply(document);
    }
  }

  public String getAdditionalProposalInfo() {
    return originalProposal.getAdditionalProposalInfo();
  }

  public IContextInformation getContextInformation() {
    return originalProposal.getContextInformation();
  }

  public String getDisplayString() {
    return originalProposal.getDisplayString();
  }

  public Image getImage() {
    return originalProposal.getImage();
  }

  public Point getSelection(IDocument document) {
    if (cursorPosition == CURSOR_POSITION_NOT_SET) {
      return originalProposal.getSelection(document);
    } else {
      return new Point(cursorPosition, 0);
    }
  }

}
