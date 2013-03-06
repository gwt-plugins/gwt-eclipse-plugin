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
package com.google.gwt.eclipse.core.validators.java;

import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.markers.GWTProblemType;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed Java reference from within a JSNI method. Extends the GWT class
 * {@link com.google.gwt.dev.util.JsniRef} and adds information about the offset
 * within its containing Java source file.
 */
public class JsniJavaRef extends JsniRef {

  public static String CTOR_NAME = "new";

  public static JsniJavaRef findEnclosingJavaRef(ITextSelection sel,
      ITextSelection jsniBlock, IDocument document) {

    // TODO: once we cache Java references in JSNI blocks (for Java search and
    // refactoring integration) we can just query that to figure out if a Java
    // reference contains the current selection, so this entire method will
    // disappear

    // TODO: like JsniScanner, this method doesn't correctly find the boundary
    // of certain field references like:
    // @com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem.focus();
    // However, because this is all going to be deprecated soon, we're not going
    // to spend time to track down this bug and fix it here

    try {
      // Scan to the left until we hit the beginning of the Java reference
      int jsniStart = jsniBlock.getOffset();
      int start = sel.getOffset();
      while (true) {
        // If we hit the beginning of the JSNI block, we must not be inside a
        // valid Java reference
        if (start <= jsniStart) {
          return null;
        }

        // We hit the start of the Java reference
        if (document.getChar(start) == '@') {
          break;
        }

        start--;
      }

      int jsniEnd = jsniBlock.getOffset() + jsniBlock.getLength();

      // Start one before the actual end of the selection, so we can include
      // the last character in the selection in the scan
      int end = sel.getOffset() + sel.getLength() - 1;

      boolean enteredParamTypes = false;
      while (true) {
        // If we hit the end of the JSNI block, we must not be inside a
        // valid Java reference
        if (end >= jsniEnd) {
          return null;
        }

        char ch = document.getChar(end++);

        if (!enteredParamTypes) {
          // Skip the @ at the beginning of the Java reference
          if (ch == '@') {
            continue;
          }

          // If we're in the class/member identifier, allowable characters
          // include any Java identifier parts, '.' for separating package
          // segments, and '$' for separating and inner and outer classes
          if (Character.isJavaIdentifierPart(ch) || ch == '.' || ch == '$') {
            continue;
          }

          // Skip over the :: that separates the class from the member
          if (ch == ':') {
            continue;
          }

          // If we hit the '(' we're at the end of the member name and at the
          // start of the parameter types list
          if (ch == '(') {
            enteredParamTypes = true;
            continue;
          }

          if (ch == ')') {
            /*
             * If we hit a ')', we're either at the end of a method or
             * constructor parameter list (this is the case if the user manually
             * selected the entire Java reference before hitting F3), or we're
             * AFTER the end of a field reference, and the next character just
             * happens to be a ')'. We search the Java ref string for a '(' to
             * see which case this is, and if it's the latter we'll need to back
             * up one space, so we just fall through and break after this 'if'
             * block.
             */
            String javaRefSoFar = document.get(start, end - start);
            if (javaRefSoFar.indexOf('(') > -1) {
              break;
            }
          }

          // Anything else means we're at the end of a reference
          end--;
          break;

        } else {
          // Inside the parameter types list, skip everything until we get
          // to the closing ')'

          if (ch == ')') {
            break;
          }
        }
      }

      return parse(document.get(start, end - start));

    } catch (BadLocationException e) {
      return null;
    }
  }

  public static JsniJavaRef parse(String refString) {
    JsniRef ref = JsniRef.parse(refString);
    if (ref == null) {
      return null;
    }
    return new JsniJavaRef(ref);
  }

  private int offset = 0;

  private IPath source = null;

  public JsniJavaRef(JsniJavaRef ref) {
    this((JsniRef) ref);
    this.source = ref.source;
    this.offset = ref.offset;
  }

  private JsniJavaRef(JsniRef ref) {
    super(ref.className(), ref.memberName(), ref.paramTypesString(),
        (ref.isMethod() && !ref.matchesAnyOverload()) ? ref.paramTypes() : null);
  }

  public String dottedClassName() {
    return className().replace('$', '.');
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof JsniJavaRef) && toString().equals(obj.toString());
  }

  public int getClassOffset() {
    // Add 1 for '@'
    return offset + 1;
  }

  public int getMemberOffset() {
    // Add 2 for '::'
    return getClassOffset() + className().length() + 2;
  }

  public int getOffset() {
    return offset;
  }

  public int getParamTypesOffset() {
    if (paramTypesString() == null) {
      return -1;
    }
    // Add 1 for '('
    return getMemberOffset() + memberName().length() + 1;
  }

  public IPath getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public boolean isConstructor() {
    return (isMethod() && CTOR_NAME.equals(memberName()));
  }

  public String readableMemberSignature() {
    String sig = memberName();

    if (isMethod()) {
      if (isConstructor()) {
        sig = dottedClassName();
      }

      List<String> readableParamTypes = new ArrayList<String>();
      if (matchesAnyOverload()) {
        readableParamTypes.add("*");
      } else {
        for (String paramType : paramTypes()) {
          paramType = paramType.replace('/', '.');
          paramType = Signature.toString(paramType);

          readableParamTypes.add(paramType);
        }
      }

      sig += ("(" + Util.join(readableParamTypes, ", ") + ")");
    }

    return sig;
  }

  public IJavaElement resolveJavaElement(IJavaProject project)
      throws UnresolvedJsniJavaRefException {
    IJavaElement element = null;

    // 0. Ignore the magic null reference
    if (className().equals("null")) {
      throw new UnresolvedJsniJavaRefException(null, this);
    }

    // 1. Try to find the type in the project's classpath
    IType type = JavaModelSearch.findType(project, dottedClassName());
    if (type == null) {
      throw new UnresolvedJsniJavaRefException(
          GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE, this);
    }

    // TODO: remove this check once we can validate against super source

    // 1A. Do not validate JRE types (they could contain references to members
    // which are only defined in the emulated versions in super source)
    if (type.isBinary()) {
      IPackageFragmentRoot pkgRoot = (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
      try {
        IClasspathEntry cpEntry = pkgRoot.getRawClasspathEntry();
        if (cpEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
            && cpEntry.getPath().segment(0).equals(JavaRuntime.JRE_CONTAINER)) {
          throw new UnresolvedJsniJavaRefException(null, this);
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
        throw new UnresolvedJsniJavaRefException(null, this);
      }
    }

    // 2. Create a super-type hierarchy for the type, which we'll use for
    // finding its super classes and implemented interfaces
    ITypeHierarchy hierarchy;
    try {
      hierarchy = type.newSupertypeHierarchy(null);
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e, "Error creating type hierarchy for "
          + className());
      throw new UnresolvedJsniJavaRefException(
          GWTProblemType.JSNI_JAVA_REF_UNRESOLVED_TYPE, this);
    }

    if (isMethod()) {
      if (isConstructor()) {
        if (matchesAnyOverload()) {
          element = JavaModelSearch.findFirstCtor(type);
        } else {
          String[] ctorParamTypes = paramTypes();
          try {
            /*
             * If the constructor is on a non-static inner class, the reference
             * will have an extra parameter that specifies the enclosing type.
             * We'll need to check that type and then remove the parameter
             * before using JavaModelSearch to find the constructor.
             */
            IJavaElement typeParent = type.getParent();
            if (typeParent.getElementType() == IJavaElement.TYPE
                && !Flags.isStatic(type.getFlags())) {

              // Make sure we do have the enclosing type as the first parameter
              if (ctorParamTypes.length == 0) {
                throw new UnresolvedJsniJavaRefException(
                    GWTProblemType.JSNI_JAVA_REF_NO_MATCHING_CTOR, this);
              }

              // Now verify that the type of the first parameter is actually the
              // the same type as the enclosing type
              IType parentType = (IType) typeParent;
              String enclosingTypeName = parentType.getFullyQualifiedName('.');
              String enclosingTypeParam = JavaModelSearch.getQualifiedTypeName(ctorParamTypes[0]);
              if (!enclosingTypeName.equals(enclosingTypeParam)) {
                throw new UnresolvedJsniJavaRefException(
                    GWTProblemType.JSNI_JAVA_REF_NO_MATCHING_CTOR, this);
              }

              // Drop the first parameter (the enclosing type)
              ctorParamTypes = new String[paramTypes().length - 1];
              System.arraycopy(paramTypes(), 1, ctorParamTypes, 0,
                  ctorParamTypes.length);
            }
          } catch (JavaModelException e) {
            GWTPluginLog.logError(e);
            // Continue on and try to resolve the reference anyway
          }

          // 3A. Find a constructor for this type with matching parameter types
          element = JavaModelSearch.findCtorInHierarchy(hierarchy, type,
              ctorParamTypes);
          if (element == null) {
            throw new UnresolvedJsniJavaRefException(
                GWTProblemType.JSNI_JAVA_REF_NO_MATCHING_CTOR, this);
          }
        }
      } else {
        // 3B. Find a method for this type with the same name
        element = JavaModelSearch.findMethodInHierarchy(hierarchy, type,
            memberName());
        if (element == null) {
          try {
            if (type.isEnum()) {
              // Ignore the magic Enum::values() method
              if (memberName().equals("values") && !matchesAnyOverload()
                  && paramTypes().length == 0) {
                // Throwing this exception with a null GWTProblemType indicates
                // that the ref doesn't resolve, but can be ignored anyway.
                throw new UnresolvedJsniJavaRefException(null, this);
              }
            }
          } catch (JavaModelException e) {
            GWTPluginLog.logError(e);
          }

          throw new UnresolvedJsniJavaRefException(
              GWTProblemType.JSNI_JAVA_REF_MISSING_METHOD, this);
        }

        if (!matchesAnyOverload()) {
          // Now try to match the method's parameter types
          element = JavaModelSearch.findMethodInHierarchy(hierarchy, type,
              memberName(), paramTypes());
          if (element == null) {
            try {
              if (type.isEnum()) {
                // Ignore the synthetic Enum::valueOf(String) method.
                // Note that valueOf(Class,String) is not synthetic.
                if (memberName().equals("valueOf") && paramTypes().length == 1
                    && paramTypes()[0].equals("Ljava/lang/String;")) {
                  // Throwing this exception with a null GWTProblemType
                  // indicates that the ref doesn't resolve, but can be ignored
                  // anyway.
                  throw new UnresolvedJsniJavaRefException(null, this);
                }
              }
            } catch (JavaModelException e) {
              GWTPluginLog.logError(e);
            }

            throw new UnresolvedJsniJavaRefException(
                GWTProblemType.JSNI_JAVA_REF_NO_MATCHING_METHOD, this);
          }
        }
      }

    } else {
      // 3C. Find a field with the same name
      assert (isField());
      element = JavaModelSearch.findFieldInHierarchy(hierarchy, type,
          memberName());
      if (element == null) {
        throw new UnresolvedJsniJavaRefException(
            GWTProblemType.JSNI_JAVA_REF_MISSING_FIELD, this);
      }
    }

    assert (element != null);
    return element;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void setSource(IPath source) {
    this.source = source;
  }

  public String simpleClassName() {
    String[] classParts = dottedClassName().split("\\.");
    return classParts[classParts.length - 1];
  }
}
