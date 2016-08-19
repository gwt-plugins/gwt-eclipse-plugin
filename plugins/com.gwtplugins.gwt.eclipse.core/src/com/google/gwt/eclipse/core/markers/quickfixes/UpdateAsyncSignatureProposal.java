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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates an async method signature to match the associated sync declaration.
 */
public class UpdateAsyncSignatureProposal extends
    AbstractUpdateSignatureProposal {

  private static final String DEFAULT_CALLBACK_NAME = "callback";

  static final UpdateSignatureProposalBuilder BUILDER = new UpdateSignatureProposalBuilder() {
    public AbstractUpdateSignatureProposal createProposal(RpcPair rpcPair,
        int relevance) {
      return new UpdateAsyncSignatureProposal(rpcPair, relevance);
    }
  };

  /**
   * Creates a quick-fix proposal for a marker on an async type declaration.
   * 
   * @param node the marker AST node (the async type's simple name).
   * @param methodBindingKey binding key for the problem member method.
   * @return the list of completion proposals for this marker.
   */
  public static List<IJavaCompletionProposal> createProposalsForProblemsOnAsyncType(ASTNode node,
      String methodBindingKey) {

    RpcPair rpcPair = resolveRpcPair(node, methodBindingKey,
        PeerTypeResolver.SYNC_RESOLVER);

    return createProposal(rpcPair, BUILDER);
  }

  /**
   * Creates a quick-fix proposal for a marker on a sync method declaration.
   * 
   * @param node the marker AST node (the sync method's simple name).
   * @return the list of completion proposals for this marker.
   */
  public static List<IJavaCompletionProposal> createProposalsForProblemsOnSyncMethod(ASTNode node) {
    RpcPair rpcPair = resolveRpcPair(node, PeerTypeResolver.ASYNC_RESOLVER);

    return createProposal(rpcPair, BUILDER);
  }

  protected UpdateAsyncSignatureProposal(RpcPair rpcPair, int relevance) {
    super(rpcPair, relevance);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<SingleVariableDeclaration> adjustDstParams(
      MethodDeclaration method) {

    // The destination method is in the async interface
    List<SingleVariableDeclaration> params = method.parameters();

    if (Util.getCallbackParameter(method) != null) {
      params = params.subList(0, params.size() - 1);
    }

    return params;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<SingleVariableDeclaration> adjustSrcParams(
      MethodDeclaration method) {

    // The source method is in the sync interface
    return method.parameters();
  }

  @Override
  protected List<SingleVariableDeclaration> computeParams(AST ast,
      MethodDeclaration srcMethod, MethodDeclaration dstMethod,
      ImportRewrite imports) {

    // Clone the sync method parameters
    List<SingleVariableDeclaration> params = new ArrayList<SingleVariableDeclaration>();
    params.addAll(JavaASTUtils.cloneParameters(ast, adjustSrcParams(srcMethod),
        imports));

    // Append an AsyncCallback
    params.add(Util.createAsyncCallbackParameter(ast,
        srcMethod.getReturnType2(), computeCallBackName(dstMethod), imports));

    return params;
  }

  @Override
  protected Type computeReturnType(AST ast, MethodDeclaration srcMethod,
      MethodDeclaration dstMethod, ImportRewrite imports) {

    // Use the previous async return type if valid
    ITypeBinding typeBinding = dstMethod.getReturnType2().resolveBinding();
    if (typeBinding != null
        && Util.VALID_ASYNC_RPC_RETURN_TYPES.contains(typeBinding.getQualifiedName())) {
      return JavaASTUtils.normalizeTypeAndAddImport(ast,
          dstMethod.getReturnType2(), imports);
    }

    return ast.newPrimitiveType(PrimitiveType.VOID);
  }

  private String computeCallBackName(MethodDeclaration method) {
    // Use the current name if found
    SingleVariableDeclaration callback = Util.getCallbackParameter(method);
    if (callback != null) {
      return callback.getName().getIdentifier();
    }

    return DEFAULT_CALLBACK_NAME;
  }
}
