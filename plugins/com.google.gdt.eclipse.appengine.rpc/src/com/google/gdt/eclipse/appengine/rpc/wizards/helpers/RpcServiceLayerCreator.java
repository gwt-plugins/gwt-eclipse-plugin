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
package com.google.gdt.eclipse.appengine.rpc.wizards.helpers;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.nature.AppEngineConnectedNature;
import com.google.gdt.eclipse.appengine.rpc.util.CodegenUtils;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryUtils;
import com.google.gdt.eclipse.appengine.rpc.util.RpcType;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generator for RPC service layer classes
 */
@SuppressWarnings({"restriction", "nls"})
public class RpcServiceLayerCreator {

  /**
   * Class used in stub creation routines to add needed imports to a compilation
   * unit.
   */
  static class ImportsManager {

    private final ImportRewrite importsRewrite;

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

  private static final String SHARED_FOLDER_NAME = "shared"; //$NON-NLS-1$
  private static final String PREFS_DIRECTORY = ".settings/"; //$NON-NLS-N$
  private static final String JDT_APT_PREFS = "org.eclipse.jdt.apt.core.prefs"; //$NON-NLS-N$
  private static final String JDT_PREFS = "org.eclipse.jdt.core.prefs"; //$NON-NLS-N$
  private static final String FACTORYPATH_FILE = ".factorypath"; //$NON-NLS-N$
  private static final String APT_FOLDER = ".apt_generated"; //$NON-NLS-N$

  public static RpcServiceLayerCreator createNewRpcServiceLayerCreator() {
    return new RpcServiceLayerCreator();
  }

  private String serviceName;
  private List<IType> entityList;
  private IType current;
  private String entityIdType;
  private String packageName;
  private IProject gaeProject, androidProject;
  private IPackageFragmentRoot gaeProjectSrc;
  private IJavaElement serviceJavaElement;

  private final List<IType> requestTypes = new ArrayList<IType>();
  private String lineDelimiter;
  private String annotationPackageName;

  RpcServiceLayerCreator() {
  }

  public void create(IProgressMonitor monitor) {

    lineDelimiter = System.getProperty("line.separator", "\n"); //$NON-NLS-N$

    gaeProject = gaeProjectSrc.getJavaProject().getProject();
    List<IProject> androidProjects = AppEngineConnectedNature.getAndroidProjects(gaeProject.getName());
    if (androidProjects.size() > 0) {
      androidProject = androidProjects.get(0);
    }
    try {

      IPackageFragment pack = gaeProjectSrc.getPackageFragment(packageName);
      if (!pack.exists()) {
        pack = gaeProjectSrc.createPackageFragment(packageName, true,
            new SubProgressMonitor(monitor, 2));
      }
      annotationPackageName = packageName.replace("server", "annotation");
      if (!annotationPackageName.contains("annotation")) { //$NON-NLS-N$
        annotationPackageName += ".annotation"; //$NON-NLS-N$
      }
      IPackageFragment annotationPackage = gaeProjectSrc.getPackageFragment(annotationPackageName);
      if (!annotationPackage.exists()) {
        annotationPackage = gaeProjectSrc.createPackageFragment(
            annotationPackageName, true, new SubProgressMonitor(monitor, 2));
      }
      ICompilationUnit annotationCU = annotationPackage.getCompilationUnit("ServiceMethod.java"); //$NON-NLS-N$
      if (!annotationCU.exists()) {
        createCompilationUnit(annotationPackage,
            "ServiceMethod", RpcType.ANNOTATION, monitor); //$NON-NLS-N$
      }
      // create service
      createCompilationUnit(pack, serviceName, RpcType.SERVICE, monitor);
      // create service locator
      createCompilationUnit(pack, serviceName + "Locator",
          RpcType.SERVICE_LOCATOR, monitor);
      // for all the entities, generate Locator
      for (IType anEntity : entityList) {
        current = anEntity;
        if (RequestFactoryUtils.shouldBeProxiedAsAnEntity(current)) {
          for (IMethod method : current.getMethods()) {
            if (method.getElementName().equals("getId")) {
              entityIdType = Signature.toString(method.getReturnType());
            }
          }
        } else {
          entityIdType = "Void";
        }
        createCompilationUnit(current.getPackageFragment(),
            current.getElementName() + "Locator", RpcType.LOCATOR, //$NON-NLS-N$
            new SubProgressMonitor(monitor, 1));
      }

      generateSharedFiles(packageName, monitor);
      configureApt(gaeProject, monitor);
      if (androidProject != null) {
        configureApt(androidProject, monitor);
      }

    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    } catch (CoreException e) {
      AppEngineRPCPlugin.log(e);
    } catch (MalformedTreeException e) {
      AppEngineRPCPlugin.log(e);
    } catch (BadLocationException e) {
      AppEngineRPCPlugin.log(e);
    } catch (IOException e) {
      AppEngineRPCPlugin.log(e);
    }
  }

  public IJavaElement getElement() {
    return serviceJavaElement;
  }

  public boolean serviceExists() {
    IPackageFragment pack = gaeProjectSrc.getPackageFragment(packageName);
    if (!pack.exists()) {
      return false;
    }
    ICompilationUnit serviceUnit = pack.getCompilationUnit(serviceName
        + ".java"); //$NON-NLS-N$
    return serviceUnit.exists();
  }

  public void setEntities(Iterable<IType> entityTypes) {
    entityList = (List<IType>) entityTypes;
  }

  public void setGaeProjectSrc(IPackageFragmentRoot gaeProjectSrc) {
    this.gaeProjectSrc = gaeProjectSrc;
    try {
      this.packageName = inferPackageName(gaeProjectSrc.getJavaProject());
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    }
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private void addAptFactoryPathFile(IProject project, IProgressMonitor monitor)
      throws CoreException, IOException {
    String factorypathInfo = "<factorypath>\n "
        + "<factorypathentry kind=\"EXTJAR\" id=\""
        + getGwtContainerPath(JavaCore.create(project))
        + "/requestfactory-apt.jar\" enabled=\"true\" "
        + "runInBatchMode=\"false\"/>\n</factorypath>";
    IFile factoryPath = project.getFile(FACTORYPATH_FILE);
    if (!factoryPath.exists()) {
      copyFile(factorypathInfo, factoryPath, monitor);
    }
  }

  private void addAptSourceFolder(IProject project, IProgressMonitor monitor)
      throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] entries = javaProject.getRawClasspath();

    // add .apt_generated to classpath
    IClasspathAttribute[] attributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(
        "optional", "true")}; //$NON-NLS-N$
    IFolder aptFolder = project.getFolder(APT_FOLDER);
    IClasspathEntry entry = JavaCore.newSourceEntry(aptFolder.getFullPath(),
        ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE, null,
        attributes);
    entries = CodegenUtils.addEntryToClasspath(entries, entry);

    javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
  }

  private void addClasspathContainer(IJavaProject project, IPath containerPath)
      throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();

    IClasspathEntry entry = JavaCore.newContainerEntry(containerPath);
    entries = CodegenUtils.addEntryToClasspath(entries, entry);

    project.setRawClasspath(entries, new NullProgressMonitor());
  }

  private boolean addImport(List<String> names, List<String> entityNameList,
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

  private void addPrefsFile(IProject project, IProgressMonitor monitor)
      throws CoreException, IOException {
    IFolder pkgFolder = project.getFolder(PREFS_DIRECTORY);
    if (!pkgFolder.exists()) {
      pkgFolder.create(true /* force */, true /* local */,
          new SubProgressMonitor(monitor, 10));
    }

    IFile jdtAptPrefs = project.getFile(PREFS_DIRECTORY + JDT_APT_PREFS);
    if (!jdtAptPrefs.exists()) {
      copyFile(getJdtAptPrefs(), jdtAptPrefs, monitor);
    }
    IFile jdtPrefs = project.getFile(PREFS_DIRECTORY + JDT_PREFS);
    if (!jdtPrefs.exists()) {
      copyFile(getJdtPrefs(), jdtPrefs, monitor);
    }
  }

  private void addReqFactoryBody(IType type, IProgressMonitor monitor)
      throws MalformedTreeException, BadLocationException, CoreException {
    ICompilationUnit cu = type.getCompilationUnit();
    cu.becomeWorkingCopy(monitor);
    String source = cu.getSource();
    Document document = new Document(source);
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
    ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
    ListRewrite listRewriter = null;
    AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) astRoot.types().get(
        0);
    if (declaration != null) {
      listRewriter = rewrite.getListRewrite(declaration,
          declaration.getBodyDeclarationsProperty());
    }
    ImportRewrite importRewrite = CodeStyleConfiguration.createImportRewrite(
        astRoot, true);

    StringBuffer buf = new StringBuffer();
    for (IType request : requestTypes) {
      importRewrite.addImport(request.getFullyQualifiedName());
      buf.append(request.getElementName());
      buf.append(" "); //$NON-NLS-N$
      String name = request.getElementName();
      buf.append(name.substring(0, 1).toLowerCase() + name.substring(1));
      buf.append("();"); //$NON-NLS-N$
      buf.append(lineDelimiter);
    }

    MethodDeclaration methodDecl = (MethodDeclaration) listRewriter.getASTRewrite().createStringPlaceholder(
        buf.toString(), ASTNode.METHOD_DECLARATION);
    listRewriter.insertLast(methodDecl, null);
    TextEdit edits = rewrite.rewriteAST(document,
        cu.getJavaProject().getOptions(true));
    edits.addChild(importRewrite.rewriteImports(monitor));
    edits.apply(document);
    cu.getBuffer().setContents(document.get());
    cu.reconcile(ICompilationUnit.NO_AST, false, null, null);
    ISourceRange range = type.getSourceRange();
    IBuffer buffer = cu.getBuffer();
    String originalContent = buffer.getText(range.getOffset(),
        range.getLength());
    String formattedContent = format(originalContent,
        CodeFormatter.K_CLASS_BODY_DECLARATIONS);
    buffer.replace(range.getOffset(), range.getLength(), formattedContent);
    cu.commitWorkingCopy(true, monitor);
    cu.discardWorkingCopy();
  }

  private void configureApt(IProject project, IProgressMonitor monitor)
      throws CoreException, IOException {

    IFolder aptFolder = project.getFolder(APT_FOLDER);
    if (!aptFolder.exists()) {
      aptFolder.create(true /* force */, true /* local */,
          new SubProgressMonitor(monitor, 10));
      addAptSourceFolder(project, monitor);
    }
    // add .settings files
    addPrefsFile(project, monitor);
    addAptFactoryPathFile(project, monitor);
  }

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
    buf.append("@Override").append(lineDelimiter);
    buf.append("public ").append(current.getElementName());
    buf.append(" create(Class<? extends ").append(current.getElementName()).append(
        "> clazz) {");
    buf.append(lineDelimiter);
    if (constructors.isEmpty() || hasDefaultConstructor) {
      buf.append("return new ").append(current.getElementName()).append("();");
    } else {
      buf.append("// TODO no default constructor, creation code cannot be generated");
      buf.append(lineDelimiter).append(
          "throw new RuntimeException(String.format");
      buf.append("(\"Cannot instantiate %s\", clazz.getCanonicalName()));");
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter);
    buf.append("public ").append(current.getElementName());
    buf.append(" find(Class<? extends ").append(current.getElementName()).append(
        "> clazz, Void id) {");
    buf.append(lineDelimiter);
    buf.append("return create(clazz);");
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter);
    buf.append("public Class<").append(current.getElementName());
    buf.append("> getDomainType() {").append(lineDelimiter);
    buf.append("return ").append(current.getElementName()).append(".class;");
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter);
    buf.append("public ").append(entityIdType).append(" getId(").append(
        current.getElementName());
    buf.append(" domainObject) {").append(lineDelimiter);
    if (entityIdType.equals("Void")) {
      buf.append(" return null;");
    } else {
      buf.append("return domainObject.getId()");
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter);
    buf.append("public Class<").append(entityIdType).append("> getIdType() {");
    buf.append(lineDelimiter);
    if (entityIdType.equals("Void")) {
      buf.append("return Void.class;");
    } else {
      buf.append("return ").append(entityIdType).append(".class;");
    }
    buf.append(lineDelimiter).append("}").append(lineDelimiter);

    buf.append("@Override").append(lineDelimiter);
    buf.append("public Object getVersion(").append(current.getElementName());
    buf.append(" domainObject) {").append(lineDelimiter);
    buf.append("return null;");
    buf.append(lineDelimiter).append("}");

    type.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

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
          && !method.getElementName().equals("setId")) {
        methods.add(method);
      }
    }
    boolean hasProxy;
    List<String> typeNames;
    for (IMethod method : methods) {
      hasProxy = false;
      String methodName = method.getElementName();
      String signature = Signature.toString(method.getSignature(), methodName,
          method.getParameterNames(), false, true);

      typeNames = new ArrayList<String>();
      String returnType[] = getElementSignatures(method.getReturnType());
      for (String string : returnType) {
        typeNames.add(string);
      }
      String[] params = method.getParameterTypes();
      // we are looking at only getters and setters, so there is only one param?
      if (params.length > 0) {
        String[] param = getElementSignatures(params[0]);
        for (String string : param) {
          typeNames.add(string);
        }
      }

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

  private void constructRequestBody(IType type, ImportsManager imports,
      IProgressMonitor monitor) throws JavaModelException {

    imports.addImport("com.google.web.bindery.requestfactory.shared.ServiceName");//$NON-NLS-N$
    StringBuffer buf = new StringBuffer();
    imports.addImport("com.google.web.bindery.requestfactory.shared.Request");//$NON-NLS-N$

    for (IType entity : entityList) {
      String name = entity.getElementName();
      String proxyName = name + "Proxy"; //$NON-NLS-N$
      buf.append("Request");//$NON-NLS-N$
      buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
      buf.append(" create").append(name).append("();"); //$NON-NLS-N$
      buf.append(lineDelimiter);
      buf.append("Request");//$NON-NLS-N$
      buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
      buf.append(" read").append(name).append("(Long id);"); //$NON-NLS-N$
      buf.append(lineDelimiter);
      buf.append("Request");//$NON-NLS-N$
      buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
      buf.append(" update").append(name).append("("); //$NON-NLS-N$
      buf.append(proxyName).append(" ").append(name.toLowerCase()).append(");"); //$NON-NLS-N$
      buf.append(lineDelimiter);
      buf.append("Request");//$NON-NLS-N$
      buf.append("<Void>"); //$NON-NLS-N$
      buf.append(" delete").append(name).append("("); //$NON-NLS-N$
      buf.append(proxyName).append(" ").append(name.toLowerCase()).append(");"); //$NON-NLS-N$
      buf.append(lineDelimiter);
      buf.append("Request");//$NON-NLS-N$
      buf.append("<"); //$NON-NLS-N$
      buf.append(imports.addImport("java.util.List"));//$NON-NLS-N$
      buf.append("<").append(proxyName).append(">"); //$NON-NLS-N$
      buf.append(">"); //$NON-NLS-N$
      buf.append(" query").append(name).append("s();"); //$NON-NLS-N$
      buf.append(lineDelimiter);
    }
    type.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

  private void constructServiceBody(IType createdType, ImportsManager imports,
      IProgressMonitor monitor) throws CoreException {
    for (IType anEntity : entityList) {

      current = anEntity;
      String elementName = current.getElementName();
      imports.addImport(annotationPackageName + ".ServiceMethod");
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

  private void constructServiceLocatorBody(IType createdType,
      ImportsManager imports, IProgressMonitor monitor) throws CoreException {

    imports.addImport("com.google.web.bindery.requestfactory.shared.ServiceLocator");//$NON-NLS-N$
    StringBuffer buf = new StringBuffer();
    buf.append("@Override").append(lineDelimiter);
    buf.append("public Object getInstance(Class<?> clazz) {").append(
        lineDelimiter);
    buf.append("   try {").append(lineDelimiter);
    buf.append(" return clazz.newInstance();").append(lineDelimiter);
    buf.append(" } catch (InstantiationException e) {").append(lineDelimiter);
    buf.append(" throw new RuntimeException(e);").append(lineDelimiter);
    buf.append(" } catch (IllegalAccessException e) {").append(lineDelimiter);
    buf.append(" throw new RuntimeException(e);").append(lineDelimiter);
    buf.append("}").append(lineDelimiter);
    buf.append("}").append(lineDelimiter);
    createdType.createMethod(buf.toString(), null, false, null);
    if (monitor != null) {
      monitor.done();
    }
  }

  /**
   * Adds a method to the service class
   */
  private void constructServiceMethod(IType type, String methodName,
      boolean hasReturnType, String[] paramType, String[] paramName,
      ImportsManager imports, IProgressMonitor monitor) throws CoreException {

    StringBuffer buf = new StringBuffer();
    buf.append("@ServiceMethod").append(lineDelimiter);
    buf.append("public "); //$NON-NLS-1$
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
      RpcType rpcType, List<String> interfaces, String annotation,
      ImportsManager imports) throws CoreException {

    StringBuffer buf = new StringBuffer();
    String type = ""; //$NON-NLS-1$
    if (annotation != null) {
      buf.append(annotation);
      buf.append(lineDelimiter);
    }
    buf.append("public "); //$NON-NLS-N$
    String templateID = ""; //$NON-NLS-1$
    switch (rpcType) {
      case SERVICE:
      case LOCATOR:
      case SERVICE_LOCATOR:
        type = "class "; //$NON-NLS-1$
        templateID = CodeGeneration.CLASS_BODY_TEMPLATE_ID;

        break;
      case REQUEST:
      case REQ_FACTORY:
      case PROXY:
        type = "interface "; //$NON-NLS-1$
        templateID = CodeGeneration.INTERFACE_BODY_TEMPLATE_ID;
        break;
      case ANNOTATION:
        type = "@interface "; //$NON-NLS-1$
        templateID = CodeGeneration.ANNOTATION_BODY_TEMPLATE_ID;
        break;
    }

    buf.append(type);
    buf.append(typeName);
    if (interfaces != null) {
      writeSuperInterfaces(buf, imports, interfaces, rpcType);
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

  private void copyFile(String resourceFile, IFile destFile,
      IProgressMonitor monitor) throws CoreException, IOException {

    // Save in the project as UTF-8
    InputStream stream = new ByteArrayInputStream(
        resourceFile.getBytes("UTF-8")); //$NON-NLS-1$
    destFile.create(stream, false /* force */, new SubProgressMonitor(monitor,
        10));
  }

  private CompilationUnit createASTForImports(ICompilationUnit cu) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    parser.setResolveBindings(false);
    parser.setFocalPosition(0);
    return (CompilationUnit) parser.createAST(null);
  }

  private void createCompilationUnit(IPackageFragment pack, String name,
      RpcType rpcType, IProgressMonitor monitor) throws CoreException {
    IType createdType = null;
    ImportsManager imports;
    ICompilationUnit connectedCU = null;

    try {
      ICompilationUnit parentCU = pack.createCompilationUnit(name + ".java", //$NON-NLS-N$
          "", true, new SubProgressMonitor(monitor, 1));
      parentCU.becomeWorkingCopy(new SubProgressMonitor(monitor, 1));
      connectedCU = parentCU;
      IBuffer buffer = parentCU.getBuffer();
      String simpleTypeStub = constructSimpleTypeStub(name);
      String typeComment = null;
      switch (rpcType) {
        case ANNOTATION:
          typeComment = "/**"
              + lineDelimiter
              + " * Annotation on method specifying that the method is a service method"
              + lineDelimiter
              + "* and needs to have the corresponding request factory code "
              + lineDelimiter + "*/";
          break;
      }
      String content = CodeGeneration.getCompilationUnitContent(parentCU, null,
          typeComment, simpleTypeStub, lineDelimiter);
      buffer.setContents(content);

      CompilationUnit astRoot = createASTForImports(parentCU);
      // Set<String> existingImports = getExistingImports(astRoot);
      imports = new ImportsManager(astRoot);

      String typeContent;
      String annotation = "";
      List<String> interfaces = new ArrayList<String>();
      switch (rpcType) {
        case ANNOTATION:
          annotation = "@Target(ElementType.METHOD)" + lineDelimiter
              + "@Retention(RetentionPolicy.CLASS)";
          imports.addImport("java.lang.annotation.ElementType");
          imports.addImport("java.lang.annotation.Retention");
          imports.addImport("java.lang.annotation.RetentionPolicy");
          imports.addImport("java.lang.annotation.Target");
          break;
        case LOCATOR:
          interfaces.add("com.google.web.bindery.requestfactory.shared.Locator");
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
          annotation = "@ServiceName(value=\"" + packageName + "." + serviceName //$NON-NLS-N$
              + "\", locator=\"" + packageName + "." + serviceName
              + "Locator\")";
          break;
        case REQ_FACTORY:
          interfaces.add("com.google.web.bindery.requestfactory.shared.RequestFactory"); //$NON-NLS-N$
          break;
        case SERVICE_LOCATOR:
          interfaces.add("com.google.web.bindery.requestfactory.shared.ServiceLocator");
          break;
      }

      typeContent = constructTypeStub(parentCU, name, rpcType, interfaces,
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
      JavaModelUtil.reconcile(cu);

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
        case SERVICE_LOCATOR:
          constructServiceLocatorBody(createdType, imports,
              new SubProgressMonitor(monitor, 1));
          break;
      }

      imports.create(false, new SubProgressMonitor(monitor, 1));
      removeUnusedImports(cu, getExistingImports(astRoot), false);
      JavaModelUtil.reconcile(cu);

      ISourceRange range = createdType.getSourceRange();

      IBuffer buf = cu.getBuffer();
      String originalContent = buf.getText(range.getOffset(), range.getLength());

      String formattedContent = format(originalContent,
          CodeFormatter.K_CLASS_BODY_DECLARATIONS);
      buf.replace(range.getOffset(), range.getLength(), formattedContent);

      cu.commitWorkingCopy(true, new SubProgressMonitor(monitor, 1));
      if (rpcType == RpcType.SERVICE) {
        serviceJavaElement = cu.getPrimaryElement();
      }
    } finally {
      if (connectedCU != null) {
        connectedCU.discardWorkingCopy();
      }
      monitor.done();
    }
  }

  /**
   * Format the source
   */
  private String format(String source, int formatType) {

    TextEdit textEdit = null;
    textEdit = ToolFactory.createCodeFormatter(null).format(formatType, source,
        0, source.length(), 0, null);

    String formattedContent;
    if (textEdit != null) {
      Document document = new Document(source);
      try {
        textEdit.apply(document);
      } catch (MalformedTreeException e) {
        AppEngineRPCPlugin.log(e);
      } catch (BadLocationException e) {
        AppEngineRPCPlugin.log(e);
      }
      formattedContent = document.get();
    } else {
      formattedContent = source;
    }

    return formattedContent;
  }

  private void generateSharedFiles(String packageName, IProgressMonitor monitor)
      throws CoreException, MalformedTreeException, BadLocationException {
    String sharedPackageName = packageName;
    if (sharedPackageName.contains("server")) { //$NON-NLS-N$
      sharedPackageName = sharedPackageName.replace("server", "shared"); //$NON-NLS-N$
    } else {
      sharedPackageName += ".shared"; //$NON-NLS-N$
    }

    // get shared source folder
    IFolder gaeSharedFolder = gaeProject.getFolder(SHARED_FOLDER_NAME);
    if (!gaeSharedFolder.exists()) {
      gaeSharedFolder.create(true, true, new SubProgressMonitor(monitor, 2));
      CodegenUtils.setupSourceFolders(JavaCore.create(gaeProject),
          gaeSharedFolder, monitor);
    }

    if (androidProject != null) {
      IFolder androidLinkedFolder = androidProject.getFolder(SHARED_FOLDER_NAME);
      if (!androidLinkedFolder.exists()) {
        /* The variable workspaceLoc is required only for Eclipse 3.5.
         * For Eclipses after 3.5, the project specific path variable WORKSPACE_LOC
         * can be used instead.
         */
        String workspaceLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
        // use variables for shared folder path
        IPath sharedFolderPath = new Path(workspaceLoc + "/" //$NON-NLS-N$
            + gaeProject.getName() + "/" + SHARED_FOLDER_NAME);
        androidLinkedFolder.createLink(sharedFolderPath,
            IResource.ALLOW_MISSING_LOCAL, new SubProgressMonitor(monitor, 1));
        CodegenUtils.setupSourceFolders(JavaCore.create(androidProject),
            androidLinkedFolder, monitor);
      }
    }

    IPackageFragmentRoot srcFolder = JavaCore.create(gaeProject).getPackageFragmentRoot(
        gaeSharedFolder);
    IPackageFragment pack = srcFolder.getPackageFragment(sharedPackageName);

    if (!pack.exists()) {
      pack = srcFolder.createPackageFragment(sharedPackageName, true,
          new SubProgressMonitor(monitor, 2));
    }

    // for all the entities, generate Proxy, Request
    for (IType anEntity : entityList) {
      current = anEntity;
      createCompilationUnit(pack,
          current.getElementName() + "Proxy", RpcType.PROXY, //$NON-NLS-N$
          new SubProgressMonitor(monitor, 1));
    }

    // generate Request
    String requestFileName = serviceName.replace("Service", "") + "Request"; //$NON-NLS-N$
    createCompilationUnit(pack, requestFileName, RpcType.REQUEST, //$NON-NLS-N$
        new SubProgressMonitor(monitor, 1));
    // generate RequestFactory
    // check if there is a request factory class in the project
    String rfactoryFileName = serviceName.replace("Service", "") + "RequestFactory"; //$NON-NLS-N$
    List<IType> reqFactoryList = RequestFactoryUtils.findTypes(
        JavaCore.create(gaeProject), RpcType.REQ_FACTORY);
    if (reqFactoryList.isEmpty()) {
      createCompilationUnit(pack, rfactoryFileName, RpcType.REQ_FACTORY,
          new SubProgressMonitor(monitor, 1)); //$NON-NLS-N$
    } else {
      addReqFactoryBody(reqFactoryList.get(0), new SubProgressMonitor(monitor,
          1));
    }
  }

  private String[] getElementSignatures(String typeName) {
    if (Signature.getTypeSignatureKind(typeName) == Signature.ARRAY_TYPE_SIGNATURE) {
      return new String[] {Signature.getElementType(typeName)};
    } else {
      String[] names = Signature.getTypeArguments(typeName);
      List<String> list = new ArrayList<String>();
      list.add(Signature.getTypeErasure(typeName));
      if (names.length > 0) {
        list.add(names[0]);
      }
      return list.toArray(new String[list.size()]);
    }
  }

  @SuppressWarnings("unchecked")
  private Set<String> getExistingImports(CompilationUnit root) {
    List<ImportDeclaration> imports = root.imports();
    Set<String> res = new HashSet<String>(imports.size());
    for (int i = 0; i < imports.size(); i++) {
      res.add(ASTNodes.asString(imports.get(i)));
    }
    return res;
  }

  private String getGwtContainerPath(IJavaProject javaProject)
      throws CoreException {
    IClasspathEntry[] entries = null;

    entries = javaProject.getRawClasspath();

    for (IClasspathEntry entry : entries) {
      if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
          && entry.getPath().toString().equals(
              "com.google.gwt.eclipse.core.GWT_CONTAINER")) { //$NON-NLS-N$
        IClasspathContainer container = JavaCore.getClasspathContainer(
            entry.getPath(), javaProject);
        if (container instanceof GWTRuntimeContainer) {
          IPath path = ((GWTRuntimeContainer) container).getSdk().getInstallationPath();
          return path.toString();
        }
      }
    }
    // gwt not on classpath, add to it, set nature
    GWTRuntime gwt = GWTPreferences.getDefaultRuntime();
    IPath containerPath = SdkClasspathContainer.computeContainerPath(
        GWTRuntimeContainer.CONTAINER_ID, gwt,
        SdkClasspathContainer.Type.DEFAULT);
    if (GaeNature.isGaeProject(javaProject.getProject())) {
      addClasspathContainer(javaProject, containerPath);
      GWTNature.addNatureToProject(javaProject.getProject());
    }
    return gwt.getInstallationPath().toString();
  }

  private String getJdtAptPrefs() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("eclipse.preferences.version=1").append("\n");
    buffer.append("org.eclipse.jdt.apt.aptEnabled=true").append("\n");
    buffer.append("org.eclipse.jdt.apt.genSrcDir=.apt_generated").append("\n");
    buffer.append("org.eclipse.jdt.apt.processorOptions/verbose=false").append(
        "\n");
    buffer.append("org.eclipse.jdt.apt.reconcileEnabled=true").append("\n");
    return buffer.toString();
  }

  private String getJdtPrefs() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("eclipse.preferences.version=1");
    buffer.append("\n");
    buffer.append("org.eclipse.jdt.core.compiler.processAnnotations=enabled");
    buffer.append("\n");
    return buffer.toString();
  }

  /**
   * Compute a package name
   * 
   * @throws JavaModelException
   */
  private String inferPackageName(IJavaProject gaeJavaProject)
      throws JavaModelException {
    for (IPackageFragment pf : gaeJavaProject.getPackageFragments()) {
      if (pf.getKind() == IPackageFragmentRoot.K_SOURCE
          && pf.getElementName().contains("server")) //$NON-NLS-N$
        return pf.getElementName();
    }
    // TODO: find a better story for selecting package
    return "com.google.rpc"; //$NON-NLS-N$
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

    int importsEnd = ASTNodes.getExclusiveEnd(importsDecls.get(importsDecls.size() - 1));
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
      List<String> interfaces, RpcType rpcType) throws JavaModelException {

    int last = interfaces.size() - 1;
    if (last >= 0) {
      switch (rpcType) {
        case SERVICE_LOCATOR:
          buf.append(" implements "); //$NON-NLS-1$
          break;
        default:
          buf.append(" extends "); //$NON-NLS-1$
          break;
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
