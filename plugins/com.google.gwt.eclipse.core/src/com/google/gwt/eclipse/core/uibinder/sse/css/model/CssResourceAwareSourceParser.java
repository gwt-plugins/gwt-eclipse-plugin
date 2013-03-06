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

import org.eclipse.wst.css.core.internal.parser.CSSSourceParser;
import org.eclipse.wst.css.core.internal.parser.ICSSTokenizer;
import org.eclipse.wst.sse.core.internal.ltk.parser.RegionParser;

/**
 * Constructs a list of structured document regions from the tokenizer. This
 * version is CSS Resource-aware, meaning it can handle the custom CSS Resource
 * at-rules.
 * <p>
 * This is required for the {@link CssResourceAwareTokenizer} to be used.
 */
@SuppressWarnings("restriction")
public class CssResourceAwareSourceParser extends CSSSourceParser {
  private ICSSTokenizer tokenizer;

  /*
   * Derived from CSSSourceParser's implementation.
   */
  @Override
  public ICSSTokenizer getTokenizer() {
    if (tokenizer == null) {
      tokenizer = new CssResourceAwareTokenizer();
    }

    return tokenizer;
  }

  /*
   * Derived from CSSSourceParser's implementation.
   */
  @Override
  public RegionParser newInstance() {
    return new CssResourceAwareSourceParser();
  }

}
