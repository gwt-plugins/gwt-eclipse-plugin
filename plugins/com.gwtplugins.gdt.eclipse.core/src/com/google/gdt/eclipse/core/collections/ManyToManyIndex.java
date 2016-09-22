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

import java.util.Collections;
import java.util.Set;

/**
 * Generic many-to-many index that stores directed links (e.g. dependencies)
 * between objects of two distinct types (known as the "left" type and "right"
 * type). Any element on the left can link to any number of right elements, and
 * any right element can link to any number of left elements. Lookup is equally
 * fast from either side.
 * 
 * @param <L> type of objects on left side of graph
 * @param <R> type of objects on right side of graph
 */
public class ManyToManyIndex<L, R> {

  private final OneToManyIndex<L, R> leftIndex;

  private final OneToManyIndex<R, L> rightIndex;

  public ManyToManyIndex() {
    this.leftIndex = new OneToManyIndex<L, R>();
    this.rightIndex = new OneToManyIndex<R, L>();
  }

  public ManyToManyIndex(ManyToManyIndex<L, R> original) {
    this.leftIndex = new OneToManyIndex<L, R>(original.leftIndex);
    this.rightIndex = new OneToManyIndex<R, L>(original.rightIndex);
  }

  public void addLeftToRight(L key, R element) {
    leftIndex.addElement(key, element);
    rightIndex.addElement(element, key);
  }

  public void addRightToLeft(R key, L element) {
    rightIndex.addElement(key, element);
    leftIndex.addElement(element, key);
  }

  public void clear() {
    leftIndex.clear();
    rightIndex.clear();
  }

  public Set<L> getAllLeftElements() {
    return Collections.unmodifiableSet(leftIndex.keys());
  }

  public Set<R> getAllRightElements() {
    return Collections.unmodifiableSet(rightIndex.keys());
  }

  public Set<L> getLeftElements(R key) {
    return Collections.unmodifiableSet(rightIndex.getElements(key));
  }

  public Set<R> getRightElements(L key) {
    return Collections.unmodifiableSet(leftIndex.getElements(key));
  }

  public void putLeftToManyRights(L key, Set<R> elements) {
    leftIndex.putElements(key, elements);
    for (R element : elements) {
      rightIndex.addElement(element, key);
    }
  }

  public void putRightToManyLefts(R key, Set<L> elements) {
    rightIndex.putElements(key, elements);
    for (L element : elements) {
      leftIndex.addElement(element, key);
    }
  }

  public void removeLeftElement(L key) {
    leftIndex.removeKey(key);
    rightIndex.removeElement(key);
  }

  public void removeRightElement(R key) {
    rightIndex.removeKey(key);
    leftIndex.removeElement(key);
  }

}