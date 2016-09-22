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
package com.google.gwt.eclipse.core.formatter;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import java.util.LinkedList;

/**
 * A base formatting strategy class that reduces the boilerplate by passing the
 * document and partition to the format method.
 */
public abstract class AbstractFormattingStrategy extends
    ContextBasedFormattingStrategy {

  /**
   * Documents to be formatted by this strategy.
   */
  private final LinkedList<IDocument> documents = new LinkedList<IDocument>();

  /**
   * Partitions to be formatted by this strategy.
   */
  private final LinkedList<TypedPosition> partitions = new LinkedList<TypedPosition>();

  @Override
  public void format() {
    super.format();

    IDocument document = documents.removeFirst();
    TypedPosition partition = partitions.removeFirst();

    if (document == null || partition == null) {
      return;
    }

    format(document, partition);
  }

  @Override
  public void formatterStarts(IFormattingContext context) {
    super.formatterStarts(context);

    IDocument document = (IDocument) context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM);
    TypedPosition position = (TypedPosition) context.getProperty(FormattingContextProperties.CONTEXT_PARTITION);

    partitions.addLast(position);
    documents.addLast(document);
  }

  @Override
  public void formatterStops() {
    super.formatterStops();

    partitions.clear();
    documents.clear();
  }

  protected abstract void format(IDocument document, TypedPosition partition);

}
