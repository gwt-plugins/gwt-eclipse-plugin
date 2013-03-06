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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Since we need to keep modifications to index and reverseIndex atomic,
 * synchronize on the current instance of this class when touching either index
 * or reverseIndex. Those individual maps have not been made thread-safe since
 * they are not exposed to anything outside of this class, and this class has
 * its own mechanism for thread-safety.
 */
/**
 * Generic index that maps single objects to sets of another type of object. The
 * set of elements for a particular key is guaranteed to contain unique items;
 * if an element is added which already exists, it is a no-op. Lookup is equally
 * fast from either direction (i.e. key to elements vs. element to keys).
 * <p>
 * This class is fully thread-safe.
 * 
 * @param <K> the type of the key objects
 * @param <E> the type of the element objects
 */
public class OneToManyIndex<K, E> {

  private final Map<K, Set<E>> index = new HashMap<K, Set<E>>();

  private final Map<E, Set<K>> reverseIndex = new HashMap<E, Set<K>>();

  public OneToManyIndex() {
  }

  public OneToManyIndex(OneToManyIndex<K, E> original) {
    synchronized (this) {
      for (K key : original.keys()) {
        putElements(key, original.getElements(key));
      }
    }
  }

  /**
   * Adds an element to the set for a particular key.
   */
  public void addElement(K key, E element) {
    synchronized (this) {
      // If the key is already indexed, just add this element to its set
      if (index.containsKey(key)) {
        index.get(key).add(element);
      } else {
        // Otherwise, insert a new key in the index with this element
        Set<E> elements = new HashSet<E>(Collections.singleton(element));
        index.put(key, elements);
      }

      // Update the reverse index in the same way
      if (reverseIndex.containsKey(element)) {
        reverseIndex.get(element).add(key);
      } else {
        Set<K> keys = new HashSet<K>(Collections.singleton(key));
        reverseIndex.put(element, keys);
      }
    }
  }

  public void clear() {
    synchronized (this) {
      index.clear();
      reverseIndex.clear();
    }
  }

  /**
   * Returns a new set of elements contained in this index.
   */
  public Set<E> elements() {
    synchronized (this) {
      return new HashSet<E>(reverseIndex.keySet());
    }
  }

  /**
   * Gets a new set of elements for the given key.
   * 
   * @param key the key whose elements should be returned
   * @return a non-null set of elements
   */
  public Set<E> getElements(K key) {
    synchronized (this) {
      Set<E> elements = index.get(key);
      if (elements != null) {
        return new HashSet<E>(elements);
      }
      return Collections.emptySet();
    }
  }

  /**
   * Returns a new set containing the keys which map to the given element.
   */
  public Set<K> getKeys(E element) {
    synchronized (this) {
      Set<K> keys = reverseIndex.get(element);
      if (keys != null) {
        return new HashSet<K>(keys);
      }
      return Collections.emptySet();
    }
  }

  public boolean hasElement(E element) {
    synchronized (this) {
      return reverseIndex.containsKey(element);
    }
  }

  public boolean hasKey(K key) {
    synchronized (this) {
      return index.containsKey(key);
    }
  }

  /**
   * Returns a new set containing the keys of this index.
   */
  public Set<K> keys() {
    synchronized (this) {
      return new HashSet<K>(index.keySet());
    }
  }

  /**
   * Sets the elements corresponding to a particular key. The existing set for
   * that key (if it exists) is cleared.
   */
  public void putElements(K key, Set<E> elements) {
    synchronized (this) {
      // Clear any existing index entries
      removeKey(key);

      // Only add the key if it has elements; this ensures we don't "pollute"
      // the index with a bunch of keys with no elements.
      if (!elements.isEmpty()) {
        for (E element : elements) {
          addElement(key, element);
        }
      }
    }
  }

  public void removeElement(E element) {
    synchronized (this) {
      // Remove from the main index
      for (Set<E> elements : index.values()) {
        elements.remove(element);
      }

      // Remove from the reverse index
      reverseIndex.remove(element);
    }
  }

  public void removeKey(K key) {
    synchronized (this) {
      // Remove from the main index
      index.remove(key);

      // Remove from the reverse index
      for (Set<K> keys : reverseIndex.values()) {
        keys.remove(key);
      }
    }
  }

}