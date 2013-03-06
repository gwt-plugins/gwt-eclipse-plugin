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
package com.google.gwt.eclipse.core.uibinder.sse.css;

import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelRepairer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.css.core.internal.modelhandler.CSSModelLoader;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;

/**
 * Extracts a block of CSS from a document (can be any {@link IDocument}
 * implementation) and generates a CSS model and its associated documents.
 */
@SuppressWarnings("restriction")
public class CssExtractor {

  /**
   * Extracts a CSS block from a document and creates the CSS model and
   * documents.
   * <p>
   * This requires a model loader to create the model, instead of taking in an
   * empty model. The reason is, there are multiple ways to create a model, but
   * we need a way that also creates the associated documents. The model loader
   * is guaranteed to do this, which is why we specifically take this as a
   * parameter.
   * 
   * @param cssModelLoader a model loader used to create the empty model (see
   *          {@link com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareModelLoader}
   *          )
   * @return a container for the model and associated documents, or null if it
   *         could not be successfully extracted
   */
  public static CssExtractor extract(IDocument document, int offset,
      int length, CSSModelLoader cssModelLoader) {
    try {
      String cssBlock = document.get(offset, length);
      return extract(cssBlock, cssModelLoader);
    } catch (BadLocationException e) {
      // Likely not to happen, but in case it does, return null
      return null;
    }
  }

  /**
   * @see #extract(IDocument, int, int, CSSModelLoader)
   */
  public static CssExtractor extract(String cssBlock,
      CSSModelLoader cssModelLoader) {
    // Create an empty model, and its associated documents
    ICSSModel model = (ICSSModel) cssModelLoader.createModel();
    ICSSDocument cssDocument = model.getDocument();
    IStructuredDocument structuredDocument = model.getStructuredDocument();

    // Copy the contents
    structuredDocument.set(cssBlock);

    // Fix CSS Resource custom at-rules
    CssResourceAwareModelRepairer modelRepairer = new CssResourceAwareModelRepairer(
        structuredDocument, model);
    modelRepairer.repair();

    return new CssExtractor(cssDocument, structuredDocument, model);
  }

  private final ICSSDocument cssDocument;
  private final IStructuredDocument structuredDocument;
  private final ICSSModel cssModel;

  private CssExtractor(ICSSDocument cssDocument,
      IStructuredDocument structuredDocument, ICSSModel cssModel) {
    this.cssDocument = cssDocument;
    this.structuredDocument = structuredDocument;
    this.cssModel = cssModel;
  }

  public ICSSDocument getCssDocument() {
    return cssDocument;
  }

  public ICSSModel getCssModel() {
    return cssModel;
  }

  public IStructuredDocument getStructuredDocument() {
    return structuredDocument;
  }
}
