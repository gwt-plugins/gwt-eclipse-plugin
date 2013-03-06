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
package com.google.gwt.eclipse.core.uibinder.contentassist;

import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.XmlUtilities.NodeVisitor;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderXmlModelUtilities;
import com.google.gwt.eclipse.core.uibinder.problems.UiBinderProblemMarkerManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes the valid values for the first fragment of a EL expression (e.g.
 * {___.foo.bar}).
 */
@SuppressWarnings("restriction")
public class ElExpressionFirstFragmentComputer {

  /**
   * Container for an EL expression first fragment and the node it references.
   */
  public static class ElExpressionFirstFragment {
    private final Node node;
    private final String value;

    public ElExpressionFirstFragment(String value, Node node) {
      this.value = value;
      this.node = node;
    }

    /**
     * Gets the node that defines this first fragment value.
     */
    public Node getNode() {
      return node;
    }

    /**
     * Gets the value of this first fragment.
     */
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private static enum ImportResult {
    ERROR, FIELD_NOT_STATIC, FIELD_NOT_VISIBLE, FIELD_UNDEFINED, IMPORT_MISSING_FIELD_ATTR, TYPE_NOT_VISIBLE, TYPE_UNDEFINED, YES
  }

  /**
   * Computes possible first fragments for EL expressions.
   * 
   * @param document The document to compute the first fragments from
   * @param xmlFile The xml file the document came from. This is used to
   *          determine the java files that references this ui.xml file so that
   *          "package" of this ui.xml file can be determined for importing
   *          protected or package default fields. May be null, but protected /
   *          package default fields can't be checked for visibility.
   * @param javaProject The java project the document belongs to.
   * @param problemMarkerManager The problem marker manage to mark problems as
   *          the fragments are identified. May be null.
   * @return An ElExpressionFirstFragmentComputer that contains first fragments
   *         and duplicate first fragments.
   */
  public static ElExpressionFirstFragmentComputer compute(Document document,
      final IFile xmlFile, final IJavaProject javaProject,
      final UiBinderProblemMarkerManager problemMarkerManager) {
    final Set<ElExpressionFirstFragment> fragments = new HashSet<ElExpressionFirstFragment>();
    final Set<ElExpressionFirstFragment> dupFragments = new HashSet<ElExpressionFirstFragment>();

    XmlUtilities.visitNodes(document, new NodeVisitor() {
      
      public boolean visitNode(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          return true;
        }

        List<ElExpressionFirstFragment> currentFirstFragments = new ArrayList<ElExpressionFirstFragment>();

        if (!UiBinderXmlModelUtilities.isImportElement(node)) {
          ElExpressionFirstFragment elExpressionFirstFragment = createFirstFragmentWithFieldAttribute(node);
          if (elExpressionFirstFragment == null) {
            if (UiBinderXmlModelUtilities.isStyleElement(node)) {
              elExpressionFirstFragment = new ElExpressionFirstFragment(
                  UiBinderConstants.UI_BINDER_STYLE_ELEMENT_IMPLICIT_FIELD_VALUE,
                  node);
            }
          }

          if (elExpressionFirstFragment != null) {
            currentFirstFragments.add(elExpressionFirstFragment);
          }
        } else {
          List<ElExpressionFirstFragment> imports = getFirstFragmentsFromImport(node);
          currentFirstFragments.addAll(imports);
        }

        for (ElExpressionFirstFragment elExpressionFirstFragment : currentFirstFragments) {
          ElExpressionFirstFragment fragment = findFragment(elExpressionFirstFragment.getValue());
          if (fragment != null) {
            /**
             * The original is placed in fragments and dupFragments to reduce
             * popping up misleading problems on {style.blah} where it would
             * complain the fragment blah is not found.
             */
            dupFragments.add(fragment);
            dupFragments.add(elExpressionFirstFragment);
          } else {
            fragments.add(elExpressionFirstFragment);
          }
        }

        return true;
      }

      private ImportResult canImportField(IField field) {

        try {
          int flags = field.getFlags();

          if (!Flags.isStatic(flags)) {

            // if it's an enum, then it doesn't have any of these modifiers, but
            // can still be imported
            if (Flags.isEnum(flags)) {
              return ImportResult.YES;
            } else {
              // must be static
              return ImportResult.FIELD_NOT_STATIC;
            }

          } else if (Flags.isPrivate(flags)) {

            return ImportResult.FIELD_NOT_VISIBLE;

          } else if (Flags.isProtected(flags) || Flags.isPackageDefault(flags)) {
            // if it's protected / package default, packages must be the same
            IPackageDeclaration[] decs = field.getCompilationUnit().getPackageDeclarations();
            if (decs.length > 0 && xmlFile != null) {
              String enclosingTypeDec = decs[0].getElementName();
              Set<IType> ownerTypes = UiBinderUtilities.getSubtypesFromXml(
                  xmlFile, javaProject);
              for (IType type : ownerTypes) {
                if (!enclosingTypeDec.equals(type.getPackageFragment().getElementName())) {
                  return ImportResult.FIELD_NOT_VISIBLE;
                }
              }

            } else {
              // default package
              return ImportResult.TYPE_NOT_VISIBLE;
            }
          }
        } catch (JavaModelException e) {
          return ImportResult.ERROR;
        }

        return ImportResult.YES;
      }

      /**
       * Checks if t1 is accessible from t2 for the purposes of UiBinder. In the
       * case of nested inner classes, t1 and all of its enclosing classes are
       * recursively checked for accessibility from t2, and the first
       * inaccessible type is returned.
       * 
       * @param t1 The target type to check for accessibility
       * @param t2 The source type to check for accessibility from
       * @return null if t1 is accessible from t2, or the first type found that
       *         is inaccessible
       */
      private IType computeFirstInaccessibleType(IType t1, IType t2) {

        int flags;
        try {
          flags = t1.getFlags();
        } catch (JavaModelException e) {
          return t1;
        }

        if (Flags.isPrivate(flags)) {
          return t1;
        } else if (Flags.isProtected(flags) || Flags.isPackageDefault(flags)) {
          // check if the type's packages are the same if t1 is protected or
          // package default.
          // don't need to worry about inheritance with the protected case
          // because UiBinder subtypes
          // can't inherit from arbitrary classes.
          
          boolean samePackage = t1.getPackageFragment().getElementName().equals(
              t2.getPackageFragment().getElementName());

          if (!samePackage) {
            return t1;
          }
        }

        // don't need to check public because we're going to recurse up into
        // possible container classes until we hit null, which means we haven't
        // any problems (ie, public is never a problem)

        if (t1.getParent() instanceof IType) {
          return computeFirstInaccessibleType((IType) t1.getParent(), t2);
        } else {
          return null;
        }
      }

      private ElExpressionFirstFragment createFirstFragmentWithFieldAttribute(
          Node node) {

        Node fieldAttr = UiBinderXmlModelUtilities.getFieldAttribute(node);
        if (fieldAttr == null || fieldAttr.getNodeValue() == null) {
          return null;
        }

        ElExpressionFirstFragment elExpressionFirstFragment = new ElExpressionFirstFragment(
            fieldAttr.getNodeValue(), node);
        return elExpressionFirstFragment;
      }

      private ElExpressionFirstFragment findFragment(String fragmentStr) {
        for (ElExpressionFirstFragment curFragment : fragments) {
          if (curFragment.getValue().equals(fragmentStr)) {
            return curFragment;
          }
        }

        return null;
      }

      private List<ElExpressionFirstFragment> getFirstFragmentsFromImport(
          Node node) {

        Node importFieldNode = UiBinderXmlModelUtilities.getFieldAttribute(node);
        if (importFieldNode == null) {
          markProblem(node, ImportResult.IMPORT_MISSING_FIELD_ATTR);
          return Collections.emptyList();
        }

        String importFieldValue = importFieldNode.getNodeValue();
        String lastFragment = importFieldValue.substring(importFieldValue.lastIndexOf('.') + 1);

        // didn't enter anything!
        if (lastFragment.length() == 0) {
          markProblem(importFieldNode, ImportResult.TYPE_UNDEFINED);
          return Collections.emptyList();
        }

        // the class from which we're importing fields
        IType enclosingType = UiBinderXmlModelUtilities.resolveElementToJavaType(
            (IDOMElement) node, javaProject);

        // make sure the enclosing type exists
        if (enclosingType == null) {
          markProblem(importFieldNode, ImportResult.TYPE_UNDEFINED);
          return Collections.emptyList();
        }

        if (xmlFile != null) {
          boolean problemFound = false;
          // make sure the subtype can access the enclosing type
          Set<IType> subtypes = UiBinderUtilities.getSubtypesFromXml(xmlFile,
              javaProject);
          for (IType type : subtypes) {
            IType inaccessibleType = computeFirstInaccessibleType(enclosingType, type);
            if (inaccessibleType != null) {
              markProblem(importFieldNode, ImportResult.TYPE_NOT_VISIBLE,
                  inaccessibleType.getFullyQualifiedName('.'));
              problemFound = true;
            }
          }
          
          if (problemFound) {
            return Collections.emptyList();
          }
        }

        List<ElExpressionFirstFragment> fragments = new ArrayList<ElExpressionFirstFragment>();

        IField[] fields;
        try {
          fields = enclosingType.getFields();
        } catch (JavaModelException e) {
          GWTPluginLog.logError(e);
          return Collections.emptyList();
        }

        // import a glob, import everything we can from the type
        if ("*".equals(lastFragment)) {
          for (IField field : fields) {
            if (canImportField(field) == ImportResult.YES) {
              ElExpressionFirstFragment elExpressionFirstFragment = new ElExpressionFirstFragment(
                  field.getElementName(), node);
              fragments.add(elExpressionFirstFragment);
            }
          }

        } else { // import a single field

          IField field = JavaModelSearch.findField(enclosingType, lastFragment);
          if (field != null) {

            ImportResult ir;
            if ((ir = canImportField(field)) == ImportResult.YES) {

              ElExpressionFirstFragment elExpressionFirstFragment = new ElExpressionFirstFragment(
                  lastFragment, node);
              fragments.add(elExpressionFirstFragment);

            } else {
              markProblem(importFieldNode, ir);
            }
          } else {
            markProblem(importFieldNode, ImportResult.FIELD_UNDEFINED);
          }
        }
        return fragments;
      }

      private void markProblem(Node node, ImportResult ir) {
        markProblem(node, ir, null);
      }

      private void markProblem(Node node, ImportResult ir, String text) {
        if (problemMarkerManager != null) {

          if (text == null) {
            text = node.getNodeValue();
          }

          IRegion region;
          if (node instanceof IDOMAttr) {
            region = XmlUtilities.getAttributeValueRegion((IDOMAttr) node);
          } else if (node instanceof IDOMElement) {
            int start = ((IDOMElement) node).getStartOffset();
            int end = ((IDOMElement) node).getStartEndOffset();
            region = new Region(start, end - start);
          } else {
            return;
          }

          switch (ir) {
            case FIELD_NOT_STATIC:
              problemMarkerManager.setFieldNotStaticError(region, text);
              break;
            case FIELD_NOT_VISIBLE:
              problemMarkerManager.setFieldNotVisibleError(region, text);
              break;
            case FIELD_UNDEFINED:
              problemMarkerManager.setFieldUndefinedError(region, text);
              break;
            case TYPE_NOT_VISIBLE:
              problemMarkerManager.setTypeNotVisibleError(region, text);
              break;
            case TYPE_UNDEFINED:
              problemMarkerManager.setTypeUndefinedError(region, text);
              break;
            case IMPORT_MISSING_FIELD_ATTR:
              problemMarkerManager.setImportMissingFieldAttr(region, text);
              break;
            default:
              GWTPluginLog.logWarning("Unknown UiBinder XML problem type: " + ir.toString());
              break;
          }
        }
      }
    });

    return new ElExpressionFirstFragmentComputer(fragments, dupFragments);
  }

  private final Set<ElExpressionFirstFragment> duplicateFirstFragments;
  private final Set<ElExpressionFirstFragment> firstFragments;

  private ElExpressionFirstFragmentComputer(
      Set<ElExpressionFirstFragment> firstFragments,
      Set<ElExpressionFirstFragment> duplicateFirstFragments) {
    this.firstFragments = firstFragments;
    this.duplicateFirstFragments = duplicateFirstFragments;
  }

  public Set<ElExpressionFirstFragment> getDuplicateFirstFragments() {
    return duplicateFirstFragments;
  }

  public Set<ElExpressionFirstFragment> getFirstFragments() {
    return firstFragments;
  }

}
