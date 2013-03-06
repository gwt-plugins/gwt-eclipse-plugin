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
package com.google.gwt.eclipse.core.uibinder.model.reference;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.XmlUtilities.NodeVisitor;
import com.google.gdt.eclipse.core.java.ClasspathResourceUtilities;
import com.google.gdt.eclipse.core.java.JavaModelSearch;
import com.google.gdt.eclipse.core.reference.Reference;
import com.google.gdt.eclipse.core.reference.ReferenceManager;
import com.google.gdt.eclipse.core.reference.location.ClasspathRelativeFileReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.IReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.LogicalJavaElementReferenceLocation;
import com.google.gdt.eclipse.core.reference.location.ReferenceLocationType;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.LogicalPackage;
import com.google.gdt.eclipse.core.reference.logicaljavamodel.UiBinderImportReferenceType;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.clientbundle.ClientBundleUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderConstants;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities.ElExpressionFragmentVisitor;
import com.google.gwt.eclipse.core.uibinder.UiBinderXmlModelUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.ElExpressionFirstFragmentComputer;
import com.google.gwt.eclipse.core.uibinder.contentassist.ElExpressionFirstFragmentComputer.ElExpressionFirstFragment;
import com.google.gwt.eclipse.core.uibinder.contentassist.computers.CssSelectorNameCollector;
import com.google.gwt.eclipse.core.uibinder.problems.IValidationResultPlacementStrategy;
import com.google.gwt.eclipse.core.uibinder.problems.UiBinderProblemMarkerManager;
import com.google.gwt.eclipse.core.uibinder.sse.css.CssExtractor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Node;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds references and validates UiBinder XML templates.
 * <p>
 * Do not re-use this parser for more than one parse.
 */
@SuppressWarnings("restriction")
public class UiBinderXmlParser {

  /**
   * The results from an UiBinder XML parse.
   */
  public static class ParseResults {
    private final Set<String> fieldNames;
    private final Set<String> javaTypeReferences;

    public ParseResults(Set<String> fieldNames, Set<String> javaTypeReferences) {
      this.fieldNames = fieldNames;
      this.javaTypeReferences = javaTypeReferences;
    }

    public Set<String> getFieldNames() {
      return fieldNames;
    }

    public Set<String> getJavaTypeReferences() {
      return javaTypeReferences;
    }
  }

  /**
   * These are synthetic elements that belong to deprecated panels. Without our
   * hardcoding, they would throw off validation (the validator would think they
   * are widgets since they start with a capital letter.)
   */
  private static final Set<String> IGNORED_CAPITALIZED_SYNTHETIC_ELEMENTS = new HashSet<String>(
      Arrays.asList(new String[] {
          "com.google.gwt.user.client.ui.Dock",
          "com.google.gwt.user.client.ui.Cell",
          "com.google.gwt.user.client.ui.Tab",
          "com.google.gwt.user.client.ui.TabHTML",
          "com.google.gwt.user.client.ui.MenuItemHTML"}));

  /**
   * Returns a new instance of the parser.
   * 
   * @param xmlModel the XML model to be parsed
   * @param referenceManager an optional reference manager that receives the
   *          parsed references types and XML files
   * @return an instance of the parser
   * @throws FileNotFoundException if a file backing the model could not be
   *           found
   * @throws UiBinderException if the file does not live in a java project
   */
  public static UiBinderXmlParser newInstance(IDOMModel xmlModel,
      ReferenceManager referenceManager,
      IValidationResultPlacementStrategy<?> validationResultPlacementStrategy)
      throws FileNotFoundException, UiBinderException {
    // TODO: turn this method into a static parse method, because the usage
    // pattern
    // for this class is always UiBinderXmlParser.newInstance().parse().

    IFile xmlFile = SseUtilities.resolveFile(xmlModel);
    if (xmlFile == null) {
      throw new FileNotFoundException(
          "Could not find the file backing the XML model.");
    }

    IJavaProject javaProject = JavaCore.create(xmlFile.getProject());
    if (!javaProject.exists()) {
      throw new UiBinderException(
          "The UiBinder XML file is not part of a Java project.");
    }

    IPath xmlClasspathRelativePath;
    try {
      xmlClasspathRelativePath = ClasspathResourceUtilities.getClasspathRelativePathOfResource(
          xmlFile, javaProject);
      if (xmlClasspathRelativePath == null) {
        throw new UiBinderException(
            "Could not resolve classpath-relative path to UiBinder XML template file.");
      }
    } catch (JavaModelException e) {
      throw new UiBinderException(e);
    }

    return new UiBinderXmlParser(xmlModel, xmlFile, xmlClasspathRelativePath,
        referenceManager, validationResultPlacementStrategy, javaProject);
  }

  private final IPath classpathRelativeDir;

  private ElExpressionFirstFragmentComputer firstFragmentComputer;
  private final IJavaProject javaProject;
  /**
   * Fully qualified Java types that this ui.xml refers to.
   */
  private final Set<String> javaTypeReferences = new HashSet<String>();
  private final NodeVisitor parseNodesVisitor = new NodeVisitor() {
    public boolean visitNode(Node node) {
      parse((IDOMNode) node);
      return true;
    }
  };
  private final UiBinderProblemMarkerManager problemMarkerManager;
  /**
   * Receives references. This can be null if the client does not care to have
   * references tracked.
   */
  private final ReferenceManager referenceManager;
  private final IFile xmlFile;
  private final IDOMModel xmlModel;

  private final ClasspathRelativeFileReferenceLocation xmlReferenceLocation;

  private UiBinderXmlParser(IDOMModel xmlModel, IFile xmlFile,
      IPath xmlClasspathRelativePath, ReferenceManager referenceManager,
      IValidationResultPlacementStrategy<?> validationResultPlacementStrategy,
      IJavaProject javaProject) {
    this.referenceManager = referenceManager;
    this.xmlModel = xmlModel;
    this.xmlFile = xmlFile;
    this.javaProject = javaProject;

    xmlReferenceLocation = new ClasspathRelativeFileReferenceLocation(
        xmlClasspathRelativePath);
    classpathRelativeDir = xmlClasspathRelativePath.removeLastSegments(1);
    problemMarkerManager = new UiBinderProblemMarkerManager(xmlFile,
        xmlModel.getStructuredDocument(), validationResultPlacementStrategy);
  }

  public ParseResults parse() {
    if (!UiBinderConstants.UI_BINDER_ENABLED) {
      return null;
    }

    // We'll regenerate the references and problems sourced on this resource, so
    // clear out the old ones
    if (referenceManager != null) {
      referenceManager.removeReferences(referenceManager.getReferencesWithMatchingResource(
          xmlFile, EnumSet.of(ReferenceLocationType.SOURCE)));
    }
    problemMarkerManager.clear();

    // the ElExpressionFirstFragmentComputer marks problems as it finds first
    // fragments,
    // so this must be placed after problemMarkerManager.clear() so that its
    // markers
    // don't get cleared.
    firstFragmentComputer = ElExpressionFirstFragmentComputer.compute(
        xmlModel.getDocument(), xmlFile, javaProject, problemMarkerManager);

    // Add self-reference so when the user saves the ui.xml file, this parser
    // gets called
    addReference(xmlReferenceLocation, xmlReferenceLocation);

    XmlUtilities.visitNodes(xmlModel.getDocument().getDocumentElement(),
        parseNodesVisitor);

    markDuplicateFieldErrors();

    return new ParseResults(getFieldNames(), javaTypeReferences);
  }

  private void addReference(IReferenceLocation sourceLocation,
      IReferenceLocation targetLocation) {
    if (referenceManager != null) {
      referenceManager.addReference(new Reference(sourceLocation,
          targetLocation, javaProject.getProject()));
    }
  }

  private IFile getExistingFile(IPath classpathRelativePath) {
    try {
      IFile resFile = (IFile) ClasspathResourceUtilities.resolveFile(
          classpathRelativePath, javaProject);
      return resFile != null && resFile.exists() ? resFile : null;

    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not resolve file at "
          + classpathRelativePath);
      return null;
    }
  }

  private Set<String> getFieldNames() {
    Set<String> fieldNames = new HashSet<String>();
    for (ElExpressionFirstFragment fragment : firstFragmentComputer.getFirstFragments()) {
      fieldNames.add(fragment.getValue());
    }

    return fieldNames;
  }

  private void markDuplicateFieldErrors() {
    for (ElExpressionFirstFragment fragment : firstFragmentComputer.getDuplicateFirstFragments()) {
      IRegion region;
      IDOMAttr fieldAttribute = (IDOMAttr) UiBinderXmlModelUtilities.getFieldAttribute(fragment.getNode());
      if (fieldAttribute != null) {
        region = XmlUtilities.getAttributeValueRegion(fieldAttribute);
      } else {
        List<IRegion> tagRegions = XmlUtilities.getElementTagRegions(
            (IDOMElement) fragment.getNode(), true);
        region = tagRegions.size() > 0 ? tagRegions.get(0) : null;
      }

      if (region != null) {
        problemMarkerManager.setDuplicateFieldError(region, fragment.getValue());
      }
    }
  }

  private void parse(IDOMNode node) {
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE:
        parseElement((IDOMElement) node);
        break;

      case Node.ATTRIBUTE_NODE:
        parseAttribute((IDOMAttr) node);
        break;
    }
  }

  private void parseAttribute(IDOMAttr attr) {
    tryParseElExpression(attr);
    tryParseUrnImport(attr);
  }

  private void parseElement(IDOMElement element) {
    tryValidatePrefix(element);
    tryParseWidgetFromElement(element);
    tryParseWithElement(element);
    tryParseStyleElement(element);
    tryParseResourceElement(element);
    tryParseUiImportElement(element);
  }

  private void setFieldReferenceFirstFragmentUndefinedError(
      IRegion attrValueRegion, IRegion exprContentRegion, String exprContents) {
    String firstFragment = UiBinderUtilities.getFirstFragment(exprContents);
    int start = attrValueRegion.getOffset() + exprContentRegion.getOffset();
    problemMarkerManager.setFirstFragmentUndefinedError(new Region(start,
        firstFragment.length()), firstFragment);
  }

  private void tryParseCssElExpression(IDOMElement styleElement,
      String remainingFragments, int remainingFragmentsOffsetInDoc,
      IRegion fieldRefRegion) {
    CssExtractor extractor = UiBinderXmlModelUtilities.createCssExtractorForStyleElement(
        styleElement, javaProject);
    if (extractor != null) {
      for (String selector : CssSelectorNameCollector.getValidSelectorNames(extractor.getCssDocument())) {
        if (selector.equals(remainingFragments)) {
          return;
        }
      }
    }

    if (remainingFragments.length() > 0) {
      problemMarkerManager.setCssSelectorFragmentUndefinedError(new Region(
          remainingFragmentsOffsetInDoc, remainingFragments.length()),
          remainingFragments);
    } else {
      problemMarkerManager.setCssSelectorFragmentUnspecifiedError(fieldRefRegion);
    }
  }

  /**
   * Parses a field reference within an attribute.
   */
  private void tryParseElExpression(final IDOMAttr attr) {
    final IRegion attrValueRegion = XmlUtilities.getAttributeValueRegion(attr);
    String attrValue = attr.getNodeValue();
    if (attrValueRegion == null || attrValue == null) {
      return;
    }

    List<IRegion> exprContentRegions = UiBinderUtilities.getElExpressionRegions(attrValue);
    for (final IRegion exprContentRegion : exprContentRegions) {
      String exprContents = attrValue.substring(exprContentRegion.getOffset(),
          exprContentRegion.getOffset() + exprContentRegion.getLength());

      ElExpressionFirstFragment firstFragment = UiBinderUtilities.findMatchingElExpressionFirstFragment(
          exprContents, firstFragmentComputer.getFirstFragments());
      if (firstFragment == null) {
        // The entered first fragment does not match any defined field
        setFieldReferenceFirstFragmentUndefinedError(attrValueRegion,
            exprContentRegion, exprContents);
        continue;
      }

      // Try to parse the remaining fragments
      String allButFirstFragment = UiBinderUtilities.getAllButFirstFragment(exprContents);
      int allButFirstFragmentOffsetInDoc = attrValueRegion.getOffset()
          + exprContentRegion.getOffset()
          + (exprContents.length() - allButFirstFragment.length());

      /*
       * A <ui:style> can have a inline CSS, external stylesheet, and a type
       * attribute. The members of the CssResource pointed to by the type
       * attribute are a subset of the selectors defined by the inline CSS and
       * external stylesheet. Therefore, we should use the CssResource subtype
       * which provides all valid selectors.
       */
      IType fieldType = UiBinderXmlModelUtilities.resolveElementToJavaType(
          (IDOMElement) firstFragment.getNode(), javaProject);
      if (UiBinderXmlModelUtilities.isStyleElement(firstFragment.getNode())) {
        Region fieldRefRegion = new Region(exprContentRegion.getOffset()
            + attrValueRegion.getOffset(), exprContentRegion.getLength());
        tryParseCssElExpression((IDOMElement) firstFragment.getNode(),
            allButFirstFragment, allButFirstFragmentOffsetInDoc, fieldRefRegion);
      } else if (fieldType != null) {
        // It is, so parse the remaining fragments as java
        tryParseJavaElExpression(fieldType, allButFirstFragment,
            allButFirstFragmentOffsetInDoc);
      }
    }
  }

  /**
   * Parses field references referring to java types.
   */
  private void tryParseJavaElExpression(IType elementType,
      String allButFirstFragment, final int allButFirstFragmentOffsetInDoc) {

    if ("".equals(allButFirstFragment)) {
      // There are not any more fragments, which may be valid (e.g.
      // image="{imageResource}")
      return;
    }

    // We visit each fragment and add references/problems accordingly
    UiBinderUtilities.resolveJavaElExpression(elementType, allButFirstFragment,
        new ElExpressionFragmentVisitor() {
          public void visitNonterminalPrimitiveFragment(String fragment,
              int offset, int length) {
            problemMarkerManager.setPrimitiveFragmentWithLeftoverFragmentsError(
                new Region(allButFirstFragmentOffsetInDoc + offset, length),
                fragment);
          }

          public void visitResolvedFragmentMethod(IMethod method, int offset,
              int length) {
            javaTypeReferences.add(method.getDeclaringType().getFullyQualifiedName());
          }

          public void visitUnresolvedFragment(String fragment, int offset,
              int length, IType enclosingType) {
            problemMarkerManager.setMethodFragmentUndefinedError(new Region(
                allButFirstFragmentOffsetInDoc + offset, length), fragment);

            // Add a reference to the type the method should be located in, so
            // when the type is modified, we revalidate ourselves
            javaTypeReferences.add(enclosingType.getFullyQualifiedName());
          }
        });
  }

  /**
   * Parses the <ui:image> or <ui:data> elements.
   */
  private void tryParseResourceElement(IDOMElement element) {
    if (!UiBinderXmlModelUtilities.isImageElement(element)
        && !UiBinderXmlModelUtilities.isDataElement(element)) {
      return;
    }

    IDOMAttr srcAttribute = (IDOMAttr) UiBinderXmlModelUtilities.getSrcAttribute(element);
    if (srcAttribute == null) {
      return;
    }

    IRegion srcAttributeRegion = XmlUtilities.getAttributeValueRegion(srcAttribute);
    if (srcAttributeRegion == null) {
      // XML editor will flag invalid XML syntax
      return;
    }

    IPath classpathRelativePath = classpathRelativeDir.append(srcAttribute.getNodeValue());
    IFile file = getExistingFile(classpathRelativePath);

    if (classpathRelativePath != null) {
      addReference(xmlReferenceLocation,
          new ClasspathRelativeFileReferenceLocation(classpathRelativePath));
    }

    if (file == null || classpathRelativePath == null) {
      problemMarkerManager.setResourceNotFoundError(srcAttributeRegion,
          srcAttribute.getNodeValue());
    }
  }

  /**
   * Parses ui:style elements.
   */
  private void tryParseStyleElement(IDOMElement element) {
    if (!UiBinderXmlModelUtilities.isStyleElement(element)) {
      return;
    }

    // Check for duplicate selector names
    CssExtractor extractor = UiBinderXmlModelUtilities.createCssExtractorForStyleElement(
        element, javaProject);
    if (extractor != null) {
      String errorMessage = CssSelectorNameCollector.getDuplicateSelectorNamesErrorMessage(extractor.getCssDocument());
      if (errorMessage != null) {
        List<IRegion> elementTagRegions = XmlUtilities.getElementTagRegions(
            element, true);
        problemMarkerManager.setDuplicateCssSelectorError(
            elementTagRegions.get(0), errorMessage);
      }
    }

    tryParseStyleElementSrcAttribute(element);
    tryParseTypeAttribute(element);
  }

  private void tryParseStyleElementSrcAttribute(IDOMElement styleElement) {
    IDOMAttr attribute = (IDOMAttr) UiBinderXmlModelUtilities.getSrcAttribute(styleElement);
    if (attribute == null) {
      return;
    }

    IRegion attributeValueRegion = XmlUtilities.getAttributeValueRegion(attribute);
    if (attributeValueRegion == null) {
      // XML editor will flag invalid XML syntax
      return;
    }

    for (String cssPathString : UiBinderUtilities.getPathsFromDelimitedString(attribute.getNodeValue())) {
      IPath cssClasspathRelativePath = classpathRelativeDir.append(cssPathString);
      IFile cssFile = getExistingFile(cssClasspathRelativePath);

      // Add a reference to the CSS contents
      if (cssClasspathRelativePath != null) {
        addReference(
            xmlReferenceLocation,
            new ClasspathRelativeFileReferenceLocation(cssClasspathRelativePath));
      }

      if (cssFile == null || !cssFile.exists()
          || cssClasspathRelativePath == null) {
        problemMarkerManager.setCssFileNotFoundError(attributeValueRegion,
            cssPathString);
      }
    }
  }

  /**
   * Parses the "type" attribute on <ui:with> and <ui:style> elements.
   */
  private void tryParseTypeAttribute(IDOMElement element) {
    IDOMAttr typeAttr = (IDOMAttr) UiBinderXmlModelUtilities.getTypeAttribute(element);
    if (typeAttr == null) {
      return;
    }

    IRegion valueRegion = XmlUtilities.getAttributeValueRegion(typeAttr);
    if (valueRegion == null) {
      // XML editor will flag invalid XML syntax
      return;
    }

    String fqType = typeAttr.getNodeValue();
    if (fqType == null) {
      return;
    }

    final IType type = JavaModelSearch.findType(javaProject, fqType);
    if (!JavaModelSearch.isValidElement(type)) {
      problemMarkerManager.setTypeUndefinedError(valueRegion, fqType);
    } else {
      if (UiBinderXmlModelUtilities.isStyleElement(element)) {
        // Ensure the type is a CssResource subtype
        IType cssResourceType = ClientBundleUtilities.findCssResourceType(javaProject);
        if (cssResourceType != null) {
          try {
            if (!JavaUtilities.isSubtype(cssResourceType, type)) {
              problemMarkerManager.setNotCssResourceSubtypeError(valueRegion,
                  fqType);
            }
          } catch (JavaModelException e) {
            GWTPluginLog.logWarning(e,
                "Could not validate the <ui:style>'s type attribute.");
          }
        }
      }
    }

    javaTypeReferences.add(fqType);
  }

  /**
   * @param element
   */
  private void tryParseUiImportElement(IDOMElement element) {
    if (!UiBinderXmlModelUtilities.isImportElement(element)) {
      return;
    }

    IType enclosingType = UiBinderXmlModelUtilities.resolveElementToJavaType(
        element, javaProject);

    if (enclosingType != null) {
      addReference(xmlReferenceLocation,
          new LogicalJavaElementReferenceLocation(
              new UiBinderImportReferenceType(enclosingType)));
    }
  }

  /**
   * Parses a urn:import scheme on the UiBinder element attributes.
   */
  private void tryParseUrnImport(IDOMAttr attr) {
    if (!UiBinderXmlModelUtilities.isUiBinderElement(attr.getOwnerElement())
        || !UiBinderConstants.XMLNS_NAMESPACE.equals(attr.getNamespaceURI())
        || attr.getNodeValue() == null
        || !attr.getNodeValue().startsWith(
            UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING)) {
      return;
    }

    int urnImportLength = UiBinderConstants.URN_IMPORT_NAMESPACE_BEGINNING.length();
    String packageName = attr.getNodeValue().substring(urnImportLength);
    addReference(xmlReferenceLocation, new LogicalJavaElementReferenceLocation(
        new LogicalPackage(packageName)));

    IPackageFragmentRoot[] packageFragmentRoots;
    try {
      // Don't call JavaModelSearch.getPackageFragments here, which incurs an expensive
      // exists check that is redundant with JavaModelSearch.isValidElement below.
      packageFragmentRoots = javaProject.getAllPackageFragmentRoots();
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e, "Could not parse UiBinder urn:import attribute");
      return;
    }
    for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
      IPackageFragment packageFragment = packageFragmentRoot.getPackageFragment(packageName);
      if (JavaModelSearch.isValidElement(packageFragment)) {
        return; // Return as soon as we find any valid package with the name in the import
      }
    }

    IRegion attrValueRegion = XmlUtilities.getAttributeValueRegion(attr);
    if (attrValueRegion == null) {
      // XML editor will flag invalid XML syntax
      return;
    }

    int offset = attrValueRegion.getOffset() + urnImportLength;
    int length = attrValueRegion.getLength() - urnImportLength;

    problemMarkerManager.setPackageUndefinedError(new Region(offset, length), packageName);
  }

  /**
   * Parses a widget reference from an element. For example, g:Button.
   */
  private void tryParseWidgetFromElement(IDOMElement element) {
    String fqWidgetType = UiBinderXmlModelUtilities.computeQualifiedWidgetTypeName(element);
    if (fqWidgetType == null) {
      return;
    }

    if (IGNORED_CAPITALIZED_SYNTHETIC_ELEMENTS.contains(fqWidgetType)) {
      return;
    }
    
    // even if this type doens't exist, we'll add the name so that if the type
    // is added later, this ui.xml file gets revalidated.
    javaTypeReferences.add(fqWidgetType);
    
    final IType type = JavaModelSearch.findType(javaProject, fqWidgetType);
    boolean validType = JavaModelSearch.isValidElement(type);

    // e.g. <g:Button> and </g:Button> will be different regions in the loop
    // below
    List<IRegion> tagRegions = XmlUtilities.getElementTagRegions(element, false);
    for (IRegion region : tagRegions) {
      if (!validType) {
        problemMarkerManager.setWidgetUndefinedError(region, fqWidgetType);
      }
    }
  }

  private void tryParseWithElement(IDOMElement element) {
    if (!UiBinderXmlModelUtilities.isWithElement(element)) {
      return;
    }

    tryParseTypeAttribute(element);
  }

  private void tryValidatePrefix(IDOMElement element) {
    String prefix = element.getPrefix();
    if (prefix != null && element.getNamespaceURI() == null) {
      // The prefix is entered, but it does not bind to a namespace
      for (IRegion region : XmlUtilities.getElementTagRegions(element, true)) {
        problemMarkerManager.setNamespacePrefixUndefinedError(region, prefix);
      }
    }
  }
}
