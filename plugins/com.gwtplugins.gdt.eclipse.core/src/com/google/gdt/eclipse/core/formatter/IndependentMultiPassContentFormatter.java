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
package com.google.gdt.eclipse.core.formatter;

import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.StringUtilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;

import java.util.HashMap;
import java.util.Map;

/*
 * The implementation is tightly coupled to the fact that the superclass does:
 * try { formatMaster } finally { formatSlaves } (both the sequential calls and
 * the try-finally). The try-finally is required to ensure we clean up the temp
 * model we create.
 */
/**
 * A multiple-pass content formatter implementation that keeps the master's
 * changes independent from the slaves, and only applies (master and slave)
 * changes to the main document if the pre-master and post-slave document text
 * is different (to prevent empty undos).
 * <p>
 * When setting slave strategies, you have to use
 * {@link #setSlaveStrategy2(IFormattingStrategy, String)} instead of
 * {@link #setSlaveStrategy(IFormattingStrategy, String)}.
 * <p>
 * The problem with the traditional {@link MultiPassContentFormatter} is the
 * master formats the entire document (even those partitions that are not its
 * own), and the slaves clean up after the master on each slave's partition.
 * When we are dealing with inlined CSS in a UiBinder XML file, the XML
 * formatter (master) left-aligns all the CSS (since it thinks the CSS is just
 * another content region). This counts as one text replacement. Then, the CSS
 * formatter (slave) properly formats the CSS. This counts as another text
 * replacement. To the undo manager, it looks like there have been two changes,
 * even if the pre-master and post-slave text is the exact same!
 * <p>
 * This formatter will prevent the master formatter from touching partitions for
 * which we have a slave formatting strategy. It is not enough to revert all
 * partitions with non-default content type because, for example, an XML
 * document has multiple partition types that the XML formatter needs to handle.
 */
public class IndependentMultiPassContentFormatter extends
    MultiPassContentFormatter {

  /**
   * The document's default content type, which will be formatted by the master
   * formatting strategy.
   */
  private final String defaultContentType;

  /**
   * Creates a temporary document that is a near-replica of the original
   * document. This is used as the target document for master and slave
   * formatting, so their potentially-useless changes do not cause churn in the
   * undo manager.
   */
  private IDocumentCloner documentCloner;

  /**
   * Indicates whether the master formatter had an error, in which case the
   * slave should not continue.
   */
  private boolean masterFormatterHadError;

  /**
   * Partitioning of the document to be formatted.
   */
  private final String partitioning;

  /**
   * The text of the document before the master format. The validity is only
   * guaranteed during the
   * {@link #formatMaster(IFormattingContext, IDocument, int, int)} and
   * {@link #formatSlave(IFormattingContext, IDocument, int, int, String)}
   * calls.
   */
  private String premasterDocumentText;

  /**
   * The saved text (from before master formatting) of partitions for which we
   * have a slave formatting strategy. The validity is only guaranteed during
   * the {@link #formatMaster(IFormattingContext, IDocument, int, int)} and
   * {@link #formatSlave(IFormattingContext, IDocument, int, int, String)}
   * calls.
   */
  private String[] savedPartitionText;

  /**
   * Stores the strategies for each content type. The
   * {@link MultiPassContentFormatter} does not properly implement
   * {@link #getFormattingStrategy(String)}, and we cannot get to its
   * <code>fStrategies</code> variable, so we keep track of this locally. We
   * cannot override {@link #setSlaveStrategy(IFormattingStrategy, String)}, so
   * clients of this class MUST use
   * {@link #setSlaveStrategy2(IFormattingStrategy, String)}.
   */
  private final Map<String, IFormattingStrategy> strategies = new HashMap<String, IFormattingStrategy>();

  /**
   * The temporary document used by the master and slave formatters. The
   * validity is only guaranteed during the
   * {@link #formatMaster(IFormattingContext, IDocument, int, int)} and
   * {@link #formatSlave(IFormattingContext, IDocument, int, int, String)}
   * calls.
   */
  private IDocument tempDocument;

  private boolean replaceSlavePartitionsDuringMasterFormat;

  private boolean checkForNonwhitespaceChanges;

  public IndependentMultiPassContentFormatter(String partitioning, String type,
      IDocumentCloner documentCloner) {
    super(partitioning, type);

    this.partitioning = partitioning;
    this.defaultContentType = type;
    this.documentCloner = documentCloner;
  }

  /**
   * Whether to check for non-whitespace changes before applying the new
   * formatting. If there are non-whitespace changes, the formatting will not be
   * applied and a warning will be emitted to the error log.
   */
  public void setCheckForNonwhitespaceChanges(boolean checkForNonwhitespaceChanges) {
    this.checkForNonwhitespaceChanges = checkForNonwhitespaceChanges;
  }

  /**
   * Sets whether to replace the slaves with whitespace when formatting the
   * master. This is useful when the syntax of slaves allows for content that
   * would normally confuse the XML formatter. For example, in CSS the '%' is
   * common, but this prevents the Eclipse XML formatter from formatting regions
   * after the '%'.
   * 
   * @param replaceSlavesDuringMasterFormat whether to replace the slaves'
   *          partitions with whitespace when formatting the master
   */
  public void setReplaceSlavePartitionsDuringMasterFormat(
      boolean replaceSlavesDuringMasterFormat) {
    this.replaceSlavePartitionsDuringMasterFormat = replaceSlavesDuringMasterFormat;
  }

  public void setSlaveStrategy2(IFormattingStrategy strategy, String type) {
    setSlaveStrategy(strategy, type);
    strategies.put(type, strategy);
  }

  @Override
  protected void formatMaster(IFormattingContext context, IDocument document,
      int offset, int length) {
    // Reset error state
    masterFormatterHadError = false;

    // Save the entire doc text before we run the master formatter
    premasterDocumentText = document.get();

    // Create the document clone
    tempDocument = documentCloner.clone(document);
    if (tempDocument == null) {
      masterFormatterHadError = true;
      return;
    }

    recordSlavePartitionsText();

    if (replaceSlavePartitionsDuringMasterFormat) {
      replaceSlavePartitionsWithDummyText();
    }

    // Ensure the superclass works off the temp document
    setToTempDocument(context);
    super.formatMaster(context, tempDocument, offset, length);
  }

  @Override
  protected void formatSlaves(IFormattingContext context, IDocument document,
      int offset, int length) {
    try {
      if (masterFormatterHadError) {
        // Do not proceed if the master formatter had an error
        return;
      }

      // Revert any master changes of partitions for which we have a slave
      // formatting strategy
      try {
        ITypedRegion[] partitions = TextUtilities.computePartitioning(
            tempDocument, partitioning, 0, tempDocument.getLength(), false);
        if (partitions.length == savedPartitionText.length) {
          for (int i = 0; i < savedPartitionText.length; i++) {
            if (savedPartitionText[i] == null) {
              continue;
            }

            tempDocument.replace(partitions[i].getOffset(),
                partitions[i].getLength(), savedPartitionText[i]);

            if (partitions[i].getLength() != savedPartitionText[i].length()) {
              // Recompute partitions since our replacement affects subsequent
              // offsets
              partitions = TextUtilities.computePartitioning(tempDocument,
                  partitioning, 0, tempDocument.getLength(), false);
            }
          }
        }
      } catch (BadLocationException e) {
        // This will never happen, according to super.formatSlaves
      }

      if (length > tempDocument.getLength()) {
        // Safeguard against the master formatter shortening the document
        length = tempDocument.getLength();
      }

      // Ensure the superclass works off the temp document
      setToTempDocument(context);
      super.formatSlaves(context, tempDocument, offset, length);

      String tempText = tempDocument.get();
      if (!tempText.equals(premasterDocumentText)) {
        if (!checkForNonwhitespaceChanges
            || StringUtilities.equalsIgnoreWhitespace(tempText,
            premasterDocumentText, true)) {
          // Replace the text since it is different than what we started the
          // with
          document.set(tempText);
        } else {
          CorePluginLog.logWarning("Not applying formatting since it would cause non-whitespace changes.");
        }
      }
    } finally {
      /*
       * This will always be called. The super.format method tries calling
       * formatMaster, and in a finally block calls formatSlaves. We try-finally
       * on entry into formatSlaves.
       */
      documentCloner.release(tempDocument);
    }
  }

  private boolean isSlaveContentType(String contentType) {
    return !contentType.equals(defaultContentType)
        && strategies.get(contentType) != null;
  }

  /**
   * Records the text of partitions for which we have a slave formatting
   * strategy.
   */
  private void recordSlavePartitionsText() {
    try {
      ITypedRegion[] partitions = TextUtilities.computePartitioning(
          tempDocument, partitioning, 0, tempDocument.getLength(), false);
      savedPartitionText = new String[partitions.length];
      for (int i = 0; i < savedPartitionText.length; i++) {
        if (!isSlaveContentType(partitions[i].getType())) {
          continue;
        }

        savedPartitionText[i] = tempDocument.get(partitions[i].getOffset(),
            partitions[i].getLength());
      }
    } catch (BadLocationException e) {
      // This will never happen, according to super.formatSlaves
    }
  }

  private void replaceSlavePartitionsWithDummyText() {
    try {
      ITypedRegion[] partitions = TextUtilities.computePartitioning(
          tempDocument, partitioning, 0, tempDocument.getLength(), false);
      for (int i = 0; i < partitions.length; i++) {
        if (!isSlaveContentType(partitions[i].getType())) {
          continue;
        }

        // Ideally, we'd like to use whitespace as the dummy text, but it may
        // cause the partition to be lost by the master formatter. Instead this
        // uses periods.
        tempDocument.replace(partitions[i].getOffset(),
            partitions[i].getLength(), StringUtilities.repeatCharacter('.',
                partitions[i].getLength()));
      }
    } catch (BadLocationException e) {
      // This should not happen according to super class
    }
  }

  private void setToTempDocument(IFormattingContext context) {
    context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM,
        tempDocument);
  }

}
