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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gdt.eclipse.core.JavaUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gdt.eclipse.core.contentassist.JavaContentAssistUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.PackageBasedNamespaceManager;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.contentassist.AbstractCompletionProposal;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAnyElement;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMVisitor;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.text.MessageFormat;
import java.util.List;

/**
 * Computes a set of content proposals for subclasses of GWT's
 * <code>Widget</code> class.
 * 
 * See http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=11
 */
@SuppressWarnings("restriction")
public class WidgetProposalComputer extends AbstractJavaProposalComputer {

  private static class CmAnyElementTracker extends CMVisitor {

    public static boolean containsAnyElement(
        CMElementDeclaration rootElementDecl) {
      CmAnyElementTracker tracker = new CmAnyElementTracker(rootElementDecl);
      tracker.visitCMNode(rootElementDecl);
      return tracker.anyElementSeen;
    }

    private final CMElementDeclaration rootElementDeclaration;
    private boolean anyElementSeen = false;

    private CmAnyElementTracker(CMElementDeclaration rootElementDeclaration) {
      this.rootElementDeclaration = rootElementDeclaration;
    }

    @Override
    public void visitCMAnyElement(CMAnyElement anyElement) {
      anyElementSeen = true;
    }

    @Override
    public void visitCMElementDeclaration(
        CMElementDeclaration elementDeclaration) {
      if (elementDeclaration.equals(rootElementDeclaration)) {
        super.visitCMElementDeclaration(elementDeclaration);
      } else {
        // Do not call super since we do not want to traverse into child
        // elements
      }
    }
  }

  /**
   * Collects all the widget CompletionProposal (from the evaluation context
   * codeComplete method) and creates an ICompletionProposal for each.
   */
  private class WidgetCompletionRequestor extends
      ProposalGeneratingCompletionRequestor {
    private final String packageName;

    /**
     * @param packageName the package whose widgets should be returned, or null
     *          for all widgets
     */
    public WidgetCompletionRequestor(String packageName) {
      super(WidgetProposalComputer.this.getReplaceOffset(),
          getEnteredText().length(), new int[] {CompletionProposal.TYPE_REF},
          getJavaProject(), false);

      this.packageName = packageName;
    }

    @Override
    protected ICompletionProposal createProposal(CompletionProposal proposal) {
      if (proposal.getKind() != CompletionProposal.TYPE_REF) {
        return null;
      }

      // NOTE: Resulting qualified name is dot-separated, even for enclosing
      // types. Generic signatures are not produced. See
      // org.eclipse.jdt.internal.codeassist.CompletionEngine.createTypeProposal.
      String qualifiedTypeName = String.valueOf(Signature.toCharArray(proposal.getSignature()));
      String typePackage = JavaUtilities.getPackageName(qualifiedTypeName);
      String typeSimpleName = Signature.getSimpleName(qualifiedTypeName);

      if (packageName != null && !typePackage.equals(packageName)) {
        return null;
      }

      ICompletionProposal javaCompletionProposal = JavaContentAssistUtilities.getJavaCompletionProposal(
          proposal, getContext(), getJavaProject());
      if (javaCompletionProposal != null) {
        return new WidgetProposal(typeSimpleName, typePackage, null,
            javaCompletionProposal.getDisplayString(),
            javaCompletionProposal.getImage(), getReplaceOffset(),
            getReplaceLength(), packageManager);
      } else {
        return new WidgetProposal(typeSimpleName, typePackage, null,
            typeSimpleName, null, getReplaceOffset(), getReplaceLength(),
            packageManager);
      }
    }
  }

  /**
   * The proposal for each widget suggested.
   */
  private static class WidgetProposal extends AbstractCompletionProposal {

    private final String simpleTypeName;
    private final String packageName;

    private int startPosition;

    /**
     * The remembered cursor position. Note: Only valid after
     * {@link #apply(IDocument)} has been called. Also, if linked mode was
     * entered, this is *not* where the cursor will end up (see
     * {@link #getSelection(IDocument)}).
     */
    private int cursorPosition;

    /**
     * Whether linked mode was entered.
     */
    private boolean wasLinkedModeEntered;

    private int replaceLength;

    private final PackageBasedNamespaceManager packageManager;

    public WidgetProposal(String simpleTypeName, String packageName,
        String additionalInfo, String displayString, Image image,
        int replaceOffset, int replaceLength,
        PackageBasedNamespaceManager packageManager) {
      super(additionalInfo, displayString, image);

      this.simpleTypeName = simpleTypeName;
      this.packageName = packageName;
      this.startPosition = replaceOffset;
      this.replaceLength = replaceLength;
      this.packageManager = packageManager;
    }

    @Override
    public Point getSelection(IDocument document) {
      // If linked mode was entered, it handled setting the cursor position, so
      // we return null here
      return wasLinkedModeEntered ? null : new Point(cursorPosition, 0);
    }

    @Override
    public void onApply(IDocument document, ITextViewer viewer)
        throws BadLocationException {
      IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForEdit(
          document);
      if (model == null) {
        return;
      }

      try {
        apply(document, viewer,
            (IDOMElement) model.getDocument().getDocumentElement());
      } finally {
        model.releaseFromEdit();
      }
    }

    private void apply(IDocument document, ITextViewer viewer,
        IDOMElement rootElement) throws BadLocationException {
      String prefix = packageManager.getPrefix(packageName);
      boolean isAlreadyImported = prefix != null;

      if (!isAlreadyImported) {
        prefix = packageManager.resolvePrefix(packageName);

        // Update the root element with the import
        int oldDocumentLength = document.getLength();
        packageManager.writeToElement(rootElement);

        // Ensure our start position is up-to-date with any changes made above
        startPosition += document.getLength() - oldDocumentLength;
      }

      // e.g. g:Button
      String prefixedElementName = MessageFormat.format("{0}:{1}", prefix,
         simpleTypeName);

      // e.g. g:Button></g:Button>, where the cursor will be placed between the
      // opening and closing tags (note: this is how the XML editor implements
      // generating the closing tag too.)
      String replacementText = MessageFormat.format("{0}></{0}>",
          prefixedElementName);
      document.replace(startPosition, replaceLength, replacementText);

      // Add one for the '>'
      cursorPosition = startPosition + prefixedElementName.length() + 1;

      if (!isAlreadyImported && viewer != null) {
        enterLinkedModeForPrefix(document, viewer, rootElement, prefix);
      }
    }

    /**
     * Enters the linked mode for editing the namespace prefix we generated.
     */
    private void enterLinkedModeForPrefix(IDocument document,
        ITextViewer viewer, IDOMElement rootElement, String prefix)
        throws BadLocationException {

      int linkedPosSequence = 0;

      // The prefix is the first thing entered at the start position
      LinkedPosition pos1 = new LinkedPosition(document, startPosition,
          prefix.length(), linkedPosSequence++);

      // The prefix is also at the cursor position + 2 (those two following
      // characters are '<' and '/')
      LinkedPosition pos2 = new LinkedPosition(document, cursorPosition + 2,
          prefix.length(), linkedPosSequence++);

      IDOMElement rootDomElement = (IDOMElement) rootElement;
      // TODO: use UiBinderConstants.XMLNS_PREFIX, but that has been modified in
      // a parallel CL. Will switch to using that constant in the latter of this
      // and that other CL.
      final String xmlnsPrefix = "xmlns:";
      String fullPrefixName = xmlnsPrefix + prefix;
      IDOMAttr domAttribute = (IDOMAttr) rootDomElement.getAttributeNode(fullPrefixName);
      LinkedPosition pos3 = new LinkedPosition(document,
          domAttribute.getStartOffset() + xmlnsPrefix.length(),
          prefix.length(), linkedPosSequence++);

      LinkedPositionGroup group = new LinkedPositionGroup();
      group.addPosition(pos1);
      group.addPosition(pos2);
      group.addPosition(pos3);

      // Boilerplate stuff below
      LinkedModeModel model = new LinkedModeModel();
      model.addGroup(group);
      model.forceInstall();

      LinkedModeUI ui = new LinkedModeUI(model, viewer);
      ui.enter();

      wasLinkedModeEntered = true;
    }
  }

  public static WidgetProposalComputer newUsingContentAssistRequest(
      ContentAssistRequest contentAssistRequest, IJavaProject javaProject) {

    short nodeType = contentAssistRequest.getNode().getNodeType();
    // If the user is completing at <_, then it will be a text node
    if (nodeType != Node.ELEMENT_NODE && nodeType != Node.TEXT_NODE) {
      return null;
    }

    IDOMElement rootElement = (IDOMElement) XmlUtilities.getRootElement(contentAssistRequest.getNode());
    if (rootElement == null || javaProject == null) {
      // Perhaps it is an empty document
      return null;
    }

    // Ensure we are not editing the root element
    if (contentAssistRequest.getStartOffset() <= rootElement.getStartEndOffset()) {
      return null;
    }

    Element parentElement = (Element) ((contentAssistRequest.getParent() instanceof Element)
        ? contentAssistRequest.getParent() : null);

    try {
      return new WidgetProposalComputer(contentAssistRequest.getMatchString(),
          contentAssistRequest.getReplacementBeginPosition(), rootElement,
          parentElement, javaProject);
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e, "Could not create the widget proposal computer.");
    } catch (UiBinderException e) {
      GWTPluginLog.logError(e, "Could not create the widget proposal computer.");
    }

    return null;
  }

  private final Element parentElement;
  private final Element rootElement;
  private final PackageBasedNamespaceManager packageManager = new PackageBasedNamespaceManager();

  /**
   * Constructs a widget completion proposal computer.
   * 
   * @param text any text already entered for the widget
   * @param offset the document offset of the <code>text</code>
   * @param rootElement the root element containing the namespaces
   * @param javaProject the java project containing the file being edited
   * @throws UiBinderException
   * @throws JavaModelException
   */
  public WidgetProposalComputer(String text, int offset, Element rootElement,
      Element parentElement, IJavaProject javaProject)
      throws JavaModelException, UiBinderException {
    super(javaProject, text, offset, text.length());

    this.rootElement = rootElement;
    this.parentElement = parentElement;

    packageManager.readFromElement(rootElement);
  }

  public void computeProposals(List<ICompletionProposal> proposals)
      throws UiBinderException {

    if (!isWidgetAllowed()) {
      return;
    }

    IEvaluationContext evalContext = createEvaluationContext();
    String packageName = null;
    String prefix = XmlUtilities.getPrefix(enteredText);
    if (prefix != null) {
      packageName = packageManager.getPackageName(prefix);
      if (packageName == null) {
        // The user has specified a namespace prefix but there is not a matching
        // package, so no widgets here
        return;
      }
    }

    ProposalGeneratingCompletionRequestor requestor = new WidgetCompletionRequestor(
        packageName);

    String unprefixedEnteredText = XmlUtilities.getUnprefixed(enteredText);

    try {
      if (unprefixedEnteredText.trim().length() > 0) {
        evalContext.codeComplete(enteredText, enteredText.length(), requestor);
      } else {
        // Offer all possible types available to the project as
        // completions. See
        // http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=11
        CodeCompleteProposalComputer.completeCodeUsingAlphabet(evalContext,
            requestor);
      }
      proposals.addAll(requestor.getProposals());
    } catch (JavaModelException e) {
      throw new UiBinderException(e);
    }
  }

  private boolean isWidgetAllowed() {
    if (parentElement == null) {
      // No parent element, widget not allowed
      return false;
    }

    ModelQuery modelQuery = ModelQueryUtil.getModelQuery(rootElement.getOwnerDocument());
    CMElementDeclaration parentCmElementDecl = modelQuery.getCMElementDeclaration(parentElement);
    if (parentCmElementDecl == null) {
      // There is no content model for this element (e.g. third party panels),
      // so we aren't sure. Propose anyway.
      return true;
    }

    // If the CM element declaration allows for any element, the widget is
    // allowed
    return CmAnyElementTracker.containsAnyElement(parentCmElementDecl);
  }
}
