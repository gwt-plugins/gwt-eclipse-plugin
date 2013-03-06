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
package com.google.gwt.eclipse.core.actions;

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.editors.java.GWTPartitions;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Sample editor action that provides information about the selection point and
 * the containing content type.
 */
public class EditorAction implements IEditorActionDelegate {

  private IEditorPart targetEditor;

  public void run(IAction action) {
    if (targetEditor == null) {
      GWTPluginLog.logWarning("targetEditor is null");
      return;
    }

    IEditorInput editorInput = targetEditor.getEditorInput();
    IResource resource = (IResource) editorInput.getAdapter(IResource.class);
    ITextEditor javaEditor = (ITextEditor) targetEditor;
    ITextSelection sel = (ITextSelection) javaEditor.getSelectionProvider().getSelection();
    IDocument document = javaEditor.getDocumentProvider().getDocument(
        javaEditor.getEditorInput());

    IDocumentExtension3 document3 = (IDocumentExtension3) document;
    IDocumentPartitioner gwtPartitioner = document3.getDocumentPartitioner(GWTPartitions.GWT_PARTITIONING);

    String[] partitionings = document3.getPartitionings();
    String partitioning = (gwtPartitioner != null
        ? GWTPartitions.GWT_PARTITIONING : IJavaPartitions.JAVA_PARTITIONING);

    ITypedRegion[] types;
    try {
      types = TextUtilities.computePartitioning(document, partitioning,
          sel.getOffset(), sel.getLength(), false);
    } catch (BadLocationException e) {
      GWTPluginLog.logError(e);
      return;
    }

    String msg = "File: " + resource.getName();

    msg += "\nPartitionings: ";
    for (String part : partitionings) {
      msg += "\n" + part;
    }

    msg += "\n\nContent types: ";
    for (ITypedRegion type : types) {
      msg += "\n" + type.getType();
    }

    msg += "\n\nSelection range: (offset = " + sel.getOffset() + ", length = "
        + sel.getLength() + ")";

    MessageDialog.openInformation(targetEditor.getSite().getShell(),
        "Selection Info", msg);
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    this.targetEditor = targetEditor;
  }
}
