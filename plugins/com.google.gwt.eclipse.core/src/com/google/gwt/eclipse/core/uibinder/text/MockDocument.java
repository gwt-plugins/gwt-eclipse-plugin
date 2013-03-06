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
package com.google.gwt.eclipse.core.uibinder.text;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;

/**
 * An implementation of {@link IDocument} that throws
 * {@link UnsupportedMockOperationException} for all client calls.
 */
public abstract class MockDocument implements IDocument {

  /**
   * Exception thrown when an unsupported method is called on the
   * {@link MockDocument}.
   */
  @SuppressWarnings("serial")
  public static class UnsupportedMockOperationException extends
      RuntimeException {
    public UnsupportedMockOperationException() {
    }
  }

  public void addDocumentListener(IDocumentListener listener) {
    throw new UnsupportedMockOperationException();
  }

  public void addDocumentPartitioningListener(
      IDocumentPartitioningListener listener) {
    throw new UnsupportedMockOperationException();
  }

  public void addPosition(Position position) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public void addPosition(String category, Position position)
      throws BadLocationException, BadPositionCategoryException {
    throw new UnsupportedMockOperationException();
  }

  public void addPositionCategory(String category) {
    throw new UnsupportedMockOperationException();
  }

  public void addPositionUpdater(IPositionUpdater updater) {
    throw new UnsupportedMockOperationException();
  }

  public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
    throw new UnsupportedMockOperationException();
  }

  public int computeIndexInCategory(String category, int offset)
      throws BadLocationException, BadPositionCategoryException {
    throw new UnsupportedMockOperationException();
  }

  public int computeNumberOfLines(String text) {
    throw new UnsupportedMockOperationException();
  }

  public ITypedRegion[] computePartitioning(int offset, int length)
      throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public boolean containsPosition(String category, int offset, int length) {
    throw new UnsupportedMockOperationException();
  }

  public boolean containsPositionCategory(String category) {
    throw new UnsupportedMockOperationException();
  }

  public String get() {
    throw new UnsupportedMockOperationException();
  }

  public String get(int offset, int length) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public char getChar(int offset) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public String getContentType(int offset) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public IDocumentPartitioner getDocumentPartitioner() {
    throw new UnsupportedMockOperationException();
  }

  public String[] getLegalContentTypes() {
    throw new UnsupportedMockOperationException();
  }

  public String[] getLegalLineDelimiters() {
    throw new UnsupportedMockOperationException();
  }

  public int getLength() {
    throw new UnsupportedMockOperationException();
  }

  public String getLineDelimiter(int line) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public IRegion getLineInformation(int line) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public IRegion getLineInformationOfOffset(int offset)
      throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public int getLineLength(int line) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public int getLineOffset(int line) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public int getLineOfOffset(int offset) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public int getNumberOfLines() {
    throw new UnsupportedMockOperationException();
  }

  public int getNumberOfLines(int offset, int length)
      throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public ITypedRegion getPartition(int offset) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public String[] getPositionCategories() {
    throw new UnsupportedMockOperationException();
  }

  public Position[] getPositions(String category)
      throws BadPositionCategoryException {
    throw new UnsupportedMockOperationException();
  }

  public IPositionUpdater[] getPositionUpdaters() {
    throw new UnsupportedMockOperationException();
  }

  public void insertPositionUpdater(IPositionUpdater updater, int index) {
    throw new UnsupportedMockOperationException();
  }

  public void removeDocumentListener(IDocumentListener listener) {
    throw new UnsupportedMockOperationException();
  }

  public void removeDocumentPartitioningListener(
      IDocumentPartitioningListener listener) {
    throw new UnsupportedMockOperationException();
  }

  public void removePosition(Position position) {
    throw new UnsupportedMockOperationException();
  }

  public void removePosition(String category, Position position)
      throws BadPositionCategoryException {
    throw new UnsupportedMockOperationException();
  }

  public void removePositionCategory(String category)
      throws BadPositionCategoryException {
    throw new UnsupportedMockOperationException();
  }

  public void removePositionUpdater(IPositionUpdater updater) {
    throw new UnsupportedMockOperationException();
  }

  public void removePrenotifiedDocumentListener(
      IDocumentListener documentAdapter) {
    throw new UnsupportedMockOperationException();
  }

  public void replace(int offset, int length, String text)
      throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public int search(int startOffset, String findString, boolean forwardSearch,
      boolean caseSensitive, boolean wholeWord) throws BadLocationException {
    throw new UnsupportedMockOperationException();
  }

  public void set(String text) {
    throw new UnsupportedMockOperationException();
  }

  public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
    throw new UnsupportedMockOperationException();
  }

}
