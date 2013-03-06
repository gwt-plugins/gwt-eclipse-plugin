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

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Factory for creating {@link RemoteServiceProblem}s.
 */
class RemoteServiceProblemFactory {
  private static final String[] NO_STRINGS = new String[0];

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH} on an
   * <b>asynchronous</b> interface.
   */
  static RemoteServiceProblem newAsyncCallbackTypeArgumentMismatchOnAsync(
      Type callbackTypeArgument, ITypeBinding callbackTypeArgumentBinding,
      ITypeBinding syncReturnType) {
    String[] arguments = {
        callbackTypeArgumentBinding.getName(), syncReturnType.getName()};
    final String[] problemArgs = {"async"};
    return RemoteServiceProblem.create(callbackTypeArgument,
        RemoteServiceProblemType.ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH,
        arguments, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH} on a
   * <b>synchronous</b> interface.
   */
  static RemoteServiceProblem newAsyncCallbackTypeArgumentMismatchOnSync(
      MethodDeclaration syncMethodDeclaration,
      ITypeBinding callbackTypeArgumentBinding) {
    String[] arguments = {
        callbackTypeArgumentBinding.getName(),
        syncMethodDeclaration.resolveBinding().getReturnType().getName()};
    final String[] problemArgs = {"sync"};
    return RemoteServiceProblem.create(syncMethodDeclaration.getReturnType2(),
        RemoteServiceProblemType.ASYNCCALLBACK_TYPE_ARGUMENT_MISMATCH,
        arguments, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#INVALID_ASYNC_RETURN_TYPE}.
   */
  static RemoteServiceProblem newInvalidAsyncReturnType(
      MethodDeclaration methodDeclaration) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, n = Util.VALID_ASYNC_RPC_RETURN_TYPES.size(); i < n; ++i) {
      if (i > 0) {
        if (i == n - 1) {
          sb.append(" or ");
        } else {
          sb.append(", ");
        }
      }

      String validReturnType = Util.VALID_ASYNC_RPC_RETURN_TYPES.get(i);
      sb.append(Signature.getSimpleName(validReturnType));
    }

    return RemoteServiceProblem.create(methodDeclaration.getReturnType2(),
        RemoteServiceProblemType.INVALID_ASYNC_RETURN_TYPE,
        new String[] {sb.toString()}, NO_STRINGS);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_ASYNC_METHOD} on an
   * <b>asynchronous</b> type.
   */
  static RemoteServiceProblem newMissingAsyncMethodOnAsync(
      IMethodBinding syncMethodBinding, TypeDeclaration asyncTypeDeclaration) {
    String[] messageArgs = {
        asyncTypeDeclaration.getName().getIdentifier(),
        toAsyncMethodSignature(syncMethodBinding)};
    String[] problemArgs = {"async", syncMethodBinding.getKey()};

    return RemoteServiceProblem.create(asyncTypeDeclaration.getName(),
        RemoteServiceProblemType.MISSING_ASYNC_METHOD, messageArgs, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_ASYNC_METHOD} on a
   * <b>synchronous</b> type.
   */
  static RemoteServiceProblem newMissingAsyncMethodOnSync(
      MethodDeclaration syncMethodDeclaration, ITypeBinding asyncTypeBinding) {
    String[] messageArgs = {
        asyncTypeBinding.getName(),
        syncMethodDeclaration.getName().getIdentifier()};
    String[] problemArgs = {"sync"};
    return RemoteServiceProblem.create(syncMethodDeclaration.getName(),
        RemoteServiceProblemType.MISSING_ASYNC_METHOD, messageArgs, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_ASYNC_TYPE}.
   */
  static RemoteServiceProblem newMissingAsyncType(
      TypeDeclaration syncTypeDeclaration) {
    ITypeBinding syncTypeBinding = syncTypeDeclaration.resolveBinding();
    String computeAsyncTypeName = RemoteServiceUtilities.computeAsyncTypeName(syncTypeBinding.getName());
    return RemoteServiceProblem.create(syncTypeDeclaration.getName(),
        RemoteServiceProblemType.MISSING_ASYNC_TYPE,
        new String[] {computeAsyncTypeName},
        new String[] {syncTypeBinding.getQualifiedName()});
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_SYNC_METHOD} on an
   * <b>asynchronous</b> type.
   */
  static RemoteServiceProblem newMissingSyncMethodOnAsync(
      MethodDeclaration asyncMethodDeclaration, ITypeBinding syncTypeBinding) {
    String methodName = asyncMethodDeclaration.resolveBinding().getName();
    String[] messageArgs = {syncTypeBinding.getName(), methodName};
    String[] problemArgs = {"async"};
    return RemoteServiceProblem.create(asyncMethodDeclaration.getName(),
        RemoteServiceProblemType.MISSING_SYNC_METHOD, messageArgs, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_SYNC_METHOD} on a
   * <b>synchronous</b> type.
   */
  static RemoteServiceProblem newMissingSyncMethodOnSync(
      TypeDeclaration syncTypeDeclaration, IMethodBinding asyncMethodBinding) {
    String[] messageArgs = {
        syncTypeDeclaration.getName().getIdentifier(),
        toSyncMethodSignature(asyncMethodBinding)};
    String[] problemArgs = {"sync", asyncMethodBinding.getKey()};

    return RemoteServiceProblem.create(syncTypeDeclaration.getName(),
        RemoteServiceProblemType.MISSING_SYNC_METHOD, messageArgs, problemArgs);
  }

  /**
   * Returns a new {@link RemoteServiceProblem} for a
   * {@link RemoteServiceProblemType#MISSING_ASYNCCALLBACK_PARAMETER}.
   */
  static RemoteServiceProblem newNoAsyncCallbackParameter(
      MethodDeclaration methodDeclaration) {
    return RemoteServiceProblem.create(methodDeclaration.getName(),
        RemoteServiceProblemType.MISSING_ASYNCCALLBACK_PARAMETER, NO_STRINGS,
        NO_STRINGS);
  }

  static String toAsyncMethodSignature(IMethodBinding syncMethod) {
    StringBuilder sb = new StringBuilder();
    sb.append(syncMethod.getName());
    sb.append("(");
    ITypeBinding[] parameterTypes = syncMethod.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; ++i) {
      if (i != 0) {
        sb.append(", ");
      }

      sb.append(parameterTypes[i].getName());
    }

    if (parameterTypes.length > 0) {
      sb.append(", ");
    }

    sb.append("AsyncCallback<");
    ITypeBinding returnType = syncMethod.getReturnType();
    if (returnType.isPrimitive()) {
      sb.append(Signature.getSimpleName(JavaASTUtils.getWrapperTypeName(returnType.getName())));
    } else {
      sb.append(returnType.getName());
    }
    sb.append(">");
    sb.append(")");

    return sb.toString();
  }

  static String toSyncMethodSignature(IMethodBinding asyncMethodSignature) {
    StringBuilder sb = new StringBuilder();
    ITypeBinding[] parameterTypes = asyncMethodSignature.getParameterTypes();
    sb.append(asyncMethodSignature.getName());
    sb.append("(");
    for (int i = 0; i < parameterTypes.length - 1; ++i) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(parameterTypes[i].getName());
    }
    sb.append(")");
    return sb.toString();
  }
}
