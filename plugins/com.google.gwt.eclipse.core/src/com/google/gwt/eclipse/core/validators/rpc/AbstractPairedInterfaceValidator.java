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

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.TypeRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
@SuppressWarnings("restriction")
abstract class AbstractPairedInterfaceValidator implements
    PairedInterfaceValidator {

  /**
   * Comparing type bindings from different ASTs appears to work correctly
   * unless the bindings involve arrays of primitives or type parameters. In the
   * case of arrays case,
   * {@link TypeRules#canAssign(ITypeBinding, ITypeBinding)} assumes that the
   * bindings are from the same AST and so it uses an identity comparison
   * instead of equality.
   * 
   * In the case of type parameters, two List<T>'s where T extend Serializable
   * are not considered equal or assignment compatible. In this case, we simply
   * erase to the entire type and simply check raw types.
   * 
   * TODO: Maybe create a BindingUtilities class for this?
   */
  protected static boolean canAssign(ITypeBinding lhs, ITypeBinding rhs) {
    if (containsTypeVariableReferences(lhs)
        || containsTypeVariableReferences(rhs)) {
      // One of the type bindings referenced a type parameter, so just compare
      // the erasures of each type
      lhs = lhs.getErasure();
      rhs = rhs.getErasure();
    }

    if (lhs.isArray() && rhs.isArray()) {
      if (lhs.getDimensions() == rhs.getDimensions()) {

        while (lhs.isArray()) {
          lhs = lhs.getComponentType();
        }

        while (rhs.isArray()) {
          rhs = rhs.getComponentType();
        }

        if (lhs.isPrimitive() && rhs.isPrimitive()) {
          return lhs.getKey().equals(rhs.getKey());
        }
      }
    }

    return TypeRules.canAssign(lhs, rhs);
  }

  /**
   * Returns <code>true</code> if the given type binding references a type
   * variable, at any depth.
   */
  protected static boolean containsTypeVariableReferences(
      ITypeBinding typeBinding) {
    if (typeBinding.isTypeVariable()) {
      return true;
    }

    if (typeBinding.isArray()) {
      return containsTypeVariableReferences(typeBinding.getComponentType());
    }

    if (typeBinding.isParameterizedType()) {
      ITypeBinding[] typeArguments = typeBinding.getTypeArguments();
      for (int i = 0; i < typeArguments.length; ++i) {
        if (containsTypeVariableReferences(typeArguments[i])) {
          return true;
        }
      }
    }

    if (typeBinding.isWildcardType()) {
      ITypeBinding bound = typeBinding.getBound();
      if (bound != null) {
        return containsTypeVariableReferences(bound);
      }
    }

    return false;
  }

  public List<CategorizedProblem> validate(TypeDeclaration changedInterface,
      ITypeBinding dependentTypeBinding) {
    CompilationUnit compilationUnit = getCompilationUnit(changedInterface);
    if (JavaASTUtils.hasErrors(changedInterface, compilationUnit.getProblems())) {
      /*
       * Don't validate any type that already has errors (we are assuming that
       * it has JDT errors.
       */
      return Collections.emptyList();
    }

    if (dependentTypeBinding == null) {
      return doMissingDependentInterface(changedInterface);
    }

    // Check that for every method on the changed interface, there is a
    // corresponding method on the dependent interface
    List<CategorizedProblem> problems = new ArrayList<CategorizedProblem>();
    for (MethodDeclaration methodDeclaration : changedInterface.getMethods()) {
      List<CategorizedProblem> methodProblems = doValidateMethodOnChangedType(
          methodDeclaration, dependentTypeBinding);
      problems.addAll(methodProblems);
    }

    List<ITypeBinding> superTypeBindings = new ArrayList<ITypeBinding>();
    RemoteServiceUtilities.expandSuperInterfaces(dependentTypeBinding,
        superTypeBindings);

    for (ITypeBinding typeBinding : superTypeBindings) {
      for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
        List<CategorizedProblem> dependentMethodProblems = doValidateMethodOnDependentInterface(
            methodBinding, changedInterface, typeBinding);
        problems.addAll(dependentMethodProblems);
      }
    }

    return problems;
  }

  protected abstract List<CategorizedProblem> doMissingDependentInterface(
      TypeDeclaration changedInterface);

  protected abstract List<CategorizedProblem> doValidateMethodOnChangedType(
      MethodDeclaration methodDeclaration, ITypeBinding dependentTypeBinding);

  protected abstract List<CategorizedProblem> doValidateMethodOnDependentInterface(
      IMethodBinding methodBinding, TypeDeclaration changedInterface,
      ITypeBinding dependentInterfaceBinding);

  protected CompilationUnit getCompilationUnit(ASTNode node) {
    ASTNode root = node.getRoot();
    assert (root.getNodeType() == ASTNode.COMPILATION_UNIT);
    return (CompilationUnit) root;
  }
}
