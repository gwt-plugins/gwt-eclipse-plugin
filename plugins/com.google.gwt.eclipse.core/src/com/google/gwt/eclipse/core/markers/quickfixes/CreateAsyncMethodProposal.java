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
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gwt.eclipse.core.util.Util;
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates a new async RPC method for an existing sync method.
 */
@SuppressWarnings("restriction")
public class CreateAsyncMethodProposal extends AbstractCreateMethodProposal {

  public static List<IJavaCompletionProposal> createProposalsForProblemOnAsyncType(
      ICompilationUnit asyncCompilationUnit, ASTNode problemNode,
      String syncMethodBindingKey) {
    TypeDeclaration asyncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        problemNode, ASTNode.TYPE_DECLARATION);
    assert (asyncTypeDecl != null);
    String asyncQualifiedTypeName = asyncTypeDecl.resolveBinding().getQualifiedName();

    // Lookup the sync version of the interface
    IType syncType = RemoteServiceUtilities.findSyncType(asyncTypeDecl);
    if (syncType == null) {
      return Collections.emptyList();
    }

    MethodDeclaration syncMethodDecl = JavaASTUtils.findMethodDeclaration(
        syncType.getCompilationUnit(), syncMethodBindingKey);
    if (syncMethodDecl == null) {
      return Collections.emptyList();
    }

    return Collections.<IJavaCompletionProposal> singletonList(new CreateAsyncMethodProposal(
        asyncCompilationUnit, asyncQualifiedTypeName, syncMethodDecl));
  }

  public static List<IJavaCompletionProposal> createProposalsForProblemOnSyncMethod(
      ASTNode problemNode) {
    // Find the problematic sync method declaration and its declaring type
    MethodDeclaration syncMethodDecl = ASTResolving.findParentMethodDeclaration(problemNode);
    TypeDeclaration syncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        syncMethodDecl, ASTNode.TYPE_DECLARATION);
    assert (syncTypeDecl != null);

    // Lookup the async version of the interface
    IType asyncType = RemoteServiceUtilities.findAsyncType(syncTypeDecl);
    if (asyncType == null) {
      return Collections.emptyList();
    }

    return Collections.<IJavaCompletionProposal> singletonList(new CreateAsyncMethodProposal(
        asyncType.getCompilationUnit(), asyncType.getFullyQualifiedName('.'),
        syncMethodDecl));
  }

  private CreateAsyncMethodProposal(ICompilationUnit asyncCompilationUnit,
      String asyncQualifiedTypeName, MethodDeclaration syncMethodDecl) {
    super(asyncCompilationUnit, asyncQualifiedTypeName, syncMethodDecl);
  }

  @Override
  protected MethodDeclaration createMethodDeclaration(AST ast) {
    MethodDeclaration asyncMethodDecl = ast.newMethodDeclaration();

    // New method has same name as original
    String methodName = getSyncMethodDeclaration().getName().getIdentifier();
    asyncMethodDecl.setName(ast.newSimpleName(methodName));

    // Async method has void return type by default (the user can also use
    // Request or RequestBuilder as the return type to get more functionality).
    // TODO: investigate whether we can enter linked mode after the fix is
    // applied, so the user can choose what return type to use. See
    // LinkedCorrectionProposal, which is a subclass of
    // ASTRewriteCorrectionProposal.
    asyncMethodDecl.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));

    addAsyncParameters(ast, asyncMethodDecl);

    // TODO: generate comments for new method

    return asyncMethodDecl;
  }

  @SuppressWarnings("unchecked")
  private void addAsyncParameters(AST ast, MethodDeclaration asyncMethodDecl) {
    List<SingleVariableDeclaration> asyncMethodParams = asyncMethodDecl.parameters();

    // Clone all the existing sync method parameters
    asyncMethodParams.addAll(JavaASTUtils.cloneParameters(ast,
        getSyncMethodDeclaration().parameters(), getImportRewrite()));

    List<String> existingParamNames = new ArrayList<String>();
    for (SingleVariableDeclaration param : asyncMethodParams) {
      existingParamNames.add(param.getName().getIdentifier());
    }
    String callbackParameterName = StringUtilities.computeUniqueName(
        existingParamNames.toArray(new String[0]), "callback");

    // Add the AsyncCallback parameter to the end
    asyncMethodParams.add(Util.createAsyncCallbackParameter(ast,
        getSyncMethodDeclaration().getReturnType2(), callbackParameterName,
        getImportRewrite()));
  }

  private MethodDeclaration getSyncMethodDeclaration() {
    return sourceMethodDecl;
  }
}
