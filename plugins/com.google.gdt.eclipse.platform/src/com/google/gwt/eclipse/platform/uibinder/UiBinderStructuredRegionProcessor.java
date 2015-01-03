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
package com.google.gwt.eclipse.platform.uibinder;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.wst.css.core.text.ICSSPartitions;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;
import org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration;
import org.eclipse.wst.sse.ui.internal.reconcile.StructuredRegionProcessor;
import org.eclipse.wst.sse.ui.internal.spelling.SpellcheckStrategy;

import java.lang.reflect.Field;

/*
 * We use reflection to set our custom UiBinderStructuredRegionProcessor
 * (IReconciler) on our UiBinderXmlSourceViewerConfiguration. This custom
 * processor returns an instance of us at the spellcheck strategy.
 */
/*
 * Setting this as the spellcheck strategy on the default
 * StructuredRegionProcessor was also attempted, but this class's super's
 * constructor was not initializing things properly (it depended heavily on WHEN
 * we replaced the existing spellcheck strategy with our own, and it was
 * difficult to get this timing right.)
 */
/**
 * A simple reconciler that replaces the spellcheck strategy with our own.
 * <p>
 * E33 does not have the same spellcheck strategy structure, and so this ended
 * up in platform.
 */

//TODO Migrate this back to GWT since we no longer support 3.3

@SuppressWarnings("restriction")
public class UiBinderStructuredRegionProcessor extends
    StructuredRegionProcessor {

  /**
   * Spellcheck strategy that considers inlined CSS as uninteresting.
   */
  public class UiBinderSpellcheckStrategy extends SpellcheckStrategy {

    public UiBinderSpellcheckStrategy(ISourceViewer viewer, String contentTypeId) {
      super(viewer, contentTypeId);
    }

    @Override
    protected boolean isInterestingProblem(SpellingProblem problem) {
      IStructuredDocument doc = (IStructuredDocument) getDocument();
      try {
        ITypedRegion[] partitions = doc.computePartitioning(
            problem.getOffset(), problem.getLength());
        for (ITypedRegion partition : partitions) {
          if (partition.getType().equals(ICSSPartitions.STYLE)) {
            return false;
          }
        }
      } catch (BadLocationException e) {
        // Ignore
      }

      return super.isInterestingProblem(problem);
    }
  }

  public static void setAsReconciler(
      StructuredTextViewerConfiguration configuration) throws Exception {
    try {
      IReconciler reconciler = createReconciler();

      // Reflectively set the spelling service to our own
      Field reconcilerField = StructuredTextViewerConfiguration.class.getDeclaredField("fReconciler");
      reconcilerField.setAccessible(true);
      reconcilerField.set(configuration, reconciler);
    } catch (Throwable t) {
      throw new Exception(t);
    }
  }

  /**
   * Creates and initializes the UI Binder-specific reconciler.
   */
  private static IReconciler createReconciler() {
    // Initialization mirrors StructuredTextViewerConfiguration.getReconciler.
    // Difference: We do not have an ISourceViewer, but the
    // ISourceViewer.getConfiguredDocumentPartitioning for structured text
    // always returns the DEFAULT_STRUCTURED_PARTITIONING (the method is final
    // and it is just a simple return of this value.)
    UiBinderStructuredRegionProcessor reconciler = new UiBinderStructuredRegionProcessor();
    reconciler.setDocumentPartitioning(IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING);
    return reconciler;
  }

  private IReconcilingStrategy spellcheckStrategy;

  /*
   * Derived from super's implementation.
   */
  @Override
  protected IReconcilingStrategy getSpellcheckStrategy() {
    if (spellcheckStrategy == null) {
      String contentTypeId = getContentType(getDocument());
      if (contentTypeId != null) {
        if (getTextViewer() instanceof ISourceViewer) {
          ISourceViewer viewer = (ISourceViewer) getTextViewer();
          spellcheckStrategy = new UiBinderSpellcheckStrategy(viewer,
              contentTypeId);
          spellcheckStrategy.setDocument(getDocument());
        }
      }
    }
    return spellcheckStrategy;
  }
}
