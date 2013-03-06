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

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.googleapi.core.ApiDirectoryItem;

/**
 * A StructuredApiCollection data structure accepts a combination of (installed)
 * ManagedApis and (listed by directory) ApiDirectoryEntry's to produce listings
 * for user consumption.
 */
public interface StructuredApiCollection {

  /**
   * Add a single ApiDirectoryEntry (listed by directory).
   */
  public abstract void add(ApiDirectoryItem entry);

  /**
   * Add a single ManagedApi (installed).
   */
  public abstract void add(ManagedApi managedApi);

  /**
   * Add multiple ApiDirectoryEntry's (listed by a directory)
   */
  public abstract void addAll(ApiDirectoryItem[] entries);

  /**
   * Add a multiple ManagedApis (installed).
   */
  public abstract void addAll(ManagedApi[] installedApis);

  /**
   * Provide a merged listing representing the currently installed entries.
   */
  public abstract ManagedApiListing getInstalledEntries();

  /**
   * Provide a merged listing representing all entries either installed, listed
   * by a directory (or both).
   */
  public abstract ManagedApiListing getListing();

}