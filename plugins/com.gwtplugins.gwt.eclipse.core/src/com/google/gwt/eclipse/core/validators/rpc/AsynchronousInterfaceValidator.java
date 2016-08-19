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
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import java.util.Collections;
import java.util.List;

/**
 * Validate a asynchronous interface against its synchronous version.
 */
@SuppressWarnings("restriction")
class AsynchronousInterfaceValidator extends AbstractPairedInterfaceValidator {

  @Override
  public List<CategorizedProblem> validate(TypeDeclaration changedInterface,
      ITypeBinding dependentTypeBinding) {
    if (dependentTypeBinding != null
        && !RemoteServiceUtilities.isSynchronousInterface(dependentTypeBinding)) {
      // Not really an async interface
      return Collections.emptyList();
    }

    return super.validate(changedInterface, dependentTypeBinding);
  }

  @Override
  protected List<CategorizedProblem> doMissingDependentInterface(
      TypeDeclaration changedInterface) {
    // If the sync interface is missing assume that this is not an async
    // interface
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<CategorizedProblem> doValidateMethodOnChangedType(
      MethodDeclaration methodDeclaration, ITypeBinding dependentTypeBinding) {
    MethodDeclaration node = methodDeclaration;
    IMethodBinding methodBinding = node.resolveBinding();
    List<SingleVariableDeclaration> parameters = node.parameters();
    SingleVariableDeclaration lastParameter = parameters.isEmpty() ? null
        : parameters.get(parameters.size() - 1);
    ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();

    if (!RemoteServiceUtilities.hasAsyncCallbackParameter(parameterTypes)) {
      // Method's last parameter is not of type AsyncCallback
      CategorizedProblem problem = RemoteServiceProblemFactory.newNoAsyncCallbackParameter(methodDeclaration);
      if (problem != null) {
        return Collections.singletonList(problem);
      } else {
        return Collections.emptyList();
      }
    }

    if (!hasValidReturnType(methodBinding.getReturnType())) {
      // Method's return type is not one of the accepted types
      CategorizedProblem problem = RemoteServiceProblemFactory.newInvalidAsyncReturnType(methodDeclaration);
      if (problem != null) {
        return Collections.singletonList(problem);
      } else {
        return Collections.emptyList();
      }
    }

    String[] parameterQualifiedNames = RemoteServiceUtilities.computeSyncParameterTypes(methodBinding);
    String methodName = methodBinding.getName();
    IMethodBinding dependentMethod = Bindings.findMethodInHierarchy(
        dependentTypeBinding, methodName, parameterQualifiedNames);
    if (dependentMethod == null) {
      // No version of methodDeclaration on the dependent type
      CategorizedProblem problem = RemoteServiceProblemFactory.newMissingSyncMethodOnAsync(
          methodDeclaration, dependentTypeBinding);
      if (problem != null) {
        return Collections.singletonList(problem);
      } else {
        return Collections.emptyList();
      }
    }

    return doValidateReturnTypes(node, lastParameter, parameterTypes,
        dependentMethod);
  }

  @Override
  protected List<CategorizedProblem> doValidateMethodOnDependentInterface(
      IMethodBinding methodBinding, TypeDeclaration changedInterface,
      ITypeBinding dependentInterfaceBinding) {
    String[] parameters = RemoteServiceUtilities.computeAsyncParameterTypes(methodBinding);
    String methodName = methodBinding.getName();
    if (Bindings.findMethodInHierarchy(changedInterface.resolveBinding(),
        methodName, parameters) == null) {
      CategorizedProblem problem = RemoteServiceProblemFactory.newMissingAsyncMethodOnAsync(
          methodBinding, changedInterface);
      if (problem != null) {
        return Collections.singletonList(problem);
      }
    }

    return Collections.emptyList();
  }

  /**
   * Validate that the AsyncCallback's parameterization and the sync method's
   * return type are assignment compatible.
   */
  @SuppressWarnings("unchecked")
  private List<CategorizedProblem> doValidateReturnTypes(
      MethodDeclaration node, SingleVariableDeclaration lastParameter,
      ITypeBinding[] parameterTypes, IMethodBinding dependentMethod) {
    ITypeBinding asyncCallbackParam = parameterTypes[parameterTypes.length - 1];
    if (asyncCallbackParam.isParameterizedType()) {
      ITypeBinding[] typeArguments = asyncCallbackParam.getTypeArguments();
      ITypeBinding syncReturnTypeBinding = dependentMethod.getReturnType();

      ITypeBinding typeBinding = syncReturnTypeBinding;
      if (syncReturnTypeBinding.isPrimitive()) {
        String qualifiedWrapperTypeName = JavaASTUtils.getWrapperTypeName(syncReturnTypeBinding.getQualifiedName());
        typeBinding = node.getAST().resolveWellKnownType(
            qualifiedWrapperTypeName);
      }

      boolean compatible = false;
      if (typeBinding != null) {
        compatible = canAssign(typeArguments[0], typeBinding);
      }

      if (!compatible) {
        ParameterizedType parameterizedType = (ParameterizedType) lastParameter.getType();
        List<Type> types = parameterizedType.typeArguments();
        CategorizedProblem problem = RemoteServiceProblemFactory.newAsyncCallbackTypeArgumentMismatchOnAsync(
            types.get(0), typeArguments[0], syncReturnTypeBinding);
        if (problem != null) {
          return Collections.singletonList(problem);
        }
      }
    }

    return Collections.emptyList();
  }

  private boolean hasValidReturnType(ITypeBinding returnTypeBinding) {
    return Util.VALID_ASYNC_RPC_RETURN_TYPES.contains(returnTypeBinding.getQualifiedName());
  }
}
