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

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.XmlUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.PackageBasedNamespaceManager;
import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.computers.ElExpressionProposalComputer;
import com.google.gwt.eclipse.core.uibinder.contentassist.computers.ProposalComputerFactory;
import com.google.gwt.eclipse.core.uibinder.contentassist.computers.WidgetProposalComputer;
import com.google.gwt.eclipse.core.uibinder.sse.StructuredTextPartitionerForUiBinderXml;
import com.google.gwt.eclipse.core.uibinder.text.IDocumentPartitionerFactory;
import com.google.gwt.eclipse.core.uibinder.text.StructuredDocumentCloner;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;
import org.eclipse.wst.xml.ui.internal.contentassist.XMLContentAssistProcessor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/*
 * We are using an XML Schema to get Eclipse to propose attributes and elements
 * to the user. The problem is, when the ui.xml has a doctype at the top
 * (allowing &nbps;, etc.), Eclipse will not use the XML Schema for generating
 * proposals. This is because there's a check that sees if the DTD content model
 * is non-null in which case it uses that over the XML Schema content model. To
 * get around this, we have some hackery in place to clone the XML document,
 * replace the DOCTYPE with whitespace, and generate proposals from it.
 */
/**
 * Content assist processor for the UiBinder template XML files.
 */
@SuppressWarnings("restriction")
public class UiBinderXmlCompletionProcessor extends XMLContentAssistProcessor {

  private static class DocumentChangingTextViewer extends DelegatingTextViewer {
    private final IDocument document;

    public DocumentChangingTextViewer(ITextViewer originalTextViewer,
        IDocument document) {
      super(originalTextViewer);
      this.document = document;
    }

    @Override
    public IDocument getDocument() {
      return document;
    }
  }

  /**
   * Clones the document, removes any doctype, and prepares a text viewer.
   */
  private static class DtdRemover {

    /**
     * Make sure to call {@link #release()} after you are done!
     * 
     * @return a {@link DtdRemover} or null
     */
    private static DtdRemover create(ITextViewer textViewer,
        int documentPosition) {

      IDocumentPartitionerFactory docPartitionerFactory = new IDocumentPartitionerFactory() {
        public IDocumentPartitioner createDocumentPartitioner() {
          return new StructuredTextPartitionerForUiBinderXml();
        }
      };

      StructuredDocumentCloner structuredDocumentCloner = new StructuredDocumentCloner(
          IXMLPartitions.XML_DEFAULT, docPartitionerFactory);
      IStructuredDocument clonedDoc = structuredDocumentCloner.clone(textViewer.getDocument());
      if (clonedDoc == null) {
        return null;
      }

      IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(
          clonedDoc);
      try {
        IDOMNode docType = (IDOMNode) model.getDocument().getDoctype();
        if (docType == null) {
          return null;
        }

        IStructuredDocumentRegion firstRegion = docType.getFirstStructuredDocumentRegion();
        IStructuredDocumentRegion lastRegion = docType.getLastStructuredDocumentRegion();
        int firstPos = firstRegion.getStartOffset();
        int lastPos = lastRegion.getEndOffset();
        int docTypeLen = lastPos - firstPos;

        if (docTypeLen == 0 || documentPosition >= firstPos
            && documentPosition <= lastPos) {
          return null;
        }

        try {
          clonedDoc.replace(firstPos, docTypeLen,
              StringUtilities.repeatCharacter(' ', docTypeLen));
        } catch (BadLocationException e) {
          GWTPluginLog.logError(e,
              "Unexpected bad location while removing doctype");
          return null;
        }

      } finally {
        if (model != null) {
          model.releaseFromRead();
        }
      }

      return new DtdRemover(new DocumentChangingTextViewer(textViewer,
          clonedDoc), documentPosition, structuredDocumentCloner, clonedDoc);
    }

    public final ITextViewer textViewer;
    private final IStructuredDocument clonedDocument;
    private final StructuredDocumentCloner documentCloner;

    private DtdRemover(ITextViewer textViewer, int documentPosition,
        StructuredDocumentCloner documentCloner,
        IStructuredDocument clonedDocument) {
      this.textViewer = textViewer;
      this.documentCloner = documentCloner;
      this.clonedDocument = clonedDocument;
    }

    public void release() {
      documentCloner.release(clonedDocument);
    }
  }

  private IJavaProject javaProject;

  private String filePackageName;
  
  /**
   * Whether widget proposals were added from the {@link WidgetProposalComputer}
   * . This must be cleared at the beginning of
   * {@link #computeCompletionProposals(ITextViewer, int)}.
   */
  private boolean addedDynamicWidgetProposals;

  @SuppressWarnings("unchecked")
  @Override
  public ICompletionProposal[] computeCompletionProposals(
      ITextViewer textViewer, int documentPosition) {

    rememberJavaProject(textViewer.getDocument());
    addedDynamicWidgetProposals = false;

    DtdRemover dtdRemover = DtdRemover.create(textViewer, documentPosition);
    try {
      if (dtdRemover != null) {
        textViewer = dtdRemover.textViewer;
      }

      List<ICompletionProposal> proposals = getWstCompletionProposals(
          textViewer, documentPosition);
      if (proposals == null) {
        return null;
      }

      // Sort all of the proposals based on relevance and alphabet
      Collections.sort(proposals, new CompletionProposalComparator());

      if (addedDynamicWidgetProposals) {
        removeSchemaWidgetProposals(proposals, textViewer.getDocument());
      }

      removeSchemaUiBinderElementProposal(
          (IStructuredDocument) textViewer.getDocument(), proposals);

      return proposals.toArray(new ICompletionProposal[proposals.size()]);

    } finally {
      if (dtdRemover != null) {
        dtdRemover.release();
      }
    }
  }

  @Override
  protected void addAttributeNameProposals(
      ContentAssistRequest contentAssistRequest) {
    super.addAttributeNameProposals(contentAssistRequest);
    
    addProposals(
        contentAssistRequest,
        ProposalComputerFactory.newUiFieldAttributeProposalComputer(contentAssistRequest),
        ProposalComputerFactory.newUiPhAttributeProposalComputer(contentAssistRequest),
        ProposalComputerFactory.newSetterAttributeProposalComputer(
            contentAssistRequest, javaProject));
  }

  @Override
  protected void addAttributeValueProposals(
      ContentAssistRequest contentAssistRequest) {
    super.addAttributeValueProposals(contentAssistRequest);

    if (javaProject != null) {
      addProposals(
          contentAssistRequest,
          ElExpressionProposalComputer.newUsingContentAssistRequest(
              contentAssistRequest, javaProject),
          ProposalComputerFactory.newUiImportFieldProposalComputer(
              contentAssistRequest, javaProject, filePackageName),
          ProposalComputerFactory.newWithTypeProposalComputer(
              contentAssistRequest, javaProject),
          ProposalComputerFactory.newUrnImportProposalComputer(
              contentAssistRequest, javaProject),
          ProposalComputerFactory.newUrnTypesProposalComputer(contentAssistRequest),
          ProposalComputerFactory.newUiFieldProposalComputer(
              contentAssistRequest, javaProject));
    }
  }

  @Override
  protected void addTagNameProposals(ContentAssistRequest contentAssistRequest,
      int childPosition) {
    super.addTagNameProposals(contentAssistRequest, childPosition);

    if (javaProject != null) {
      addProposals(
          contentAssistRequest,
          ProposalComputerFactory.newUiBinderRootElementProposalComputer(contentAssistRequest));

      if (addProposals(contentAssistRequest,
          WidgetProposalComputer.newUsingContentAssistRequest(
              contentAssistRequest, javaProject))) {
        addedDynamicWidgetProposals = true;
      }
    }
  }

  /**
   * Calls the given proposal computers and adds their proposals to the content
   * assist request.
   * 
   * @param contentAssistRequest the recipient of the proposals
   * @param proposalComputers the proposal computers, null is permitted
   * @return true if proposals were added to the request
   */
  private boolean addProposals(ContentAssistRequest contentAssistRequest,
      IProposalComputer... proposalComputers) {

    List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
    for (IProposalComputer proposalComputer : proposalComputers) {
      if (proposalComputer == null) {
        continue;
      }

      try {
        proposalComputer.computeProposals(proposals);
      } catch (UiBinderException e) {
        GWTPluginLog.logWarning(e, MessageFormat.format(
            "Could not compute proposals for {0}.",
            proposalComputer.getClass().getSimpleName()));
        return false;
      }
    }

    for (ICompletionProposal proposal : proposals) {
      contentAssistRequest.addProposal(proposal);
    }

    return proposals.size() > 0;
  }

  private List<ICompletionProposal> getWstCompletionProposals(
      ITextViewer textViewer, int documentPosition) {
    ICompletionProposal[] proposals = super.computeCompletionProposals(
        textViewer, documentPosition);
    if (proposals == null || proposals.length == 0) {
      return null;
    }

    return new ArrayList<ICompletionProposal>(Arrays.asList(proposals));
  }

  private void rememberJavaProject(IDocument document) {
    IFile file = SseUtilities.resolveFile(document);
    if (file != null) {
      javaProject = JavaCore.create(file.getProject());
      if (!JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
        javaProject = null;
        return;
      }
    }
    
    Set<IType> types = UiBinderUtilities.getSubtypesFromXml(file, javaProject);
    if (types.size() > 0) {
      // TODO: this picks off only the first owner type for the ui.xml file...
      // There could be more than one owner type that references the ui.xml file
      // Covering multiple packages could be expensive because we would need to kick off
      // multiple EvaluationContext's to evaluate each package
      IType type = types.iterator().next();
      filePackageName = type.getPackageFragment().getElementName();
    }
  }

  private void removeSchemaUiBinderElementProposal(
      IStructuredDocument document, List<ICompletionProposal> proposals) {
    IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(
        document);
    try {
      IDOMElement rootElement = (IDOMElement) model.getDocument().getDocumentElement();
      if (rootElement == null) {
        // There is no root element, so the <ui:UiBinder> is a valid proposal
        return;
      }

      // Remove the proposal whose display string matches the root element's
      // name (e.g. "ui:UiBinder")
      for (Iterator<ICompletionProposal> it = proposals.iterator(); it.hasNext();) {
        ICompletionProposal proposal = it.next();
        if (rootElement.getNodeName().equals(proposal.getDisplayString())) {
          it.remove();
        }
      }
    } finally {
      model.releaseFromRead();
    }
  }

  private void removeSchemaWidgetProposals(List<ICompletionProposal> proposals,
      IDocument document) {

    PackageBasedNamespaceManager packageManager = new PackageBasedNamespaceManager();

    IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(
        document);
    try {
      packageManager.readFromElement(model.getDocument().getDocumentElement());
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }

    Iterator<ICompletionProposal> it = proposals.iterator();
    while (it.hasNext()) {
      ICompletionProposal proposal = it.next();
      if (!(proposal instanceof CustomCompletionProposal)) {
        // Schema-generated widget proposals are instances of this class
        continue;
      }

      String displayString = proposal.getDisplayString();
      String prefix = XmlUtilities.getPrefix(displayString);
      if (prefix == null) {
        // Must have a namespace
        continue;
      }

      String packageName = packageManager.getPackageName(prefix);
      if (packageName == null) {
        // This doesn't map to a widget
        continue;
      }

      String widgetName = XmlUtilities.getUnprefixed(displayString);
      if (widgetName.length() == 0
          || !Character.isUpperCase(widgetName.charAt(0))) {
        // Not a widget
        continue;
      }

      // Passes our test, it must be a widget!
      it.remove();
    }
  }

}