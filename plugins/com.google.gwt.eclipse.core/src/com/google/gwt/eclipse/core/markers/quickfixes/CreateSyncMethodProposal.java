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
import com.google.gwt.eclipse.core.validators.rpc.RemoteServiceUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import java.util.Collections;
import java.util.List;

/**
 * Creates a new sync RPC method for an existing async method.
 */
@SuppressWarnings("restriction")
public class CreateSyncMethodProposal extends AbstractCreateMethodProposal {

  public static List<IJavaCompletionProposal> createProposalsForProblemOnAsyncMethod(
      ASTNode problemNode) {
    // Find the problematic async method declaration and its declaring type
    MethodDeclaration asyncMethodDecl = ASTResolving.findParentMethodDeclaration(problemNode);
    TypeDeclaration asyncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        asyncMethodDecl, ASTNode.TYPE_DECLARATION);
    assert (asyncTypeDecl != null);

    // Lookup the sync version of the interface
    IType syncType = RemoteServiceUtilities.findSyncType(asyncTypeDecl);
    if (syncType == null) {
      return Collections.emptyList();
    }

    return Collections.<IJavaCompletionProposal> singletonList(new CreateSyncMethodProposal(
        syncType.getCompilationUnit(), syncType.getFullyQualifiedName('.'),
        asyncMethodDecl));
  }

  public static List<IJavaCompletionProposal> createProposalsForProblemOnSyncType(
      ICompilationUnit syncCompilationUnit, ASTNode problemNode,
      String asyncMethodBindingKey) {
    TypeDeclaration syncTypeDecl = (TypeDeclaration) ASTResolving.findAncestor(
        problemNode, ASTNode.TYPE_DECLARATION);
    assert (syncTypeDecl != null);
    String syncQualifiedTypeName = syncTypeDecl.resolveBinding().getQualifiedName();

    // Lookup the async version of the interface
    IType asyncType = RemoteServiceUtilities.findAsyncType(syncTypeDecl);
    if (asyncType == null) {
      return Collections.emptyList();
    }

    MethodDeclaration asyncMethodDecl = JavaASTUtils.findMethodDeclaration(
        asyncType.getCompilationUnit(), asyncMethodBindingKey);
    if (asyncMethodDecl == null) {
      return Collections.emptyList();
    }

    return Collections.<IJavaCompletionProposal> singletonList(new CreateSyncMethodProposal(
        syncCompilationUnit, syncQualifiedTypeName, asyncMethodDecl));
  }

  public CreateSyncMethodProposal(ICompilationUnit syncCompilationUnit,
      String syncQualifiedTypeName, MethodDeclaration asyncMethodDecl) {
    super(syncCompilationUnit, syncQualifiedTypeName, asyncMethodDecl);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected MethodDeclaration createMethodDeclaration(AST ast) {
    MethodDeclaration syncMethodDecl = ast.newMethodDeclaration();

    // New method has same name as original
    String methodName = getAsyncMethodDeclaration().getName().getIdentifier();
    syncMethodDecl.setName(ast.newSimpleName(methodName));

    syncMethodDecl.setReturnType2(Util.computeSyncReturnType(ast,
        getAsyncMethodDeclaration(), getImportRewrite()));

    addSyncParameters(ast, syncMethodDecl);

    // TODO: generate comments for new method

    return syncMethodDecl;
  }

  @SuppressWarnings("unchecked")
  private void addSyncParameters(AST ast, MethodDeclaration syncMethodDecl) {
    // Clone all existing async method parameters but the last (AsyncCallback)
    List asyncParameters = getAsyncMethodDeclaration().parameters();
    syncMethodDecl.parameters().addAll(
        JavaASTUtils.cloneParameters(ast,
            asyncParameters.subList(0, asyncParameters.size() - 1),
            getImportRewrite()));
  }

  private MethodDeclaration getAsyncMethodDeclaration() {
    return sourceMethodDecl;
  }
}
