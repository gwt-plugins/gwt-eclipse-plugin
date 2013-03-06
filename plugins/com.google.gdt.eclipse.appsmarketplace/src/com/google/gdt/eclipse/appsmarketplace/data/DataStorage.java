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
package com.google.gdt.eclipse.appsmarketplace.data;

import java.util.List;

/**
 * Stores apps marketplace data fetched from back-end.
 */
public class DataStorage {

  private static VendorProfile vendorProfile = null;

  private static List<AppListing> appListings = null;

  private static AppListing appListingListed = null;

  public static List<AppListing> getAppListings() {
    return appListings;
  }

  public static AppListing getListedAppListing() {
    return appListingListed;
  }

  public static VendorProfile getVendorProfile() {
    return vendorProfile;
  }

  public static void clearAppListings() {
    appListings = null;
  }

  public static void clearListedAppListing() {
    appListingListed = null;
  }

  public static void clearVendorProfile() {
    vendorProfile = null;
  }

  public static void clearData() {
    clearVendorProfile();
    clearAppListings();
    clearListedAppListing();
  }

  public static void setAppListings(List<AppListing> lists) {
    appListings = lists;
  }

  public static void setListedAppListing(AppListing al) {
    appListingListed = al;
  }

  public static void setVendorProfile(VendorProfile vp) {
    vendorProfile = vp;
  }
}
