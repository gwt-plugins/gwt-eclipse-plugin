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
package com.google.gdt.eclipse.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * General purpose Java utilities.
 */
public final class JavaUtilities {

  /**
   * Produces an ArrayList for an array of elements where the list is pre-sized
   * with extra capacity. The extra capacity does not change the size of the
   * list, but does change the initial capacity of the list, thus making adds
   * more efficient if the ultimate size can be estimated.
   */
  public static <T> List<T> copyToArrayListWithExtraCapacity(T[] elements,
      int extraCapacity) {
    List<T> asList;
    if (elements != null) {
      asList = new ArrayList<T>(elements.length + extraCapacity);
      asList.addAll(Arrays.asList(elements));
    } else {
      asList = new ArrayList<T>(extraCapacity);
    }
    return asList;
  }

  /**
   * Equality check that also succeeds if both are null.
   */
  public static boolean equalsWithNullCheck(Object a, Object b) {
    if (a == null) {
      return (b == null);
    }

    return a.equals(b);
  }

  /**
   * Gets the package name from a fully qualified type name. Inner types MUST be
   * separated with '$'.
   */
  public static String getPackageName(String fullyQualifiedTypeName) {
    int periodIndex = fullyQualifiedTypeName.lastIndexOf('.');
    return periodIndex >= 0 ? fullyQualifiedTypeName.substring(0, periodIndex)
        : "";
  }

  /**
   * Gets the package name (e.g. com.google.gwt) from a package specified as a
   * path (e.g. com/google/gwt).
   * 
   * @param path the path whose segments will be joined with '.' delimiters
   * @return the package name
   */
  public static String getPackageNameFromPath(IPath path) {
    return StringUtilities.join(path.segments(), ".");
  }

  /**
   * Gets a qualified type name for a type, given its simple type name and
   * package name.
   * 
   * @param simpleTypeName the simple unqualified type name
   * @param packageName the package name; if empty or null, the
   *          <code>simpleTypeName</code> will be returned
   * @return the fully qualified type name
   */
  public static String getQualifiedTypeName(String simpleTypeName,
      String packageName) {
    return packageName != null && packageName.length() > 0 ? packageName + "."
        + simpleTypeName : simpleTypeName;
  }

  /**
   * Gets the type name (including enclosing types) from a fully qualified type
   * name. Inner types MUST be separated with '$'.
   */
  public static String getTypeName(String fullyQualifiedTypeName) {
    int periodIndex = fullyQualifiedTypeName.lastIndexOf('.');
    return periodIndex >= 0 ? fullyQualifiedTypeName.substring(periodIndex + 1)
        : fullyQualifiedTypeName;
  }

  /**
   * Answers whether the file has a <code>.class</code> extension.
   */
  public static boolean isClassFileName(String filename) {
    return ResourceUtils.endsWith(filename, ".class");
  }

  public static boolean isGoodMethodName(String methodName) {
    return validateMethodName(methodName).isOK();
  }

  /**
   * Returns whether a type is the super of another type.
   * 
   * @param superType the possible super type. If <code>null</code>, this method
   *          always returns <code>false<code>.
   * @param type the type
   */
  public static boolean isSubtype(IType superType, IType type)
      throws JavaModelException {
    if (superType == null) {
      return false;
    }

    ITypeHierarchy superTypes = type.newSupertypeHierarchy(null);
    return superTypes.contains(superType);
  }

  /**
   * Checks whether the given identifier is valid according to
   * {@link Character#isJavaIdentifierStart(char)} and
   * {@link Character#isJavaIdentifierPart(char)}. The empty string is
   * considered valid by this method.
   */
  public static boolean isValidJavaIdentifier(String identifier) {
    for (int i = 0; i < identifier.length(); i++) {
      boolean validChar = i == 0
          ? Character.isJavaIdentifierStart(identifier.charAt(i))
          : Character.isJavaIdentifierPart(identifier.charAt(i));

      if (!validChar) {
        return false;
      }
    }

    return true;
  }

  /**
   * Sanitizes a potential method name so it is both valid and follows Java
   * conventions (camel-cased, no underscores, etc.).
   */
  public static String sanitizeMethodName(String methodName) {
    assert (methodName != null && methodName.length() > 0);
    StringBuilder sb = new StringBuilder();

    // Ensure first character is valid and lower-case
    char firstChar = methodName.charAt(0);
    if (Character.isJavaIdentifierStart(firstChar)) {
      if (Character.isUpperCase(firstChar)) {
        firstChar = Character.toLowerCase(firstChar);
      }
      sb.append(firstChar);
    }

    // Replace remaining invalid characters
    boolean previousCharWasDropped = false;
    for (int i = 1; i < methodName.length(); i++) {
      char ch = methodName.charAt(i);
      if (Character.isLetterOrDigit(ch)) {
        // If we interpreted the last character as a separator and dropped it,
        // we capitalize the next character so the final name is camel-cased.
        if (previousCharWasDropped) {
          ch = Character.toUpperCase(ch);
        }
        sb.append(ch);
        previousCharWasDropped = false;
      } else {
        // Assume anything that is not alphanumeric is meant as a separator and
        // drop it. This includes characters that are invalid Java identifier
        // characters (e.g. dashes) as well as characters that are technically
        // valid, but which would look ugly in a method name (e.g. underscores).
        previousCharWasDropped = true;
      }
    }

    // If the original name was composed entirely of non-alphanumeric chars, we
    // need to return *something*, even if it's not very descriptive.
    if (sb.length() == 0) {
      sb.append("_method");
    }

    return sb.toString();
  }

  public static IStatus validateMethodName(String methodName) {
    String complianceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.compliance");
    String sourceLevel = JavaCore.getOption("org.eclipse.jdt.core.compiler.source");
    IStatus nameStatus = JavaConventions.validateMethodName(methodName,
        sourceLevel, complianceLevel);

    if (!nameStatus.isOK()) {
      return nameStatus;
    }

    // The JavaConventions class doesn't seem to be flagging method names with
    // an uppercase first character, so we need to check it ourselves.
    if (!Character.isLowerCase(methodName.charAt(0))) {
      return StatusUtilities.newWarningStatus(
          "Method name should start with a lowercase letter.",
          CorePlugin.PLUGIN_ID);
    }

    return StatusUtilities.OK_STATUS;
  }

  private JavaUtilities() {
    // Not instantiable
  }

}
