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
package com.google.gdt.eclipse.core.markers;

import com.google.gdt.eclipse.core.JavaASTUtils;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.text.MessageFormat;

/**
 * Defines a custom Java problem. Our problems reuse the standard Java problem
 * marker type, so we can transparently integrate with the Java Editor. For
 * example, the JDT editor will only allow inline quick fixes on Java problems,
 * and will not work on any other types of problems.
 * 
 * @param <T> the problem type
 */
public abstract class GdtJavaProblem<T extends IGdtProblemType> extends
    CategorizedProblem {

  protected static final String[] NO_STRINGS = new String[0];

  protected static <T extends IGdtProblemType> GdtJavaProblem<T> createProblem(
      IGdtJavaProblemFactory<T> factory, ASTNode node, T problemType,
      String[] messageArgs, String[] problemArgs) {

    return createProblem(factory, node, node.getStartPosition(),
        node.getLength(), problemType, messageArgs, problemArgs);
  }

  /**
   * Constructs a problem of a particular type with specified message and
   * problem arguments.
   */
  protected static <T extends IGdtProblemType> GdtJavaProblem<T> createProblem(
      IGdtJavaProblemFactory<T> factory, ASTNode node, int offset, int length,
      T problemType, String[] messageArgs, String[] problemArgs) {

    // Look up the problem severity in the workspace settings
    GdtProblemSeverity severity = GdtProblemSeverities.getInstance().getSeverity(
        problemType);

    if (severity != GdtProblemSeverity.IGNORE) {
      return factory.createProblem(node, offset, length, problemType, severity,
          messageArgs, problemArgs);
    }
    return null;
  }

  private static String getFileNameFromASTNode(ASTNode node) {
    ICompilationUnit compilationUnit = JavaASTUtils.getCompilationUnit(node);
    if (compilationUnit != null) {
      return compilationUnit.getElementName();
    }
    // We may have an unattached AST (does not belong to the Java Model)
    return "";
  }

  final int column;

  private final String filename;

  private final int id;

  private final String message;

  private final String[] problemArguments;

  private final T problemType;

  private final GdtProblemSeverity severity;

  private int startPosition, endPosition, line;

  protected GdtJavaProblem(ASTNode node, int offset, int length, T problemType,
      GdtProblemSeverity severity, String[] messageArguments,
      String[] problemArguments) {
    this.id = problemType.getProblemId();

    this.filename = getFileNameFromASTNode(node);
    this.startPosition = offset;
    this.endPosition = offset + length - 1;
    CompilationUnit cu = (CompilationUnit) node.getRoot();
    this.line = cu.getLineNumber(node.getStartPosition());
    this.column = cu.getColumnNumber(node.getStartPosition());

    this.problemType = problemType;
    this.severity = severity;
    this.message = MessageFormat.format(problemType.getMessage(),
        (Object[]) messageArguments);
    this.problemArguments = problemArguments;
  }

  protected GdtJavaProblem(String filename, int offset, int length, int line,
      int column, T problemType, GdtProblemSeverity severity,
      String[] messageArguments, String[] problemArguments) {
    this.id = problemType.getProblemId();

    this.filename = filename;
    this.startPosition = offset;
    this.endPosition = offset + length - 1;
    this.line = line;
    this.column = column;

    this.problemType = problemType;
    this.severity = severity;
    this.message = MessageFormat.format(problemType.getMessage(),
        (Object[]) messageArguments);
    this.problemArguments = problemArguments;
  }

  public String[] getArguments() {
    // Returning null here causes an NPE when JDT is creating markers from this
    // problem
    return problemArguments == null ? NO_STRINGS : problemArguments;
  }

  @Override
  public int getCategoryID() {
    return CAT_UNSPECIFIED;
  }

  public int getID() {
    return this.id;
  }

  public String getMessage() {
    return message;
  }

  public char[] getOriginatingFileName() {
    return this.filename.toCharArray();
  }

  public T getProblemType() {
    return this.problemType;
  }

  public GdtProblemSeverity getSeverity() {
    return this.severity;
  }

  public int getSourceColumnNumber() {
    return this.column;
  }

  public int getSourceEnd() {
    return this.endPosition;
  }

  public int getSourceLineNumber() {
    return this.line;
  }

  public int getSourceStart() {
    return startPosition;
  }

  public boolean isError() {
    return (severity == GdtProblemSeverity.ERROR);
  }

  public boolean isWarning() {
    return (severity == GdtProblemSeverity.WARNING);
  }

  public void setSourceEnd(int sourceEnd) {
    this.endPosition = sourceEnd;
  }

  public void setSourceLineNumber(int lineNumber) {
    this.line = lineNumber;
  }

  public void setSourceStart(int sourceStart) {
    this.startPosition = sourceStart;
  }

}