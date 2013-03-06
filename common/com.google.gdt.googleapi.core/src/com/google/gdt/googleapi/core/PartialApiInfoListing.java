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
package com.google.gdt.googleapi.core;

import com.google.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: doc me
 */
public class PartialApiInfoListing {

  Map<String, PartialApiInfo> nameToPartialMap = new HashMap<String, PartialApiInfo>();

  Map<Pair<String, String>, PartialApiInfo> nameVersionToPartialMap = new HashMap<Pair<String, String>, PartialApiInfo>();

  public boolean contains(String name) {
    return nameToPartialMap.containsKey(name);
  }

  public boolean contains(String name, String version) {
    return nameVersionToPartialMap.containsKey(new Pair<String, String>(name,
        version));
  }

  public PartialApiInfo get(String name) {
    return nameToPartialMap.get(name);
  }

  public PartialApiInfo get(String name, String version) {
    return nameVersionToPartialMap.get(new Pair<String, String>(name, version));
  }

  public void setItems(List<PartialApiInfo> entries) {
    for (PartialApiInfo entry : entries) {
      if (entry.hasName() && entry.hasVersion()) {
        // consider merge if exists already
        nameVersionToPartialMap.put(new Pair<String, String>(entry.getName(),
            entry.getVersion()), entry);
      } else if (entry.hasName()) {
        // consider merge if exists already
        nameToPartialMap.put(entry.getName(), entry);
      }
    }
  }
}
