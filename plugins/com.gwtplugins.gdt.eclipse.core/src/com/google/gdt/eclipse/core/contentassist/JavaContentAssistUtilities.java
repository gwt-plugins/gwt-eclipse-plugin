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
package com.google.gdt.eclipse.core.contentassist;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.SseUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;

/**
 * Utility methods for Java-related content assist.
 * 
 * @see XmlContentAssistUtilities
 */
@SuppressWarnings("restriction")
public final class JavaContentAssistUtilities {

  /**
   * Gets the fully qualified type name from a java completion proposal for
   * {@link CompletionProposal#TYPE_REF}.
   * 
   * @param javaProposal the java type reference completion proposal generated
   *          by a code complete
   * @return the fully qualified type name
   */
  public static String getFullyQualifiedTypeName(CompletionProposal javaProposal) {
    char[] signature = javaProposal.getSignature();
    return String.valueOf(Signature.toCharArray(signature));
  }

  /**
   * Generates an {@link IJavaCompletionProposal} from Java's
   * {@link CompletionProposal}.
   * 
   * @param completionProposal the {@link CompletionProposal}
   * @param completionContext the context of the {@link CompletionProposal}
   * @param javaProject the java project for the given completion proposal
   * @return a {@link IJavaCompletionProposal}, or null
   */
  public static IJavaCompletionProposal getJavaCompletionProposal(
      CompletionProposal completionProposal,
      CompletionContext completionContext, IJavaProject javaProject) {
    CompletionProposalCollector collector = new CompletionProposalCollector(
        javaProject);
    collector.acceptContext(completionContext);
    collector.accept(completionProposal);
    IJavaCompletionProposal[] javaCompletionProposals = collector.getJavaCompletionProposals();
    return javaCompletionProposals.length > 0 ? javaCompletionProposals[0]
        : null;
  }

  /**
   * Resolves the java project for the given {@link ContentAssistRequest}.
   * 
   * @param contentAssistRequest the request whose document will be used to get
   *          the java project
   * @return the java project corresponding to the request, or null if one could
   *         not be resolved
   */
  public static IJavaProject resolveJavaProject(
      ContentAssistRequest contentAssistRequest) {
    IDocument document = contentAssistRequest.getDocumentRegion().getParentDocument();
    IFile file = SseUtilities.resolveFile(document);
    if (file == null) {
      return null;
    }

    IJavaProject javaProject = JavaCore.create(file.getProject());
    if (!JavaProjectUtilities.isJavaProjectNonNullAndExists(javaProject)) {
      return null;
    }

    return javaProject;
  }

  private JavaContentAssistUtilities() {
  }
}
