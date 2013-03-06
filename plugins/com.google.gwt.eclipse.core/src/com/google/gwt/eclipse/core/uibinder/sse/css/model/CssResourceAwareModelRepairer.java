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

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.wst.css.core.internal.formatter.CSSSourceFormatter;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.util.AbstractCssTraverser;
import org.eclipse.wst.sse.core.internal.parser.ContextRegion;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.stylesheets.MediaList;

import java.lang.reflect.Method;

/**
 * Repairs the CSS model of CSS Resource custom at-rules. The
 * {@link CssResourceAwareTokenizer} returns "@media" for these custom at-rules
 * which allows the WST CSS implementation deal with most of them, but there is
 * still some additional repairing that must be done:
 * <ul>
 * <li>The "@else" and "@noflip" rules do not have any text following them (e.g.
 * "@if _____ {" vs. "@else {"), but the media rule parser expects at least one.
 * Since the media rule parser does not find one, it creates a media rule model
 * object that isn't properly initialized with a valid range. This class repairs
 * that range to allow the WST CSS implementation to deal with these rules.</li>
 * </ul>
 */
@SuppressWarnings("restriction")
public class CssResourceAwareModelRepairer {

  private class MyCssTraverser extends AbstractCssTraverser {

    @Override
    protected short preNode(ICSSNode node) {
      if (node.getNodeType() == ICSSNode.MEDIARULE_NODE) {
        fixPotentialEmptyMaskedMediaRule(node);
      }

      return TRAV_CONT;
    }
  }

  /**
   * If the media list of any of these rules is empty, the repairer will add a
   * dummy item to the list.
   */
  private static final String[] EMPTY_MASKED_MEDIA_RULES = {
      "@if", "@elif", "@else", "@noflip"};

  /**
   * Calls the {@link MediaList} <code>setRangeRegion</code> method.
   * 
   * @param mediaList the MediaList object that receives the call
   * @param structuredDocumentRegions the first parameter of the method
   * @param textRegion the second parameter of the method
   * @throws Throwable for safeguarding against any reflection issues (nothing
   *           is logged in this method since it doesn't have proper context of
   *           the scenario)
   */
  private static void callSetRangeRegion(MediaList mediaList,
      IStructuredDocumentRegion[] structuredDocumentRegions,
      ITextRegion textRegion) throws Throwable {
    ClassLoader classLoader = CSSSourceFormatter.class.getClassLoader();
    Class<?> cssRegionContainerClass = classLoader.loadClass("org.eclipse.wst.css.core.internal.document.CSSRegionContainer");
    Method declaredMethod = cssRegionContainerClass.getDeclaredMethod(
        "setRangeRegion", IStructuredDocumentRegion.class, ITextRegion.class,
        ITextRegion.class);
    declaredMethod.setAccessible(true);
    declaredMethod.invoke(mediaList, structuredDocumentRegions[0], textRegion,
        textRegion);
  }

  private final IStructuredDocument structuredDocument;

  private final ICSSModel cssModel;

  public CssResourceAwareModelRepairer(IStructuredDocument structuredDocument,
      ICSSModel cssModel) {
    this.structuredDocument = structuredDocument;
    this.cssModel = cssModel;
  }

  public void repair() {
    new MyCssTraverser().apply(cssModel);
  }

  private boolean containsEmptyMaskedMediaRule(CSSMediaRule mediaRule,
      IndexedRegion mediaRuleRegion) {
    for (String rule : EMPTY_MASKED_MEDIA_RULES) {
      try {
        if (structuredDocument.getLength() < rule.length()) {
          continue;
        }

        if (!rule.equalsIgnoreCase(structuredDocument.get(
            mediaRuleRegion.getStartOffset(), rule.length()))) {
          continue;
        }

        if (mediaRule.getMedia().getLength() > 0) {
          continue;
        }

        return true;
      } catch (BadLocationException e1) {
        // Shouldn't happen, continue on
      }
    }

    return false;
  }

  private void fixPotentialEmptyMaskedMediaRule(ICSSNode node) {
    CSSMediaRule mediaRule = (CSSMediaRule) node;
    IndexedRegion mediaRuleRegion = (IndexedRegion) mediaRule;

    if (!containsEmptyMaskedMediaRule(mediaRule, mediaRuleRegion)) {
      return;
    }

    // Set the range to a valid value (it won't be proper since we don't have
    // any additional words that can be categorized as CSS_MEDIUM.)
    MediaList mediaList = mediaRule.getMedia();
    IStructuredDocumentRegion[] structuredDocumentRegions = structuredDocument.getStructuredDocumentRegions(
        mediaRuleRegion.getStartOffset(), mediaRuleRegion.getLength());

    // The value we set is a 0-length region starting where the next word would
    // have been
    ITextRegion textRegion = new ContextRegion(CSSRegionContexts.CSS_MEDIUM,
        structuredDocumentRegions[0].getEndOffset()
            - structuredDocumentRegions[0].getStartOffset(), 0, 0);

    try {
      callSetRangeRegion(mediaList, structuredDocumentRegions, textRegion);
    } catch (Throwable e) {
      GWTPluginLog.logError(e, "Could not clean up the @else in the CSS model.");
    }
  }

}
