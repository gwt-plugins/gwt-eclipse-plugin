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
package com.google.gdt.eclipse.appengine.rpc.markers.quickfixes;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;
import com.google.gdt.eclipse.appengine.rpc.util.CodegenUtils;
import com.google.gdt.eclipse.appengine.rpc.util.CompilationUnitCreator;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryCodegenUtils;
import com.google.gdt.eclipse.appengine.rpc.util.RequestFactoryUtils;
import com.google.gdt.eclipse.appengine.rpc.util.RpcType;
import com.google.gdt.eclipse.platform.jdt.text.correction.proposals.ASTRewriteCorrectionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a the missing service methods that need to be added to the request
 * context
 */
@SuppressWarnings("restriction")
public class CreateRequestFactoryMethodProposal extends
    ASTRewriteCorrectionProposal {

  private static final Image ICON = JavaPlugin.getImageDescriptorRegistry().get(
      JavaPluginImages.DESC_MISC_PUBLIC);

  private static final int RELEVANCE = 100;

  public static List<IJavaCompletionProposal> createProposals(
      ICompilationUnit serviceCompilationUnit, ASTNode problemNode) {

    try {

      String serviceName = serviceCompilationUnit.findPrimaryType().getElementName();
      IType requestContext = RequestFactoryUtils.getRequestForService(
          serviceCompilationUnit.findPrimaryType().getFullyQualifiedName(),
          serviceCompilationUnit.getJavaProject());

      MethodDeclaration serviceMethod = null;
      if (problemNode instanceof SimpleName) {
        problemNode = problemNode.getParent();
      }
      if (problemNode instanceof MethodDeclaration) {
        serviceMethod = (MethodDeclaration) problemNode;
      }
      return Collections.<IJavaCompletionProposal> singletonList(new CreateRequestFactoryMethodProposal(
          serviceName, requestContext.getCompilationUnit(), requestContext,
          serviceMethod));
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
    }
    return null;
  }

  private static String generateDisplayName(String typeName, String methodName) {
    return MessageFormat.format("Generate method ''{0}'' in type ''{1}''",
        methodName, typeName);
  }

  private IType requestContextType;
  protected final MethodDeclaration serviceMethod;
  private List<IType> projectEntities;
  private List<IType> entityList;
  private List<IType> proxies;

  private CreateRequestFactoryMethodProposal(String serviceName,
      ICompilationUnit requestCompilationUnit, IType requestContext,
      MethodDeclaration method) {

    super(generateDisplayName(requestContext.getElementName(),
        method.getName().getIdentifier()), requestCompilationUnit, null,
        RELEVANCE, ICON);
    this.requestContextType = requestContext;
    this.serviceMethod = method;
  }

  @Override
  public void apply(IDocument document) {
    super.apply(document);
    // do rest of codegen
    boolean done = false;
    if (!entityList.isEmpty()) {
      done = generateProxyLocator(proxies.get(0).getPackageFragment())
          && addServiceMethods();
    }
    if (!done) {
      // TODO : indicate to user there was failure in quickfix codegen
    }
  }

  protected MethodDeclaration createMethodDeclaration(ASTRewrite rewriter) {

    StringBuffer buf = new StringBuffer();
    try {
      IMethod method = (IMethod) serviceMethod.resolveBinding().getJavaElement();
      projectEntities = RequestFactoryUtils.findTypes(
          requestContextType.getJavaProject(), RpcType.ENTITY);
      proxies = RequestFactoryUtils.findTypes(method.getJavaProject(),
          RpcType.PROXY);
      entityList = RequestFactoryCodegenUtils.getEntitiesInMethodSignature(
          method, projectEntities);

      buf.append(RequestFactoryCodegenUtils.constructMethodSignature(method,
          projectEntities));

      List<String> entityName = new ArrayList<String>();
      for (IType entity : entityList) {
        entityName.add(entity.getFullyQualifiedName());
      }

      for (IType proxy : proxies) {
        IAnnotation annotation = proxy.getAnnotation("ProxyForName"); //$NON-NLS-N$
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        for (IMemberValuePair pair : values) {
          if (pair.getMemberName().equals("value")) {
            String typeName = (String) pair.getValue();
            if (entityName.contains(typeName)) {
              // entity has proxy, remove from list
              removeFromEntityList(typeName);
            }
          }
        }
      }
      // if any proxies were created, add those methods too
      for (IType entity : entityList) {
        String methodString = RequestFactoryCodegenUtils.constructRequestForEntity(entity.getElementName());
        buf.append(methodString);
      }
    } catch (JavaModelException e) {
      AppEngineRPCPlugin.log(e);
      return null;
    }

    MethodDeclaration methodDeclaration = (MethodDeclaration) rewriter.createStringPlaceholder(
        CodegenUtils.format(buf.toString(),
            CodeFormatter.K_CLASS_BODY_DECLARATIONS),
        ASTNode.METHOD_DECLARATION);
    return methodDeclaration;
  }

  @Override
  protected ASTRewrite getRewrite() {

    CompilationUnit targetAstRoot = ASTResolving.createQuickFixAST(
        getCompilationUnit(), null);
    createImportRewrite(targetAstRoot);
    // Find the target type declaration
    TypeDeclaration typeDecl = (TypeDeclaration) targetAstRoot.types().get(0);
    if (typeDecl == null) {
      return null;
    }
    ASTRewrite rewrite = ASTRewrite.create(targetAstRoot.getAST());
    // Generate the new method declaration
    MethodDeclaration methodDecl = createMethodDeclaration(rewrite);
    if (methodDecl != null) {
      ChildListPropertyDescriptor property = ASTNodes.getBodyDeclarationsProperty(typeDecl);
      ListRewrite listRewriter = rewrite.getListRewrite(typeDecl, property);
      listRewriter.insertLast(methodDecl, null);
    }
    return rewrite;
  }

  /**
   * Add service methods for entities
   */
  private boolean addServiceMethods() {

    IMethod method = (IMethod) serviceMethod.resolveBinding().getJavaElement();
    ICompilationUnit cu = method.getCompilationUnit();
    String source = null;
    try {
      source = cu.getSource();
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
      StringBuffer buf = new StringBuffer();
      for (IType entity : entityList) {
        buf.append(CodegenUtils.format(
            RequestFactoryCodegenUtils.constructServiceMethods(entity, method),
            CodeFormatter.K_CLASS_BODY_DECLARATIONS));
      }
      MethodDeclaration methodDecl = (MethodDeclaration) listRewriter.getASTRewrite().createStringPlaceholder(
          buf.toString(), ASTNode.METHOD_DECLARATION);
      listRewriter.insertLast(methodDecl, null);

      TextEdit edits = rewrite.rewriteAST(document,
          cu.getJavaProject().getOptions(true));
      edits.apply(document);
      cu.getBuffer().setContents(document.get());
      cu.reconcile(ICompilationUnit.NO_AST, false, null, null);
    } catch (CoreException e) {
      AppEngineRPCPlugin.log(e);
      return false;
    } catch (MalformedTreeException e) {
      AppEngineRPCPlugin.log(e);
      return false;
    } catch (BadLocationException e) {
      AppEngineRPCPlugin.log(e);
      return false;
    }
    return true;
  }

  /**
   * generates the proxy & locator
   */
  private boolean generateProxyLocator(IPackageFragment pack) {

    for (IType entity : entityList) {
      CompilationUnitCreator creator = new CompilationUnitCreator(
          projectEntities, null);
      try {
        creator.create(entity, pack, entity.getElementName() + "Proxy",
            RpcType.PROXY, new NullProgressMonitor());

        creator.create(entity, entity.getPackageFragment(),
            entity.getElementName() + "Locator", RpcType.LOCATOR,
            new NullProgressMonitor());
      } catch (CoreException e) {
        AppEngineRPCPlugin.log(e);
        return false;
      }
    }
    return true;
  }

  private void removeFromEntityList(String typeName) {
    int index = 0;
    for (IType type : entityList) {
      if (type.getFullyQualifiedName().equals(typeName)) {
        index = entityList.indexOf(type);
      }
    }
    entityList.remove(index);
  }

}
