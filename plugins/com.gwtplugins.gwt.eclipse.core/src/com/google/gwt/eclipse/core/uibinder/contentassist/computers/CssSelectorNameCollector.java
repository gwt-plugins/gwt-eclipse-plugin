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
package com.google.gwt.eclipse.core.uibinder.contentassist.computers;

import com.google.gwt.eclipse.core.uibinder.sse.css.CssModelUtilities;
import com.google.gwt.uibinder.attributeparsers.CssNameConverter;
import com.google.gwt.uibinder.attributeparsers.CssNameConverter.Failure;

import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPageRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelector;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelectorItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSelectorList;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSSimpleSelector;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;

import java.util.HashSet;
import java.util.Set;

/**
 * A CSS node visitor that collects selector names.
 */
@SuppressWarnings("restriction")
public class CssSelectorNameCollector implements
    CssModelUtilities.CssNodeVisitor {

  /**
   * @return a non-null error message for the duplicate selector names, or null
   *         if there are no duplicates.
   */
  public static String getDuplicateSelectorNamesErrorMessage(ICSSNode node) {
    Set<String> literalSelectorNames = getLiteralSelectorNames(node);
    CssNameConverter cssNameConverter = new CssNameConverter();
    try {
      cssNameConverter.convertSet(literalSelectorNames);
      return null;
    } catch (Failure e) {
      return e.getMessage();
    }    
  }

  /**
   * Returns the selector names as they appear in the CSS model.
   */
  public static Set<String> getLiteralSelectorNames(ICSSNode node) {
    CssSelectorNameCollector collector = new CssSelectorNameCollector();
    CssModelUtilities.visitCssNodes(node, collector);
    return collector.getSelectorNames();
  }

  /**
   * Returns all valid selector names for the selectors in the CSS model. For
   * example, a selector "pretty-text" would result in a set of "pretty-text"
   * and "prettyText".
   */
  public static Set<String> getValidSelectorNames(ICSSNode node) {
    Set<String> literalSelectorNames = getLiteralSelectorNames(node);
    Set<String> validSelectorNames = new HashSet<String>(literalSelectorNames);
    CssNameConverter cssNameConverter = new CssNameConverter();

    for (String selectorName : literalSelectorNames) {
      validSelectorNames.add(cssNameConverter.convertName(selectorName));
    }

    return validSelectorNames;
  }

  private final Set<String> selectorNames = new HashSet<String>();

  public void visitCssNode(ICSSNode cssNode) {
    ICSSSelectorList selectors;
    switch (cssNode.getNodeType()) {
      case ICSSNode.STYLERULE_NODE:
        selectors = ((ICSSStyleRule) cssNode).getSelectors();
        break;

      case ICSSNode.PAGERULE_NODE:
        // @Sprite is masked as a page rule
        selectors = ((ICSSPageRule) cssNode).getSelectors();
        break;

      default:
        // Not a node type that contains selectors
        return;
    }

    for (int selectorPos = 0; selectorPos < selectors.getLength(); selectorPos++) {
      ICSSSelector selector = selectors.getSelector(selectorPos);
      for (int itemPos = 0; itemPos < selector.getLength(); itemPos++) {
        ICSSSelectorItem item = selector.getItem(itemPos);
        if (item.getItemType() == ICSSSelectorItem.SIMPLE) {
          ICSSSimpleSelector simpleItem = (ICSSSimpleSelector) item;
          for (int classPos = 0; classPos < simpleItem.getNumOfClasses(); classPos++) {
            String name = simpleItem.getClass(classPos);
            selectorNames.add(name);
          }
        }
      }
    }
  }

  private Set<String> getSelectorNames() {
    return selectorNames;
  }
}

