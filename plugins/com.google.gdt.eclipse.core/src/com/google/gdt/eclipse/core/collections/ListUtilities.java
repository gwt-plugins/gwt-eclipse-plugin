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
package com.google.gdt.eclipse.core.collections;

import java.util.List;

/**
 * Utility methods for lists.
 */
public final class ListUtilities {

  /**
   * A {@link List#indexOf(Object)} which begins the search at a particular
   * index.
   */
  public static <T> int indexOf(List<T> list, T string,
      int beginIndex) {
    for (int i = beginIndex; i < list.size(); i++) {
      if (string.equals(list.get(i))) {
        return i;
      }
    }
  
    return -1;
  }

  private ListUtilities() {
  }
}
