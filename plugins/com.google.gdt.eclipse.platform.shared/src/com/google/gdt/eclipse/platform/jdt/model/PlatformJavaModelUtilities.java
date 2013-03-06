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
package com.google.gdt.eclipse.platform.jdt.model;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Utility methods for the Java model supported by the current platform.
 */
public class PlatformJavaModelUtilities {

  /**
   * Exception thrown when IAnnotation (and related classes) are not available
   * on the current Eclipse version.
   */
  @SuppressWarnings("serial")
  public static class IAnnotationNotAvailableException extends Exception {
  }

  /**
   * Gets the corresponding IAnnotation.
   * 
   * @param qualifiedAnnotationName the fully qualified annotation type name
   * @param annotatable the IAnnotatable java element which contains the
   *          annotation
   * @param contextType the type which is used to lookup imports
   * @return the IAnnotation or null
   * @throws JavaModelException
   */
  public static Object getAnnotation(String qualifiedAnnotationName,
      Object annotatable, IType contextType) throws JavaModelException,
      IAnnotationNotAvailableException {
    for (IAnnotation annotation : ((IAnnotatable) annotatable).getAnnotations()) {
      if (qualifiedAnnotationName.equals(resolveTypeName(contextType,
          annotation.getElementName()))) {
        return annotation;
      }
    }

    return null;
  }

  /**
   * Gets the value of an annotation, if the value is of the given type.
   * 
   * @param annotation the IAnnotation from which the value will be returned
   * @param type the type of value expected (if the type does not match this,
   *          null will be returned)
   * @return the value of the annotation, or null
   * @throws JavaModelException
   */
  @SuppressWarnings("unchecked")
  public static <T> T getSingleMemberAnnotationValue(Object annotation, Class<T> type)
      throws JavaModelException, IAnnotationNotAvailableException {
    IMemberValuePair[] pairs = ((IAnnotation) annotation).getMemberValuePairs();
    if (pairs.length == 0) {
      return null;
    }

    Object value = pairs[0].getValue();
    return type.isInstance(value) ? (T) value : null;
  }

  /*
   * Copied from JavaModelSearch since platform cannot depend on those plugins.
   */
  private static String resolveTypeName(IType context, String typeName)
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

}
