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
package com.google.gwt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.JavaASTUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Changes the return type of an RPC async method.
 */
@SuppressWarnings("restriction")
public class ChangeAsyncMethodReturnTypeProposal extends
    LinkedCorrectionProposal {

  private static final Image ICON = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

  private static final int RELEVANCE = 100;

  public static List<IJavaCompletionProposal> createProposals(
      ASTNode problemNode) {
    MethodDeclaration methodDecl = ASTResolving.findParentMethodDeclaration(problemNode);
    return Collections.<IJavaCompletionProposal> singletonList(new ChangeAsyncMethodReturnTypeProposal(
        methodDecl));
  }

  private final MethodDeclaration methodDecl;

  public ChangeAsyncMethodReturnTypeProposal(MethodDeclaration methodDecl) {
    super("Change method return type",
        JavaASTUtils.getCompilationUnit(methodDecl), null, RELEVANCE, ICON);
    this.methodDecl = methodDecl;
  }

  @Override
  protected ASTRewrite getRewrite() throws CoreException {
    CompilationUnit targetAstRoot = ASTResolving.createQuickFixAST(
        getCompilationUnit(), null);
    AST ast = targetAstRoot.getAST();
    createImportRewrite(targetAstRoot);

    ASTRewrite rewrite = ASTRewrite.create(targetAstRoot.getAST());

    // Find the method declaration in the AST we just generated (the one that
    // the AST rewriter is hooked up to).
    MethodDeclaration rewriterAstMethodDecl = JavaASTUtils.findMethodDeclaration(
        targetAstRoot, methodDecl.resolveBinding().getKey());
    if (rewriterAstMethodDecl == null) {
      return null;
    }

    // Set up the list of valid return types
    List<ITypeBinding> validReturnTypeBindings = new ArrayList<ITypeBinding>();
    validReturnTypeBindings.add(ast.resolveWellKnownType("void"));

    IJavaProject javaProject = getCompilationUnit().getJavaProject();
    ITypeBinding requestBinding = JavaASTUtils.resolveType(javaProject,
        "com.google.gwt.http.client.Request");
    if (requestBinding != null) {
      validReturnTypeBindings.add(requestBinding);
    }
    ITypeBinding requestBuilderBinding = JavaASTUtils.resolveType(javaProject,
        "com.google.gwt.http.client.RequestBuilder");
    if (requestBuilderBinding != null) {
      validReturnTypeBindings.add(requestBuilderBinding);
    }

    // Set default proposal return type
    Type newReturnType = getImportRewrite().addImport(
        validReturnTypeBindings.get(0), ast);
    rewrite.replace(rewriterAstMethodDecl.getReturnType2(), newReturnType, null);

    // Use linked mode to choose from one of the other valid return types
    String key = "return_type";
    addLinkedPosition(rewrite.track(newReturnType), true, key);
    for (ITypeBinding binding : validReturnTypeBindings) {
      addLinkedPositionProposal(key, binding);
    }

    return rewrite;
  }
}
