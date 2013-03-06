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

  public ManagedApiEntry getEntryByName(String name) {
    for (ManagedApiEntry entry : entries) {
      if (null != entry.getName() && entry.getName().equals(name)) {
        return entry;
      }
    }
    return null;
  }
}
