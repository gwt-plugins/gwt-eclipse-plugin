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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a list of GPE-managed APIs. This type maintains indices to keep
 * lookups fast.
 */
public class ApiDirectoryListing implements Serializable {

  private static final long serialVersionUID = 7694365511250533635L;

  private static Comparator<ReadableApiInfo> comparator = new ReadableApiInfoComparator();

  private List<ApiDirectoryItem> apiDirectoryItems;

  /**
   * Map from identifier (name and version) to an ApiDirectoryItem.
   */
  private transient Map<String, ApiDirectoryItem> itemsByIdentifier;

  private transient HashMap<String, List<ApiDirectoryItem>> itemsByName;

  public ApiDirectoryListing applyMapping(ApiDirectoryItemMapping mapping) {
    ApiDirectoryListing listing = new ApiDirectoryListing();
    List<ApiDirectoryItem> newItems = new LinkedList<ApiDirectoryItem>();
    for (ApiDirectoryItem item : apiDirectoryItems) {
      ApiDirectoryItem mappingResult = mapping.apply(item, listing);
      if (mappingResult != null) {
        newItems.add(mappingResult);
      }
    }
    listing.setItems(newItems);
    return listing;
  }

  public boolean contains(String name) {
    for (ApiDirectoryItem item : itemsByIdentifier.values()) {
      if (nullsafeEquals(name, item.getName())) {
        return true;
      }
    }
    return false;
  }

  public boolean contains(String name, String version) {
    return itemsByIdentifier.containsKey(createIdentifier(name, version));
  }

  public ApiDirectoryItem get(int index) {
    return apiDirectoryItems.get(index);
  }

  public ApiDirectoryItem get(String id) {
    return itemsByIdentifier.get(id);
  }

  public ApiDirectoryItem get(String name, String version) {
    return itemsByIdentifier.get(createIdentifier(name, version));
  }

  public ApiDirectoryItem[] getByName(String name) {
    if (itemsByName.containsKey(name)) {
      List<ApiDirectoryItem> items = itemsByName.get(name);
      return items.toArray(new ApiDirectoryItem[items.size()]);
    } else {
      return new ApiDirectoryItem[0];
    }
  }

  public ApiDirectoryItem[] getItems() {
    return apiDirectoryItems.toArray(new ApiDirectoryItem[apiDirectoryItems.size()]);
  }

  public ApiDirectoryItem getPreferredByName(String name) {
    if (itemsByName.containsKey(name)) {
      List<ApiDirectoryItem> items = itemsByName.get(name);
      for (ApiDirectoryItem item : items) {
        if (item.isPreferred()) {
          return item;
        }
      }
    }
    return null;
  }

  public void indexItems() {
    this.itemsByIdentifier = new HashMap<String, ApiDirectoryItem>();
    for (ApiDirectoryItem item : this.apiDirectoryItems) {
      itemsByIdentifier.put(item.getIdentifier(), item);
    }

    this.itemsByName = new HashMap<String, List<ApiDirectoryItem>>();
    for (ApiDirectoryItem item : this.apiDirectoryItems) {
      List<ApiDirectoryItem> directoryItemList = null;
      if (!itemsByName.containsKey(item.getName())) {
        directoryItemList = new ArrayList<ApiDirectoryItem>();
        itemsByName.put(item.getName(), directoryItemList);
      } else {
        directoryItemList = itemsByName.get(item.getName());
      }
      directoryItemList.add(item);
    }
  }

  public ApiDirectoryListing mergeListingsByName(
      ApiDirectoryListing otherListing) {
    ApiDirectoryListing listing = new ApiDirectoryListing();
    Map<String, ApiDirectoryItem> itemMap = new HashMap<String, ApiDirectoryItem>();
    for (ApiDirectoryItem item : otherListing.itemsByIdentifier.values()) {
      itemMap.put(item.getName(), item);
    }
    for (ApiDirectoryItem item : itemsByIdentifier.values()) {
      itemMap.put(item.getName(), item);
    }
    listing.setItems(itemMap.values());
    return listing;
  }

  public ApiDirectoryListing mergeListingsById(
      ApiDirectoryListing otherListing) {
    ApiDirectoryListing listing = new ApiDirectoryListing();
    Map<String, ApiDirectoryItem> itemMap = new HashMap<String, ApiDirectoryItem>();
    for (ApiDirectoryItem item : otherListing.itemsByIdentifier.values()) {
      itemMap.put(item.getIdentifier(), item);
    }
    for (ApiDirectoryItem item : itemsByIdentifier.values()) {
      itemMap.put(item.getIdentifier(), item);
    }
    listing.setItems(itemMap.values());
    return listing;
  }

  public void setItems(Collection<ApiDirectoryItem> items) {
    this.apiDirectoryItems = new ArrayList<ApiDirectoryItem>();
    this.apiDirectoryItems.addAll(sortItems(items));
    indexItems();
  }

  private String createIdentifier(String name, String version) {
    return "" + name + ":" + version;
  }

  private boolean nullsafeEquals(String str1, String str2) {
    if (str1 == null && str2 == null) {
      return true;
    } else if (str1 != null && str2 != null) {
      return str1.equals(str2);
    } else {
      return false;
    }
  }

  /**
   * Customize the deserialization to include recreating transient indices.
   */
  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    indexItems();
  }

  private List<ApiDirectoryItem> sortItems(
      Collection<ApiDirectoryItem> localItems) {
    ApiDirectoryItem[] localItemsArray = localItems.toArray(new ApiDirectoryItem[localItems.size()]);
    Arrays.sort(localItemsArray, comparator);
    return Arrays.asList(localItemsArray);
  }
}
