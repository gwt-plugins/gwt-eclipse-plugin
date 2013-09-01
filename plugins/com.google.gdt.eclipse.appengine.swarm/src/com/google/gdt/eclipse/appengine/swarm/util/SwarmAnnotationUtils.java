/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.appengine.swarm.util;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import java.util.List;

/**
 * Utility class for detecting Swarm Annotations in a given compilation unit.
 */
public class SwarmAnnotationUtils {

  private static final String PRIMARY_KEY = "PrimaryKey";
  private static final String ID = "Id";
  private static final String QSTRING = "QString;";
  private static final String STRING = "String";
  private static final String LONG = "Long";

  public static void collectApiTypes(
      List<IType> types, ICompilationUnit cu)
      throws JavaModelException {
    for (IType t : cu.getTypes()) {
      // @Api has @Inherited attribute, so go with supertypes
      if (isAnnotatedTypeInHierarchy(SwarmType.API, t)) {
        types.add(t);
      }
    }
  }

  public static void collectSwarmTypes(List<IType> types, ICompilationUnit cu)
      throws JavaModelException {
    for (SwarmType swarmType : SwarmType.values()) {
      collectTypes(types, cu, swarmType);
    }
  }

  public static void collectSwarmTypesInHierarchy(List<IType> types, ICompilationUnit cu)
      throws JavaModelException {
    for (SwarmType swarmType : SwarmType.values()) {
      if (SwarmType.API.equals(swarmType)) {
        collectApiTypes(types, cu);
      } else {
        collectTypes(types, cu, swarmType);
      }
    }
  }

  public static void collectTypes(
      List<IType> types, ICompilationUnit cu, SwarmType type)
      throws JavaModelException {
    IType[] topLevelTypes = cu.getTypes();
    for (IType t : topLevelTypes) {
      if (isAnnotatedType(type, t)) {
        types.add(t);
      }
    }
  }

  /**
   * Gets the id/primary key field name for the entity.
   *
   * @param type The entity.
   * @return The id/primary key field name of the entity.
   */
  public static String getId(IType type) throws JavaModelException {
    for (IField field : type.getFields()) {
      for (IAnnotation annotation : field.getAnnotations()) {
        if (annotation.getElementName().equals(PRIMARY_KEY)
            || annotation.getElementName().equals(ID)) {
          String idName = field.getElementName();
          return "get" + Character.toUpperCase(idName.charAt(0)) +
              (idName.length() == 1 ? "" : idName.substring(1));
        }
      }
    }
    return "getId";
  }

  public static String getPrimaryKeyType(IType type) throws JavaModelException {
    for (IField field : type.getFields()) {
      for (IAnnotation annotation : field.getAnnotations()) {
        if (annotation.getElementName().equals(PRIMARY_KEY)
            || annotation.getElementName().equals(ID)) {
          return field.getTypeSignature().equals(QSTRING) ? STRING : LONG;
        }
      }
    }
    return LONG;
  }

  public static SwarmType getSwarmType(IType type) throws JavaModelException {
    for (SwarmType swarmType : SwarmType.values()) {
      if (isAnnotatedType(swarmType, type)) {
        return swarmType;
      }
    }
    return null;
  }

  private static boolean isAnnotatedType(SwarmType swarmType, IType type)
      throws JavaModelException {
    for (IAnnotation annotation : type.getAnnotations()) {
      if (swarmType.equals(annotation.getElementName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isAnnotatedTypeInHierarchy(SwarmType swarmType, IType type)
      throws JavaModelException {
    if (isAnnotatedType(swarmType, type)) {
      return true;
    }
    ITypeHierarchy hierarchy = type.newSupertypeHierarchy(null);
    IType[] allSupertypes = hierarchy.getAllSupertypes(type);
    for (int i = allSupertypes.length - 1; i >= 0; i--) {
      IType superType = allSupertypes[i];
      if (isAnnotatedType(swarmType, superType)) {
        return true;
      }
    }
    return false;
  }
}
