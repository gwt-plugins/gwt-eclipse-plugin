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
package com.google.gdt.eclipse.appengine.rpc.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates the request factory classes - service, request, proxy, locator and
 * requestfactory.
 */
@SuppressWarnings("restriction")
public class CompilationUnitCreator {

  /**
   * Class used in stub creation routines to add needed imports to a compilation
   * unit.
   */
  static class ImportsManager {

    private ImportRewrite importsRewrite;

    public ImportsManager(CompilationUnit astRoot) {
      importsRewrite = CodegenUtils.createImportRewrite(astRoot, true);
    }

    public String addImport(ITypeBinding typeBinding) {
      return importsRewrite.addImport(typeBinding);
    }

    public String addImport(String qualifiedTypeName) {
      return importsRewrite.addImport(qualifiedTypeName);
    }

    public String addStaticImport(String declaringTypeName, String simpleName,
        boolean isField) {
      return importsRewrite.addStaticImport(declaringTypeName, simpleName,
          isField);
    }

    public void create(boolean needsSave, IProgressMonitor monitor)
        throws CoreException {
      TextEdit edit = importsRewrite.rewriteImports(monitor);
      CodegenUtils.applyEdit(importsRewrite.getCompilationUnit(), edit,
          needsSave, null);
    }

    public ICompilationUnit getCompilationUnit() {
      return importsRewrite.getCompilationUnit();
    }

    public void removeImport(String qualifiedName) {
      importsRewrite.removeImport(qualifiedName);
    }

    public void removeStaticImport(String qualifiedName) {
      importsRewrite.removeStaticImport(qualifiedName);
    }
  }

  private String lineDelimiter;
  private IType current;
  private String entityIdType;
  private List<IType> entityList;
  private String serviceName;
  private List<IType> requestTypes = new ArrayList<IType>();

  public CompilationUnitCreator(List<IType> list, String fullyQualifiedName) {
    entityList = list;
    serviceName = fullyQualifiedName;
  }

  /**
   * 
   * @param type IType - entity to generate request factory code
   * @param pack IPackageFragment - package for the file
   * @param name String - name of the file
   * @param rpcType int - whether proxy, locator, service, request,
   *          requestfactory
   * @param monitor IProgressMonitor
   * @return IJavaElement - the created element
   * @throws CoreException
   */
  public IJavaElement create(IType type, IPackageFragment pack, String name,
      RpcType rpcType, IProgressMonitor monitor) throws CoreException {

    IJavaElement element = null;
    IType createdType = null;
    ImportsManager imports;
    ICompilationUnit connectedCU = null;
    current = type;
    lineDelimiter = System.getProperty("line.separator", "\n"); //$NON-NLS-N$

    try {
      ICompilationUnit parentCU = pack.createCompilationUnit(name + ".java", //$NON-NLS-N$
          "", true, new SubProgressMonitor(monitor, 1));
      parentCU.becomeWorkingCopy(new SubProgressMonitor(monitor, 1));
      connectedCU = parentCU;
      IBuffer buffer = parentCU.getBuffer();
      String simpleTypeStub = constructSimpleTypeStub(name);
      String content = CodeGeneration.getCompilationUnitContent(parentCU, null,
          null, simpleTypeStub, lineDelimiter);
      buffer.setContents(content);

      CompilationUnit astRoot = createASTForImports(parentCU);
      imports = new ImportsManager(astRoot);

      String typeContent;
      String annotation = "";
      List<String> interfaces = new ArrayList<String>();
      boolean isInterface = true;
      switch (rpcType) {
        case SERVICE:
          isInterface = false;
          break;
        case LOCATOR:
          isInterface = false;
          interfaces.add("com.google.web.bindery.requestfactory.shared.Locator"); //$NON-NLS-N$
          if (RequestFactoryUtils.shouldBeProxiedAsAnEntity(type)) {
            for (IMethod method : type.getMethods()) {
              if (method.getElementName().equals("getId")) { //$NON-NLS-N$
                entityIdType = Signature.toString(method.getReturnType());
              }
            }
          } else {
            entityIdType = "Void"; //$NON-NLS-N$
          }
          break;
        case PROXY:
          if (RequestFactoryUtils.shouldBeProxiedAsAnEntity(current)) {
            interfaces.add("com.google.web.bindery.requestfactory.shared.EntityProxy"); //$NON-NLS-N$
          } else {
            interfaces.add("com.google.web.bindery.requestfactory.shared.ValueProxy");//$NON-NLS-N$
          }
          annotation = "@ProxyForName(value=\"" + current.getFullyQualifiedName() //$NON-NLS-N$
              + "\",\nlocator = \"" + current.getFullyQualifiedName()
              + "Locator\")";
          break;
        case REQUEST:
          interfaces.add("com.google.web.bindery.requestfactory.shared.RequestContext");//$NON-NLS-N$
          annotation = "@ServiceName(\"" + serviceName //$NON-NLS-N$
              + "\")";
          break;
        case REQ_FACTORY:
          interfaces.add("com.google.web.bindery.requestfactory.shared.RequestFactory"); //$NON-NLS-N$
          break;
      }

      typeContent = constructTypeStub(parentCU, name, isInterface, interfaces,
          annotation, imports);
      int index = content.lastIndexOf(simpleTypeStub);
      if (index == -1) {
        AbstractTypeDeclaration typeNode = (AbstractTypeDeclaration) astRoot.types().get(
            0);
        int start = ((ASTNode) typeNode.modifiers().get(0)).getStartPosition();
        int end = typeNode.getStartPosition() + typeNode.getLength();
        buffer.replace(start, end - start, typeContent);
      } else {
        buffer.replace(index, simpleTypeStub.length(), typeContent);
      }

      createdType = parentCU.getType(name);

      ICompilationUnit cu = createdType.getCompilationUnit();
      imports.create(false, new SubProgressMonitor(monitor, 1));
      cu.reconcile(ICompilationUnit.NO_AST, false, null, null);

      astRoot = createASTForImports(cu);
      imports = new ImportsManager(astRoot);

      switch (rpcType) {
        case SERVICE:
          constructServiceBody(createdType, imports, new SubProgressMonitor(
              monitor, 1));
          break;
        case LOCATOR:
          constructLocatorBody(createdType, imports, new SubProgressMonitor(
              monitor, 1));
          break;
        case PROXY:
          constructProxyBody(createdType, imports, new SubProgressMonitor(
              monitor, 1));
          break;
        case REQUEST:
          requestTypes.add(createdType);
          constructRequestBody(createdType, imports, monitor);
          break;

        case REQ_FACTORY:
          constructReqFactoryBody(createdType, imports, new SubProgressMonitor(
              monitor, 1));
          break;
      }

      imports.create(false, new SubProgressMonitor(monitor, 1));
      removeUnusedImports(cu, getExistingImports(astRoot), false);
      cu.reconcile(ICompilationUnit.NO_AST, false, null, null);

      ISourceRange range = createdType.getSourceRange();

      IBuffer buf = cu.getBuffer();
      String originalContent = buf.getText(range.getOffset(), range.getLength());

      String formattedContent = CodegenUtils.format(originalContent,
          CodeFormatter.K_CLASS_BODY_DECLARATIONS);
      buf.replace(range.getOffset(), range.getLength(), formattedContent);

      cu.commitWorkingCopy(true, new SubProgressMonitor(monitor, 1));

      element = cu.getPrimaryElement();

    } finally {
      if (connectedCU != null) {
        connectedCU.discardWorkingCopy();
      }
    }
    monitor.done();
    return element;
  }

  private boolean addImport(Set<String> names, List<String> entityNameList,
      IImportDeclaration[] importDeclarations, ImportsManager imports) {
    boolean addedImport = false;
    for (String name : names) {
      if (Signature.getTypeSignatureKind(name) == Signature.CLASS_TYPE_SIGNATURE) {
        if (entityNameList.contains(Signature.toString(name))) {
          addedImport = true;
        } else {
          for (IImportDeclaration imp : importDeclarations) {
            if (imp.getElementName().contains(Signature.toString(name))) {
              imports.addImport(imp.getElementName());
            }
          }
        }
      }
    }
    return addedImport;
  }

  /**
   * Constructs the locator body
   */
  private void constructLocatorBody(IType type, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {

    List<IMethod> constructors = new ArrayList<IMethod>();
    boolean hasDefaultConstructor = false;
    for (IMethod method : current.getMethods()) {
      if (method.isConstructor()) {
        if ((method.getNumberOfParameters() == 0)
            && !Flags.isPrivate(method.getFlags())) {
          hasDefaultConstructor = true;
        }
        constructors.add(method);
      }
    }

    StringBuffer buf = new StringBuffer();
    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public ").append(current.getElementName()); //$NON-NLS-N$
    buf.append(" create(Class<? extends ").append(current.getElementName()).append( //$NON-NLS-1$
        "> clazz) {"); //$NON-NLS-2$
    buf.append(lineDelimiter);
    if (constructors.isEmpty() || hasDefaultConstructor) {
      buf.append("return new ").append(current.getElementName()).append("();"); //$NON-NLS-N$
    } else {
      buf.append("// TODO no default constructor, creation code cannot be generated"); //$NON-NLS-N$
      buf.append(lineDelimiter).append(
          "throw new RuntimeException(String.format"); //$NON-NLS-N$
      buf.append("(\"Cannot instantiate %s\", clazz.getCanonicalName()));"); //$NON-NLS-N$
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public ").append(current.getElementName()); //$NON-NLS-N$
    buf.append(" find(Class<? extends ").append(current.getElementName()).append( //$NON-NLS-1$
        "> clazz, Void id) {"); //$NON-NLS-2$
    buf.append(lineDelimiter);
    buf.append("return create(clazz);"); //$NON-NLS-N$
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public Class<").append(current.getElementName()); //$NON-NLS-N$
    buf.append("> getDomainType() {").append(lineDelimiter); //$NON-NLS-N$
    buf.append("return ").append(current.getElementName()).append(".class;"); //$NON-NLS-N$
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public ").append(entityIdType).append(" getId(").append( //$NON-NLS-1$//$NON-NLS-2$
        current.getElementName());
    buf.append(" domainObject) {").append(lineDelimiter); //$NON-NLS-N$
    if (entityIdType.equals("Void")) { //$NON-NLS-N$
      buf.append(" return null;"); //$NON-NLS-N$
    } else {
      buf.append("return domainObject.getId()"); //$NON-NLS-N$
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public Class<").append(entityIdType).append("> getIdType() {"); //$NON-NLS-1$//$NON-NLS-2$
    buf.append(lineDelimiter);
    if (entityIdType.equals("Void")) { //$NON-NLS-N$
      buf.append("return Void.class;"); //$NON-NLS-N$
    } else {
      buf.append("return ").append(entityIdType).append(".class;"); //$NON-NLS-1$//$NON-NLS-2$
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter); //$NON-NLS-N$
    buf.append("public Object getVersion(").append(current.getElementName()); //$NON-NLS-N$
    buf.append(" domainObject) {").append(lineDelimiter); //$NON-NLS-N$
    buf.append("return null;"); //$NON-NLS-N$
    buf.append(lineDelimiter).append("}");

    type.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Constructs the proxy body
   */
  private void constructProxyBody(IType type, ImportsManager imports,
      IProgressMonitor monitor) throws JavaModelException {

    imports.addImport("com.google.web.bindery.requestfactory.shared.ProxyForName"); //$NON-NLS-N$
    StringBuffer buf = new StringBuffer();
    IImportDeclaration[] importDeclarations = current.getCompilationUnit().getImports();
    // create list of entity names
    List<String> entityNameList = new ArrayList<String>();
    for (IType entity : entityList) {
      entityNameList.add(entity.getElementName());
    }

    // get list of all property methods
    List<IMethod> methods = new ArrayList<IMethod>();
    for (IMethod method : current.getMethods()) {
      if (RequestFactoryUtils.isPropertyAccessor(method)
          && !method.getElementName().equals("setId")) { //$NON-NLS-N$
        methods.add(method);
      }
    }
    boolean hasProxy;
    Set<String> typeNames;
    for (IMethod method : methods) {
      hasProxy = false;
      String methodName = method.getElementName();
      String signature = Signature.toString(method.getSignature(), methodName,
          method.getParameterNames(), false, true);

      typeNames = new HashSet<String>();
      typeNames.addAll(JavaUtils.getParamsAndReturnTypeNames(method));

      hasProxy = addImport(typeNames, entityNameList, importDeclarations,
          imports);

      // check if to replace Task with TaskProxy
      if (hasProxy) {
        for (String entityName : entityNameList) {
          if (signature.contains(entityName)) {
            if (methodName.contains("get") || methodName.contains("is") || methodName.contains("has")) { //$NON-NLS-N$           
              signature = signature.replaceFirst(entityName, entityName
                  + "Proxy"); //$NON-NLS-N$
            } else if (methodName.contains("set")) { //$NON-NLS-N$
              int index = signature.lastIndexOf(entityName);
              signature = signature.substring(0, index) + entityName + "Proxy" //$NON-NLS-N$
                  + signature.substring(index + entityName.length());
            }
          }
        }
      }
      buf.append(signature);
      buf.append(";"); //$NON-NLS-N$
      buf.append(lineDelimiter);
    }
    if (!methods.isEmpty()) {
      type.createMethod(buf.toString(), null, false, null);
    }
    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Constructs the requestFactory body
   */
  private void constructReqFactoryBody(IType type, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {

    for (IType request : requestTypes) {
      StringBuffer buf = new StringBuffer();
      buf.append(imports.addImport(request.getFullyQualifiedName()));
      buf.append(" "); //$NON-NLS-N$
      String name = request.getElementName();
      buf.append(name.substring(0, 1).toLowerCase() + name.substring(1));
      buf.append("();"); //$NON-NLS-N$
      buf.append(lineDelimiter);
      type.createMethod(buf.toString(), null, false, null);
    }

    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Constructs the request body
   */
  private void constructRequestBody(IType type, ImportsManager imports,
      IProgressMonitor monitor) throws JavaModelException {

    imports.addImport("com.google.web.bindery.requestfactory.shared.ServiceName");//$NON-NLS-N$
    StringBuffer buf = new StringBuffer();
    imports.addImport("com.google.web.bindery.requestfactory.shared.Request");//$NON-NLS-N$
    imports.addImport("java.util.List"); //$NON-NLS-N$

    for (IType entity : entityList) {
      buf.append(RequestFactoryCodegenUtils.constructRequestForEntity(entity.getElementName()));
    }

    type.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Constructs the service body
   */
  private void constructServiceBody(IType createdType, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {
    for (IType anEntity : entityList) {

      current = anEntity;
      String elementName = current.getElementName();
      constructServiceMethod(createdType, " create" + elementName, true, null, //$NON-NLS-N$
          null, imports, new SubProgressMonitor(monitor, 1));
      constructServiceMethod(createdType, " read" + elementName, true, //$NON-NLS-N$
          new String[] {"java.lang.Long"}, new String[] {" id"}, imports, //$NON-NLS-N$
          new SubProgressMonitor(monitor, 1));
      constructServiceMethod(createdType, " update" + elementName, true, //$NON-NLS-N$
          new String[] {current.getFullyQualifiedName()}, new String[] {" " //$NON-NLS-N$
              + elementName.toLowerCase()}, imports, new SubProgressMonitor(
              monitor, 1));
      constructServiceMethod(createdType, " delete" + elementName, false, //$NON-NLS-N$
          new String[] {current.getFullyQualifiedName()}, new String[] {" " //$NON-NLS-N$
              + elementName.toLowerCase()}, imports, //$NON-NLS-N$
          new SubProgressMonitor(monitor, 1));
      constructServiceMethod(createdType, " query" + elementName + "s", true, //$NON-NLS-N$
          null, null, imports, new SubProgressMonitor(monitor, 1));
    }
  }

  /**
   * Adds a method to the service class
   */
  private void constructServiceMethod(IType type, String methodName,
      boolean hasReturnType, String[] paramType, String[] paramName,
      ImportsManager imports, IProgressMonitor monitor) throws CoreException {

    StringBuffer buf = new StringBuffer();
    buf.append("public static "); //$NON-NLS-1$
    if (hasReturnType) {
      if (methodName.startsWith(" query")) { //$NON-NLS-1$
        buf.append(imports.addImport("java.util.List"));
        buf.append("<"); //$NON-NLS-N$
        buf.append(imports.addImport(current.getFullyQualifiedName()));
        buf.append(">"); //$NON-NLS-N$
      } else {
        buf.append(imports.addImport(current.getFullyQualifiedName()));
      }

    } else {
      buf.append("void"); //$NON-NLS-1$
    }
    buf.append(methodName);
    buf.append("("); //$NON-NLS-N$
    if (paramType != null) {
      for (int i = 0; i < paramType.length; i++) {
        if (i > 0) {
          buf.append(", "); //$NON-NLS-N$
        }
        buf.append(imports.addImport(paramType[i]));
        buf.append(paramName[i]);
      }
    }
    buf.append(") {"); //$NON-NLS-1$
    buf.append(lineDelimiter);
    final String content = CodeGeneration.getMethodBodyContent(
        type.getCompilationUnit(), type.getTypeQualifiedName('.'), methodName,
        false, "", lineDelimiter); //$NON-NLS-1$ //$NON-NLS-2$
    if (content != null && content.length() != 0)
      buf.append(content);
    if (hasReturnType) {
      buf.append("return null;"); //$NON-NLS-1$
    }
    buf.append(lineDelimiter);
    buf.append("}"); //$NON-NLS-1$
    type.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

  private String constructSimpleTypeStub(String name) {
    StringBuffer buf = new StringBuffer("public class "); //$NON-NLS-1$
    buf.append(name);
    buf.append("{ }"); //$NON-NLS-1$
    return buf.toString();
  }

  private String constructTypeStub(ICompilationUnit parentCU, String typeName,
      boolean isInterface, List<String> interfaces, String annotation,
      ImportsManager imports) throws CoreException {

    StringBuffer buf = new StringBuffer();
    String type = ""; //$NON-NLS-1$
    if (annotation != null) {
      buf.append(annotation);
      buf.append(lineDelimiter);
    }
    buf.append("public "); //$NON-NLS-N$
    String templateID = ""; //$NON-NLS-1$

    if (isInterface) {
      type = "interface "; //$NON-NLS-1$
      templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
    } else {
      type = "class "; //$NON-NLS-1$
      templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;
    }
    buf.append(type);
    buf.append(typeName);
    if (interfaces != null) {
      writeSuperInterfaces(buf, imports, interfaces, true);
    }

    buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
    String typeBody = CodeGeneration.getTypeBody(templateID, parentCU,
        parentCU.getElementName(), lineDelimiter);
    if (typeBody != null) {
      buf.append(typeBody);
    } else {
      buf.append(lineDelimiter);
    }
    buf.append('}').append(lineDelimiter); //$NON-NLS-N$
    return buf.toString();
  }

  private CompilationUnit createASTForImports(ICompilationUnit cu) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    parser.setResolveBindings(false);
    parser.setFocalPosition(0);
    return (CompilationUnit) parser.createAST(null);
  }

  @SuppressWarnings("unchecked")
  private Set<String> getExistingImports(CompilationUnit root) {
    List<ImportDeclaration> imports = (List<ImportDeclaration>) root.imports();
    Set<String> res = new HashSet<String>(imports.size());
    for (int i = 0; i < imports.size(); i++) {
      res.add(ASTNodes.asString(imports.get(i)));
    }
    return res;
  }

  @SuppressWarnings("unchecked")
  private void removeUnusedImports(ICompilationUnit cu,
      Set<String> existingImports, boolean needsSave) throws CoreException {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    parser.setResolveBindings(true);

    CompilationUnit root = (CompilationUnit) parser.createAST(null);
    if (root.getProblems().length == 0) {
      return;
    }

    List<ImportDeclaration> importsDecls = root.imports();
    if (importsDecls.isEmpty()) {
      return;
    }
    ImportsManager imports = new ImportsManager(root);

    int importsEnd = ASTNodes.getExclusiveEnd((ASTNode) importsDecls.get(importsDecls.size() - 1));
    IProblem[] problems = root.getProblems();
    for (int i = 0; i < problems.length; i++) {
      IProblem curr = problems[i];
      if (curr.getSourceEnd() < importsEnd) {
        int id = curr.getID();
        if (id == IProblem.UnusedImport || id == IProblem.NotVisibleType) {
          int pos = curr.getSourceStart();
          for (int k = 0; k < importsDecls.size(); k++) {
            ImportDeclaration decl = importsDecls.get(k);
            if (decl.getStartPosition() <= pos
                && pos < decl.getStartPosition() + decl.getLength()) {
              if (existingImports.isEmpty()
                  || !existingImports.contains(ASTNodes.asString(decl))) {
                String name = decl.getName().getFullyQualifiedName();
                if (decl.isOnDemand()) {
                  name += ".*"; //$NON-NLS-1$
                }
                if (decl.isStatic()) {
                  imports.removeStaticImport(name);
                } else {
                  imports.removeImport(name);
                }
              }
              break;
            }
          }
        }
      }
    }
    imports.create(needsSave, null);
  }

  private void writeSuperInterfaces(StringBuffer buf, ImportsManager imports,
      List<String> interfaces, boolean isInterface) throws JavaModelException {

    int last = interfaces.size() - 1;
    if (last >= 0) {
      if (!isInterface) {
        buf.append(" implements "); //$NON-NLS-1$
      } else {
        buf.append(" extends "); //$NON-NLS-1$
      }
      String[] intfs = interfaces.toArray(new String[interfaces.size()]);
      ITypeBinding[] bindings;
      bindings = new ITypeBinding[intfs.length];
      for (int i = 0; i <= last; i++) {
        ITypeBinding binding = bindings[i];
        if (binding != null) {
          buf.append(imports.addImport(binding));
        } else {
          buf.append(imports.addImport(intfs[i]));
        }
        if (intfs[i].equals("com.google.web.bindery.requestfactory.shared.Locator")) {
          buf.append("<").append(current.getElementName());
          buf.append(", ").append(entityIdType).append(">");
        }
        if (i < last) {
          buf.append(',');
        }
      }
    }
  }

}
