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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ui.viewers.BaseChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.ChangeListener;
import com.google.gdt.eclipse.core.ui.viewers.SelectableControlListContentFilter;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.googleapi.search.Match;
import com.google.gdt.googleapi.search.SearchTokenizer;
import com.google.gdt.googleapi.search.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchFilter filters APIs using a search query using naive search match
 * rules.
 */
public class ApiSearchFilter extends BaseChangeListener<ChangeListener>
    implements SelectableControlListContentFilter<ManagedApiEntry> {

  private List<Term> searchTerms = new ArrayList<Term>();

  public ManagedApiEntry apply(ManagedApiEntry element) {
    if (searchTerms.size() > 0) {
      Term[] terms = searchTerms.toArray(new Term[searchTerms.size()]);
      Match[] matches = element.match(terms);
      if (matches != null && matches.length >= 1) {
        return element;
      } else {
        return null;
      }
    } else {
      return element;
    }
  }

  public void setSearchQuery(String searchQuery) {
    List<Term> newSearchTerms = new ArrayList<Term>();
    newSearchTerms.addAll(SearchTokenizer.queryToTerms(searchQuery));
    if (!newSearchTerms.equals(searchTerms)) {
      searchTerms = newSearchTerms;
      fireChangeEvent();
    }
  }

}
