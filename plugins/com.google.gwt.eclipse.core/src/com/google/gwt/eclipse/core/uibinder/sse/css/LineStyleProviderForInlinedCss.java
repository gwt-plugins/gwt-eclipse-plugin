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

import com.google.gdt.eclipse.core.SseUtilities;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.uibinder.sse.css.model.CssResourceAwareTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.wst.css.core.internal.parserz.CSSTextToken;
import org.eclipse.wst.css.ui.internal.style.LineStyleProviderForCSS;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Provides line styling for inlined CSS.
 * <p>
 * This class is derived from
 * {@link org.eclipse.wst.css.ui.internal.style.LineStyleProviderForEmbeddedCSS}
 * . The major difference is this uses the {@link CssResourceAwareTokenizer}.
 */
@SuppressWarnings({"restriction", "unchecked"})
public class LineStyleProviderForInlinedCss extends LineStyleProviderForCSS {

  @Override
  public boolean prepareRegions(ITypedRegion typedRegion, int lineRequestStart,
      int lineRequestLength, Collection holdResults) {

    ITypedRegion partition = SseUtilities.getPartition(getDocument(),
        typedRegion.getOffset());

    int regionStart = partition.getOffset();
    int regionEnd = regionStart + partition.getLength();

    List tokens;
    String content;
    try {
      content = getDocument().get(partition.getOffset(), partition.getLength());
    } catch (BadLocationException e1) {
      GWTPluginLog.logWarning(e1,
          "Unexpected bad location while highlighting a CSS region.");
      return false;
    }

    CssResourceAwareTokenizer t = new CssResourceAwareTokenizer(
        new StringReader(content));
    try {
      tokens = t.parseText();
    } catch (IOException e) {
      return false;
    }

    boolean result = false;

    if (0 < tokens.size()) {
      int start = regionStart;
      int end = start;
      Iterator i = tokens.iterator();
      while (i.hasNext()) {
        CSSTextToken token = (CSSTextToken) i.next();
        end = start + token.length;
        int styleLength = token.length;
        /* The token starts in the region */
        if (regionStart <= start && start < regionEnd) {
          /*
           * [239415] The region may not span the total length of the token -
           * Adjust the length so that it doesn't overlap with other style
           * ranges
           */
          if (regionEnd < end) {
            styleLength = regionEnd - start;
          }

          // We should not add style ranges outside the region we have been
          // called for.
          if (isContained(typedRegion, start, styleLength)) {
            addStyleRange(holdResults, getAttributeFor(token.kind), start,
                styleLength);
          }
        } else if (start <= regionStart && regionStart < end) {
          /* The region starts in the token */
          /* The token may not span the total length of the region */
          if (end < regionEnd) {
            styleLength = end - regionStart;
          }

          if (isContained(typedRegion, regionStart, styleLength)) {
            addStyleRange(holdResults, getAttributeFor(token.kind),
                regionStart, styleLength);
          }
        }
        start += token.length;
      }
      result = true;
    }

    return result;
  }

  @Override
  protected TextAttribute getAttributeFor(ITextRegion region) {
    return null;
  }

  private void addStyleRange(Collection holdResults, TextAttribute attribute,
      int start, int end) {
    if (attribute != null) {
      holdResults.add(new StyleRange(start, end, attribute.getForeground(),
          attribute.getBackground(), attribute.getStyle()));
    } else {
      holdResults.add(new StyleRange(start, end, null, null));
    }
  }

  private boolean isContained(ITypedRegion region, int position, int length) {
    return region.getOffset() <= position
        && position + length <= region.getOffset() + region.getLength();
  }
}
