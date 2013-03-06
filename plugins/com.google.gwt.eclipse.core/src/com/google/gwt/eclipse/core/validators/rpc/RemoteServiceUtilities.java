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
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC problem marker and quick fix utility methods.
 */
@SuppressWarnings("restriction")
public class RemoteServiceUtilities {

  public static final String ASYNCCALLBACK_QUALIFIED_NAME = "com.google.gwt.user.client.rpc.AsyncCallback";

  private static final String ASYNC_TYPE_NAME_SUFFIX = "Async";

  private static final String[] NO_STRINGS = new String[0];

  private static final String REMOTE_SERVICE_QUALIFIED_NAME = "com.google.gwt.user.client.rpc.RemoteService";

  /**
   * NOTE: Remove this once we know what is going on with RPC2. 
   */
  private static final String RPC_SERVICE_QUALIFIED_NAME = "com.google.gwt.rpc.client.RpcService";

  /**
   * Computes the asynchronous method parameter types from the synchronous
   * method model.
   * 
   * @throws RemoteServiceException if there was an error resolving a type
   */
  public static String[] computeAsyncParameterTypes(IMethod syncMethod)
      throws RemoteServiceException {
    List<String> parameters = new ArrayList<String>();

    for (String parameterSig : syncMethod.getParameterTypes()) {
      parameters.add(resolveToQualifiedErasure(syncMethod.getDeclaringType(),
          parameterSig));
    }

    return computeAsyncParameterTypes(parameters);
  }

  /**
   * Computes the asynchronous method parameter types from the synchronous
   * method binding.
   */
  public static String[] computeAsyncParameterTypes(
      IMethodBinding syncMethodBinding) {
    List<String> parameters = new ArrayList<String>();

    for (ITypeBinding typeBinding : syncMethodBinding.getParameterTypes()) {
      parameters.add(typeBinding.getErasure().getQualifiedName());
    }

    return computeAsyncParameterTypes(parameters);
  }

  public static String computeAsyncTypeName(String qualifiedSyncTypeName) {
    return qualifiedSyncTypeName + ASYNC_TYPE_NAME_SUFFIX;
  }

  // TODO: move this somewhere more general
  public static String computeMethodSignature(String returnType,
      String methodName, String[] parameterTypes) {
    StringBuilder sb = new StringBuilder();
    sb.append(returnType);
    sb.append(" ");
    sb.append(methodName);
    sb.append("(");
    sb.append(StringUtilities.join(parameterTypes, ", "));
    sb.append(")");
    return sb.toString();
  }

  /**
   * Computes the synchronous method parameter types from the asynchronous
   * method model.
   * 
   * @throws RemoteServiceException if there was an error resolving a type
   */
  public static String[] computeSyncParameterTypes(IMethod asyncMethod)
      throws RemoteServiceException {
    List<String> parameters = new ArrayList<String>();

    String[] asyncParamTypeSigs = asyncMethod.getParameterTypes();
    for (int i = 0; i < asyncParamTypeSigs.length - 1; i++) {
      parameters.add(resolveToQualifiedErasure(asyncMethod.getDeclaringType(),
          asyncParamTypeSigs[i]));
    }

    return parameters.toArray(NO_STRINGS);
  }

  public static String computeSyncTypeName(String qualifiedAsyncTypeName) {
    if (!isAsyncTypeName(qualifiedAsyncTypeName)) {
      // Return null if the name passed in does not end in Async
      return null;
    }

    // Strip off the Async
    return qualifiedAsyncTypeName.substring(0, qualifiedAsyncTypeName.length()
        - ASYNC_TYPE_NAME_SUFFIX.length());
  }

  public static void expandSuperInterfaces(ITypeBinding binding,
      List<ITypeBinding> bindings) {
    if (bindings.contains(binding)) {
      return;
    }

    bindings.add(binding);

    ITypeBinding[] intfs = binding.getInterfaces();
    for (ITypeBinding intf : intfs) {
      expandSuperInterfaces(intf, bindings);
    }
  }

  public static IType findAsyncType(TypeDeclaration syncTypeDeclaration) {
    IJavaProject javaProject = JavaASTUtils.getCompilationUnit(
        syncTypeDeclaration).getJavaProject();
    String syncQualifiedTypeName = syncTypeDeclaration.resolveBinding().getQualifiedName();

    String asyncQualifiedTypeName = RemoteServiceUtilities.computeAsyncTypeName(syncQualifiedTypeName);
    return JavaModelSearch.findType(javaProject, asyncQualifiedTypeName);
  }

  public static IType findSyncType(TypeDeclaration asyncTypeDeclaration) {
    IJavaProject javaProject = JavaASTUtils.getCompilationUnit(
        asyncTypeDeclaration).getJavaProject();
    String asyncQualifiedTypeName = asyncTypeDeclaration.resolveBinding().getQualifiedName();

    String syncQualifiedTypeName = RemoteServiceUtilities.computeSyncTypeName(asyncQualifiedTypeName);
    return JavaModelSearch.findType(javaProject, syncQualifiedTypeName);
  }

  public static boolean isAsyncInterface(IType type) throws JavaModelException {
    return resolveSyncType(type) != null;
  }

  public static boolean isAsyncTypeName(String typeName) {
    return typeName.endsWith(ASYNC_TYPE_NAME_SUFFIX);
  }

  /**
   * Returns <code>true</code> if the type is or extends the RemoteService 
   * interface.
   */
  public static boolean isSyncInterface(IType type) throws JavaModelException {
    IJavaProject javaProject = type.getJavaProject();
    if (!GWTNature.isGWTProject(javaProject.getProject())) {
      return false;
    }

    ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
    IType remoteServiceInterface = javaProject.findType(REMOTE_SERVICE_QUALIFIED_NAME);
    return remoteServiceInterface != null
        && hierarchy.contains(remoteServiceInterface);
  }

  public static IType resolveAsyncType(IType syncInterface)
      throws JavaModelException {
    String asyncQualifiedName = computeAsyncTypeName(syncInterface.getFullyQualifiedName('.'));
    return syncInterface.getJavaProject().findType(asyncQualifiedName);
  }

  public static IType resolveSyncType(IType asyncInterface)
      throws JavaModelException {

    String asyncQualifiedName = asyncInterface.getFullyQualifiedName('.');
    String syncQualifiedName = computeSyncTypeName(asyncQualifiedName);
    if (syncQualifiedName == null) {
      return null;
    }

    return asyncInterface.getJavaProject().findType(syncQualifiedName);
  }

  /**
   * Computes the synchronous method parameter types from the asynchronous
   * method binding.
   */
  static String[] computeSyncParameterTypes(IMethodBinding asyncMethodBinding) {
    List<String> parameters = new ArrayList<String>();

    ITypeBinding[] parameterTypes = asyncMethodBinding.getParameterTypes();
    for (int i = 0; i < parameterTypes.length - 1; ++i) {
      parameters.add(parameterTypes[i].getErasure().getQualifiedName());
    }

    return parameters.toArray(NO_STRINGS);
  }

  static ITypeBinding getAsyncCallbackParam(IMethodBinding methodBinding) {
    ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
    if (hasAsyncCallbackParameter(parameterTypes)) {
      return parameterTypes[parameterTypes.length - 1];
    }

    return null;
  }

  static boolean hasAsyncCallbackParameter(ITypeBinding[] parameterTypes) {
    if (parameterTypes.length == 0) {
      return false;
    }

    ITypeBinding lastParameter = parameterTypes[parameterTypes.length - 1];
    ITypeBinding erasure = lastParameter.getErasure();
    if (erasure == null) {
      // Problem with the erasure, check the start of the parameterized type
      return lastParameter.getQualifiedName().startsWith(
          RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME);
    }

    return erasure.getQualifiedName().equals(
        RemoteServiceUtilities.ASYNCCALLBACK_QUALIFIED_NAME);
  }

  static boolean isSynchronousInterface(ITypeBinding typeBinding) {
    String qualifiedName = typeBinding.getQualifiedName();
    if (qualifiedName.equals(REMOTE_SERVICE_QUALIFIED_NAME) ||
        qualifiedName.equals(RPC_SERVICE_QUALIFIED_NAME)) {
      // Ignore the RemoteService and RpcService marker interfaces
      return false;
    }

    ITypeBinding remoteServiceBinding = Bindings.findTypeInHierarchy(
        typeBinding, REMOTE_SERVICE_QUALIFIED_NAME);
    return remoteServiceBinding != null;
  }

  private static String[] computeAsyncParameterTypes(
      List<String> qualifiedSyncParamTypes) {
    qualifiedSyncParamTypes.add(ASYNCCALLBACK_QUALIFIED_NAME);
    return qualifiedSyncParamTypes.toArray(NO_STRINGS);
  }

  private static String resolveToQualifiedErasure(IType context,
      String typeSignature) throws RemoteServiceException {
    try {
      String typeErasureSig = Signature.getTypeErasure(typeSignature);
      String type = Signature.getSignatureSimpleName(typeErasureSig);
      String qualifiedErasure = JavaModelSearch.resolveTypeName(context, type);
      if (qualifiedErasure == null) {
        throw new RemoteServiceException("Could not resolve type " + type);
      }
      return qualifiedErasure;
    } catch (JavaModelException e) {
      throw new RemoteServiceException(e);
    }
  }

}
