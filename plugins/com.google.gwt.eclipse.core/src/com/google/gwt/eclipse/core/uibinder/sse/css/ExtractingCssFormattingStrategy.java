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
import com.google.gwt.eclipse.core.formatter.AbstractFormattingStrategy;
import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelLoader;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.wst.css.core.internal.formatter.CSSSourceFormatter;
import org.eclipse.wst.css.core.internal.formatter.CSSSourceFormatterFactory;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

/**
 * The base formatting strategy for CSS that extracts the CSS from the document
 * (whose model may not be a CSS model), formats the extracted CSS, validates
 * the formatting is correct, and re-applies it to the document.
 * <p>
 * Validation is required because there are cases (e.g. "@media { b { } }")
 * where the built-in CSS formatter corrupts the CSS. In order to prevent
 * changes (and excess undos) to the document if the formatted CSS is corrupt,
 * we use the extracted CSS as a sandbox for the formatter.
 */
@SuppressWarnings("restriction")
public class ExtractingCssFormattingStrategy extends AbstractFormattingStrategy {

  /**
   * Allows subclasses to process the formatted CSS block further.
   * <p>
   * The transformation should not touch anything but whitespace.
   * 
   * @param formattedCssBlock the formatted CSS block as a string
   * @return the processed CSS block, or null to prevent formatting
   */
  protected String adjustFormattedCssWhitespace(String formattedCssBlock,
      IDocument originalDocument, TypedPosition partition,
      CssExtractor extractor) {
    return formattedCssBlock;
  }

  @Override
  protected void format(IDocument document, TypedPosition partition) {

    CssExtractor extractor = CssExtractor.extract(document,
        partition.getOffset(), partition.getLength(),
        new CssResourceAwareModelLoader());
    if (extractor == null) {
      GWTPluginLog.logError("Could not format CSS block due to error in extracting the document.");
      return;
    }

    ICSSDocument cssDocument = extractor.getCssDocument();
    String formattedCssBlock = formatCss(cssDocument);

    formattedCssBlock = adjustFormattedCssWhitespace(formattedCssBlock,
        document, partition, extractor);
    if (formattedCssBlock == null) {
      return;
    }

    try {
      String currentText = document.get(partition.getOffset(),
          partition.getLength());

      if (formattedCssBlock.equals(currentText)) {
        // Do nothing
        return;
      }

      if (!StringUtilities.equalsIgnoreWhitespace(formattedCssBlock,
          currentText, true)) {
        // Do nothing, since the formatting cause non-whitespace/non-case
        // changes, which a formatter is not supposed to do
        // (Give it a dummy exception so it prints a stack trace)
        GWTPluginLog.logError(
            new Exception(),
            "Could not format text because the CSS formatter caused non-whitespace/non-case changes.  Please ensure your CSS is valid.");
        return;
      }

      document.replace(partition.getOffset(), partition.getLength(),
          formattedCssBlock.toString());
    } catch (BadLocationException e) {
      GWTPluginLog.logWarning(e, "Could not format CSS block.");
    }
  }

  private String formatCss(ICSSDocument cssDocument) {
    CSSSourceFormatter formatter = CSSSourceFormatterFactory.getInstance().getSourceFormatter(
        (INodeNotifier) cssDocument);
    String formattedCssBlock = formatter.format(cssDocument).toString();
    return formattedCssBlock;
  }

}
