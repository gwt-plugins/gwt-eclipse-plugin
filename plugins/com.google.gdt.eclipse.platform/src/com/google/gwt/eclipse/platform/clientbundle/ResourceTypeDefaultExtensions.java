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
package com.google.gwt.eclipse.platform.clientbundle;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for mapping between a client bundle resource type and its
 * default file extensions.
 * 
 * This version queries the DefaultExtensions annotations declared on the
 * ResourcePrototype subtypes.
 */

// TODO Migrate this back to GWT since we no longer support 3.3
public class ResourceTypeDefaultExtensions {

  private static final String DEFAULT_EXTENSIONS_ANNOTATION_NAME = "com.google.gwt.resources.ext.DefaultExtensions";

  /**
   * Find all default extensions for this resource type. If the type itself does
   * not declare any extensions, it may inherit extensions from any of the
   * interfaces in its super type hierarchy.
   */
  public static String[] getDefaultExtensions(IType resourceType)
      throws JavaModelException {
    String[] extensions = getDeclaredDefaultExtensions(resourceType);

    // Check the super interface hierarchy for @DefaultExtensions
    if (extensions.length == 0) {
      ITypeHierarchy superHierarchy = resourceType.newSupertypeHierarchy(null);
      IType[] superInterfaces = superHierarchy.getAllSuperInterfaces(resourceType);
      for (IType superInterface : superInterfaces) {
        extensions = getDeclaredDefaultExtensions(superInterface);
        if (extensions.length > 0) {
          break;
        }
      }
    }
    return extensions;
  }

  /**
   * Find the default extensions declared directly by this type.
   */
  private static String[] getDeclaredDefaultExtensions(IType resourceType)
      throws JavaModelException {
    // Find the @DefaultExtensions annotation
    IAnnotation[] annotations = resourceType.getAnnotations();
    for (IAnnotation annotation : annotations) {
      if (isDefaultExtensionsAnnotation(annotation)) {
        // @DefaultExtensions should have single member-value pair: "value"
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        if (values.length == 1) {
          if (values[0].getMemberName().equals("value")) {
            Object value = values[0].getValue();
            // The extensions will be stored as Object[] of strings
            if (value instanceof Object[]) {
              List<String> extensions = new ArrayList<String>();
              for (Object extension : (Object[]) value) {
                assert (extension instanceof String);
                extensions.add((String) extension);
              }
              return extensions.toArray(new String[0]);
            }
          }
        }
      }
    }
    return new String[0];
  }

  private static boolean isDefaultExtensionsAnnotation(IAnnotation annotation) {
    String name = annotation.getElementName();

    // Annotation name can be either simple or qualified, depending on whether
    // the annotation is a binary or source type
    return name.equals(DEFAULT_EXTENSIONS_ANNOTATION_NAME)
        || name.equals(Signature.getSimpleName(DEFAULT_EXTENSIONS_ANNOTATION_NAME));
  }

  private ResourceTypeDefaultExtensions() {
    // Not instantiable
  }

}
