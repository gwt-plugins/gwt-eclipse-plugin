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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.ArrayList;
import java.util.List;

/*
 * This class creates a mock document that attempts to capture these edits. This
 * is a bit more robust and scalable than using reflection.
 */
/**
 * Captures the text from an {@link ICompletionProposal}. The
 * {@link ICompletionProposal}'s API allows its implementors to do any free-
 * form edits to the document without having to every say exactly what edits it
 * will do. Therefore, there is not any API to get the insertions, replacements,
 * or deletions that the proposal will perform.
 * <p>
 * This currently is tailored to the
 * {@link org.eclipse.jface.text.contentassist.CompletionProposal}
 * implementation of {@link ICompletionProposal}. This class may work with other
 * implementations, but nothing is guaranteed.
 */
public class CompletionProposalTextCapturer {

  /**
   * A container for a captured text edit.
   * <p>
   * This can represent an insertion (length will be 0), replacement, or
   * deletion (text will be empty).
   */
  public static class CapturedText {
    private final int offset;
    private final int length;
    private final String text;

    public CapturedText(int offset, int length, String text) {
      this.offset = offset;
      this.length = length;
      this.text = text;
    }

    public int getLength() {
      return length;
    }

    public int getOffset() {
      return offset;
    }

    public String getText() {
      return text;
    }
  }

  /**
   * There was an error capturing the text from the proposal.
   */
  @SuppressWarnings("serial")
  public static class CapturingFailedException extends Exception {
    public CapturingFailedException(String message) {
      super(message);
    }

    public CapturingFailedException(String msg, Throwable e) {
      super(msg, e);
    }
  }

  private static class CapturingDocument extends MockDocument {
    private final List<CapturedText> capturedText = new ArrayList<CapturedText>();

    public List<CapturedText> getCapturedText() {
      return capturedText;
    }

    @Override
    public void replace(int offset, int length, String text)
        throws BadLocationException {
      capturedText.add(new CapturedText(offset, length, text));
    }
  }

  public static List<CapturedText> capture(ICompletionProposal proposal)
      throws CapturingFailedException {
    CapturingDocument capturingDocument = new CapturingDocument();

    try {
      proposal.apply(capturingDocument);
    } catch (MockDocument.UnsupportedMockOperationException e) {
      throw new CapturingFailedException(
          "Could not capture the text of the given proposal.", e);
    }

    return capturingDocument.getCapturedText();
  }
}
