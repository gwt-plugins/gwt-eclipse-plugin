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

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Proposes completions for JSNI method bodies. This proposal computer looks at
 * the current native method and tries to propose completions that would make it
 * a proper JSNI method body.
 */
@SuppressWarnings("restriction")
public class JsniMethodBodyCompletionProposalComputer implements
    IJavaCompletionProposalComputer {
  public static final String SIMPLE_EXTENSION_ID = "jsniCompletionProposalComputer";
  public static final String THIS = "this";
  public static final String WND = "$wnd";

  private static final String JSNI_METHOD_OPEN_BRACE = "/*-{";
  
  private static final List<IContextInformation> NO_CONTEXTS = Collections.emptyList();

  private static final List<ICompletionProposal> NO_PROPOSALS = Collections.emptyList();

  /**
   * Return the property name based on the property accessor name. This method
   * is default access to facilitate testing.
   */
  static String computePropertyNameFromAccessorMethodName(String prefix,
      String propertyAccessorMethodName) {
    assert (propertyAccessorMethodName.startsWith(prefix));
    String propertyName = propertyAccessorMethodName.substring(prefix.length());
    if (propertyName.length() > 0) {
      // Lower case the first letter of the property name.
      propertyName = propertyName.substring(0, 1).toLowerCase()
          + propertyName.substring(1);
    }

    return propertyName;
  }

  /**
   * Creates the standard JSNI delimiter blocks. This method is default access
   * to facilitate testing.
   */
  static String createJsniBlock(IJavaProject project, String body,
      int indentationUnits) {
    StringBuilder sb = new StringBuilder();
    sb.append("/*-{\n");
    sb.append(CodeFormatterUtil.createIndentString(indentationUnits + 1,
        project));

    if (body != null) {
      sb.append(body);
    }

    sb.append("\n");
    sb.append(CodeFormatterUtil.createIndentString(indentationUnits, project));
    sb.append("}-*/;\n");
    return sb.toString();
  }

  /**
   * Returns the indentation units for a given project, document, line and line
   * offset.
   */
  static int measureIndentationUnits(IDocument document,
      int lineOfInvocationOffset, int lineOffset, IJavaProject project)
      throws BadLocationException {
    Map<?, ?> options = project.getOptions(true);
    String lineText = document.get(lineOffset,
        document.getLineLength(lineOfInvocationOffset));
    int indentationUnits = IndentManipulation.measureIndentUnits(lineText,
        IndentManipulation.getTabWidth(options),
        IndentManipulation.getIndentWidth(options));
    return indentationUnits;
  }

  private static String createJsIndexedPropertyReadExpression(String propName,
      String indexerName, boolean isStatic) {
    String propRefExpression;
    if (propName.length() > 0) {
      propRefExpression = "." + propName;
    } else {
      propRefExpression = "";
    }
    return (isStatic ? WND : THIS) + propRefExpression + "[" + indexerName
        + "]";
  }

  private static String createJsIndexedPropertyWriteExpression(String propName,
      String indexerName, String value, boolean isStatic) {
    return createJsIndexedPropertyReadExpression(propName, indexerName,
        isStatic)
        + " = " + value + ";";
  }

  /**
   * Creates a JSNI method invocation expression.
   */
  private static String createJsMethodInvocationExpression(String methodName,
      boolean isStatic, String... paramNames) {
    StringBuilder sb = new StringBuilder();
    sb.append(isStatic ? WND : THIS);
    sb.append(".");
    sb.append(methodName);
    sb.append("(");
    for (int i = 0; i < paramNames.length; i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(paramNames[i]);
    }
    sb.append(");");

    return sb.toString();
  }

  private static String createJsPropertyReadExpression(String propName,
      boolean isStatic) {
    return (isStatic ? WND : THIS) + "." + propName + ";";
  }

  private static String createJsPropertyWriteExpression(String propName,
      String propValue, boolean isStatic) {
    StringBuilder sb = new StringBuilder();

    sb.append(isStatic ? WND : THIS);
    if (propName.length() > 0) {
      sb.append(".");
      sb.append(propName);
    }
    sb.append(" = ");
    sb.append(propValue);
    sb.append(";");
    return sb.toString();
  }

  private static JavaCompletionProposal createProposal(int flags,
      String replacementString, int replacementOffset, int numCharsFilled, 
      int numCharsToOverwrite, String displayString) {
    Image image;
    GWTPlugin plugin = GWTPlugin.getDefault();
    if (Flags.isPublic(flags)) {
      image = plugin.getImage(GWTImages.JSNI_PUBLIC_METHOD_SMALL);
    } else if (Flags.isPrivate(flags)) {
      image = plugin.getImage(GWTImages.JSNI_PRIVATE_METHOD_SMALL);
    } else if (Flags.isProtected(flags)) {
      image = plugin.getImage(GWTImages.JSNI_PROTECTED_METHOD_SMALL);
    } else {
      image = plugin.getImage(GWTImages.JSNI_DEFAULT_METHOD_SMALL);
    }
    replacementString = replacementString.substring(numCharsFilled);
    return new JavaCompletionProposal(replacementString, replacementOffset,
        numCharsToOverwrite, image, "/*-{ " + displayString + " }-*/;", 0);
  }

  /**
   * Returns <code>true</code> if the type can be used as an index of an indexed
   * property.
   * 
   * @param typeSignature type signature
   * @return <code>true</code> if the type can be used as an index of an indexed
   *         property
   */
  private static boolean isIndexType(String typeSignature) {
    return Signature.SIG_INT.equals(typeSignature)
        || Signature.SIG_LONG.equals(typeSignature)
        || Signature.SIG_SHORT.equals(typeSignature)
        || Signature.SIG_BYTE.equals(typeSignature)
        || Signature.SIG_CHAR.equals(typeSignature);
  }

  public List<ICompletionProposal> computeCompletionProposals(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    if (!(context instanceof JavaContentAssistInvocationContext)) {
      // Not in a java content assist content.
      return NO_PROPOSALS;
    }

    try {
      JavaContentAssistInvocationContext jcaic = (JavaContentAssistInvocationContext) context;
      ICompilationUnit compilationUnit = jcaic.getCompilationUnit();

      /*
       * Resolves issue 3560,
       * http://code.google.com/p/google-web-toolkit/issues/detail?id=3650. We
       * need to have a reconciled compilation unit if we are to use it, but we
       * don't want to tickle the hierarchy view bug.
       */
      compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);

      int invocationOffset = jcaic.getInvocationOffset();
      IJavaElement elementAt = compilationUnit.getElementAt(invocationOffset);
      
      if (elementAt == null) {
        // Can't determine the element at the specified offset.
        return NO_PROPOSALS;
      }

      if (IJavaElement.METHOD != elementAt.getElementType()) {
        // Not a method.
        return NO_PROPOSALS;
      }

      IMethod method = (IMethod) elementAt;

      IType thisType = method.getDeclaringType();
      if (thisType.isInterface()) {
        // Don't propose anything for interfaces.
        return NO_PROPOSALS;
      }
      
      ISourceRange sourceRange = method.getSourceRange();
      if (sourceRange == null) {
        // No source code.
        // TODO: Is this possible?
        return NO_PROPOSALS;
      }
      
      String methodSource = method.getSource();
      int invocationIdx = invocationOffset - sourceRange.getOffset();
      
      // Sometimes, if incomplete JSNI method has /* and is followed by any global 
      // comment of format /*..*/, compilation unit separates the code after
      // incomplete JSNI method's /* as a separate block from the incomplete method.
      // So we need to check whether the block before the invocation offset's block
      // is the incomplete JSNI method that we are interested in.
      

      IJavaElement prevElement = compilationUnit.getElementAt(sourceRange.getOffset() - 1);
      if (IJavaElement.METHOD == prevElement.getElementType()){
        
        IMethod prevMethod = (IMethod) prevElement;
        
        if ((prevMethod.getDeclaringType().isInterface() == false)
            && (Flags.isNative(prevMethod.getFlags()) == true)){
          
          String prevMethodSource = prevMethod.getSource();
          if (prevMethodSource.trim().endsWith(")") == true){
            methodSource = prevMethodSource.concat(methodSource);
            method = prevMethod;
            invocationIdx += prevMethodSource.length();
          }
        }
      }

      int flags = method.getFlags();
      if (!Flags.isNative(flags)) {
        // If the method is not native then no proposals.
        return NO_PROPOSALS;
      }
      
      // Eliminating comments that might precede native method declaration, so that
      // following code can safely assume first ')' found is that of function declaration.
      int idxMultiLineComment = methodSource.trim().indexOf("/*");
      int idxSingleLineComment = methodSource.trim().indexOf("//");
      while ((idxMultiLineComment == 0) || (idxSingleLineComment == 0)) {
        if (idxMultiLineComment == 0) {
          invocationIdx -= methodSource.indexOf("*/") + 2;
          methodSource = methodSource.substring(methodSource.indexOf("*/") + 2);
        } else {
          invocationIdx -= methodSource.indexOf('\n') + 1;
          methodSource = methodSource.substring(methodSource.indexOf('\n') + 1);
        } 
        idxMultiLineComment = methodSource.trim().indexOf("/*");
        idxSingleLineComment = methodSource.trim().indexOf("//");
      }

      // Eliminating any JSNI method that might follow the JSNI method in consideration.
      int jsniMethodOpenIdx = methodSource.indexOf(JSNI_METHOD_OPEN_BRACE);
      if (jsniMethodOpenIdx != -1) {
        int jsniMethodCloseBracketIdx = methodSource.indexOf(")");
        String tempString = methodSource.substring(jsniMethodCloseBracketIdx, jsniMethodOpenIdx);
        if (tempString.trim().length() != 1) {
          methodSource = methodSource.substring(0, jsniMethodOpenIdx - 1);
        } else {
          int nextJsniMethodOpenIdx = 
              methodSource.substring(jsniMethodOpenIdx + 4).indexOf(JSNI_METHOD_OPEN_BRACE);
          if (nextJsniMethodOpenIdx != -1) {
            nextJsniMethodOpenIdx += jsniMethodOpenIdx + 4;
            methodSource = methodSource.substring(0, nextJsniMethodOpenIdx - 1);
          }
        }
      }
      
      // Check if the JSNI method is already complete.
      if (methodSource.indexOf("}-*/;") != -1) {
        // JSNI method is complete.
        return NO_PROPOSALS;
      }
      
      // Check position of invocation offset.
      int numCharsFilled = 0, numCharsToOverwrite = 0;
      
      String tempString = "";
      if (methodSource.substring(methodSource.indexOf(")") + 1).trim().indexOf("/") != -1){
        tempString = methodSource.substring(methodSource.indexOf(")"), methodSource.indexOf("/"));
      }
      
      if ((methodSource.substring(methodSource.indexOf(")") + 1).trim().indexOf("/") == 0)
          && (tempString.indexOf('\n') == -1)){

        int jsniMethodOpenSlashIdx = methodSource.indexOf("/");

        if (jsniMethodOpenSlashIdx > invocationIdx) {
          // Invocation index is placed before JSNI open slash.
          return NO_PROPOSALS;
        }
        
        String jsniCompletedString = methodSource.substring(jsniMethodOpenSlashIdx, invocationIdx);

        if (jsniCompletedString.indexOf(JSNI_METHOD_OPEN_BRACE) != -1) {
          jsniCompletedString = jsniCompletedString.trim();
        }
        
        if (JSNI_METHOD_OPEN_BRACE.startsWith(jsniCompletedString)) {
          numCharsFilled = jsniCompletedString.length();
        } else {
          // Invocation index placement does not allow auto-completion.
          return NO_PROPOSALS;
        }
      } else {
        int jsniMethodCloseBracketIdx = methodSource.indexOf(")") + 1;
        
        if (jsniMethodCloseBracketIdx > invocationIdx) {
          // Invocation index is not placed after method's close bracket.
          return NO_PROPOSALS;
        }
        if (methodSource.substring(jsniMethodCloseBracketIdx, invocationIdx).trim().length() != 0) {
          // Do not auto-complete if there is anything other than space between the two indices.
          return NO_PROPOSALS;
        }
      }
      
      methodSource = methodSource.substring(invocationIdx);
      int endIdx = methodSource.length();
      if (methodSource.indexOf(" ") != -1) {
        endIdx = methodSource.indexOf(" ");
        if (methodSource.indexOf("\n") != -1 && (endIdx > methodSource.indexOf("\n"))) {
          endIdx = methodSource.indexOf("\n");
        }
      } else if (methodSource.indexOf("\n") != -1) {
        endIdx = methodSource.indexOf("\n");
      }
      
      numCharsToOverwrite = methodSource.substring(0, endIdx).trim().length();
      
      IDocument document = jcaic.getDocument();
      int lineOfInvocationOffset = document.getLineOfOffset(invocationOffset);
      int lineOffset = document.getLineOffset(lineOfInvocationOffset);

      IJavaProject project = jcaic.getProject();
      int indentationUnits = measureIndentationUnits(document,
          lineOfInvocationOffset, lineOffset, project);

      List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

      proposeEmptyJsniBlock(project, method, invocationOffset,
          indentationUnits, proposals, numCharsFilled, numCharsToOverwrite);

      boolean isStatic = Flags.isStatic(flags);
      if (method.getReturnType().equals(Signature.SIG_VOID)) {
        proposeSetters(project, method, invocationOffset, indentationUnits,
            isStatic, proposals, numCharsFilled, numCharsToOverwrite);
      } else {
        proposeGetters(project, method, invocationOffset, indentationUnits,
            isStatic, proposals, numCharsFilled, numCharsToOverwrite);
      }
      return proposals;
    } catch (JavaModelException e) {
      // Default to no proposals.
    } catch (BadLocationException e) {
      // Default to no proposals.
    }

    return NO_PROPOSALS;
  }

  public List<IContextInformation> computeContextInformation(
      ContentAssistInvocationContext context, IProgressMonitor monitor) {
    return NO_CONTEXTS;
  }

  public String getErrorMessage() {
    // Default to no error reporting.
    return null;
  }

  public void sessionEnded() {
  }

  public void sessionStarted() {
  }

  /**
   * Proposes a JSNI method of the form <code>return this.property[x]</code> if
   * the java method has a single integral type parameter.
   */
  private void maybeProposeIndexedPropertyRead(IJavaProject project,
      IMethod method, int invocationOffset, int indentationUnits,
      List<ICompletionProposal> proposals, String propertyName,
      String[] parameterNames, boolean isStatic, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    if (parameterNames.length != 1) {
      return;
    }

    String indexParameterType = method.getParameterTypes()[0];
    if (isIndexType(indexParameterType)) {
      String expression = "return "
          + createJsIndexedPropertyReadExpression(propertyName,
              parameterNames[0], isStatic) + ";";

      String code = createJsniBlock(project, expression, indentationUnits);
      proposals.add(createProposal(method.getFlags(), code, invocationOffset,
          numCharsFilled, numCharsToOverwrite, expression));
    }
  }

  /**
   * Proposes a JSNI method of the form
   * <code>this.property[x] = newPropertyValue</code> if the java method has two
   * parameters and the first is an integral type.
   */
  private void maybeProposeIndexedPropertyWrite(IJavaProject project,
      IMethod method, String propertyName, int invocationOffset,
      int indentationUnits, boolean isStatic,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String[] parameterNames = method.getParameterNames();
    if (parameterNames.length != 2) {
      return;
    }

    String indexParameterType = method.getParameterTypes()[0];
    if (isIndexType(indexParameterType)) {
      String expression = createJsIndexedPropertyWriteExpression(propertyName,
          parameterNames[0], parameterNames[1], isStatic);
      String code = createJsniBlock(project, expression, indentationUnits);
      proposals.add(createProposal(method.getFlags(), code, invocationOffset,
          numCharsFilled, numCharsToOverwrite, expression));
    }
  }

  /**
   * Proposes a JSNI method of the form <code>return this.property</code> if the
   * java method has no parameters.
   */
  private void maybeProposePropertyRead(IJavaProject project, IMethod method,
      String propertyName, int invocationOffset, int indentationUnits,
      boolean isStatic, List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String[] parameterNames = method.getParameterNames();
    if (parameterNames.length == 0 && propertyName.length() > 0) {
      String expression = "return "
          + createJsPropertyReadExpression(propertyName, isStatic);

      String code = createJsniBlock(project, expression, indentationUnits);
      proposals.add(createProposal(method.getFlags(), code, invocationOffset,
          numCharsFilled, numCharsToOverwrite, expression));
    }
  }

  /**
   * Proposes a JSNI method of the form
   * <code>this.property = newPropertyValue</code> if the java method has a
   * single parameter.
   */
  private void maybeProposePropertyWrite(IJavaProject project, IMethod method,
      String propertyName, int invocationOffset, int indentationUnits,
      boolean isStatic, List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String[] parameterNames = method.getParameterNames();
    if (parameterNames.length == 1 && propertyName.length() > 0) {
      String expression = createJsPropertyWriteExpression(propertyName,
          parameterNames[0], isStatic);

      String code = createJsniBlock(project, expression, indentationUnits);
      proposals.add(createProposal(method.getFlags(), code, invocationOffset,
          numCharsFilled, numCharsToOverwrite, expression));
    }
  }

  /**
   * Proposes an JSNI method where the cursor is inside of the JSNI block and
   * set to the proper indentation for the file.
   */
  private void proposeEmptyJsniBlock(IJavaProject project, IMethod method,
      int invocationOffset, int indentationUnits,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String code = createJsniBlock(project, "", indentationUnits);
    int cursorPosition = (CodeFormatterUtil.createIndentString(
        indentationUnits + 1, project)).length() + (5 - numCharsFilled);
    JavaCompletionProposal javaCompletionProposal = createProposal(
        method.getFlags(), code, invocationOffset, numCharsFilled, numCharsToOverwrite, "");
    javaCompletionProposal.setCursorPosition(cursorPosition);
    proposals.add(javaCompletionProposal);
  }

  /**
   * Proposes a getter that is assumed to delegate to a method with the same
   * name as the java method.
   */
  private void proposeGetterDelegate(IJavaProject project, IMethod method,
      int invocationOffset, int indentationUnits, boolean isStatic,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String methodName = method.getElementName();
    String[] parameterNames = method.getParameterNames();
    String expression = "return "
        + createJsMethodInvocationExpression(methodName, isStatic,
            parameterNames);
    String code = createJsniBlock(project, expression, indentationUnits);
    proposals.add(createProposal(method.getFlags(), code, invocationOffset,
        numCharsFilled, numCharsToOverwrite, expression));
  }

  private void proposeGetters(IJavaProject project, IMethod method,
      int invocationOffset, int indentationUnits, boolean isStatic,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {

    proposeGetterDelegate(project, method, invocationOffset, indentationUnits,
        isStatic, proposals, numCharsFilled, numCharsToOverwrite);

    // Maybe propose bean-style getter.
    String methodName = method.getElementName();
    String propertyName = methodName;
    if (methodName.startsWith("get")) {
      propertyName = computePropertyNameFromAccessorMethodName("get",
          methodName);
    } else if (methodName.startsWith("is")) {
      propertyName = computePropertyNameFromAccessorMethodName("is", methodName);
    }

    String[] parameterNames = method.getParameterNames();
    if (propertyName != methodName && propertyName.length() > 0) {
      String expression = "return "
          + createJsMethodInvocationExpression(propertyName, isStatic,
              parameterNames);
      String code = createJsniBlock(project, expression, indentationUnits);
      proposals.add(createProposal(method.getFlags(), code, invocationOffset,
          numCharsFilled, numCharsToOverwrite, expression));
    }

    maybeProposePropertyRead(project, method, propertyName, invocationOffset,
        indentationUnits, isStatic, proposals,
        numCharsFilled, numCharsToOverwrite);

    maybeProposeIndexedPropertyRead(project, method, invocationOffset,
        indentationUnits, proposals, propertyName, parameterNames, isStatic,
        numCharsFilled, numCharsToOverwrite);
  }

  /**
   * Proposes a setter that is assumed to delegate to a method with the same
   * name as the java method.
   */
  private void proposeSetterDelegate(IJavaProject project, IMethod method,
      int invocationOffset, int indentationUnits, boolean isStatic,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {
    String[] parameterNames = method.getParameterNames();
    String expression = createJsMethodInvocationExpression(
        method.getElementName(), isStatic, parameterNames);
    String code = createJsniBlock(project, expression, indentationUnits);
    proposals.add(createProposal(method.getFlags(), code, invocationOffset,
        numCharsFilled, numCharsToOverwrite, expression));
  }

  private void proposeSetters(IJavaProject project, IMethod method,
      int invocationOffset, int indentationUnits, boolean isStatic,
      List<ICompletionProposal> proposals, int numCharsFilled, 
      int numCharsToOverwrite) throws JavaModelException {

    proposeSetterDelegate(project, method, invocationOffset, indentationUnits,
        isStatic, proposals, numCharsFilled, numCharsToOverwrite);

    String[] parameterNames = method.getParameterNames();
    String methodName = method.getElementName();
    String propertyName = methodName;
    if (methodName.startsWith("set")) {
      propertyName = computePropertyNameFromAccessorMethodName("set",
          methodName);
      if (propertyName.length() > 0) {
        String expression = createJsMethodInvocationExpression(propertyName,
            isStatic, parameterNames);
        String code = createJsniBlock(project, expression, indentationUnits);
        proposals.add(createProposal(method.getFlags(), code, invocationOffset,
            numCharsFilled, numCharsToOverwrite, expression));
      }
    }

    maybeProposePropertyWrite(project, method, propertyName, invocationOffset,
        indentationUnits, isStatic, proposals,
        numCharsFilled, numCharsToOverwrite);

    maybeProposeIndexedPropertyWrite(project, method, propertyName,
        invocationOffset, indentationUnits, isStatic, proposals,
        numCharsFilled, numCharsToOverwrite);
  }
}
