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
package com.google.gwt.eclipse.core.uibinder.sse.css.model;

import org.eclipse.wst.css.core.internal.modelhandler.ModelHandlerForCSS;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;

/**
 * Entry point for generating structured models and documents from
 * CssResource-aware CSS files. This is the default handler for these files, set
 * via the "org.eclipse.wst.sse.core.modelHandler" extension point.
 */
@SuppressWarnings("restriction")
public class ModelHandlerForCssResource extends ModelHandlerForCSS {

  /*
   * Derived from ModelHandlerForCSS's implementation.
   */
  @Override
  public IDocumentLoader getDocumentLoader() {
    return new CssResourceAwareDocumentLoader();
  }

  /*
   * Derived from ModelHandlerForCSS's implementation.
   */
  @Override
  public IModelLoader getModelLoader() {
    return new CssResourceAwareModelLoader();
  }
}
