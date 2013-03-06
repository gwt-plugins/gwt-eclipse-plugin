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

import org.joda.time.LocalDate;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * A single entry as retrieved from an API Directory.
 */
public class ApiDirectoryItem implements ApiInfo, Serializable {
  private static final long serialVersionUID = 1374205264749584091L;

  private ApiInfoImpl info;

  public ApiDirectoryItem(ApiInfoImpl info) {
    if (null == info) {
      throw new IllegalArgumentException(
          "Illegal to construct ApiDirectoryItem with null value");
    }
    this.info = info;
  }

  public ApiInfoImpl copyApiInfo() {
    ApiInfoImpl retval = null;
    try {
      retval = (ApiInfoImpl) (info.clone());
    } catch (CloneNotSupportedException e) {
    }
    return retval;
  }

  public ApiInfoImpl copyApiInfo(String identifier) {
    ApiInfoImpl retval = null;
    try { 
      retval = info.cloneWithId(identifier);
    } catch (CloneNotSupportedException e) {
    }
    return retval;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ApiDirectoryItem other = (ApiDirectoryItem) obj;
    if (info == null) {
      if (other.info != null)
        return false;
    } else if (!info.equals(other.info))
      return false;
    return true;
  }

  public String getDescription() {
    return info.getDescription();
  }

  public URL getDiscoveryLink() {
    return info.getDiscoveryLink();
  }

  public String getDisplayName() {
    return info.getDisplayName();
  }

  public URL getDocumentationLink() {
    return info.getDocumentationLink();
  }

  public URL getDownloadLink() {
    return info.getDownloadLink();
  }

  public URL getIconLink(String key) {
    return info.getIconLink(key);
  }

  public Set<String> getIconLinkKeys() {
    return info.getIconLinkKeys();
  }

  public String getIdentifier() {
    return info.getIdentifier();
  }

  public String[] getLabels() {
    return info.getLabels();
  }

  public String getName() {
    return info.getName();
  }

  public String getPublisher() {
    return info.getPublisher();
  }

  public Integer getRanking() {
    return info.getRanking();
  }

  public LocalDate getReleaseDate() {
    return info.getReleaseDate();
  }

  public URL getReleaseNotesLink() {
    return info.getReleaseNotesLink();
  }

  public URL getTosLink() {
    return info.getTosLink();
  }

  public String getVersion() {
    return info.getVersion();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((info == null) ? 0 : info.hashCode());
    return result;
  }

  public boolean hasIconLinkKey(String reference) {
    return info.hasIconLinkKey(reference);
  }

  public boolean hasLabel(String label) {
    return info.hasLabel(label);
  }

  public Boolean isPreferred() {
    return info.isPreferred();
  }

  public ApiDirectoryItem rewriteURLs(URL originalBaseURL, URL targetBaseURL)
      throws MalformedURLException {
    ApiInfoImpl copy = copyApiInfo();
    copy.setDiscoveryLink(rewriteURL(getDiscoveryLink(), originalBaseURL,
        targetBaseURL));
    copy.setDocumentationLink(rewriteURL(getDocumentationLink(),
        originalBaseURL, targetBaseURL));
    copy.setDownloadLink(rewriteURL(getDownloadLink(), originalBaseURL,
        targetBaseURL));
    copy.setReleaseNotesLink(rewriteURL(getReleaseNotesLink(), originalBaseURL,
        targetBaseURL));
    copy.setTosLink(rewriteURL(getTosLink(), originalBaseURL, targetBaseURL));

    copy.clearIconLinks();
    for (String key : getIconLinkKeys()) {
      copy.putIconLink(key,
          rewriteURL(getIconLink(key), originalBaseURL, targetBaseURL));
    }
    return new ApiDirectoryItem(copy);
  }

  @Override
  public String toString() {
    return "ApiDirectoryEntry [info=" + info + "]";
  }

  private boolean nullsafeEquals(String a, String b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else {
      return a.equals(b);
    }
  }

  private URL rewriteURL(URL link, URL originalBaseURL, URL targetBaseURL)
      throws MalformedURLException {
    if (link == null || originalBaseURL == null || targetBaseURL == null) {
      return link;
    } else {
      if (nullsafeEquals(link.getProtocol(), originalBaseURL.getProtocol())
          && link.getPath().startsWith(originalBaseURL.getPath())) {
        return new URL(targetBaseURL, link.getPath().substring(
            originalBaseURL.getPath().length()));
      }
      return link;
    }
  }
}
