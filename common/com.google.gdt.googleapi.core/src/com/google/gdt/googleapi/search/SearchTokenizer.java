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
package com.google.gdt.googleapi.search;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A simple tokenizer to turn search queries (e.g. an unqualified string) into
 * search terms (normalized, individual terms to search against).
 */
public class SearchTokenizer {

  public static Collection<Term> queryToTerms(String query) {
    return (new SearchTokenizer(query)).getTerms();
  }

  private final String searchQuery;

  public SearchTokenizer(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  public Collection<Term> getTerms() {
    List<Term> terms = new ArrayList<Term>();
    BreakIterator bi = BreakIterator.getWordInstance();
    bi.setText(searchQuery);
    int start = bi.first();
    int end = start;
    while ((end = bi.next()) != BreakIterator.DONE) {
      if (end - start > 0
          && Character.isLetterOrDigit(searchQuery.charAt(start))) {
        terms.add(new Term(searchQuery.substring(start, end)));
      }
      start = end;
    }
    return terms;
  }
}
