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
package com.google.gwt.eclipse.core.clientbundle;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.TypeHierarchyCache;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.platform.clientbundle.ResourceTypeDefaultExtensions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.corext.dom.Bindings;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility methods for working with <code>ClientBundle</code> interfaces.
 */
@SuppressWarnings("restriction")
public final class ClientBundleUtilities {

  public static final String CLIENT_BUNDLE_SOURCE_ANNOTATION_NAME = "com.google.gwt.resources.client.ClientBundle.Source";

  public static final String CLIENT_BUNDLE_TYPE_NAME = "com.google.gwt.resources.client.ClientBundle";

  public static final String CLIENT_BUNDLE_WITH_LOOKUP_TYPE_NAME = "com.google.gwt.resources.client.ClientBundleWithLookup";

  public static final String CSS_RESOURCE_TYPE_NAME = "com.google.gwt.resources.client.CssResource";

  public static final String DATA_RESOURCE_TYPE_NAME = "com.google.gwt.resources.client.DataResource";

  public static final String RESOURCE_PROTOTYPE_TYPE_NAME = "com.google.gwt.resources.client.ResourcePrototype";

  private static final String GWT_CREATE_RESOURCE_TYPE_NAME = "com.google.gwt.resources.client.GwtCreateResource";

  private static final TypeHierarchyCache resourceTypesCache = new TypeHierarchyCache();

  private static final String TEXT_RESOURCE_TYPE_NAME = "com.google.gwt.resources.client.TextResource";

  public static IType findClientBundleType(IJavaProject javaProject) {
    return JavaModelSearch.findType(javaProject, CLIENT_BUNDLE_TYPE_NAME);
  }

  public static IType findCssResourceType(IJavaProject javaProject) {
    return JavaModelSearch.findType(javaProject, CSS_RESOURCE_TYPE_NAME);
  }

  public static IType[] findResourcePrototypeSubtypes(IJavaProject javaProject)
      throws JavaModelException {
    IType resourcePrototype = findResourcePrototypeType(javaProject);
    if (resourcePrototype == null) {
      return new IType[0];
    }

    return resourceTypesCache.getHierarchy(resourcePrototype).getAllSubtypes(
        resourcePrototype);
  }

  public static IType findResourcePrototypeType(IJavaProject javaProject) {
    return JavaModelSearch.findType(javaProject, RESOURCE_PROTOTYPE_TYPE_NAME);
  }

  public static boolean isClientBundle(IJavaProject javaProject, IType type)
      throws JavaModelException {
    return JavaUtilities.isSubtype(findClientBundleType(javaProject), type);
  }

  public static boolean isClientBundle(IJavaProject javaProject, String typeName)
      throws JavaModelException {
    IType type = JavaModelSearch.findType(javaProject, typeName);
    if (type == null) {
      return false;
    }
    return isClientBundle(javaProject, type);
  }

  public static boolean isClientBundle(ITypeBinding typeBinding) {
    ITypeBinding clientBundleBinding = Bindings.findTypeInHierarchy(
        typeBinding, CLIENT_BUNDLE_TYPE_NAME);
    return clientBundleBinding != null;
  }

  public static boolean isCssResource(IJavaProject javaProject, IType type)
      throws JavaModelException {
    return JavaUtilities.isSubtype(findCssResourceType(javaProject), type);
  }

  public static boolean isGwtCreateResource(ITypeBinding typeBinding) {
    ITypeBinding gwtCreateResourceBinding = JavaASTUtils.findTypeInHierarchy(
        typeBinding, GWT_CREATE_RESOURCE_TYPE_NAME);
    return gwtCreateResourceBinding != null;
  }

  public static boolean isResourceType(IJavaProject javaProject, IType type)
      throws JavaModelException {
    return JavaUtilities.isSubtype(findResourcePrototypeType(javaProject), type);
  }

  public static boolean isResourceType(ITypeBinding typeBinding) {
    ITypeBinding resourcePrototypeBinding = Bindings.findTypeInHierarchy(
        typeBinding, RESOURCE_PROTOTYPE_TYPE_NAME);
    return resourcePrototypeBinding != null;
  }

  public static String suggestMethodName(IFile file) {
    String filename = ResourceUtils.filenameWithoutExtension(file);

    // If the filename is empty after removing the extension (e.g. it had a
    // leading dot like .hidden), then just use the original filename
    if (filename.length() == 0) {
      filename = file.getName();
    }

    // If the filename (without extension) would make a good method name, use it
    if (JavaUtilities.isGoodMethodName(filename)) {
      return filename;
    }

    // Turn the filename into a suitable Java method name
    return JavaUtilities.sanitizeMethodName(filename);
  }

  public static String suggestResourceTypeName(IJavaProject javaProject,
      IFile file) {
    String ext = file.getFileExtension();
    if (ext == null) {
      return DATA_RESOURCE_TYPE_NAME;
    }

    // The @DefaultExtensions include the leading dot, so we need to as well
    ext = "." + ext;

    try {
      List<String> matchingResourceTypes = new ArrayList<String>();

      // Look for @DefaultExtensions on all ResourcePrototype subtypes
      IType[] resourceTypes = findResourcePrototypeSubtypes(javaProject);
      for (IType resourceType : resourceTypes) {
        String[] defaultExtensions = ResourceTypeDefaultExtensions.getDefaultExtensions(resourceType);
        for (String defaultExtension : defaultExtensions) {
          if (ResourceUtils.areFilenamesEqual(ext, defaultExtension)) {
            String resourceTypeName = resourceType.getFullyQualifiedName('.');
            matchingResourceTypes.add(resourceTypeName);
          }
        }
      }

      // Now see what we found
      if (matchingResourceTypes.size() > 0) {
        /*
         * If TextResource was a match, prefer it. This is necessary since it
         * and ExternalTextResource both declare .txt as their default
         * extension. Since TextResource (which inlines its content into the
         * compiled output) is the more commonly used variant, we want to use it
         * by default. If we're wrong, the user can always change it manually.
         */
        if (matchingResourceTypes.contains(TEXT_RESOURCE_TYPE_NAME)) {
          return TEXT_RESOURCE_TYPE_NAME;
        }

        /*
         * If we don't have TextResource in the mix, return the first matching
         * ResourcePrototype subtype we found. In the case of multiple resource
         * types that declare the same default file extensions, the order in
         * which they are found is undefined, so which one we return is
         * undefined as well. In general, multiple resource types will not have
         * the same default extensions, so this is probably good enough (that
         * is, it's not worth creating new UI/settings for this edge case).
         */
        return matchingResourceTypes.get(0);
      }

    } catch (JavaModelException e) {
      GWTPluginLog.logError(e, "Unable to suggest resource type for file "
          + file.getName());
    }

    return DATA_RESOURCE_TYPE_NAME;
  }

  private ClientBundleUtilities() {
    // Not instantiable
  }

}
