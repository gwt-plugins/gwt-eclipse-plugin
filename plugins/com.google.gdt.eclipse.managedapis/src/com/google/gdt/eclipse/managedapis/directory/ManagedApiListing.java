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
package com.google.gdt.eclipse.managedapis.directory;


import com.google.gdt.googleapi.core.ApiDirectoryItem;

import java.util.List;

/**
 * A model representing a collection of entries for display to a user.
 */
public class ManagedApiListing {
  private List<ManagedApiEntry> entries;

  public ManagedApiListing(List<ManagedApiEntry> entries) {
    this.entries = entries;
  }

  public List<ManagedApiEntry> getEntries() {
    return entries;
  }

  /**
   * Returns the managed API with the specified identifier
   * 
   * @param identifier Id of the managed API to search for
   * @return managed API with the specified identifier
   */
  public ManagedApiEntry getEntryByIdentifier(String identifier) {
    for (ManagedApiEntry entry : entries) {
      ApiDirectoryItem item = entry.getDirectoryEntry();
      if (item != null) {
        String id = item.getIdentifier();
        if (id != null && id.equals(identifier)) {
          return entry;
        }
      }
    }
    return null;
  }

  public ManagedApiEntry getEntryByName(String name) {
    for (ManagedApiEntry entry : entries) {
      if (null != entry.getName() && entry.getName().equals(name)) {
        return entry;
      }
    }
    return null;
  }
}
