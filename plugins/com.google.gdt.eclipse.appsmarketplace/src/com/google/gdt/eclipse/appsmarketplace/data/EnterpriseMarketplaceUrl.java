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

import com.google.api.client.googleapis.GoogleUrl;
import com.google.gdt.eclipse.core.StringUtilities;

/**
 * Generates Google apps marketplace related URLs.
 */
public class EnterpriseMarketplaceUrl {
  private static final String MARKETPLACE_API_HOST = System.getenv(
      "MARKETPLACE_API_HOST");
  private static final String MARKETPLACE_FRONTEND_HOST = System.getenv(
      "MARKETPLACE_FRONTEND_HOST");

  public static String generateAppListingUrl() {
    GoogleUrl url = new GoogleUrl(getApiHost());
    url.setPathParts(GoogleUrl.toPathParts("/appsmarket/v2/appListing/"));
    return url.toString();
  }

  public static String generateFrontendCreateListingUrl() {
    GoogleUrl url = new GoogleUrl(getFrontendHost());
    url.setPathParts(GoogleUrl.toPathParts(
        "/enterprise/marketplace/blankListing"));
    return url.toString();
  }

  public static String generateFrontendCreateVendorProfileUrl() {
    GoogleUrl url = new GoogleUrl(getFrontendHost());
    url.setPathParts(
        GoogleUrl.toPathParts("/enterprise/marketplace/myProfile"));
    return url.toString();
  }

  public static String generateFrontendViewListingUrl() {
    GoogleUrl url = new GoogleUrl(getFrontendHost());
    url.setPathParts(GoogleUrl.toPathParts(
        "/enterprise/marketplace/viewVendorProfile"));
    return url.toString();
  }

  public static String generateVendorProfileUrl() {
    GoogleUrl url = new GoogleUrl(getApiHost());
    url.setPathParts(GoogleUrl.toPathParts("/appsmarket/v2/vendorProfile"));
    return url.toString();
  }

  private static String getApiHost() {
    String host;
    if (StringUtilities.isEmpty(MARKETPLACE_API_HOST)) {
      host = new String("https://www.googleapis.com/");
    } else {
      host = MARKETPLACE_API_HOST;
    }
    return host;
  }

  private static String getFrontendHost() {
    String host;
    if (StringUtilities.isEmpty(MARKETPLACE_FRONTEND_HOST)) {
      host = new String("http://www.google.com/");
    } else {
      host = MARKETPLACE_FRONTEND_HOST;
    }
    return host;
  }
}