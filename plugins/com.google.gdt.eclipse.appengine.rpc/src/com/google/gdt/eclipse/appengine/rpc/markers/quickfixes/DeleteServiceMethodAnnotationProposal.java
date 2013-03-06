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

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.platform.jdt.text.correction.proposals.ASTRewriteCorrectionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.graphics.Image;

import java.util.Collections;
import java.util.List;

/**
 * Deletes the Service Method annotation
 */
@SuppressWarnings("restriction")
public class DeleteServiceMethodAnnotationProposal extends
    ASTRewriteCorrectionProposal {

  private static final Image ICON = JavaPlugin.getImageDescriptorRegistry().get(
      JavaPluginImages.DESC_MISC_PUBLIC);

  private static final int RELEVANCE = 99;

  public static List<IJavaCompletionProposal> createProposals(
      ICompilationUnit serviceCompilationUnit, ASTNode problemNode) {

    String serviceName = serviceCompilationUnit.findPrimaryType().getElementName();

    MethodDeclaration serviceMethod = null;
    if (problemNode instanceof SimpleName) {
      problemNode = problemNode.getParent();
    }
    if (problemNode instanceof MethodDeclaration) {
      serviceMethod = (MethodDeclaration) problemNode;
    }
    return Collections.<IJavaCompletionProposal> singletonList(new DeleteServiceMethodAnnotationProposal(
        serviceName, serviceCompilationUnit, serviceMethod));
  }

  private MethodDeclaration serviceMethod;

  public DeleteServiceMethodAnnotationProposal(String name,
      ICompilationUnit cu, MethodDeclaration serviceMethod) {

    super("Remove @ServiceMethod annotation", cu, null, RELEVANCE, ICON); //$NON-NLS-N$
    this.serviceMethod = serviceMethod;
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
        targetAstRoot, serviceMethod.resolveBinding().getKey());
    if (rewriterAstMethodDecl == null) {
      return null;
    }

    ASTNode annotationNode = null;
    List<IExtendedModifier> modifiers = rewriterAstMethodDecl.modifiers();
    for (IExtendedModifier modifier : modifiers) {
      if (modifier.isAnnotation()) {
        String name = ((Annotation) modifier).getTypeName().toString();
        if (name.equals("ServiceMethod")) { //$NON-NLS-N$
          annotationNode = (ASTNode) modifier;
          break;
        }
      }
    }

    if (annotationNode != null) {
      rewrite.remove(annotationNode, null);
    }
    return rewrite;
  }

}
