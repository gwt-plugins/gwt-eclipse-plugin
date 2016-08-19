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

import org.eclipse.wst.css.core.internal.encoding.CSSDocumentLoader;
import org.eclipse.wst.sse.core.internal.document.IDocumentLoader;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;

/**
 * Constructs a structured model for CSS, but is CSS Resource-aware.
 * <p>
 * This is required to have WST use the CSS Resource-aware source parser
 * {@link CssResourceAwareSourceParser}.
 */
@SuppressWarnings("restriction")
public class CssResourceAwareDocumentLoader extends CSSDocumentLoader {

  /*
   * Derived from CSSDocumentLoader's implementation.
   */
  @Override
  public RegionParser getParser() {
    return new CssResourceAwareSourceParser();
  }

  /*
   * Derived from CSSDocumentLoader's implementation.
   */
  @Override
  public IDocumentLoader newInstance() {
    return new CssResourceAwareDocumentLoader();
  }

}
