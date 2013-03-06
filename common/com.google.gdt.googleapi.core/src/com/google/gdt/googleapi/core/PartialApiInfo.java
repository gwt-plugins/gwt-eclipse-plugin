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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents information about APIs. Instance may be partial and apply to a
 * number of APIs.
 */
public class PartialApiInfo implements Cloneable, MutableApiInfo, Serializable {
  private static final long serialVersionUID = 1319564389920794716L;

  /**
   * Body text providing long-format description -- no formatting.
   */
  private String description;

  private URL discoveryLink;

  /**
   * Display name for the API (title in discovery doc).
   */
  private String displayName;

  /**
   * Link to documentation site for API.
   */
  private URL documentationLink;

  /**
   * Link to download the API bundle.
   */
  private URL downloadLink;

  /**
   * Links to the icon for the current API mapped by format (e.g. dimensions).
   */
  private Map<String, URL> iconLinks = new HashMap<String, URL>();

  private Set<String> labels = new HashSet<String>();

  /**
   * Identifier for the API.
   */
  private String name;

  private Boolean preferred;

  /**
   * Name of the API provider
   */
  private String publisher;

  /**
   * Override sort order. Default ranking effectively 0. Higher value means list
   * further towards the top.
   */
  private Integer ranking;

  /**
   * The timestamp of the last update of the API listing.
   */
  private LocalDate releaseDate;

  /**
   * Link to release notes for specific release.
   */
  private URL releaseNotesLink;

  /**
   * Link to Terms of Service for the API version.
   */
  private URL tosLink;

  /**
   * A version string of unspecified format
   */
  private String version;

  public void addLabel(String label) {
    labels.add(label);
  }

  public void clearIconLinks() {
    this.iconLinks.clear();
  }

  public void clearLabels() {
    labels.clear();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    PartialApiInfo clone = (PartialApiInfo) super.clone();
    clone.iconLinks = new HashMap<String, URL>();
    clone.iconLinks.putAll(iconLinks);
    clone.labels = new HashSet<String>();
    clone.labels.addAll(labels);
    return clone;
  }

  public String getDescription() {
    return description;
  }

  public URL getDiscoveryLink() {
    return discoveryLink;
  }

  public String getDisplayName() {
    return displayName == null ? name : displayName;
  }

  public URL getDocumentationLink() {
    return documentationLink;
  }

  public URL getDownloadLink() {
    return downloadLink;
  }

  public URL getIconLink(String reference) {
    return iconLinks.get(reference);
  }

  public Set<String> getIconLinkKeys() {
    return iconLinks.keySet();
  }

  public String[] getLabels() {
    return labels.toArray(new String[labels.size()]);
  }

  public String getName() {
    return name;
  }

  public String getPublisher() {
    return publisher;
  }

  public Integer getRanking() {
    return ranking != null ? ranking : 0;
  }

  public LocalDate getReleaseDate() {
    return releaseDate;
  }

  public URL getReleaseNotesLink() {
    return releaseNotesLink;
  }

  public URL getTosLink() {
    return tosLink;
  }

  public String getVersion() {
    return version;
  }

  public boolean hasDescription() {
    return description != null;
  }

  public boolean hasDiscoveryLink() {
    return discoveryLink != null;
  }

  public boolean hasDisplayName() {
    return displayName != null;
  }

  public boolean hasDocumentationLink() {
    return documentationLink != null;
  }

  public boolean hasDownloadLink() {
    return downloadLink != null;
  }

  public boolean hasIconLinkForKey(String iconKey) {
    return iconLinks.containsKey(iconKey);
  }

  public boolean hasIconLinkKey(String reference) {
    return iconLinks.containsKey(reference);
  }

  public boolean hasLabel(String label) {
    if (labels != null) {
      for (String l : labels) {
        if (l.equals(label)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasLabels() {
    return labels.size() > 0;
  }

  public boolean hasName() {
    return this.name != null;
  }

  public boolean hasPreferred() {
    return preferred != null;
  }

  public boolean hasPublisher() {
    return publisher != null;
  }

  public boolean hasRanking() {
    return ranking != null;
  }

  public boolean hasReleaseDate() {
    return releaseDate != null;
  }

  public boolean hasReleaseNotesLink() {
    return releaseNotesLink != null;
  }

  public boolean hasTosLink() {
    return tosLink != null;
  }

  public boolean hasVersion() {
    return version != null;
  }

  public Boolean isPreferred() {
    return preferred;
  }

  public void putIconLink(String reference, URL link) {
    this.iconLinks.put(reference, link);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setDiscoveryLink(URL discoveryLink) {
    this.discoveryLink = discoveryLink;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setDocumentationLink(URL documentationLink) {
    this.documentationLink = documentationLink;
  }

  public void setDownloadLink(URL downloadLink) {
    this.downloadLink = downloadLink;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPreferred(boolean preferred) {
    this.preferred = preferred;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public void setRanking(int ranking) {
    this.ranking = ranking;
  }

  public void setReleaseDate(LocalDate releaseDate) {
    this.releaseDate = releaseDate;
  }

  public void setReleaseNotesLink(URL releaseNotesLink) {
    this.releaseNotesLink = releaseNotesLink;
  }

  public void setTosLink(URL tosLink) {
    this.tosLink = tosLink;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
