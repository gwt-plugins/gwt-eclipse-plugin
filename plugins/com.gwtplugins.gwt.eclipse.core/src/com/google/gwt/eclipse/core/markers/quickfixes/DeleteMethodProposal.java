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
import com.google.gdt.eclipse.platform.jdt.text.correction.proposals.ASTRewriteCorrectionProposal;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.internal.WorkbenchPlugin;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * Deletes a method on an RPC async or sync interface.
 */
@SuppressWarnings("restriction")
public class DeleteMethodProposal extends ASTRewriteCorrectionProposal {

  private static final Image ICON = WorkbenchPlugin.getDefault().getSharedImages().getImage(
      ISharedImages.IMG_TOOL_DELETE);

  private static final int RELEVANCE = 99;

  public static List<IJavaCompletionProposal> createProposalsForProblemOnAsyncType(
      ASTNode problemNode, String extraSyncMethodBindingKey) {
    TypeDeclaration asyncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        problemNode, ASTNode.TYPE_DECLARATION);
    assert (asyncTypeDecl != null);

    IType syncType = RemoteServiceUtilities.findSyncType(asyncTypeDecl);
    if (syncType == null) {
      return null;
    }

    MethodDeclaration extraSyncMethodDecl = JavaASTUtils.findMethodDeclaration(
        syncType.getCompilationUnit(), extraSyncMethodBindingKey);
    if (extraSyncMethodDecl == null) {
      return null;
    }

    return Collections.<IJavaCompletionProposal> singletonList(new DeleteMethodProposal(
        syncType.getCompilationUnit(), extraSyncMethodDecl));
  }

  public static List<IJavaCompletionProposal> createProposalsForProblemOnExtraMethod(
      ASTNode problemNode) {
    MethodDeclaration methodDecl = ASTResolving.findParentMethodDeclaration(problemNode);

    return Collections.<IJavaCompletionProposal> singletonList(new DeleteMethodProposal(
        JavaASTUtils.getCompilationUnit(methodDecl), methodDecl));
  }

  public static List<IJavaCompletionProposal> createProposalsForProblemOnSyncType(
      ASTNode problemNode, String extraAsyncMethodBindingKey) {
    TypeDeclaration syncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        problemNode, ASTNode.TYPE_DECLARATION);
    assert (syncTypeDecl != null);

    IType asyncType = RemoteServiceUtilities.findAsyncType(syncTypeDecl);
    if (asyncType == null) {
      return null;
    }

    MethodDeclaration extraAsyncMethodDecl = JavaASTUtils.findMethodDeclaration(
        asyncType.getCompilationUnit(), extraAsyncMethodBindingKey);
    if (extraAsyncMethodDecl == null) {
      return null;
    }

    return Collections.<IJavaCompletionProposal> singletonList(new DeleteMethodProposal(
        asyncType.getCompilationUnit(), extraAsyncMethodDecl));
  }

  private static String generateDisplayName(MethodDeclaration methodDecl) {
    TypeDeclaration typeDecl = (TypeDeclaration) ASTResolving.findParentType(methodDecl);
    return MessageFormat.format("Remove method ''{0}'' from type ''{1}''",
        methodDecl.getName().getIdentifier(),
        typeDecl.getName().getIdentifier());
  }

  private final MethodDeclaration methodDecl;

  private DeleteMethodProposal(ICompilationUnit cu, MethodDeclaration methodDecl) {
    super(generateDisplayName(methodDecl), cu, null, RELEVANCE, ICON);
    this.methodDecl = methodDecl;
  }

  @Override
  protected ASTRewrite getRewrite() throws CoreException {
    CompilationUnit targetAstRoot = ASTResolving.createQuickFixAST(
        getCompilationUnit(), null);
    createImportRewrite(targetAstRoot);

    ASTRewrite rewrite = ASTRewrite.create(targetAstRoot.getAST());

    // Find the method declaration in the AST we just generated (the one that
    // the AST rewriter is hooked up to).
    MethodDeclaration rewriterAstMethodDecl = JavaASTUtils.findMethodDeclaration(
        targetAstRoot, methodDecl.resolveBinding().getKey());
    if (rewriterAstMethodDecl == null) {
      return null;
    }

    // Remove the extra method declaration
    rewrite.remove(rewriterAstMethodDecl, null);

    return rewrite;
  }

}
