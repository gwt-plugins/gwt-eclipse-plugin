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
package com.google.gwt.eclipse.platform.editors.java.contentassist;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;

/**
 * TODO: docme.
 */
public abstract class AbstractJsniCompletionProposal implements
    ICompletionProposal, ICompletionProposalExtension3,
    ICompletionProposalExtension5, IJavaCompletionProposal {

  public static final String JSNI_CTOR_METHOD = "new";

  protected static String fixCtorDisplayString(String displayString) {
    int leftParenPos = displayString.indexOf('(');
    if (leftParenPos > -1) {
      // Replace the class simple name with the JSNI constructor method name
      // (e.g. 'String()' --> 'new()')
      displayString = JSNI_CTOR_METHOD + displayString.substring(leftParenPos);
    }

    return displayString;
  }

  protected final CompletionProposal wrappedProposal;

  protected final IJavaCompletionProposal jdtProposal;

  protected AbstractJsniCompletionProposal(IJavaCompletionProposal jdtProposal,
      CompletionProposal wrappedProposal) {
    this.jdtProposal = jdtProposal;
    this.wrappedProposal = wrappedProposal;
  }

}
