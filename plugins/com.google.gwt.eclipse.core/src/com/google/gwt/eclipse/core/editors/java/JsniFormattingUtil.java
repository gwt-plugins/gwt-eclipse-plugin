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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;
import com.google.gwt.eclipse.core.validators.java.JsniParser;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;
import org.eclipse.wst.jsdt.internal.corext.util.CodeFormatterUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for formatting JSNI methods. This is not a full-blown
 * JavaScript pretty-printer, but it does apply the correct outer indentation to
 * JSNI blocks, to correct the JDT bug which slides them to the right.
 */
@SuppressWarnings("restriction")
public class JsniFormattingUtil {

  private static class JsniJavaRefReplacementResult {
    private String jsni;
    private Map<String, String> replacements;

    public JsniJavaRefReplacementResult(String jsni,
        Map<String, String> replacements) {
      this.jsni = jsni;
      this.replacements = replacements;
    }

    public String getJsni() {
      return jsni;
    }

    public Map<String, String> getReplacements() {
      return replacements;
    }
  }

  /**
   * Walks the AST for the CompilationUnit we're formatting to figure out the
   * offset of the method declaration containing a particular JSNI block. There
   * doesn't seem to be any built in equivalent of ICompilationUnit's
   * getElementAt() method, and we can't convert the CompilationUnit to an
   * ICompilationUnit here since the Compilation is unattached (not part of the
   * Java Model).
   */
  private static class JsniMethodFinder extends ASTVisitor {

    private final int jsniBlockOffset;

    private int jsniMethodDeclarationOffset = 0;

    public JsniMethodFinder(int jsniBlockOffset) {
      super();
      this.jsniBlockOffset = jsniBlockOffset;
      this.jsniMethodDeclarationOffset = jsniBlockOffset;
    }

    public int getJsniMethodDeclarationOffset() {
      return jsniMethodDeclarationOffset;
    }

    @Override
    public boolean visit(MethodDeclaration method) {
      int offset = method.getStartPosition();
      int length = method.getLength();

      // Test if the JSNI block starts within this native method
      if (jsniBlockOffset > offset && jsniBlockOffset < (offset + length)) {
        jsniMethodDeclarationOffset = offset;
      }

      return false;
    }
  }

  private static final String EMPTY_STRING = "";

  /**
   * Same as format(IDocument, Map, String[]), except the formatting options
   * are taken from the given project.
   * 
   */
  @SuppressWarnings("unchecked")
  public static TextEdit format(IDocument document, IJavaProject project,
      String[] originalJsniMethods) {
    Map jsOptions = JavaScriptCore.create(project.getProject()).getOptions(true);
    Map jOptions = project.getOptions(true);
    return format(document, jOptions, jsOptions, originalJsniMethods);
  }

  /**
   * Returns a text edit that formats the given document according to the given
   * settings.
   * 
   * @param document The document to format.
   * @param javaFormattingPrefs The formatting preferences for Java, used to
   *          determine the method level indentation.
   * @param javaScriptFormattingPrefs The formatting preferences for JavaScript.
   *          See org.eclipse.wst.jsdt.internal.formatter
   *          .DefaultCodeFormatterOptions and
   *          org.eclipse.wst.jsdt.core.formatter.DefaultCodeFormatterConstants
   * @param originalJsniMethods The original jsni methods to use if the
   *          formatter fails to format the method. The original jsni Strings
   *          must be in the same order that the jsni methods occur in the
   *          document. This is to work around the Java formatter blasting the
   *          jsni tabbing for the format-on-save action. May be null.
   * @return A text edit that when applied to the document, will format the jsni
   *         methods.
   */
  @SuppressWarnings("unchecked")
  public static TextEdit format(IDocument document, Map javaFormattingPrefs,
      Map javaScriptFormattingPrefs, String[] originalJsniMethods) {
    TextEdit combinedEdit = new MultiTextEdit();
    try {
      ITypedRegion[] regions = TextUtilities.computePartitioning(document,
          GWTPartitions.GWT_PARTITIONING, 0, document.getLength(), false);

      // Format all JSNI blocks in the document
      int i = 0;
      for (ITypedRegion region : regions) {
        if (region.getType().equals(GWTPartitions.JSNI_METHOD)) {
          String originalJsniMethod = null;
          if (originalJsniMethods != null && i < originalJsniMethods.length) {
            originalJsniMethod = originalJsniMethods[i];
          }
          TextEdit edit = format(document, new TypedPosition(region),
              javaFormattingPrefs, javaScriptFormattingPrefs,
              originalJsniMethod);
          if (edit != null) {
            combinedEdit.addChild(edit);
          }
          i++;
        }
      }
      return combinedEdit;

    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
      return null;
    }
  }

  public static TextEdit format(IDocument document, TypedPosition partition,
      Map<String, String> javaFormattingPrefs,
      Map<String, String> javaScriptFormattingPrefs, String original) {
    try {
      // Extract the JSNI block out of the document
      int offset = partition.getOffset();
      int length = partition.getLength();

      // Determine the line delimiter, indent string, and tab/indent widths
      String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
      int tabWidth = IndentManipulation.getTabWidth(javaFormattingPrefs);
      int indentWidth = IndentManipulation.getIndentWidth(javaFormattingPrefs);

      // Get indentation level of the first line of the JSNI block (this should
      // be the line containing the JSNI method declaration)
      int methodDeclarationOffset = getMethodDeclarationOffset(document, offset);
      int jsniLine1 = document.getLineOfOffset(methodDeclarationOffset);
      int methodIndentLevel = getLineIndentLevel(document, jsniLine1, tabWidth,
          indentWidth);
      DefaultCodeFormatter defaultCodeFormatter = new DefaultCodeFormatter(
          javaFormattingPrefs);
      String indentLine = defaultCodeFormatter.createIndentationString(methodIndentLevel);

      // Extract the JSNI body out of the block and split it up by line
      String jsniSource = document.get(offset, length);
      String body = JsniParser.extractMethodBody(jsniSource);

      String formattedJs;

      // JSNI Java references mess up the JS formatter, so replace them
      // with place holder values
      JsniJavaRefReplacementResult replacementResults = replaceJsniJavaRefs(body);
      body = replacementResults.getJsni();

      TextEdit formatEdit = CodeFormatterUtil.format2(
          CodeFormatter.K_STATEMENTS, body, methodIndentLevel + 1,
          lineDelimiter, javaScriptFormattingPrefs);

      if (formatEdit != null) {

        body = restoreJsniJavaRefs(replacementResults);

        Document d = new Document(body);
        formatEdit.apply(d);

        formattedJs = d.get();

        if (!formattedJs.startsWith(lineDelimiter)) {
          formattedJs = lineDelimiter + formattedJs;
        }

        if (!formattedJs.endsWith(lineDelimiter)) {
          formattedJs = formattedJs + lineDelimiter;
        }

        formattedJs = formattedJs + indentLine;

        formattedJs = "/*-{" + formattedJs + "}-*/";

      } else {

        if (original == null) {
          return null;
        }

        formattedJs = original; // formatting failed, use the original string
      }

      return new ReplaceEdit(offset, length, formattedJs);

    } catch (Exception e) {
      GWTPluginLog.logError(e);
      return null;
    }
  }

  public static String[] getJsniMethods(IDocument document) {

    try {
      List<String> jsniMethods = new LinkedList<String>();
      ITypedRegion[] regions = TextUtilities.computePartitioning(document,
          GWTPartitions.GWT_PARTITIONING, 0, document.getLength(), false);

      // Format all JSNI blocks in the document
      for (ITypedRegion region : regions) {
        if (region.getType().equals(GWTPartitions.JSNI_METHOD)) {
          String jsni = document.get(region.getOffset(), region.getLength());
          jsniMethods.add(jsni);
        }
      }

      return jsniMethods.toArray(new String[0]);

    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
      return null;
    }
  }

  private static int getLineIndentLevel(IDocument document, int line,
      int tabWidth, int indentWidth) throws BadLocationException {
    int lineOffset = document.getLineOffset(line);
    return getLineIndentLevel(document.get(lineOffset,
        document.getLineLength(line)), tabWidth, indentWidth);
  }

  private static int getLineIndentLevel(String line, int tabWidth,
      int indentWidth) {
    return IndentManipulation.measureIndentUnits(line, tabWidth, indentWidth);
  }

  private static int getMethodDeclarationOffset(IDocument document, int offset) {
    // Have JDT parse the compilation unit
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setResolveBindings(false);
    parser.setSource(document.get().toCharArray());
    ASTNode ast = parser.createAST(null);

    // Figure out the offset of the containing method declaration
    JsniMethodFinder finder = new JsniMethodFinder(offset);
    ast.accept(finder);
    return finder.getJsniMethodDeclarationOffset();
  }

  private static String makeJsToken(String s) {
    int hashCode = s.hashCode();
    // js variable names can't have negative signs in them
    String jsToken = "_" + (hashCode < 0 ? "N" + Math.abs(hashCode) : hashCode);

    // pad the hash so it's the same length as the original reference so that
    // things like formatting line wrap works
    while (jsToken.length() < s.length()) {
      jsToken = jsToken + "_";
    }
    return jsToken;
  }

  private static JsniJavaRefReplacementResult replaceJsniJavaRefs(String jsni) {

    Map<String, String> replacements = new HashMap<String, String>();

    Pattern p = Pattern.compile("@[a-zA-Z0-9._$]+::[a-zA-Z0-9_$]+(\\(.*?\\)\\(.*?\\))?");

    Matcher m = p.matcher(jsni);

    while (m.find()) {
      int start = m.start();
      int end = m.end();

      String ref = jsni.substring(start, end);
      String jsToken = makeJsToken(ref);

      // if the map already contains the js token, and the token's original jsni
      // ref is not the one we've found, js-tokenize the token
      while (replacements.containsKey(jsToken)
          && !replacements.get(jsToken).equals(ref)) {
        jsToken = makeJsToken(jsToken);
      }

      replacements.put(jsToken, ref);
    }

    for (Entry<String, String> kvp : replacements.entrySet()) {
      jsni = jsni.replace(kvp.getValue(), kvp.getKey());
    }

    return new JsniJavaRefReplacementResult(jsni, replacements);
  }

  private static String restoreJsniJavaRefs(JsniJavaRefReplacementResult result) {

    String jsni = result.getJsni();
    for (Entry<String, String> kvp : result.getReplacements().entrySet()) {
      jsni = jsni.replace(kvp.getKey(), kvp.getValue());
    }

    return jsni;
  }

  private static String stripLeadingBlankLines(String text, String lineDelimiter) {
    String[] lines = text.split(lineDelimiter);
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].trim().length() > 0) {
        List<String> linesList = Arrays.asList(lines);
        linesList = linesList.subList(i, lines.length);
        return Util.join(linesList, lineDelimiter);
      }
    }

    return EMPTY_STRING;
  }

  private static String stripTrailingWhitespace(String text) {
    char[] chars = text.toCharArray();
    int end = chars.length - 1;

    while (end > 0) {
      if (!Character.isWhitespace(chars[end])) {
        return text.substring(0, end + 1);
      }
      end--;
    }

    return EMPTY_STRING;
  }
}
