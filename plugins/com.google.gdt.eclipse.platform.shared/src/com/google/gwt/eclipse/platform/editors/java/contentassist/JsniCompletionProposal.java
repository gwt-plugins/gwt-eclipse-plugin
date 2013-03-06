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
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.viewers.StyledString;

/**
 * Overrides Eclipse 3.4+ functionality (styled display text) for
 * {@link com.google.gwt.eclipse.core.editors.java.contentassist.JsniCompletionProposal }
 */
public abstract class JsniCompletionProposal extends
    AbstractJsniCompletionProposal implements ICompletionProposalExtension6 {

  public JsniCompletionProposal(IJavaCompletionProposal jdtProposal,
      CompletionProposal wrappedProposal) {
    super(jdtProposal, wrappedProposal);
  }

  // TODO: This code is duplicated verbatim in the e34 platform plugin. When we
  // drop support for Eclipse 3.3, move all platform-specific code for JSNI
  // auto-completion back into the GWT plugin.
  public StyledString getStyledDisplayString() {
    StyledString styledDisplayString = ((ICompletionProposalExtension6) jdtProposal).getStyledDisplayString();

    if (wrappedProposal.isConstructor()) {
      String displayString = styledDisplayString.getString();
      displayString = fixCtorDisplayString(displayString);

      // Ctor display strings have two styles: the ctor name and signature
      // use the default style, and the class name uses the qualifier style
      // (by default, gray foreground color). We need to recreate that styled
      // string, but with our own ctor name ('new') in place of the original.
      int qualifierSeparatorPos = displayString.lastIndexOf('-');
      if (qualifierSeparatorPos > -1) {
        String ctorSignature = displayString.substring(0,
            qualifierSeparatorPos + 1);
        String className = displayString.substring(qualifierSeparatorPos + 1);

        styledDisplayString = new StyledString(ctorSignature);
        styledDisplayString.append(className, StyledString.QUALIFIER_STYLER);
      }
    }

    return styledDisplayString;
  }

}