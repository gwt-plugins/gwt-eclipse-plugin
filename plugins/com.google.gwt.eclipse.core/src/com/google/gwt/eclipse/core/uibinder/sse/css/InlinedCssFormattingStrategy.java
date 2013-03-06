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

import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formatter for CSS blocks inlined in another document.
 */
@SuppressWarnings({"restriction"})
public class InlinedCssFormattingStrategy extends
    ExtractingCssFormattingStrategy {
  
  @Override
  protected String adjustFormattedCssWhitespace(String formattedCssBlock,
      IDocument document, TypedPosition partition, CssExtractor extractor) {
    try {
      String cssLineSeparator = extractor.getStructuredDocument().getLineDelimiter();
      String xmlLineSeparator = ((IStructuredDocument) document).getLineDelimiter();
      // The formattedCssBlock starts at column 0, we need to indent it to fit
      // with the <ui:style>'s indentation
      formattedCssBlock = indentCssForXml(formattedCssBlock, document,
          partition, cssLineSeparator, xmlLineSeparator);
      return formattedCssBlock;
    } catch (BadLocationException e) {
      GWTPluginLog.logWarning(e, "Could not format CSS block.");
    }

    return null;
  }

  private String computeOneXmlIndentString() {
    Preferences preferences = XMLCorePlugin.getDefault().getPluginPreferences();
    char indentChar = ' ';
    String indentCharPref = preferences.getString(XMLCorePreferenceNames.INDENTATION_CHAR);
    if (XMLCorePreferenceNames.TAB.equals(indentCharPref)) {
      indentChar = '\t';
    }
    int indentationWidth = preferences.getInt(XMLCorePreferenceNames.INDENTATION_SIZE);

    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < indentationWidth; i++) {
      indent.append(indentChar);
    }
    return indent.toString();
  }

  private String indentCssForXml(String formattedCssBlock, IDocument document,
      TypedPosition partition, String cssLineSeparator, String xmlLineSeparator)
      throws BadLocationException {

    String oneXmlIndent = computeOneXmlIndentString();

    int lineNumberInDocument = document.getLineOfOffset(partition.getOffset());
    int offsetOfLineInDocument = document.getLineOffset(lineNumberInDocument);
    String lineContents = document.get(offsetOfLineInDocument,
        document.getLineLength(lineNumberInDocument));
    int offsetOfNonwhitespaceInLine = StringUtilities.findNonwhitespaceCharacter(
        lineContents, 0);

    // The indent string that will be used for the closing tag </ui:style>
    String styleElementIndentString;
    // The indent string that will be used for to precede each line of the CSS
    // block
    String cssBlockIndentString;

    if (offsetOfLineInDocument + offsetOfNonwhitespaceInLine == partition.getOffset()) {
      // The CSS block is alone on this line, use whatever indentation it has
      cssBlockIndentString = lineContents.substring(0,
          offsetOfNonwhitespaceInLine);
      styleElementIndentString = cssBlockIndentString.replace(oneXmlIndent, "");

    } else {
      // Something else is before the CSS block on this line (likely the style
      // tag)
      styleElementIndentString = lineContents.substring(0,
          offsetOfNonwhitespaceInLine);
      cssBlockIndentString = styleElementIndentString + oneXmlIndent;
    }

    return processCssBlock(formattedCssBlock, cssLineSeparator,
        xmlLineSeparator, cssBlockIndentString, styleElementIndentString);
  }

  private String processCssBlock(String formattedCssBlock,
      String cssLineSeparator, String xmlLineSeparator,
      CharSequence cssIndentString, String styleElementIndentString) {
    String[] cssBlockLines = formattedCssBlock.split(Pattern.quote(cssLineSeparator));
    List<String> processedCssBlockLines = new ArrayList<String>(
        cssBlockLines.length);

    // Start the CSS on a line of its own
    processedCssBlockLines.add("");

    for (String curCssLine : cssBlockLines) {
      // Precede the CSS line with its indent string
      processedCssBlockLines.add(cssIndentString + curCssLine);
    }

    // Add the indent for the </ui:style>
    processedCssBlockLines.add(styleElementIndentString.toString());

    return StringUtilities.join(processedCssBlockLines, xmlLineSeparator);
  }
}
