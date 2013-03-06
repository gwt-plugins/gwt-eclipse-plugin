/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;
import com.google.gdt.eclipse.managedapis.directory.StructuredApiCollection;
import com.google.gdt.googleapi.core.ApiDirectoryItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * StructuredApiListing applies business rules to the structure of APIs. The
 * StructuredApiListing is organized around APIs by name. For each API name,
 * there can be a collection of available APIs (with different versions/release
 * statuses) and up to one installed API. The class will track up to one version
 * with a release status tag.
 */
public class LatestVersionOnlyStructuredApiCollection implements
    StructuredApiCollection {

  class NamedApiContainer {
    private String apiName;
    private ApiDirectoryItem availableApi;
    private ManagedApi installedApi;

    NamedApiContainer(ApiDirectoryItem apiListing) {
      if (null != apiListing) {
        this.apiName = apiListing.getName();
        this.availableApi = apiListing;
      }
    }

    NamedApiContainer(ManagedApi installedApi) {
      this.apiName = installedApi.getName();
      this.installedApi = installedApi;
    }

    public String getApiName() {
      return apiName;
    }

    public ApiDirectoryItem getAvailableApi() {
      return availableApi;
    }

    public ManagedApi getInstalledApi() {
      return installedApi;
    }

    public void setApiListing(ApiDirectoryItem apiListing) {
      if (null != apiListing) {
        if (!(apiListing.getName().equals(apiName))) {
          throw new IllegalArgumentException(
              "Name of installed API must match the container");
        }
        availableApi = apiListing;
      }
    }

    public void setInstalledApi(ManagedApi installedApi) {
      if (!(installedApi.getName().equals(apiName))) {
        throw new IllegalArgumentException(
            "Name of installed API must match the container");
      }
      this.installedApi = installedApi;
    }
  }

  private Map<String, NamedApiContainer> listings = new LinkedHashMap<String, NamedApiContainer>();

  public void add(ApiDirectoryItem entry) {
    if (null == entry.getIdentifier()) {
      throw new IllegalArgumentException("Entry Identifier is null");
    }
    if (listings.containsKey(entry.getIdentifier())) {
      listings.get(entry.getIdentifier()).setApiListing(entry);
    } else {
      listings.put(entry.getIdentifier(), new NamedApiContainer(entry));
    }
  }

  public void add(ManagedApi managedApi) {
    if (null == managedApi.getIdentifier()) {
      throw new IllegalArgumentException("ManagedApi Identifier is null");
    }
    String managedApiId = managedApi.getIdentifier();
    if (listings.containsKey(managedApiId)) {
      listings.get(managedApiId).setInstalledApi(managedApi);
    } else {
      listings.put(managedApiId, new NamedApiContainer(managedApi));
    }
  }

  public void addAll(ApiDirectoryItem[] entries) {
    for (ApiDirectoryItem api : entries) {
      add(api);
    }
  }

  public void addAll(ManagedApi[] installedApis) {
    for (ManagedApi api : installedApis) {
      add(api);
    }
  }

  public ManagedApiListing getInstalledEntries() {
    List<ManagedApiEntry> entries = new ArrayList<ManagedApiEntry>();
    for (NamedApiContainer container : listings.values()) {
      ManagedApi installed = container.getInstalledApi();
      if (null != installed) {
        entries.add(new ManagedApiEntry(container.getAvailableApi(), installed));
      }
    }
    return new ManagedApiListing(entries);
  }

  public ManagedApiListing getListing() {
    List<ManagedApiEntry> entries = new ArrayList<ManagedApiEntry>();
    for (NamedApiContainer container : listings.values()) {
      entries.add(new ManagedApiEntry(container.getAvailableApi(),
          container.getInstalledApi()));
    }
    return new ManagedApiListing(entries);
  }
}
