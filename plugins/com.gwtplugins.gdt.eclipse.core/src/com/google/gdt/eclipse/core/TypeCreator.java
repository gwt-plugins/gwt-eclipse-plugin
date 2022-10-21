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
package com.google.gdt.eclipse.core;

import com.google.gdt.eclipse.platform.jdt.formatter.CodeFormatterFlags;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.CorextMessages;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.ValidateEditException;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

/**
 * Generates new top-level types. By itself, it creates types with empty bodies, but subclasses can override
 * createTypeMembers() to populate the type.
 *
 * NOTE: Much of the implementation is based on the type generation code in
 * {@link org.eclipse.jdt.ui.wizards.NewTypeWizardPage}.
 */
@SuppressWarnings("restriction")
public class TypeCreator {

  /**
   * Type of type to create.
   */
  public enum ElementType {
    CLASS, INTERFACE
  }

  private final boolean addComments;

  private final ElementType elementType;

  private final String[] interfaces;

  private final String lineDelimiter;

  private final IPackageFragment pckg;

  private final String simpleTypeName;

  public TypeCreator(IPackageFragment pckg, String simpleTypeName, ElementType elementType, String[] interfaces,
      boolean addComments) {
    this.pckg = pckg;
    this.simpleTypeName = simpleTypeName;
    this.elementType = elementType;
    this.interfaces = interfaces;
    this.addComments = addComments;
    this.lineDelimiter = StubUtility.getLineDelimiterUsed(pckg.getJavaProject());
  }

  /**
   * Creates the new type.
   *
   * NOTE: If this method throws a {@link JavaModelException}, its {@link JavaModelException#getJavaModelStatus()}
   * method can provide more detailed information about the problem.
   */
  public IType createType() throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();

    ICompilationUnit cu = null;
    try {
      String cuName = simpleTypeName + ".java";

      // Create empty compilation unit
      cu = pckg.createCompilationUnit(cuName, "", false, monitor);
      cu.becomeWorkingCopy(monitor);
      IBuffer buffer = cu.getBuffer();

      // Need to create a minimal type stub here so we can create an import
      // rewriter a few lines down. The rewriter has to be in place when we
      // create the real type stub, so we can use it to transform the names of
      // any interfaces this type extends/implements.
      String dummyTypeStub = createDummyTypeStub();

      // Generate the content (file comment, package declaration, type stub)
      String cuContent = createCuContent(cu, dummyTypeStub);
      buffer.setContents(cuContent);

      ImportRewrite imports = StubUtility.createImportRewrite(cu, true);

      // Create the real type stub and replace the dummy one
      int typeDeclOffset = cuContent.lastIndexOf(dummyTypeStub);
      if (typeDeclOffset != -1) {
        String typeStub = createTypeStub(cu, imports);
        buffer.replace(typeDeclOffset, dummyTypeStub.length(), typeStub);
      }

      // Let our subclasses add members
      IType type = cu.getType(simpleTypeName);
      createTypeMembers(type, imports);

      // Rewrite the imports and apply the edit
      TextEdit edit = imports.rewriteImports(monitor);
      applyEdit(cu, edit, false, null);

      // Format the Java code
      String formattedSource = formatJava(type);
      buffer.setContents(formattedSource);

      // Save the new type
      JavaModelUtil.reconcile(cu);
      cu.commitWorkingCopy(true, monitor);

      return type;
    } finally {
      if (cu != null) {
        cu.discardWorkingCopy();
      }
    }
  }

  // Copied from JavaModelUtil
  private static void applyEdit(ICompilationUnit cu, TextEdit edit, boolean save, IProgressMonitor monitor)
      throws CoreException, ValidateEditException {
    IFile file = (IFile) cu.getResource();
    if (!save || !file.exists()) {
      cu.applyTextEdit(edit, monitor);
    } else {
      if (monitor == null) {
        monitor = new NullProgressMonitor();
      }
      monitor.beginTask(CorextMessages.JavaModelUtil_applyedit_operation, 2);
      try {
        IStatus status = Resources.makeCommittable(file, null);
        if (!status.isOK()) {
          throw new ValidateEditException(status);
        }

        cu.applyTextEdit(edit, SubMonitor.convert(monitor, 1));

        cu.save(SubMonitor.convert(monitor, 1), true);
      } finally {
        monitor.done();
      }
    }
  }

  /**
   * Subclasses should override this method to add methods and/or fields to the generated type.
   *
   * @param newType
   *          the newly-generated type
   * @param imports
   *          import rewriter
   * @throws CoreException
   */
  protected void createTypeMembers(IType newType, ImportRewrite imports) throws CoreException {
    // Do nothing by default
  }

  private void addInterfaces(StringBuffer buffer, ImportRewrite imports) {
    if (elementType == ElementType.CLASS) {
      buffer.append(" implements ");
    } else {
      buffer.append(" extends ");
    }

    for (int i = 0; i < interfaces.length; i++) {
      buffer.append(imports.addImport(interfaces[i]));
      if (i < interfaces.length - 1) {
        buffer.append(',');
      }
    }
  }

  private String createCuContent(ICompilationUnit cu, String typeContent) throws CoreException {
    String fileComment = getFileComment(cu);
    String typeComment = getTypeComment(cu);
    IPackageFragment cuPckg = (IPackageFragment) cu.getParent();

    // Use the 'New Java File' code template specified by workspace preferences
    String content = CodeGeneration.getCompilationUnitContent(cu, fileComment, typeComment, typeContent, lineDelimiter);
    if (content != null) {
      // Parse the generated source to make sure it's error-free
      ASTParser parser = ASTParser.newParser(AST.JLS3);
      parser.setProject(cu.getJavaProject());
      parser.setSource(content.toCharArray());
      CompilationUnit unit = (CompilationUnit) parser.createAST(null);
      if ((cuPckg.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
        return content;
      }
    }

    // If we didn't have a template to use, just generate the source by hand
    StringBuffer buf = new StringBuffer();
    if (!cuPckg.isDefaultPackage()) {
      buf.append("package ").append(cuPckg.getElementName()).append(';');
    }
    buf.append(lineDelimiter).append(lineDelimiter);
    if (typeComment != null) {
      buf.append(typeComment).append(lineDelimiter);
    }
    buf.append(typeContent);
    return buf.toString();
  }

  private String createDummyTypeStub() {
    return "public class " + simpleTypeName + "{ }";
  }

  private String createTypeStub(ICompilationUnit cu, ImportRewrite imports) throws CoreException {
    StringBuffer buffer = new StringBuffer();

    // Default modifiers is just: public
    buffer.append("public ");

    String type = "";
    String templateID = "";
    switch (elementType) {
    case CLASS:
      type = "class ";
      templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
      break;
    case INTERFACE:
      type = "interface ";
      templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
      break;
    }
    buffer.append(type);
    buffer.append(simpleTypeName);

    addInterfaces(buffer, imports);

    buffer.append(" {").append(lineDelimiter);

    // Generate the type body according to the template in preferences
    String typeBody = CodeGeneration.getTypeBody(templateID, cu, simpleTypeName, lineDelimiter);
    if (typeBody != null) {
      buffer.append(typeBody);
    } else {
      buffer.append(lineDelimiter);
    }

    buffer.append('}').append(lineDelimiter);
    return buffer.toString();
  }

  private String formatJava(IType type) throws JavaModelException {
    String source = type.getCompilationUnit().getSource();
    CodeFormatter formatter = ToolFactory.createCodeFormatter(type.getJavaProject().getOptions(true));
    TextEdit formatEdit = formatter.format(CodeFormatterFlags.getFlagsForCompilationUnitFormat(), source, 0,
        source.length(), 0, lineDelimiter);
    if (formatEdit == null) {
      CorePluginLog.logError("Could not format source for " + type.getCompilationUnit().getElementName());
      return source;
    }

    Document document = new Document(source);
    try {
      formatEdit.apply(document);
      source = document.get();
    } catch (BadLocationException e) {
      CorePluginLog.logError(e);
    }

    source = Strings.trimLeadingTabsAndSpaces(source);
    return source;
  }

  private String getFileComment(ICompilationUnit cu) throws CoreException {
    if (addComments) {
      return CodeGeneration.getFileComment(cu, lineDelimiter);
    }
    return null;
  }

  private String getTypeComment(ICompilationUnit cu) {
    if (addComments) {
      try {
        String comment = CodeGeneration.getTypeComment(cu, simpleTypeName, new String[0], lineDelimiter);
        if (comment != null && isValidComment(comment)) {
          return comment;
        }
      } catch (CoreException e) {
        CorePluginLog.logError(e);
      }
    }
    return null;
  }

  private boolean isValidComment(String template) {
    IScanner scanner = ToolFactory.createScanner(true, false, false, false);
    scanner.setSource(template.toCharArray());
    try {
      int next = scanner.getNextToken();
      while (TokenScanner.isComment(next)) {
        next = scanner.getNextToken();
      }
      return next == ITerminalSymbols.TokenNameEOF;
    } catch (InvalidInputException e) {
      // If there are lexical errors, the comment is invalid
    }
    return false;
  }

}
