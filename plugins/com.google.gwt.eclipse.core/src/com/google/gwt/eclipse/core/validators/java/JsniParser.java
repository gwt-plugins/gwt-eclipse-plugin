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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.editors.java.GWTPartitions;
import com.google.gwt.eclipse.core.markers.GWTJavaProblem;
import com.google.gwt.eclipse.core.markers.GWTProblemType;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Parses JSNI blocks and collects all Java references.
 */
public final class JsniParser {

  /**
   * Lightweight version of {@link JsParserException} that contains only the
   * information we need for the Problems view (message, doc offset).
   */
  @SuppressWarnings("serial")
  public static class JavaScriptParseException extends Exception {

    private final int offset;

    public JavaScriptParseException(String message, int offset) {
      super(message);
      this.offset = offset;
    }

    public int getOffset() {
      return offset;
    }
  }

  /**
   * Default implementation for {@link SourceInfo}, which is now required by
   * {@link JsParser}. We actually don't need any of the functionality that it
   * provides; it is only needed by GWT's compiler tools, such as "Story of Your
   * Compile" and "Web-Mode Stack Traces".
   */
  @SuppressWarnings("serial")
  private static class SourceInfoAdapter implements SourceInfo {
    public void addCorrelation(Correlation c) {
    }

    public void copyMissingCorrelationsFrom(SourceInfo other) {
    }

    public List<Correlation> getAllCorrelations() {
      return Collections.emptyList();
    }

    public List<Correlation> getAllCorrelations(Axis axis) {
      return Collections.emptyList();
    }

    public int getEndPos() {
      return -1;
    }

    public String getFileName() {
      return "unknown";
    }

    public Correlation getPrimaryCorrelation(Axis axis) {
      return null;
    }

    public Set<Correlation> getPrimaryCorrelations() {
      return Collections.emptySet();
    }

    public Correlation[] getPrimaryCorrelationsArray() {
      return null;
    }

    public int getStartLine() {
      return -1;
    }

    public int getStartPos() {
      return -1;
    }

    public SourceInfo makeChild(Class<?> caller, String description) {
      return this;
    }

    public SourceInfo makeChild(Class<?> caller, String description,
        SourceInfo... merge) {
      return this;
    }

    public void merge(SourceInfo... sourceInfos) {
    }
  }

  public static final String JSNI_BLOCK_END = "}-*/";

  public static final String JSNI_BLOCK_START = "/*-{";

  private static final String JS_FUNCTION_FOOTER = "}";

  private static final String JS_FUNCTION_HEADER = "function(){";

  public static String extractMethodBody(String jsniMethod) {
    int startPos = jsniMethod.indexOf(JSNI_BLOCK_START);
    int endPos = jsniMethod.lastIndexOf(JSNI_BLOCK_END);
    if (startPos < 0 || endPos < 0) {
      return null;
    }

    startPos += JSNI_BLOCK_START.length();
    return jsniMethod.substring(startPos, endPos);
  }

  public static ITypedRegion getEnclosingJsniRegion(ITextSelection selection,
      IDocument document) {
    try {
      ITypedRegion region = TextUtilities.getPartition(document,
          GWTPartitions.GWT_PARTITIONING, selection.getOffset(), false);

      if (region.getType().equals(GWTPartitions.JSNI_METHOD)) {
        int regionEnd = region.getOffset() + region.getLength();
        int selectionEnd = selection.getOffset() + selection.getLength();

        // JSNI region should entirely contain the selection
        if (region.getOffset() <= selection.getOffset()
            && regionEnd >= selectionEnd) {
          return region;
        }
      }
    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
    }

    return null;
  }

  public static JavaValidationResult parse(MethodDeclaration method) {
    final JavaValidationResult result = new JavaValidationResult();

    try {
      try {
        // Find all Java references
        result.addAllJavaRefs(findJavaRefs(method));

        // Validate the Java references
        for (JsniJavaRef ref : result.getJavaRefs()) {
          GWTJavaProblem problem = validateJavaRef(method, ref);
          if (problem != null) {
            result.addProblem(problem);
          }
        }
      } catch (JavaScriptParseException e) {
        // Add the offset of the method declaration to get a document offset
        int offset = e.getOffset() + method.getStartPosition();

        // Add the problem as a 1 character wide error, since we don't get the
        // length of the error "region" from JsParser
        result.addProblem(GWTJavaProblem.create(method, offset, 1,
            GWTProblemType.JSNI_PARSE_ERROR, e.getMessage()));
      } catch (IOException e) {
        GWTPluginLog.logError(e, "IO error while parsing JSNI method "
            + method.getName());
      } catch (InternalCompilerException e) {
        String errorMsg = "Unexpected error parsing JSNI method "
            + method.getName() + ": " + e.getMessage();
        Throwable cause = (e.getCause() != null ? e.getCause() : e);
        GWTPluginLog.logError(cause, errorMsg);
      }
    } catch (BadLocationException e) {
      GWTPluginLog.logError(e, "Error translating JS parse error location in "
          + method.getName());
    }

    return result;
  }

  public static JsBlock parse(String jsniMethod) throws IOException,
      BadLocationException, JavaScriptParseException {

    String jsni = extractMethodBody(jsniMethod);
    if (jsni == null) {
      // TODO: if this file is client code (and if we got into JsniParser, it
      // should be), warn about missing JSNI block?
      return null;
    }

    // Wrap JavaScript code in a fake function for parsing
    jsni = JS_FUNCTION_HEADER + jsni + JS_FUNCTION_FOOTER;

    try {
      return parseFunctionBlock(jsni, 0);
    } catch (JsParserException e) {
      // Calculate the offset of the error within the fake JS function
      SourceDetail source = e.getSourceDetail();
      DefaultLineTracker lineTracker = new DefaultLineTracker();
      lineTracker.set(jsni);
      int offset = lineTracker.getLineOffset(source.getLine())
          + source.getLineOffset() - 1;

      // Calculate the offset within the original JSNI method declaration
      offset -= JS_FUNCTION_HEADER.length();
      offset += jsniMethod.indexOf(JSNI_BLOCK_START);
      offset += JSNI_BLOCK_START.length();

      // The errors we get back from JsParser tend to be all lower case, so
      // for consistency with JDT errors we always capitalize the first char
      String errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.length() > 0) {
        char[] errorChars = e.getMessage().toCharArray();
        errorChars[0] = Character.toUpperCase(errorChars[0]);
        errorMessage = new String(errorChars);
      }

      // Rethrow a new exception with our fixed up source location
      throw new JavaScriptParseException(errorMessage, offset);
    }
  }

  private static List<JsniJavaRef> findJavaRefs(
      final MethodDeclaration jsniMethod) throws IOException,
      JavaScriptParseException, BadLocationException {
    final String jsniSource = JavaASTUtils.getSource(jsniMethod);
    ICompilationUnit cu = JavaASTUtils.getCompilationUnit(jsniMethod);
    final IPath cuPath = cu.getResource().getFullPath();
    final List<JsniJavaRef> javaRefs = new ArrayList<JsniJavaRef>();

    JsBlock js = JsniParser.parse(jsniSource);
    if (js != null) {
      // Visit the JavaScript AST to find all Java references
      new JsVisitor() {
        @SuppressWarnings("unchecked")
        @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          String ident = x.getIdent();
          if (ident.indexOf("@") != -1) {
            JsniJavaRef javaRef = JsniJavaRef.parse(ident);
            if (javaRef != null) {
              // Set the reference's Java source file
              javaRef.setSource(cuPath);

              // To get the Java reference offset, we have to do an indexOf on
              // its identifier. To make sure we catch multiple references to
              // the same Java element, we need to start at the index one past
              // the start of the last Java reference we found (if any)
              int fromIndex = 0;
              if (javaRefs.size() > 0) {
                fromIndex = javaRefs.get(javaRefs.size() - 1).getOffset()
                    - jsniMethod.getStartPosition() + 1;
              }
              int offset = jsniSource.indexOf(ident, fromIndex)
                  + jsniMethod.getStartPosition();

              // Set the reference's offset within the Java source file
              javaRef.setOffset(offset);

              javaRefs.add(javaRef);
            }
          }
        }
      }.accept(js);
    }

    return javaRefs;
  }

  @SuppressWarnings("serial")
  private static JsBlock parseFunctionBlock(String js, final int startLine)
      throws JsParserException, IOException {

    JsProgram jsPgm = new JsProgram();
    StringReader r = new StringReader(js);
    List<JsStatement> stmts = JsParser.parse(new SourceInfoAdapter() {
      @Override
      public int getStartLine() {
        return startLine;
      }
    }, jsPgm.getScope(), r);

    // Rip the body out of the parsed function and attach the JavaScript
    // AST to the method.
    //
    JsFunction fn = (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
    return fn.getBody();
  }

  private static GWTJavaProblem validateJavaRef(MethodDeclaration jsniMethod,
      JsniJavaRef ref) {
    ICompilationUnit cu = JavaASTUtils.getCompilationUnit(jsniMethod);

    try {
      ref.resolveJavaElement(cu.getJavaProject());
      return null;

    } catch (UnresolvedJsniJavaRefException e) {
      // A null problem type indicates that we should ignore the unresolved
      // reference. This happens, for example, on @null::nullMethod().
      if (e.getProblemType() == null) {
        return null;
      }

      // If we did find an unresolved Java reference, return a problem marker
      // for it with the appropriate type and error message
      int offset = 0, length = 0;
      String[] messageArgs = new String[0];

      switch (e.getProblemType()) {
        case JSNI_JAVA_REF_UNRESOLVED_TYPE:
          offset = ref.getClassOffset();
          length = ref.className().length();
          messageArgs = new String[] {ref.className()};
          break;
        case JSNI_JAVA_REF_NO_MATCHING_CTOR:
          offset = ref.getMemberOffset();
          length = ref.memberName().length();
          messageArgs = new String[] {
              ref.readableMemberSignature(), ref.simpleClassName()};
          break;
        case JSNI_JAVA_REF_MISSING_METHOD:
          offset = ref.getMemberOffset();
          length = ref.memberName().length();
          messageArgs = new String[] {ref.simpleClassName(), ref.memberName()};
          break;
        case JSNI_JAVA_REF_NO_MATCHING_METHOD:
          offset = ref.getMemberOffset();
          length = ref.memberName().length();
          messageArgs = new String[] {
              ref.readableMemberSignature(), ref.simpleClassName()};
          break;
        case JSNI_JAVA_REF_MISSING_FIELD:
          offset = ref.getMemberOffset();
          length = ref.memberName().length();
          messageArgs = new String[] {ref.className(), ref.memberName()};
          break;
        default:
          assert (false);
          return null;
      }

      return GWTJavaProblem.create(jsniMethod, offset, length,
          e.getProblemType(), messageArgs);
    }
  }

  private JsniParser() {
    // Not instantiable
  }
}
