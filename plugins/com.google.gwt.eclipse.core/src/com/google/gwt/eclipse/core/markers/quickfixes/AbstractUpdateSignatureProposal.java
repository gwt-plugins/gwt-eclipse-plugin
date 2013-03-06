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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.graphics.Image;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * Updates a sync/async method signature to match the associated method
 * declaration.
 */
public abstract class AbstractUpdateSignatureProposal extends
    ASTRewriteCorrectionProposal {

  /**
   * A builder for UpdateAsyncSignatureProposal / UpdateSyncSignatureProposal
   * instances.
   */
  protected interface UpdateSignatureProposalBuilder {
    AbstractUpdateSignatureProposal createProposal(RpcPair rpcPair,
        int relevance);
  }

  /**
   * Finds the associated sync/async RPC type declaration for a given async/sync
   * type declaration.
   */
  enum PeerTypeResolver {
    ASYNC_RESOLVER {
      @Override
      IType getPeerType(TypeDeclaration decl) {
        return RemoteServiceUtilities.findAsyncType(decl);
      }
    },
    SYNC_RESOLVER {
      @Override
      IType getPeerType(TypeDeclaration decl) {
        return RemoteServiceUtilities.findSyncType(decl);
      }
    };

    /**
     * Finds the sync/async counterpart type associated with the given type.
     */
    abstract IType getPeerType(TypeDeclaration decl);
  }

  /**
   * Encapsulates RPC type/method pairs.
   * 
   * In this signature update context, the "source" type/method is the entity
   * used as a template for updating the "destination" type/method.
   */
  static class RpcPair {
    TypeDeclaration srcType, dstType;
    MethodDeclaration srcMethod, dstMethod;

    public RpcPair(TypeDeclaration srcType, MethodDeclaration srcMethod,
        TypeDeclaration dstType) {

      this.srcType = srcType;
      this.srcMethod = srcMethod;
      this.dstType = dstType;
    }
  }

  private static final Image ICON = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
  private static final String LABEL_FORMAT = "Update method ''{0}'' in type ''{1}''";
  private static final int DEFAULT_RELEVANCE = 100;
  private static final int UNIQUE_RELEVANCE = 200;

  protected static List<IJavaCompletionProposal> createProposal(
      RpcPair rpcPair, UpdateSignatureProposalBuilder builder) {

    if (rpcPair == null) {
      return Collections.emptyList();
    }

    // We cannot determine the best method candidate from a static context,
    // so that action is deferred to the non-static getRewrite(). For now we
    // just count the candidates to decide whether we should create a proposal.
    int candidates = candidateCount(rpcPair.dstType,
        rpcPair.srcMethod.getName().getIdentifier());
    if (candidates == 0) {
      return Collections.emptyList();
    }

    // If there's only one candidate method, chances are pretty good that the
    // signature update is the preferred quick-fix. Prioritize this proposal
    // accordingly.
    int relevance = (candidates == 1) ? UNIQUE_RELEVANCE : DEFAULT_RELEVANCE;

    return Collections.<IJavaCompletionProposal> singletonList(builder.createProposal(
        rpcPair, relevance));
  }

  /**
   * Finds the related RPC components for a given method: declaring type & peer
   * type.
   * 
   * @param node the method's SimpleName AST node.
   * @param peerResolver mapping function for the associated type.
   * @return the {@link RpcPair} encapsulation of the related components.
   */
  protected static RpcPair resolveRpcPair(ASTNode node,
      PeerTypeResolver peerResolver) {

    // Find the source method declaration and its declaring type
    MethodDeclaration methodDecl = ASTResolving.findParentMethodDeclaration(node);
    TypeDeclaration typeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        node, ASTNode.TYPE_DECLARATION);
    assert (typeDecl != null);

    // Find the associated sync/async peer type declaration (update target)
    IType dstType = peerResolver.getPeerType(typeDecl);
    if (dstType == null) {
      return null;
    }

    CompilationUnit astRoot = ASTResolving.createQuickFixAST(
        dstType.getCompilationUnit(), null);
    TypeDeclaration dstTypeDecl = JavaASTUtils.findTypeDeclaration(astRoot,
        dstType.getFullyQualifiedName('.'));

    if (dstTypeDecl == null) {
      return null;
    }

    return new RpcPair(typeDecl, methodDecl, dstTypeDecl);
  }

  /**
   * Finds the related RPC components for a given type and method key: declaring
   * method declaration & peer type.
   * 
   * @param node the type's SimpleName AST node.
   * @param methodBindingKey the method binding key.
   * @param peerResolver mapping function for the associated type.
   * @return the {@link RpcPair} encapsulation of the related components.
   */
  protected static RpcPair resolveRpcPair(ASTNode node, String methodBindingKey,
      PeerTypeResolver peerResolver) {

    // Find the destination type declaration
    TypeDeclaration dstTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        node, ASTNode.TYPE_DECLARATION);
    assert (dstTypeDecl != null);

    // Find the associated sync/async source type declaration
    IType type = peerResolver.getPeerType(dstTypeDecl);
    if (type == null) {
      return null;
    }

    CompilationUnit targetAstRoot = ASTResolving.createQuickFixAST(
        type.getCompilationUnit(), null);
    TypeDeclaration typeDecl = JavaASTUtils.findTypeDeclaration(targetAstRoot,
        type.getFullyQualifiedName('.'));

    // Find the source method declaration.
    MethodDeclaration methodDecl = JavaASTUtils.findMethodDeclaration(
        type.getCompilationUnit(), methodBindingKey);

    if (typeDecl == null || methodDecl == null) {
      return null;
    }

    return new RpcPair(typeDecl, methodDecl, dstTypeDecl);
  }

  private static int candidateCount(TypeDeclaration type, String name) {
    int count = 0;

    for (MethodDeclaration method : type.getMethods()) {
      if (name.equals(method.getName().getIdentifier())) {
        count += 1;
      }
    }

    return count;
  }

  private final RpcPair rpcPair;

  protected AbstractUpdateSignatureProposal(RpcPair rpcPair, int relevance) {

    super(MessageFormat.format(LABEL_FORMAT,
        rpcPair.srcMethod.getName().getIdentifier(),
        rpcPair.dstType.getName().getIdentifier()),
        JavaASTUtils.getCompilationUnit(rpcPair.dstType), null, relevance,
        ICON);

    this.rpcPair = rpcPair;
  }

  /**
   * Returns the destination method parameters minus the async callback (if
   * applicable).
   */
  protected abstract List<SingleVariableDeclaration> adjustDstParams(
      MethodDeclaration method);

  /**
   * Returns the source method parameters minus the async callback (if
   * applicable).
   */
  protected abstract List<SingleVariableDeclaration> adjustSrcParams(
      MethodDeclaration method);

  /**
   * Generates the parameters list for the destination method.
   */
  protected abstract List<SingleVariableDeclaration> computeParams(AST ast,
      MethodDeclaration srcMethod, MethodDeclaration dstMethod,
      ImportRewrite imports);

  /**
   * Calculates the return type for the destination method.
   */
  protected abstract Type computeReturnType(AST ast,
      MethodDeclaration srcMethod, MethodDeclaration dstMethod,
      ImportRewrite imports);

  @Override
  protected ASTRewrite getRewrite() {
    MethodDeclaration dstMethod = findBestUpdateMatch(rpcPair);

    CompilationUnit astRoot = ASTResolving.createQuickFixAST(
        getCompilationUnit(), null);
    createImportRewrite(astRoot);

    ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());

    MethodDeclaration rewriterDstMethod = JavaASTUtils.findMethodDeclaration(
        astRoot, dstMethod.resolveBinding().getKey());
    if (rewriterDstMethod == null) {
      return null;
    }

    MethodDeclaration newSignature = computeMethodSignature(rewrite.getAST(),
        rpcPair, rewriterDstMethod);

    rewrite.replace(rewriterDstMethod, newSignature, null);

    return rewrite;
  }

  /**
   * Generates a sync/async method declaration based on the associated
   * async/sync method signature.
   */
  private MethodDeclaration computeMethodSignature(AST ast, RpcPair rpcPair,
      MethodDeclaration dstMethod) {

    MethodDeclaration method = ast.newMethodDeclaration();

    // New method has same name as original
    String methodName = rpcPair.srcMethod.getName().getIdentifier();
    method.setName(ast.newSimpleName(methodName));

    // Update the parameters
    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> params = method.parameters();
    params.addAll(computeParams(ast, rpcPair.srcMethod, dstMethod,
        getImportRewrite()));

    // Update the return type
    method.setReturnType2(computeReturnType(ast, rpcPair.srcMethod, dstMethod,
        getImportRewrite()));

    return method;
  }

  /**
   * Finds the best signature update candidate method in the peer type (smallest
   * edit distance between the argument lists).
   */
  private MethodDeclaration findBestUpdateMatch(RpcPair rpcPair) {
    int minDistance = 0;

    String name = rpcPair.srcMethod.getName().getIdentifier();
    List<SingleVariableDeclaration> params = adjustSrcParams(rpcPair.srcMethod);

    // Scan the target's methods and pick the smallest edit distance candidate
    for (MethodDeclaration dstMethod : rpcPair.dstType.getMethods()) {
      if (name.equals(dstMethod.getName().getIdentifier())) {
        List<SingleVariableDeclaration> targetParams = adjustDstParams(dstMethod);

        int distance = JavaASTUtils.editDistance(params, targetParams);
        if ((rpcPair.dstMethod == null) || (distance < minDistance)) {
          rpcPair.dstMethod = dstMethod;
          minDistance = distance;
        }
      }
    }

    return rpcPair.dstMethod;
  }
}
