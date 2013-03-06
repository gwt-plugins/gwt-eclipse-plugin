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
package com.google.gwt.eclipse.core.uibinder.contentassist;

import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEventConsumer;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

/**
 * An {@link ITextViewer} implementation that delegates to an existing
 * {@link ITextViewer}.
 */
@SuppressWarnings("deprecation")
public class DelegatingTextViewer implements ITextViewer {
  private final ITextViewer originalTextViewer;

  public DelegatingTextViewer(ITextViewer originalTextViewer) {
    this.originalTextViewer = originalTextViewer;
  }

  public void activatePlugins() {
    originalTextViewer.activatePlugins();
  }

  public void addTextInputListener(ITextInputListener listener) {
    originalTextViewer.addTextInputListener(listener);
  }

  public void addTextListener(ITextListener listener) {
    originalTextViewer.addTextListener(listener);
  }

  public void addViewportListener(IViewportListener listener) {
    originalTextViewer.addViewportListener(listener);
  }

  public void changeTextPresentation(TextPresentation presentation,
      boolean controlRedraw) {
    originalTextViewer.changeTextPresentation(presentation, controlRedraw);
  }

  public int getBottomIndex() {
    return originalTextViewer.getBottomIndex();
  }

  public int getBottomIndexEndOffset() {
    return originalTextViewer.getBottomIndexEndOffset();
  }

  public IDocument getDocument() {
    return originalTextViewer.getDocument();
  }

  public IFindReplaceTarget getFindReplaceTarget() {
    return originalTextViewer.getFindReplaceTarget();
  }

  public Point getSelectedRange() {
    return originalTextViewer.getSelectedRange();
  }

  public ISelectionProvider getSelectionProvider() {
    return originalTextViewer.getSelectionProvider();
  }

  public ITextOperationTarget getTextOperationTarget() {
    return originalTextViewer.getTextOperationTarget();
  }

  public StyledText getTextWidget() {
    return originalTextViewer.getTextWidget();
  }

  public int getTopIndex() {
    return originalTextViewer.getTopIndex();
  }

  public int getTopIndexStartOffset() {
    return originalTextViewer.getTopIndexStartOffset();
  }

  public int getTopInset() {
    return originalTextViewer.getTopInset();
  }

  public IRegion getVisibleRegion() {
    return originalTextViewer.getVisibleRegion();
  }

  public void invalidateTextPresentation() {
    originalTextViewer.invalidateTextPresentation();
  }

  public boolean isEditable() {
    return originalTextViewer.isEditable();
  }

  public boolean overlapsWithVisibleRegion(int offset, int length) {
    return originalTextViewer.overlapsWithVisibleRegion(offset, length);
  }

  public void removeTextInputListener(ITextInputListener listener) {
    originalTextViewer.removeTextInputListener(listener);
  }

  public void removeTextListener(ITextListener listener) {
    originalTextViewer.removeTextListener(listener);
  }

  public void removeViewportListener(IViewportListener listener) {
    originalTextViewer.removeViewportListener(listener);
  }

  public void resetPlugins() {
    originalTextViewer.resetPlugins();
  }

  public void resetVisibleRegion() {
    originalTextViewer.resetVisibleRegion();
  }

  public void revealRange(int offset, int length) {
    originalTextViewer.revealRange(offset, length);
  }

  public void setAutoIndentStrategy(IAutoIndentStrategy strategy,
      String contentType) {
    originalTextViewer.setAutoIndentStrategy(strategy, contentType);
  }

  public void setDefaultPrefixes(String[] defaultPrefixes, String contentType) {
    originalTextViewer.setDefaultPrefixes(defaultPrefixes, contentType);
  }

  public void setDocument(IDocument document) {
    originalTextViewer.setDocument(document);
  }

  public void setDocument(IDocument document, int modelRangeOffset,
      int modelRangeLength) {
    originalTextViewer.setDocument(document, modelRangeOffset, modelRangeLength);
  }

  public void setEditable(boolean editable) {
    originalTextViewer.setEditable(editable);
  }

  public void setEventConsumer(IEventConsumer consumer) {
    originalTextViewer.setEventConsumer(consumer);
  }

  public void setIndentPrefixes(String[] indentPrefixes, String contentType) {
    originalTextViewer.setIndentPrefixes(indentPrefixes, contentType);
  }

  public void setSelectedRange(int offset, int length) {
    originalTextViewer.setSelectedRange(offset, length);
  }

  public void setTextColor(Color color) {
    originalTextViewer.setTextColor(color);
  }

  public void setTextColor(Color color, int offset, int length,
      boolean controlRedraw) {
    originalTextViewer.setTextColor(color, offset, length, controlRedraw);
  }

  public void setTextDoubleClickStrategy(ITextDoubleClickStrategy strategy,
      String contentType) {
    originalTextViewer.setTextDoubleClickStrategy(strategy, contentType);
  }

  public void setTextHover(ITextHover textViewerHover, String contentType) {
    originalTextViewer.setTextHover(textViewerHover, contentType);
  }

  public void setTopIndex(int index) {
    originalTextViewer.setTopIndex(index);
  }

  public void setUndoManager(IUndoManager undoManager) {
    originalTextViewer.setUndoManager(undoManager);
  }

  public void setVisibleRegion(int offset, int length) {
    originalTextViewer.setVisibleRegion(offset, length);
  }

}
