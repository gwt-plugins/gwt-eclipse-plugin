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
package com.google.gdt.eclipse.core.sdk;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sdk {@link java.util.Set Set} where element equality is determined solely by
 * {@link Sdk#getName()} and the insertion order is maintained.
 * 
 * @param <E> sdk type
 */
public class SdkSet<E extends Sdk> extends AbstractSet<E> {
  private E defaultSdk;
  private final Map<String, E> sdks;

  public SdkSet() {
    sdks = new LinkedHashMap<String, E>();
  }

  public SdkSet(SdkSet<E> setToCopy) {
    defaultSdk = setToCopy.defaultSdk;
    sdks = new LinkedHashMap<String, E>(setToCopy.sdks);
  }

  @Override
  public boolean add(E e) {
    return sdks.put((String) getKey(e), e) == null;
  }

  @Override
  public void clear() {
    sdks.clear();
  }

  @Override
  public boolean contains(Object o) {
    return sdks.containsKey(getKey(o));
  }

  public E findSdk(String name) {
    return sdks.get(name);
  }

  public E getDefault() {
    E sdk = sdks.get(getKey(defaultSdk));
    if (sdk == null && !sdks.isEmpty()) {
      sdk = sdks.values().iterator().next();
    }

    return sdk;
  }

  @Override
  public boolean isEmpty() {
    return sdks.isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return sdks.values().iterator();
  }

  @Override
  public boolean remove(Object o) {
    return sdks.remove(getKey(o)) == o;
  }

  public void setDefault(E defaultSdk) {
    assert (contains(defaultSdk));
    this.defaultSdk = defaultSdk;
  }

  @Override
  public int size() {
    return sdks.size();
  }

  /**
   * Returns the xml encoded form of this instance.
   * 
   * @return instance as a string of XML
   */
  public String toXml() {
    StringBuilder sb = new StringBuilder();

    sb.append("<sdks defaultSdk=\"");
    if (getDefault() != null) {
      sb.append(getDefault().getName());
    }
    sb.append("\">\n");

    for (E sdk : sdks.values()) {
      sb.append("\t");
      sb.append(sdk.toXml());
      sb.append("\n");
    }

    sb.append("</sdks>\n");
    return sb.toString();
  }

  private Object getKey(Object o) {
    if (o instanceof Sdk) {
      return ((Sdk) o).getName();
    } else {
      return o;
    }
  }
}
