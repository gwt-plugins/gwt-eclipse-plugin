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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import java.util.Collections;
import java.util.List;

/**
 * Validate a synchronous interface against its asynchronous version.
 */
@SuppressWarnings("restriction")
class SynchronousInterfaceValidator extends AbstractPairedInterfaceValidator {

  @Override
  protected List<CategorizedProblem> doMissingDependentInterface(
      TypeDeclaration changedInterface) {
    ValidationSuppressionVisitor warningSuppressionVisitor = new ValidationSuppressionVisitor();

    /*
     * BodyDeclaration.modifiers returns a raw type, which can only be an
     * instance of IExtendedModifier. IExtendedModifier is only implemented by
     * the Annotation and Modifier ASTNode subtypes.
     */
    @SuppressWarnings("unchecked")
    List<ASTNode> modifiers = changedInterface.modifiers();
    for (ASTNode modifier : modifiers) {
      modifier.accept(warningSuppressionVisitor);
    }

    if (warningSuppressionVisitor.shouldSuppressValidation()) {
      return Collections.emptyList();
    }

    CategorizedProblem problem = RemoteServiceProblemFactory.newMissingAsyncType(changedInterface);
    if (problem != null) {
      return Collections.singletonList(problem);
    }

    return Collections.emptyList();
  }

  @Override
  protected List<CategorizedProblem> doValidateMethodOnChangedType(
      MethodDeclaration methodDeclaration, ITypeBinding dependentTypeBinding) {
    IMethodBinding methodBinding = methodDeclaration.resolveBinding();
    String[] parameters = RemoteServiceUtilities.computeAsyncParameterTypes(methodBinding);
    String methodName = methodBinding.getName();
    IMethodBinding dependentMethod = Bindings.findMethodInHierarchy(
        dependentTypeBinding, methodName, parameters);
    if (dependentMethod == null) {
      // No asynchronous version of methodDeclaration was found
      CategorizedProblem problem = RemoteServiceProblemFactory.newMissingAsyncMethodOnSync(
          methodDeclaration, dependentTypeBinding);
      if (problem != null) {
        return Collections.singletonList(problem);
      } else {
        return Collections.emptyList();
      }
    }

    ITypeBinding asyncCallbackType = RemoteServiceUtilities.getAsyncCallbackParam(dependentMethod);
    assert (asyncCallbackType != null);

    if (asyncCallbackType.isParameterizedType()) {
      // Check that the synchronous method's return type is assignment
      // compatible with the async parameter's parameterization if any
      ITypeBinding returnType = methodBinding.getReturnType();
      ITypeBinding typeBinding = returnType;
      if (returnType.isPrimitive()) {
        typeBinding = methodDeclaration.getAST().resolveWellKnownType(
            JavaASTUtils.getWrapperTypeName(returnType.getQualifiedName()));
      }

      ITypeBinding[] typeArguments = asyncCallbackType.getTypeArguments();
      if (!canAssign(typeBinding, typeArguments[0])) {
        CategorizedProblem problem = RemoteServiceProblemFactory.newAsyncCallbackTypeArgumentMismatchOnSync(
            methodDeclaration, typeArguments[0]);
        if (problem != null) {
          return Collections.singletonList(problem);
        }
      }
    }

    return Collections.emptyList();
  }

  @Override
  protected List<CategorizedProblem> doValidateMethodOnDependentInterface(
      IMethodBinding dependentMethodBinding, TypeDeclaration changedInterface,
      ITypeBinding dependentTypeBinding) {
    String[] parameters = RemoteServiceUtilities.computeSyncParameterTypes(dependentMethodBinding);
    if (Bindings.findMethodInHierarchy(changedInterface.resolveBinding(),
        dependentMethodBinding.getName(), parameters) == null) {
      CategorizedProblem problem1 = RemoteServiceProblemFactory.newMissingSyncMethodOnSync(
          changedInterface, dependentMethodBinding);
      if (problem1 != null) {
        return Collections.singletonList(problem1);
      }
    }

    return Collections.emptyList();
  }

}
