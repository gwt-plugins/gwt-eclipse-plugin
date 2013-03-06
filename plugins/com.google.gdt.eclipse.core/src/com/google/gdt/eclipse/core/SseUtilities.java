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
package com.google.gdt.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import java.io.IOException;
import java.util.Set;

/**
 * Utilities for structured source editing.
 */
@SuppressWarnings("restriction")
public class SseUtilities {

  /**
   * Searches for the specified structured document region type, in the
   * direction specified.
   * 
   * @param types if one of these is found, the method will return
   * @param abortTypes optional, if one of these is found before one of
   *          <code>types</code> is found, the method will return null
   * @param searchStartRegion the region to start the search at, exclusive
   * @param searchForward true to search forward, false to search backward
   * @return the region, or null
   */
  public static IStructuredDocumentRegion findStructuredDocumentRegion(
      Set<String> types, Set<String> abortTypes,
      IStructuredDocumentRegion searchStartRegion,
      boolean searchForward) {

    IStructuredDocumentRegion region = searchForward
        ? searchStartRegion.getNext() : searchStartRegion.getPrevious();
    while (region != null) {
      ITextRegionList textRegions = region.getRegions();
      for (int i = 0; i < textRegions.size(); i++) {
        String curType = textRegions.get(i).getType();

        if (abortTypes != null && abortTypes.contains(curType)) {
          return null;
        } else if (types.contains(curType)) {
          return region;
        }
      }

      region = searchForward ? region.getNext() : region.getPrevious();
    }
    
    return null;    
  }

  /**
   * @return the active structured text viewer, or null
   */
  public static StructuredTextViewer getActiveTextViewer() {
    // Need to get the workbench window from the UI thread
    final IWorkbenchWindow[] windowHolder = new IWorkbenchWindow[1];
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        windowHolder[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      }
    });
   
    IWorkbenchWindow window = windowHolder[0];
    if (window == null) {
      return null;
    }
    
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return null;
    }

    IEditorPart editor = page.getActiveEditor();
    if (editor == null) {
      return null;
    }

    /*
     * TODO: change the following to use AdapterUtilities.getAdapter()
     * and either a) have GWTD register an adapter factory or b) add a direct
     * IAdaptable.getAdapter() call to AdapterUtilities.getAdapter().
     */
    StructuredTextEditor structuredEditor = (StructuredTextEditor) editor.getAdapter(StructuredTextEditor.class);
    if (structuredEditor == null) {
      return null;
    }

    return structuredEditor.getTextViewer();
  }

  /**
   * Gets a structured model for the given resource. The client MUST release the
   * model (from read) after the client is finished (in a try-finally block to
   * be safe.)
   */
  public static IDOMModel getModelForRead(IResource resource) throws IOException,
      CoreException {
    if (!(resource instanceof IFile)) {
      return null;
    }

    // First try to get an existing model. Calling getModelForRead directly will
    // create a new model instead of giving us the existing model.
    IDOMModel model = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(
        (IFile) resource);
    if (model == null) {
      // If it does not exist, then get a new model
      model = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(
          (IFile) resource);
    }

    return model;
  }

  /**
   * Finds the partition containing the given offset.
   * 
   * @param document the document to search
   * @param offset the offset used to find a matching partition
   * @return the partition, or null
   */
  public static ITypedRegion getPartition(IStructuredDocument document, int offset) {
    ITypedRegion[] partitions;
    try {
      partitions = TextUtilities.computePartitioning(document,
          IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING, 0,
          document.getLength(), true);
    } catch (BadLocationException e) {
      CorePluginLog.logError(e, "Unexpected bad location exception.");
      return null;
    }

    for (ITypedRegion partition : partitions) {
      if (partition.getOffset() <= offset
          && offset < partition.getOffset() + partition.getLength()) {
        return partition;
      }
    }
    
    return null;
  }

  /**
   * Resolves the file that is associated with the given document.
   * 
   * @return an IFile pointing to the file in the workspace, or null if one
   *         could not be resolved.
   */
  public static IFile resolveFile(IDocument document) {
    IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
        document);
    try {
      return resolveFile(model);
    } finally {
      model.releaseFromRead();
    }
  }

  /**
   * Resolves the file that is associted with the given model.
   * 
   * @return an IFile pointing to the file in the workspace, or null if one
   *         could not be resolved.
   */
  public static IFile resolveFile(IStructuredModel model) {
    return (IFile) ResourceUtils.getResource(new Path(model.getBaseLocation()));
  }

}
