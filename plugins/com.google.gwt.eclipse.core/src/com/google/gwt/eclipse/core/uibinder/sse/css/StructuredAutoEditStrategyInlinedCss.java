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
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelLoader;
import com.google.gwt.eclipse.core.uibinder.text.SimpleDocumentCommand;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.wst.css.ui.internal.autoedit.StructuredAutoEditStrategyCSS;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An auto edit strategy which can deal with inlined CSS. It extracts the
 * inlined CSS and applies it to the default CSS autoedit strategy.
 */
@SuppressWarnings("restriction")
public class StructuredAutoEditStrategyInlinedCss implements IAutoEditStrategy {

  /**
   * Matches a left-curly-brace followed by a newline (with whitespace in
   * between).
   */
  private static final Pattern NEWLINE_AFTER_OPEN_BRACE = Pattern.compile(
      ".*[{]\\s*\\n\\s*$", Pattern.DOTALL);

  private final StructuredAutoEditStrategyCSS originalCssAutoEditStrategy = new StructuredAutoEditStrategyCSS();

  public void customizeDocumentCommand(IDocument document,
      DocumentCommand command) {

    if (!prepareForOriginalCssAutoEditStrategy(document, command)) {
      return;
    }

    IStructuredDocument structuredDoc = (IStructuredDocument) document;
    ITypedRegion region = SseUtilities.getPartition(structuredDoc,
        command.offset);
    if (region == null) {
      // Ignore
      return;
    }

    CssExtractor extractor = CssExtractor.extract(document, region.getOffset(),
        region.getLength(), new CssResourceAwareModelLoader());

    // Create a temporary document command with offsets aligned to the extracted
    // CSS
    DocumentCommand commandForExtractedCss = new SimpleDocumentCommand();
    SimpleDocumentCommand.copyFields(command, commandForExtractedCss,
        region.getOffset());

    originalCssAutoEditStrategy.customizeDocumentCommand(
        extractor.getStructuredDocument(), commandForExtractedCss);

    // Ensure only whitespace changes were made
    if (!StringUtilities.equalsIgnoreWhitespace(command.text,
        commandForExtractedCss.text, true)) {
      // The original strategy will corrupt the command in at least this
      // scenario:
      // CSS settings use spaces, and you're entering the } in:
      // \tb {
      // \t b: b;
      // \t }
      GWTPluginLog.logWarning("Could not correct the indentation, one known cause is conflicting XML and CSS settings for tabs versus spaces, please check your settings.");
      return;
    }

    // Copy changes back, removing the offsets for the extracted CSS
    SimpleDocumentCommand.copyFields(commandForExtractedCss, command,
        -region.getOffset());
  }

  private boolean checkAndPrepareForNewline(IDocument document,
      DocumentCommand command) {
    try {
      String documentAndCommandText = document.get(0, command.offset)
          + command.text;
      /*
       * The only case where we want to add an indentation after newline is if
       * there is a opening curly brace.
       */
      Matcher matcher = NEWLINE_AFTER_OPEN_BRACE.matcher(documentAndCommandText);
      if (!matcher.matches()) {
        return false;
      }
    } catch (BadLocationException e) {
      return false;
    }

    /*
     * The StructuredAutoEditStrategyCSS will only add proper indentation if the
     * last character is a newline. We remove the whitespace following the
     * newline which was added by the XML autoedit strategy that comes before
     * us.
     */
    command.text = command.text.replaceAll("\\n\\s+$", "\n");
    return command.text.endsWith("\n");
  }

  /**
   * Checks to see if the given command can be handled by the
   * {@link StructuredAutoEditStrategyCSS} strategy. In some instances, it may
   * need to prepare the command by tweaking some of the text.
   * <p>
   * Calling the original strategy is expensive if done on every character
   * entered (since we are required to extract and generate a CSS model). This
   * method allows us to do this when we are sure the original strategy can
   * handle the command.
   * 
   * @param document the document receiving the command
   * @param command the document command
   * @return false is this command is unabled to be handled
   */
  private boolean prepareForOriginalCssAutoEditStrategy(IDocument document,
      DocumentCommand command) {
    if (command.length != 0 || command.text == null) {
      return false;
    }

    if (!(checkAndPrepareForNewline(document, command)
        || command.text.equals("}")
        || command.text.equals("]") || command.text.equals(")"))) {
      return false;
    }

    return true;
  }
}
