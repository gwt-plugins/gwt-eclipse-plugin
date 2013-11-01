/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors;

import com.google.common.collect.Lists;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteEntry;
import com.google.gdt.eclipse.drive.editors.webautocomplete.AutocompleteEntryHolder;
import com.google.gdt.eclipse.drive.editors.webautocomplete.Autocompleter;
import com.google.gdt.eclipse.drive.editors.webautocomplete.MethodAutocompleteEntry;
import com.google.gdt.eclipse.drive.editors.webautocomplete.ReverseCharIterator;
import com.google.gdt.eclipse.drive.editors.webautocomplete.ReverseCharIterator.EditorConnector;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;

import java.util.List;

/**
 * Offers completion suggestions from the Apps Script APIs and JavaScript keywords, based on the
 * logic in {@link com.google.gdt.eclipse.drive.editors.webautocomplete}.
 */
public class WebEditorCompletionProcessor implements IContentAssistProcessor {

  // Maximum number of lines the {@link Autocompleter} will visit when determining possible
  // completions.
  private static final int MAX_LINES_FOR_AUTOCOMPLETE = 50;

  private static final TemplateContextType APPS_SCRIPT_TEMPLATE_CONTEXT_TYPE =
      new TemplateContextType("Apps Script");
      
  private final Autocompleter autocompleter;
  
  public static WebEditorCompletionProcessor make(AutocompleteEntryHolder apiData) {
    ApiDocumentationService.addKeywords(apiData, JavaScriptIdentifierNames.KEYWORDS_AND_LITERALS);
    return new WebEditorCompletionProcessor(new Autocompleter("editorId", apiData));
  }

  private WebEditorCompletionProcessor(Autocompleter autocompleter) {
    this.autocompleter = autocompleter;
  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    Autocompleter.Result result;
    IDocument document = viewer.getDocument();
    try {
      if (!inACompletableContext(document, offset)) {
        return null;
      }
      result = getWebEditorAutocompleterResult(document, offset);
    } catch (BadLocationException e) {
      // Should not happen
      DrivePlugin.logError(
          "AppsScriptCompletionProcessor.computeCompletionProposals invoked with invalid offset "
              + offset,
          e);
      return null;
    }
    List<ICompletionProposal> resultList =
        autocompleterResultToCompletionProposals(result, document, offset);
    return
        resultList.isEmpty() ?
            null : resultList.toArray(new ICompletionProposal[resultList.size()]);
  }
  
  private static boolean inACompletableContext(IDocument document, int offset)
      throws BadLocationException {
    if (offset == 0) {
      return false;
    }
    char prevChar = document.getChar(offset - 1);
    return prevChar == '.' || JavaScriptIdentifierNames.isIdentifierNameCharacter(prevChar);
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    return new IContextInformation[]{};
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[]{'.'};
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return new char[]{};
  }

  @Override
  public String getErrorMessage() {
    return "";
  }

  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  private Autocompleter.Result getWebEditorAutocompleterResult(
      IDocument document, int offset) throws BadLocationException {
    Autocompleter.Result result;
    EditorConnector editorConnector = new EclipseEditorConnector(document);
    LineBasedCursor cursor = new LineBasedCursor(document, offset);
    int offsetBeforeWhitespace = offset;
    while (offsetBeforeWhitespace > 0
        && Character.isWhitespace(document.getChar(offsetBeforeWhitespace - 1))) {
      offsetBeforeWhitespace--;
    }
    if (offsetBeforeWhitespace > 0 && document.getChar(offsetBeforeWhitespace - 1) == '.') {
      refreshInferredTypesOfVariables(document, editorConnector);
      LineBasedCursor cursorBeforeWhitespace =
          new LineBasedCursor(document, offsetBeforeWhitespace);
      result =
          autocompleter.getMatchingEntries(
              offset,
              new ReverseCharIterator(
                  editorConnector,
                  ReverseCharIterator.Mode.NO_WRAP,
                  MAX_LINES_FOR_AUTOCOMPLETE,
                  cursorBeforeWhitespace.getLineNumber(),
                  cursorBeforeWhitespace.getOffsetFromStartOfLine() - 1));
    } else {
      result =
          autocompleter.getEntriesForIncompleteString(
              offset,
              new ReverseCharIterator(
                  editorConnector,
                  ReverseCharIterator.Mode.WRAP,
                  null,
                  cursor.getLineNumber(),
                  cursor.getOffsetFromStartOfLine()));
    }
    return result;
  }

  // Recompute possible classes of variables by scanning assignments on every line.
  private void refreshInferredTypesOfVariables(
      IDocument document, EditorConnector editorConnector) throws BadLocationException {
    int lineCount = document.getNumberOfLines();
    for (int i = 0; i < lineCount; i++) {
      autocompleter.parseLine(
          new ReverseCharIterator(
              editorConnector,
              ReverseCharIterator.Mode.NO_WRAP,
              MAX_LINES_FOR_AUTOCOMPLETE,
              i,
              document.getLineLength(i)));
    }
  }

  private List<ICompletionProposal> autocompleterResultToCompletionProposals(
      Autocompleter.Result result, IDocument document, int offset) {
    List<ICompletionProposal> resultList = Lists.newLinkedList();
    int prefixLength = offset - result.getReplacementStartPos();
    int offsetOfPrefixStart = offset - prefixLength;
    for (AutocompleteEntry entry : result.getEntries()) {
      resultList.add(
          entry instanceof MethodAutocompleteEntry ?
              methodCallProposal(
                  document, prefixLength, offsetOfPrefixStart, (MethodAutocompleteEntry) entry)
              : identifierProposal(prefixLength, offsetOfPrefixStart, entry));    
    }
    return resultList;
  }

  private static AppsScriptTemplateProposal methodCallProposal(
      IDocument document, int prefixLength, int offsetOfPrefixStart, AutocompleteEntry entry) {
    return
        new AppsScriptTemplateProposal(
            new Template(
                "", "", "no context", ((MethodAutocompleteEntry) entry).methodCallTemplate(), true),
            new DocumentTemplateContext(
                APPS_SCRIPT_TEMPLATE_CONTEXT_TYPE, document, offsetOfPrefixStart, prefixLength),
            new Region(offsetOfPrefixStart, prefixLength),
            entry.getInsertedView() + " - " + entry.getReturnType(),
            entry.getDescription());
  }

  private ICompletionProposal identifierProposal(
      int prefixLength, int offsetOfPrefixStart, AutocompleteEntry entry) {
    String replacement = entry.getInsertedView();
    String displayString;
    if (autocompleter.isTypeName(entry.getEntryName())) {
      displayString = replacement;
    } else {
      String type = entry.getReturnType();
      displayString = type.isEmpty() ? replacement : replacement + " - " + type;
    }        
    return
        new CompletionProposal(
            replacement, offsetOfPrefixStart, prefixLength, replacement.length(), null,
            displayString, null, entry.getDescription());
  }

  /**
   * An {@link EditorConnector} implementation for an Eclipse {@link IDocument}.
   * (Both {@code IDocument} and {@code EditorConnector} use zero-based line numbers.)
   */
  private static class EclipseEditorConnector implements EditorConnector {
    
    private final IDocument document;
    
    public EclipseEditorConnector(IDocument document) {
      this.document = document;
    }

    @Override
    public int getNumLines() {
      return document.getNumberOfLines();
    }

    @Override
    public int getLineLength(int line) {
      try {
        return document.getLineLength(line);
      } catch (BadLocationException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public char getCharAt(int line, int ch) {
      try {
        int lineOffset = document.getLineOffset(line);
        return document.getChar(lineOffset + ch);
      } catch (BadLocationException e) {
        throw new IllegalArgumentException(e);
      }
    }    
  }
  
  private static class LineBasedCursor {
    private final int lineNumber;
    private final int offsetFromStartOfLine;
    
    public LineBasedCursor(IDocument document, int offsetInDocument) throws BadLocationException {
      lineNumber = document.getLineOfOffset(offsetInDocument);
      int offsetOfStartOfLine = document.getLineOffset(lineNumber);
      offsetFromStartOfLine = offsetInDocument - offsetOfStartOfLine;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public int getOffsetFromStartOfLine() {
      return offsetFromStartOfLine;
    }
  }

}
