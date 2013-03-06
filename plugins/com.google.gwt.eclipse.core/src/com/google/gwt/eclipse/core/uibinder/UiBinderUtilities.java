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
package com.google.gwt.eclipse.core.uibinder;

import com.google.gdt.eclipse.core.JavaASTUtils;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.java.SignatureUtilities;
import com.google.gdt.eclipse.platform.jdt.model.PlatformJavaModelUtilities;
import com.google.gdt.eclipse.platform.jdt.model.PlatformJavaModelUtilities.IAnnotationNotAvailableException;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.contentassist.ElExpressionFirstFragmentComputer.ElExpressionFirstFragment;
import com.google.gwt.eclipse.core.uibinder.model.reference.UiBinderReferenceManager;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for UiBinder.
 * <p>
 * For utility methods that involve the XML model, see
 * {@link UiBinderXmlModelUtilities}.
 */
public final class UiBinderUtilities {

  /**
   * Visitor for each fragment of an EL expression.
   */
  public interface ElExpressionFragmentVisitor {

    /**
     * Called when a fragment returns a primitive, but it isn't the last
     * fragment in the field reference.
     */
    void visitNonterminalPrimitiveFragment(String fragment, int offset, int length);

    /**
     * Called for a fragment that has been resolved to a particular method.
     * 
     * @param method the method corresponding to the fragment
     * @param offset the offset to the fragment relative to the EL contents
     * @param length the length of the fragment
     */
    void visitResolvedFragmentMethod(IMethod method, int offset, int length);

    /**
     * Called for a fragment that could not be resolved to a particular method.
     * 
     * @param fragment the text of the fragment
     * @param offset the offset to the fragment relative to the EL contents
     * @param length the length of the fragment
     * @param enclosingType the type that should have contained the method
     */
    void visitUnresolvedFragment(String fragment, int offset, int length,
        IType enclosingType);
  }

  private static final NullProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

  /**
   * Finds the exact matching first fragment from a list of first fragments.
   * 
   * @param elExpressionContents the contents of the EL expression to match
   * @param firstFragments the list of first fragments to be searched
   * @return the matching first fragment, or null
   */
  public static ElExpressionFirstFragment findMatchingElExpressionFirstFragment(
      String elExpressionContents, Set<ElExpressionFirstFragment> firstFragments) {
    String firstFragmentText = getFirstFragment(elExpressionContents);

    for (ElExpressionFirstFragment firstFragment : firstFragments) {
      if (firstFragment.getValue().equals(firstFragmentText)) {
        return firstFragment;
      }
    }

    return null;
  }

  /**
   * Returns all but the first fragment.
   * 
   * @param fragments period-separate fragments
   * @return the second fragment onwards, period separated
   */
  public static String getAllButFirstFragment(String fragments) {
    int periodIndex = fragments.indexOf('.');
    return periodIndex == -1 ? "" : fragments.substring(periodIndex + 1);
  }

  /*
   * TODO: Ripped from FieldReferenceConverter in gwt-user. Post-GWT 2.0, we'll
   * refactor that class to fulfills my needs and bring it into gwt-dev-tools.
   */
  /**
   * Finds the EL expressions's contents in the given text.
   * 
   * @param in the text containing EL expressions.
   * @return a non-null list of regions of the contents of EL expressions
   */
  public static List<IRegion> getElExpressionRegions(String in) {
    final Pattern braces = Pattern.compile("[{]([^}]*)[}]");
    final Pattern legalFirstChar = Pattern.compile("^[$_a-zA-Z].*");

    final List<IRegion> list = new ArrayList<IRegion>();

    int nextFindStart = 0;

    Matcher m = braces.matcher(in);
    while (m.find(nextFindStart)) {
      String fieldReference = m.group(1);
      if (!legalFirstChar.matcher(fieldReference).matches()) {
        nextFindStart = m.start() + 2;
        continue;
      }

      list.add(new Region(m.start(1), m.end(1) - m.start(1)));

      nextFindStart = m.end();
    }

    return list;
  }

  /**
   * Returns the first fragment.
   * 
   * @param fragments the period-separated fragments
   * @return the first fragment, without the trailing period
   */
  public static String getFirstFragment(String fragments) {
    int periodIndex = fragments.indexOf('.');
    return periodIndex == -1 ? fragments : fragments.substring(0, periodIndex);
  }

  /**
   * Returns the offset to the contents of an open EL expression, or -1 if there
   * is not an EL expression in the text.
   * <p>
   * Note: If the EL expression contents contains an escaped '{' (e.g.
   * "{com.f{{oo.bar}"), this will return -1 since it is invalid.
   * 
   * @param text the text to search
   * @return the offset to the contents of the EL expression
   */
  public static int getOpenElExpressionContentsOffset(String text) {
    int lastBrace = text.lastIndexOf('{');
    if (lastBrace == -1) {
      return -1;
    }

    return (lastBrace == 0 || text.charAt(lastBrace - 1) != '{')
        ? lastBrace + 1 : -1;
  }

  /**
   * Gets the owner type for the given UiBinder subtype.
   * 
   * @return the owner type
   * @throws UiBinderException if the given UiBinder subtype is not enclosed by
   *           an owner type
   */
  public static IType getOwnerType(IType uiBinderSubtype)
      throws UiBinderException {
    IType ownerType = uiBinderSubtype.getDeclaringType();
    if (ownerType == null) {
      throw new UiBinderException(
          "The UiBinder subtype is not enclosed by an owner class.");
    }

    return ownerType;
  }

  /**
   * Gets the package name from a namespace URI (formatted with
   * {@link UiBinderConstants#URN_IMPORT_NAMESPACE_BEGINNING}). This does not
   * check for the existence of the package.
   * 
   * @param namespaceUrn the package formatted in the URN import scheme
   * @return the package name, or null if unavailable
   */
  public static String getPackageName(String namespaceUrn) {
    if (namespaceUrn == null
        || !namespaceUrn.startsWith(UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING)) {
      return null;
    }

    String packageName = namespaceUrn.substring(UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING.length());
    return Util.isValidPackageName(packageName) ? packageName : null;
  }

  /**
   * Uses the GWT UiBinder way of splitting a delimited string of paths into the
   * individual paths.
   * 
   * @param delimitedString the delimited string containing many paths
   * @return a list of strings of individual paths
   */
  public static List<String> getPathsFromDelimitedString(String delimitedString) {
    List<String> paths = new ArrayList<String>(
        Arrays.asList(delimitedString.split("[,\\s]+")));
    Iterator<String> it = paths.iterator();
    while (it.hasNext()) {
      String path = it.next();
      if (path == null || path.length() == 0) {
        it.remove();
      }
    }

    return paths;
  }

  /**
   * Given a ui.xml file, attempts to get the java files that reference the
   * ui.xml file.
   */
  public static Set<IType> getSubtypesFromXml(IFile uiXmlFile,
      IJavaProject javaProject) {
    IPath path;
    try {
      path = ClasspathResourceUtilities.getClasspathRelativePathOfResource(
          uiXmlFile, javaProject);
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
      return Collections.emptySet();
    }
    Set<IType> subtypes = UiBinderReferenceManager.INSTANCE.getSubtypeToUiXmlIndex().getUiBinderSubtypes(
        path);
    return subtypes;
  }

  /**
   * Gets the UiBinder interface type.
   * 
   * @return the UiBinder interface type, or null
   */
  public static IType getUiBinderType(IJavaProject javaProject) {
    try {
      return javaProject.findType(UiBinderConstants.UI_BINDER_INTERFACE_NAME);
    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not get the UiBinder type.");
      return null;
    }
  }

  /**
   * Returns the (possibly non-existant) path to the UiBinder XML file for the
   * given type.
   * 
   * @param uiBinderSubtype the UiBinder subtype whose corresponding XML file
   *          will be returned
   * @return the possibly non-existant path to the UiBinder XML file, or null
   * 
   * @throws UiBinderException
   */
  public static IPath getUiXmlPath(IType uiBinderSubtype)
      throws UiBinderException {
    IPath uiBinderPackagePath = uiBinderSubtype.getPackageFragment().getResource().getLocation();
    try {
      Object uiTemplateAnnotation = PlatformJavaModelUtilities.getAnnotation(
          UiBinderConstants.UI_TEMPLATE_ANNOTATION_NAME, uiBinderSubtype,
          uiBinderSubtype);
      if (uiTemplateAnnotation != null) {
        String relativePath = PlatformJavaModelUtilities.getSingleMemberAnnotationValue(
            uiTemplateAnnotation, String.class);
        return uiBinderPackagePath.append(relativePath);
      }
    } catch (IAnnotationNotAvailableException e) {
      // Ignore, fallback
    } catch (JavaModelException e) {
      throw new UiBinderException(e);
    }

    return uiBinderPackagePath.append(getOwnerType(uiBinderSubtype).getElementName()
        + UiBinderConstants.UI_BINDER_XML_EXTENSION);
  }

  /**
   * Gets the GWT Widget type.
   * 
   * @return the GWT Widget type
   */
  public static IType getWidgetType(IJavaProject javaProject)
      throws JavaModelException, UiBinderException {
    IType widgetType = javaProject.findType(
        UiBinderConstants.GWT_USER_LIBRARY_UI_PACKAGE_NAME,
        UiBinderConstants.GWT_USER_LIBRARY_WIDGET_CLASS_NAME);
    if (widgetType == null) {
      throw new UiBinderException(
          "Could not resolve the Widget type from the GWT user library.");
    }
    return widgetType;
  }

  public static boolean isUiField(FieldDeclaration fieldDecl) {
    if ((fieldDecl.getModifiers() & Modifier.STATIC) != 0) {
      return false;
    }
    return (JavaASTUtils.findAnnotation(fieldDecl,
        UiBinderConstants.UI_FIELD_ANNOTATION_NAME) != null);
  }

  public static boolean isUiHandler(MethodDeclaration method) {
    if ((method.getModifiers() & Modifier.STATIC) != 0) {
      return false;
    }
    if (method.isConstructor()) {
      return false;
    }
    return (JavaASTUtils.findAnnotation(method,
        UiBinderConstants.UI_HANDLER_TYPE_NAME) != null);
  }

  public static boolean isUiXml(IFile file) {
    // TODO: delegate to UiBinderXmlContentDescriber instead
    return file.getName().endsWith(UiBinderConstants.UI_BINDER_XML_EXTENSION);
  }

  /**
   * Resolves a snippet (assumed to be trailing a reference to the given
   * <code>baseType</code>) to the right-most possible type.
   * <p>
   * For example, a base type of String and a snippet of
   * "toString.getClass.getDeclaredField" will return the Field type.
   * 
   * @param contextType the type which will be used as the starting point for
   *          the snippet
   * @param snippet the string to trail onto a reference to the base type
   * @param visitor optional visitor that receives the corresponding
   *          {@link IMethod} for each fragment. This will be called even for
   *          methods whose return types do not resolve.
   * @return the resolved type, or null if type is a primitive or the type could
   *         not be resolved
   */
  public static IType resolveJavaElExpression(IType contextType,
      String snippet, ElExpressionFragmentVisitor visitor) {
    int offset = 0;
    String[] snippetFragments = snippet.split("[.]");
    IType currentType = contextType;

    for (int i = 0; i < snippetFragments.length; i++) {
      if (i > 0) {
        // Count the previous fragment
        offset += snippetFragments[i - 1].length();

        // Count the period
        offset++;
      }

      String fragment = snippetFragments[i];

      try {
        ITypeHierarchy hierarchy = currentType.newSupertypeHierarchy(NULL_PROGRESS_MONITOR);
        IMethod method = JavaModelSearch.findMethodInHierarchy(hierarchy,
            currentType, fragment, new String[0]);
        if (method != null) {
          if (visitor != null) {
            visitor.visitResolvedFragmentMethod(method, offset,
                fragment.length());
          }

          String returnTypeSignature = method.getReturnType();
          if (SignatureUtilities.isPrimitiveType(returnTypeSignature)) {
            if (i != snippetFragments.length - 1) {
              // It returns a primitive but isn't the last fragment
              if (visitor != null) {
                visitor.visitNonterminalPrimitiveFragment(fragment, offset,
                    snippet.length() - offset);
              }
            }

            return null;
          }

          IType fragmentType = JavaModelSearch.resolveType(
              method.getDeclaringType(),
              returnTypeSignature);
          if (JavaModelSearch.isValidElement(fragmentType)) {
            currentType = fragmentType;
            continue;
          }
        }
      } catch (JavaModelException e) {
        // Ignore, and continue the search
      }

      if (visitor != null) {
        visitor.visitUnresolvedFragment(fragment, offset, fragment.length(),
            currentType);
      }
      return null;
    }

    return currentType;
  }

  private UiBinderUtilities() {
  }

}
