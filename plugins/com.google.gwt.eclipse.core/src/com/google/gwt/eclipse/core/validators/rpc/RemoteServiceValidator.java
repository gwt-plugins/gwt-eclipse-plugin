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
package com.google.gwt.eclipse.core.validators.rpc;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.validation.ValidationResult;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator which ensures that the synchronous and asynchronous versions of a
 * remote service are valid with respect to each other.
 */
public class RemoteServiceValidator {
  private static class RemoteServiceValidationVisitor extends ASTVisitor {
    private final PairedInterfaceValidator asynchronousInterfaceValidator = new AsynchronousInterfaceValidator();
    private final List<String> dependentTypes = new ArrayList<String>();
    private final IJavaProject javaProject;

    private final List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();

    private final PairedInterfaceValidator synchronousInterfaceValidator = new SynchronousInterfaceValidator();

    public RemoteServiceValidationVisitor(IJavaProject javaProject) {
      this.javaProject = javaProject;
    }

    @Override
    public boolean visit(TypeDeclaration changedType) {
      if (!changedType.isInterface()) {
        return true;
      }

      ITypeBinding typeBinding = changedType.resolveBinding();
      if (typeBinding == null) {
        return true;
      }

      PairedInterfaceValidator validator;
      String typeQualifiedName = typeBinding.getQualifiedName();
      TypeDeclaration dependentType = null;

      String dependentTypeQualifiedName;

      if (RemoteServiceUtilities.isSynchronousInterface(typeBinding)) {
        dependentTypeQualifiedName = RemoteServiceUtilities.computeAsyncTypeName(typeQualifiedName);
        dependentType = JavaASTUtils.findTypeDeclaration(javaProject,
            dependentTypeQualifiedName);
        validator = synchronousInterfaceValidator;
      } else {
        validator = asynchronousInterfaceValidator;
        dependentTypeQualifiedName = RemoteServiceUtilities.computeSyncTypeName(typeQualifiedName);
        if (dependentTypeQualifiedName == null) {
          // Not an async interface...
          return true;
        }

        dependentType = JavaASTUtils.findTypeDeclaration(javaProject,
            dependentTypeQualifiedName);
      }

      // Add the type dependency (even if the type doesn't yet resolve)
      dependentTypes.add(dependentTypeQualifiedName);

      ITypeBinding dependentTypeBinding = null;
      if (dependentType != null) {
        dependentTypeBinding = dependentType.resolveBinding();
      }

      problems.addAll(validator.validate(changedType, dependentTypeBinding));

      return true;
    }
  }

  public ValidationResult validate(ASTNode ast) {
    // TODO: Just pass in a CompilationUnit
    assert (ast.getNodeType() == ASTNode.COMPILATION_UNIT);
    CompilationUnit compilationUnit = (CompilationUnit) ast;
    IJavaProject javaProject = compilationUnit.getJavaElement().getJavaProject();

    RemoteServiceValidationVisitor remoteServiceValidationVisitor = new RemoteServiceValidationVisitor(
        javaProject);
    ast.accept(remoteServiceValidationVisitor);
    return new ValidationResult(remoteServiceValidationVisitor.problems,
        remoteServiceValidationVisitor.dependentTypes);
  }
}