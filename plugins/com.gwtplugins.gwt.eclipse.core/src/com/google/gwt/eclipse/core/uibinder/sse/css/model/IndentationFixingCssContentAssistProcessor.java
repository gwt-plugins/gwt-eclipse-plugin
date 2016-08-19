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
package com.google.gwt.eclipse.core.uibinder.sse.css.model;

import com.google.gwt.eclipse.core.uibinder.text.IndentationFixingCompletionProposal;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.ui.internal.contentassist.CSSContentAssistProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes the indentation of the generated code of
 * {@link CSSContentAssistProcessor} proposals.
 */
@SuppressWarnings("restriction")
public class IndentationFixingCssContentAssistProcessor extends
    CSSContentAssistProcessor {

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
      int documentPosition) {
    ICompletionProposal[] proposals = super.computeCompletionProposals(viewer,
        documentPosition);

    List<ICompletionProposal> wrappedProposals = new ArrayList<ICompletionProposal>();
    for (ICompletionProposal proposal : proposals) {
      wrappedProposals.add(new IndentationFixingCompletionProposal(proposal));
    }

    return wrappedProposals.toArray(new ICompletionProposal[wrappedProposals.size()]);
  }
}
