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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.swt.graphics.Image;

import java.text.MessageFormat;

/**
 * Creates a new method on an RPC async or sync interface from an existing
 * method defined on the other interface.
 */
@SuppressWarnings("restriction")
public abstract class AbstractCreateMethodProposal extends
    ASTRewriteCorrectionProposal {

  private static final Image ICON = JavaPlugin.getImageDescriptorRegistry().get(
      JavaPluginImages.DESC_MISC_PUBLIC);

  private static final int RELEVANCE = 100;

  private static String generateDisplayName(String typeName, String methodName) {
    return MessageFormat.format("Generate method ''{0}'' in type ''{1}''",
        methodName, typeName);
  }

  protected final MethodDeclaration sourceMethodDecl;

  protected final String targetQualifiedTypeName;

  public AbstractCreateMethodProposal(ICompilationUnit targetCu,
      String targetQualifiedTypeName, MethodDeclaration sourceMethodDecl) {
    super(generateDisplayName(Signature.getSimpleName(targetQualifiedTypeName),
        sourceMethodDecl.getName().getIdentifier()), targetCu, null, RELEVANCE,
        ICON);
    this.targetQualifiedTypeName = targetQualifiedTypeName;
    this.sourceMethodDecl = sourceMethodDecl;
  }

  protected abstract MethodDeclaration createMethodDeclaration(AST ast);

  @Override
  protected ASTRewrite getRewrite() {
    CompilationUnit targetAstRoot = ASTResolving.createQuickFixAST(
        getCompilationUnit(), null);
    createImportRewrite(targetAstRoot);

    // Find the target type declaration
    TypeDeclaration typeDecl = JavaASTUtils.findTypeDeclaration(targetAstRoot,
        targetQualifiedTypeName);
    if (typeDecl == null) {
      return null;
    }

    ASTRewrite rewrite = ASTRewrite.create(targetAstRoot.getAST());

    // Generate the new method declaration
    MethodDeclaration methodDecl = createMethodDeclaration(rewrite.getAST());

    // Add the new method declaration to the interface
    ChildListPropertyDescriptor property = ASTNodes.getBodyDeclarationsProperty(typeDecl);
    ListRewrite listRewriter = rewrite.getListRewrite(typeDecl, property);
    listRewriter.insertLast(methodDecl, null);

    return rewrite;
  }
}
