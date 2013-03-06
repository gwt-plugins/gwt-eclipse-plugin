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

import com.google.gwt.eclipse.core.uibinder.UiBinderException;
import com.google.gwt.eclipse.core.uibinder.UiBinderXmlModelUtilities;
import com.google.gwt.eclipse.core.uibinder.contentassist.IProposalComputer;
import com.google.gwt.eclipse.core.uibinder.contentassist.ReplacementCompletionProposal;
import com.google.gwt.eclipse.core.uibinder.sse.css.CssExtractor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;

import java.util.List;

/**
 * A proposal computer that generates completion proposals from CSS selector
 * names.
 */
@SuppressWarnings("restriction")
public class CssSelectorProposalComputer implements IProposalComputer {

  public static CssSelectorProposalComputer newUsingUiBinderStyleElement(
      IDOMElement styleElement, String classText, int classTextOffset,
      IJavaProject javaProject) {
    CssExtractor extractor = UiBinderXmlModelUtilities.createCssExtractorForStyleElement(
        styleElement, javaProject);
    if (extractor == null) {
      return null;
    }

    return new CssSelectorProposalComputer(extractor.getCssModel(), classText,
        classTextOffset);
  }

  private final ICSSModel cssModel;
  private final String enteredSelectorText;
  private final int enteredSelectorTextOffset;

  public CssSelectorProposalComputer(ICSSModel cssModel,
      String enteredSelectorText, int enteredSelectorTextOffset) {
    this.cssModel = cssModel;
    this.enteredSelectorText = enteredSelectorText;
    this.enteredSelectorTextOffset = enteredSelectorTextOffset;
  }

  public void computeProposals(final List<ICompletionProposal> proposals)
      throws UiBinderException {
    for (String selectorName : CssSelectorNameCollector.getLiteralSelectorNames(cssModel.getDocument())) {
      if (!selectorName.startsWith(enteredSelectorText)) {
        continue;
      }

      proposals.add(new ReplacementCompletionProposal(selectorName,
          enteredSelectorTextOffset, enteredSelectorText.length(),
          ReplacementCompletionProposal.DEFAULT_CURSOR_POSITION));
    }
  }

}
