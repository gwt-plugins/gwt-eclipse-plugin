/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.editors;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

/**
 * Facilitates definition of a {@link SourceViewerConfiguration} sublcass as a piecemeal
 * customization of the behavior of a specified {@link SourceViewerConfiguration} delegate. This
 * class is declared abstract because it only provides added value when it is extended. All methods
 * not overridden in the extension inherit their behavior from the specified delegate.
 */
@SuppressWarnings("deprecation") // org.eclipse.jface.text.IAutoIndentStrategy
public abstract class DelegatingSourceViewerConfiguration extends SourceViewerConfiguration {
  
  private SourceViewerConfiguration delegate;

  public DelegatingSourceViewerConfiguration(SourceViewerConfiguration delegate) {
    this.delegate = delegate;
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public int getTabWidth(ISourceViewer sourceViewer) {
    return delegate.getTabWidth(sourceViewer);
  }

  @Override public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
    return delegate.getUndoManager(sourceViewer);
  }

  @Override public IReconciler getReconciler(ISourceViewer sourceViewer) {
    return delegate.getReconciler(sourceViewer);
  }

  @Override public IPresentationReconciler getPresentationReconciler(
      ISourceViewer sourceViewer) {
    return delegate.getPresentationReconciler(sourceViewer);
  }

  @Override public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
    return delegate.getContentFormatter(sourceViewer);
  }

  @Override public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
    return delegate.getContentAssistant(sourceViewer);
  }

  @Override public IQuickAssistAssistant getQuickAssistAssistant(
      ISourceViewer sourceViewer) {
    return delegate.getQuickAssistAssistant(sourceViewer);
  }

  @Override public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer,
      String contentType) {
    return delegate.getAutoIndentStrategy(sourceViewer, contentType);
  }

  @Override public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer,
      String contentType) {
    return delegate.getAutoEditStrategies(sourceViewer, contentType);
  }

  @Override public String[] getDefaultPrefixes(ISourceViewer sourceViewer,
      String contentType) {
    return delegate.getDefaultPrefixes(sourceViewer, contentType);
  }

  @Override public ITextDoubleClickStrategy getDoubleClickStrategy(
      ISourceViewer sourceViewer, String contentType) {
    return delegate.getDoubleClickStrategy(sourceViewer, contentType);
  }

  @Override public String[] getIndentPrefixes(ISourceViewer sourceViewer,
      String contentType) {
    return delegate.getIndentPrefixes(sourceViewer, contentType);
  }

  @Override public String toString() {
    return delegate.toString();
  }

  @Override public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
    return delegate.getAnnotationHover(sourceViewer);
  }

  @Override public IAnnotationHover getOverviewRulerAnnotationHover(
      ISourceViewer sourceViewer) {
    return delegate.getOverviewRulerAnnotationHover(sourceViewer);
  }

  @Override public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer,
      String contentType) {
    return delegate.getConfiguredTextHoverStateMasks(sourceViewer, contentType);
  }

  @Override public ITextHover getTextHover(ISourceViewer sourceViewer,
      String contentType, int stateMask) {
    return delegate.getTextHover(sourceViewer, contentType, stateMask);
  }

  @Override public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
    return delegate.getTextHover(sourceViewer, contentType);
  }

  @Override public IInformationControlCreator getInformationControlCreator(
      ISourceViewer sourceViewer) {
    return delegate.getInformationControlCreator(sourceViewer);
  }

  @Override public IInformationPresenter getInformationPresenter(
      ISourceViewer sourceViewer) {
    return delegate.getInformationPresenter(sourceViewer);
  }

  @Override public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return delegate.getConfiguredContentTypes(sourceViewer);
  }

  @Override public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
    return delegate.getConfiguredDocumentPartitioning(sourceViewer);
  }

  @Override public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
    return delegate.getHyperlinkDetectors(sourceViewer);
  }

  @Override public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
    return delegate.getHyperlinkPresenter(sourceViewer);
  }

  @Override public int getHyperlinkStateMask(ISourceViewer sourceViewer) {
    return delegate.getHyperlinkStateMask(sourceViewer);
  }

}
