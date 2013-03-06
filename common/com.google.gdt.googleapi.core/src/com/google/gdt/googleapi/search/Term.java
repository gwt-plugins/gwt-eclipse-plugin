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

/**
 * A search Term representing an individual search-point. The term is normalized
 * by converting it to lower case.
 * 
 * Any sequence of characters can act as a search term, but typically this type
 * represents a word (e.g. "Maps" normalized to "maps").
 */
public class Term {
  private final String text;

  public Term(String text) {
    if (text != null) {
      this.text = text.toLowerCase();
    } else {
      this.text = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (obj instanceof Term && getClass() != obj.getClass())
      return false;
    Term other = (Term) obj;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    return true;
  }

  public String getText() {
    return text;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    return result;
  }
}
