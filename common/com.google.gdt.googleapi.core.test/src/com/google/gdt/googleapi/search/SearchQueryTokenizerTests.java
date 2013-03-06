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

import junit.framework.Assert;

import org.junit.Test;

import java.util.Collection;

/**
 * TODO: doc me.
 */
public class SearchQueryTokenizerTests {

  @Test
  public void testSearchQueryTokenizer() {
    Collection<Term> t1 = SearchTokenizer.queryToTerms("test");
    Assert.assertTrue(t1.size() == 1);

    Collection<Term> t2 = SearchTokenizer.queryToTerms("test1 test2");
    Assert.assertTrue(t2.size() == 2);

    Collection<Term> t3 = SearchTokenizer.queryToTerms("\"test1 test2\"");
    Assert.assertTrue(t3.size() == 2);

    Collection<Term> t4 = SearchTokenizer.queryToTerms("test*1 test?2");
    Assert.assertTrue(t4.size() == 4);

    Collection<Term> t5 = SearchTokenizer.queryToTerms("!@#$%^&");
    Assert.assertTrue(t5.size() == 0);
  }
}
