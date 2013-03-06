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
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Updates a sync method signature to match the associated async declaration.
 */
public class UpdateSyncSignatureProposal extends
    AbstractUpdateSignatureProposal {

  static final UpdateSignatureProposalBuilder BUILDER = new UpdateSignatureProposalBuilder() {
    public AbstractUpdateSignatureProposal createProposal(RpcPair rpcPair,
        int relevance) {
      return new UpdateSyncSignatureProposal(rpcPair, relevance);
    }
  };

  /**
   * Creates a quick-fix proposal for a marker on an async method declaration.
   * 
   * @param node the marker AST node (the async method's simple name).
   * @return the list of completion proposals for this marker.
   */
  public static List<IJavaCompletionProposal> createProposalsForProblemsOnAsyncMethod(
      ASTNode node) {

    RpcPair rpcPair = resolveRpcPair(node, PeerTypeResolver.SYNC_RESOLVER);

    return createProposal(rpcPair, BUILDER);
  }

  /**
   * Creates a quick-fix proposal for a marker on a sync type declaration.
   * 
   * @param node the marker AST node (the sync type's simple name).
   * @param methodBindingKey binding key for the problem member method.
   * @return the list of completion proposals for this marker.
   */
  public static List<IJavaCompletionProposal> createProposalsForProblemsOnSyncType(ASTNode node,
      String methodBindingKey) {

    RpcPair rpcPair = resolveRpcPair(node, methodBindingKey,
        PeerTypeResolver.ASYNC_RESOLVER);

    // If the async method is missing the callback, we cannot fix that with a
    // sync signature update.
    if (Util.getCallbackParameter(rpcPair.srcMethod) == null) {
      return Collections.emptyList();
    }

    return createProposal(rpcPair, BUILDER);
  }

  protected UpdateSyncSignatureProposal(RpcPair rpcPair, int relevance) {
    super(rpcPair, relevance);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<SingleVariableDeclaration> adjustDstParams(
      MethodDeclaration method) {

    // The destination method is in the sync interface
    return method.parameters();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<SingleVariableDeclaration> adjustSrcParams(
      MethodDeclaration method) {

    // The source method is in the async interface
    List<SingleVariableDeclaration> params = method.parameters();

    if (Util.getCallbackParameter(method) != null) {
      params = params.subList(0, params.size() - 1);
    }

    return params;
  }

  @Override
  protected List<SingleVariableDeclaration> computeParams(AST ast,
      MethodDeclaration srcMethod, MethodDeclaration dstMethod,
      ImportRewrite imports) {

    // Just clone the adjusted async parameters
    List<SingleVariableDeclaration> params = new ArrayList<SingleVariableDeclaration>();
    params.addAll(JavaASTUtils.cloneParameters(ast, adjustSrcParams(srcMethod),
        imports));

    return params;
  }

  @Override
  protected Type computeReturnType(AST ast, MethodDeclaration srcMethod,
      MethodDeclaration dstMethod, ImportRewrite imports) {

    // The sync return type is based on the async callback parameterization.
    return Util.computeSyncReturnType(ast, srcMethod, imports);
  }

}
