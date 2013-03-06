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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Wraps a Java quick fix proposal as a marker resolution, so we can use the
 * same code whether quick fix is invoked from the Problems view or the Java
 * editor.
 */
@SuppressWarnings("restriction")
class QuickFixCompletionProposalWrapper implements IMarkerResolution2 {

  private ICompilationUnit cu;
  private int length;
  private int offset;
  private IJavaCompletionProposal proposal;

  public QuickFixCompletionProposalWrapper(ICompilationUnit cu, int offset,
      int length, IJavaCompletionProposal proposal) {
    this.cu = cu;
    this.offset = offset;
    this.length = length;
    this.proposal = proposal;
  }

  public String getDescription() {
    return proposal.getAdditionalProposalInfo();
  }

  public Image getImage() {
    return proposal.getImage();
  }

  public String getLabel() {
    return proposal.getDisplayString();
  }

  public void run(IMarker marker) {
    try {
      IEditorPart part = EditorUtility.isOpenInEditor(cu);
      if (part == null) {
        part = JavaUI.openInEditor(cu, true, false);
        if (part instanceof ITextEditor) {
          ((ITextEditor) part).selectAndReveal(offset, length);
        }
      }
      if (part != null) {
        IEditorInput input = part.getEditorInput();
        IDocument doc = JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(
            input);
        proposal.apply(doc);
      }
    } catch (CoreException e) {
      CorePluginLog.logError(e);
    }
  }
}
