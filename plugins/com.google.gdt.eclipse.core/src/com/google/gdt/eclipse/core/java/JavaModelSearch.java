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
package com.google.gdt.eclipse.core.java;

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides methods for searching the JavaModel for particular elements. These
 * methods are roughly based on those in
 * {@link org.eclipse.jdt.internal.corext.util.JavaModelUtil} but adds the
 * ability to resolve the parameter types in method and ctor signatures
 */
@SuppressWarnings("restriction")
public class JavaModelSearch {

  /**
   * Visitor for Java element deltas.
   */
  public interface IJavaElementDeltaVisitor {
    boolean visit(IJavaElementDelta delta);
  }

  public static List<IType> findAllTopLevelTypes(IPackageFragment pckg)
      throws JavaModelException {
    List<IType> types = new ArrayList<IType>();
    for (ICompilationUnit cu : pckg.getCompilationUnits()) {
      types.addAll(Arrays.asList(cu.getTypes()));
    }
    return types;
  }

  public static IMethod findCtorInHierarchy(ITypeHierarchy hierarchy,
      IType type, String[] paramTypes) {
    return findMethodOrCtorInHierarchy(hierarchy, type, null, paramTypes, true);
  }

  public static IField findField(IType type, String fieldName) {
    IField field = type.getField(fieldName);
    if (field.exists()) {
      return field;
    }

    return null;
  }

  public static IField findFieldInHierarchy(ITypeHierarchy hierarchy,
      IType type, String fieldName) {

    IField field = findField(type, fieldName);

    // Check super class
    if (field == null) {
      IType superClass = hierarchy.getSuperclass(type);
      if (superClass != null) {
        field = findFieldInHierarchy(hierarchy, superClass, fieldName);
      }
    }

    return field;
  }

  /**
   * Find the first constructor in the specified type.
   */
  public static IJavaElement findFirstCtor(IType type) {
    try {
      for (IMethod method : type.getMethods()) {
        if (method.isConstructor()) {
          return method;
        }
      }
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
    }
    return null;
  }

  public static IMethod findMethodInHierarchy(ITypeHierarchy hierarchy,
      IType type, String methodName) {
    return findMethodOrCtorInHierarchy(hierarchy, type, methodName, false);
  }

  public static IMethod findMethodInHierarchy(ITypeHierarchy hierarchy,
      IType type, String methodName, String[] paramTypes) {
    return findMethodOrCtorInHierarchy(hierarchy, type, methodName, paramTypes,
        false);
  }

  /**
   * Finds the topmost declaration of the given method (in terms of super
   * interfaces/classes), and then finds all declarations that override that
   * method.
   * 
   * @param method the method that will be used to search
   * @return a non-null array of overridden methods
   */
  public static IMethod[] findMethodsFromTopmostHierarchy(IMethod method)
      throws CoreException {
    IProgressMonitor pm = new NullProgressMonitor();
    IMethod topmostMethod = MethodChecks.getTopmostMethod(method, null, pm);
    if (topmostMethod == null) {
      // This method is not overridden, so it is the topmost
      topmostMethod = method;
    }
    return RippleMethodFinder2.getRelatedMethods(topmostMethod, pm, null);
  }

  public static IType findType(IJavaProject project, String typeName) {
    try {
      IType type = project.findType(typeName.replace('$', '.'));
      // Only return the type if it's actually there
      if (type != null && type.exists()) {
        return type;
      }
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
    }

    return null;
  }

  /**
   * Returns a list of package fragments for the given package name.
   * 
   * @param javaProject the java project whose classpath will be searched
   * @param packageName the package name to find corresponding fragments for
   * @return a possibly empty list of package fragments
   * @throws JavaModelException
   */
  public static List<IPackageFragment> getPackageFragments(
      IJavaProject javaProject, String packageName) throws JavaModelException {
    List<IPackageFragment> packageFragments = new ArrayList<IPackageFragment>();

    for (IPackageFragmentRoot packageFragmentRootroot : javaProject.getAllPackageFragmentRoots()) {
      IPackageFragment packageFragment = packageFragmentRootroot.getPackageFragment(packageName);
      if (packageFragment.exists()) {
        packageFragments.add(packageFragment);
      }
    }

    return packageFragments;
  }

  /**
   * Extract the fully-qualified type name out of a type signature, with dots as
   * package separators.
   */
  public static String getQualifiedTypeName(String typeSignature) {
    // Make sure we're using dots for package separator, not /'s
    typeSignature = typeSignature.replace('/', '.');

    String pckg = Signature.getSignatureQualifier(typeSignature);
    String typeName = Signature.getSignatureSimpleName(typeSignature);

    if (pckg == null || pckg.length() == 0) {
      return typeName;
    }

    return pckg + "." + typeName;
  }

  /**
   * This method checks whether the element is on the classpath (to make sure
   * it's not matched by an exclusion filter), which can be expensive, see GWT
   * issue 4793.
   * <p>
   * Note: As we discover new meanings for "validity", the checks could change.
   */
  public static boolean isValidElement(IJavaElement element) {
    return element != null && element.exists()
        && element.getJavaProject().isOnClasspath(element);
  }

  /**
   * Resolves a type from the type's signature.
   * 
   * @param contextType context of the signature
   * @param typeSignature the type's signature (does not have to be fully
   *          qualified)
   * @return the resolved type, or null
   * @throws JavaModelException
   */
  public static IType resolveType(IType contextType, String typeSignature)
      throws JavaModelException {
    String fqType = resolveTypeName(contextType,
        Signature.toString(typeSignature));
    if (fqType != null) {
      IType type = contextType.getJavaProject().findType(fqType);
      if (type != null && type.exists()) {
        return type;
      }
    }

    return null;
  }

  /**
   * Resolves a type name to a fully-qualified name.
   * <p>
   * Note: This only returns the first match on ambiguous lookups.
   * 
   * @return the fully-qualified name, or null if it could not be resolved
   * @throws JavaModelException if there was an error when resolving
   */
  public static String resolveTypeName(IType context, String typeName)
      throws JavaModelException {
    // resolveType may return multiple names if there are ambiguous matches
    String[][] matches = context.resolveType(typeName);
    if (matches == null) {
      return null;
    }

    // If there are multiple matches, we'll take the first one
    String matchPckg = matches[0][0];
    String matchType = matches[0][1];

    if (matchPckg.length() == 0) {
      return matchType;
    }

    return matchPckg + "." + matchType;
  }

  public static void visitJavaElementDelta(IJavaElementDelta delta,
      IJavaElementDeltaVisitor visitor) {
    if (!visitor.visit(delta)) {
      return;
    }

    for (IJavaElementDelta childDelta : delta.getAffectedChildren()) {
      visitJavaElementDelta(childDelta, visitor);
    }
  }

  private static IMethod findMethodOrCtor(IType type, String methodName,
      String[] paramTypes, boolean isConstructor) {
    try {
      IMethod[] methods = type.getMethods();
      for (IMethod method : methods) {
        if (methodSignaturesEqual(type, methodName, paramTypes, isConstructor,
            method)) {
          return method;
        }
      }
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
    }

    return null;
  }

  private static IMethod findMethodOrCtorInHierarchy(ITypeHierarchy hierarchy,
      IType type, String methodName, boolean isConstructor) {
    try {
      IMethod[] methods = type.getMethods();
      for (IMethod method : methods) {
        if (method.getElementName().equals(methodName)) {
          return method;
        }
      }
    } catch (JavaModelException e) {
      CorePluginLog.logError(e);
      return null;
    }

    // Check super class
    IType superClass = hierarchy.getSuperclass(type);
    if (superClass != null) {
      IMethod method = findMethodOrCtorInHierarchy(hierarchy, superClass,
          methodName, isConstructor);
      if (method != null) {
        return method;
      }
    }

    if (!isConstructor) {
      // Check interfaces
      IType[] superInterfaces = hierarchy.getSuperInterfaces(type);
      for (IType superInterface : superInterfaces) {
        IMethod method = findMethodOrCtorInHierarchy(hierarchy, superInterface,
            methodName, false);
        if (method != null) {
          return method;
        }
      }
    }

    return null;
  }

  private static IMethod findMethodOrCtorInHierarchy(ITypeHierarchy hierarchy,
      IType type, String methodName, String[] paramTypes, boolean isConstructor) {
    IMethod method = findMethodOrCtor(type, methodName, paramTypes,
        isConstructor);
    if (method != null) {
      return method;
    }

    // Check super class
    IType superClass = hierarchy.getSuperclass(type);
    if (superClass != null) {
      method = findMethodOrCtorInHierarchy(hierarchy, superClass, methodName,
          paramTypes, isConstructor);
      if (method != null) {
        return method;
      }
    }

    if (!isConstructor) {
      // Check interfaces
      IType[] superInterfaces = hierarchy.getSuperInterfaces(type);
      for (IType superInterface : superInterfaces) {
        method = findMethodOrCtorInHierarchy(hierarchy, superInterface,
            methodName, paramTypes, false);
        if (method != null) {
          return method;
        }
      }
    }

    return method;
  }

  /**
   * Removes any type parameters and any array nesting in a type signature.
   */
  private static String getBaseType(String typeSignature) {
    // Strip off any type parameters
    typeSignature = Signature.getTypeErasure(typeSignature);

    // Strip off any array nesting
    typeSignature = Signature.getElementType(typeSignature);

    return typeSignature;
  }

  /**
   * Returns true if the parameter type name is a generic type parameter of
   * either the specified method or its containing type.
   */
  private static boolean isTypeParameter(IMethod method,
      String simpleParamTypeName) throws JavaModelException {
    List<ITypeParameter> typeParams = new ArrayList<ITypeParameter>();
    typeParams.addAll(Arrays.asList(method.getTypeParameters()));
    typeParams.addAll(Arrays.asList(method.getDeclaringType().getTypeParameters()));

    for (ITypeParameter typeParam : typeParams) {
      if (typeParam.getElementName().equals(simpleParamTypeName)) {
        return true;
      }
    }

    return false;
  }

  private static boolean methodSignaturesEqual(IType type2, String methodName,
      String[] paramTypes, boolean isConstructor, IMethod method2) {
    try {
      // Method names must match, unless we're comparing constructors
      if (!isConstructor && !method2.getElementName().equals(methodName)) {
        return false;
      }

      // Only compare ctors to ctors and methods to methods
      if (isConstructor != method2.isConstructor()) {
        return false;
      }

      // Parameter count must match
      String signature2 = method2.getSignature();
      String[] paramTypes2 = Signature.getParameterTypes(signature2);
      if (paramTypes.length != paramTypes2.length) {
        return false;
      }

      // Compare each parameter type
      for (int i = 0; i < paramTypes.length; i++) {
        String paramType = paramTypes[i];
        String paramType2 = paramTypes2[i];

        // Compare array nesting depth ([] = 1, [][] = 2, etc.)
        if (Signature.getArrayCount(paramType) != Signature.getArrayCount(paramType2)) {
          return false;
        }

        // Remove any array nesting and generic type parameters
        paramType = getBaseType(paramType);
        paramType2 = getBaseType(paramType2);

        // Extract the full type names from the signatures
        String paramTypeName = getQualifiedTypeName(paramType);
        String paramTypeName2 = getQualifiedTypeName(paramType2);

        if (isTypeParameter(method2, paramTypeName2)) {
          // Skip parameters whose type is a generic type parameter of the
          // method we're comparing against, or the method's containing class
          continue;

          /*
           * TODO: we're currently not checking the bounds of generic type
           * parameters, so sometimes we may return true here even when the
           * caller's method signature doesn't match the method we're comparing
           * against. We could try to add that logic here, or better still, we
           * could integrate TypeOracle and take advantage of its type searching
           * capabilities.
           */
        }

        // If we run into an unresolved parameter type in the method we're
        // searching, we'll need to resolve that before doing the comparison
        if (paramType2.charAt(0) == Signature.C_UNRESOLVED) {
          paramTypeName2 = resolveTypeName(type2, paramTypeName2);
        }

        // Finally, compare the type names
        if (!paramTypeName.equals(paramTypeName2)) {
          return false;
        }
      }

      // We passed all the checks, so the signatures are equal
      return true;

    } catch (JavaModelException e) {
      CorePluginLog.logError(e,
          "Error comparing method signatures of {0} and {1}", methodName,
          method2.getElementName());
      return false;
    }
  }
}
