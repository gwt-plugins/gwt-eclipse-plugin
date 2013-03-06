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
 * Core information describing a GPE-managed API. This class is used both for
 * describing entries in an API directory listing and APIs installed in the
 * current project.
 */
public class ApiInfoImpl implements Cloneable, ApiInfo, MutableApiInfo,
    Serializable {
  private static final long serialVersionUID = -2638022484238461586L;

  private String identifier;

  private PartialApiInfo partialApiInfo = new PartialApiInfo();

  public ApiInfoImpl(String identifier) {
    super();
    this.identifier = identifier;
  }

  public void addLabel(String label) {
    partialApiInfo.addLabel(label);
  }

  public void applyDefaults(PartialApiInfo defaults) {
    if (!partialApiInfo.hasDescription() && defaults.hasDescription()) {
      setDescription(defaults.getDescription());
    }
    if (!partialApiInfo.hasDiscoveryLink() && defaults.hasDiscoveryLink()) {
      setDiscoveryLink(defaults.getDiscoveryLink());
    }
    if (!partialApiInfo.hasDisplayName() && defaults.hasDisplayName()) {
      setDisplayName(defaults.getDisplayName());
    }
    if (!partialApiInfo.hasDocumentationLink()
        && defaults.hasDocumentationLink()) {
      setDocumentationLink(defaults.getDocumentationLink());
    }
    if (!partialApiInfo.hasDownloadLink() && defaults.hasDownloadLink()) {
      setDownloadLink(defaults.getDownloadLink());
    }
    if (!partialApiInfo.hasPreferred() && defaults.hasPreferred()) {
      setPreferred(defaults.isPreferred());
    }
    if (!partialApiInfo.hasPublisher() && defaults.hasPublisher()) {
      setPublisher(defaults.getPublisher());
    }
    if (!partialApiInfo.hasRanking() && defaults.hasRanking()) {
      setRanking(defaults.getRanking());
    }
    if (!partialApiInfo.hasReleaseDate() && defaults.hasReleaseDate()) {
      setReleaseDate(defaults.getReleaseDate());
    }
    if (!partialApiInfo.hasReleaseNotesLink() && defaults.hasReleaseNotesLink()) {
      setReleaseNotesLink(defaults.getReleaseNotesLink());
    }
    if (!partialApiInfo.hasTosLink() && defaults.hasTosLink()) {
      setTosLink(defaults.getTosLink());
    }
    if (defaults.hasLabels()) {
      for (String label : defaults.getLabels()) {
        addLabel(label);
      }
    }
    for (String iconKey : defaults.getIconLinkKeys()) {
      if (!partialApiInfo.hasIconLinkForKey(iconKey)) {
        putIconLink(iconKey, defaults.getIconLink(iconKey));
      }
    }
  }

  public void applyOverrides(PartialApiInfo override) {
    if (override.hasDescription()) {
      setDescription(override.getDescription());
    }
    if (override.hasDiscoveryLink()) {
      setDiscoveryLink(override.getDiscoveryLink());
    }
    if (override.hasDisplayName()) {
      setDisplayName(override.getDisplayName());
    }
    if (override.hasDocumentationLink()) {
      setDocumentationLink(override.getDocumentationLink());
    }
    if (override.hasDownloadLink()) {
      setDownloadLink(override.getDownloadLink());
    }
    if (override.hasPreferred()) {
      setPreferred(override.isPreferred());
    }
    if (override.hasPublisher()) {
      setPublisher(override.getPublisher());
    }
    if (override.hasRanking()) {
      setRanking(override.getRanking());
    }
    if (override.hasReleaseDate()) {
      setReleaseDate(override.getReleaseDate());
    }
    if (override.hasReleaseNotesLink()) {
      setReleaseNotesLink(override.getReleaseNotesLink());
    }
    if (override.hasTosLink()) {
      setTosLink(override.getTosLink());
    }
    if (override.hasLabels()) {
      partialApiInfo.clearLabels();
      for (String label : override.getLabels()) {
        addLabel(label);
      }
    }
    for (String iconKey : override.getIconLinkKeys()) {
      putIconLink(iconKey, override.getIconLink(iconKey));
    }
  }

  public void clearIconLinks() {
    partialApiInfo.clearIconLinks();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    ApiInfoImpl clone = (ApiInfoImpl) super.clone();
    clone.partialApiInfo = (PartialApiInfo) partialApiInfo.clone();
    return clone;
  }

  public ApiInfoImpl cloneWithId(String id) throws CloneNotSupportedException {
    ApiInfoImpl clone = (ApiInfoImpl) super.clone();
    clone.identifier = id;
    clone.partialApiInfo = (PartialApiInfo) partialApiInfo.clone();
    return clone;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ApiInfoImpl other = (ApiInfoImpl) obj;
    if (identifier == null) {
      if (other.identifier != null)
        return false;
    } else if (!identifier.equals(other.identifier))
      return false;
    return true;
  }

  public String getDescription() {
    return partialApiInfo.getDescription();
  }

  public URL getDiscoveryLink() {
    return partialApiInfo.getDiscoveryLink();
  }

  public String getDisplayName() {
    return partialApiInfo.getDisplayName();
  }

  public URL getDocumentationLink() {
    return partialApiInfo.getDocumentationLink();
  }

  public URL getDownloadLink() {
    return partialApiInfo.getDownloadLink();
  }

  public URL getIconLink(String reference) {
    return partialApiInfo.getIconLink(reference);
  }

  public Set<String> getIconLinkKeys() {
    return partialApiInfo.getIconLinkKeys();
  }

  public String getIdentifier() {
    return identifier;
  }

  public String[] getLabels() {
    return partialApiInfo.getLabels();
  }

  public String getName() {
    return partialApiInfo.getName();
  }

  public String getPublisher() {
    return partialApiInfo.getPublisher();
  }

  public Integer getRanking() {
    return partialApiInfo.getRanking();
  }

  public LocalDate getReleaseDate() {
    return partialApiInfo.getReleaseDate();
  }

  public URL getReleaseNotesLink() {
    return partialApiInfo.getReleaseNotesLink();
  }

  public URL getTosLink() {
    return partialApiInfo.getTosLink();
  }

  public String getVersion() {
    return partialApiInfo.getVersion();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((identifier == null) ? 0 : identifier.hashCode());
    return result;
  }

  public boolean hasIconLinkKey(String reference) {
    return partialApiInfo.hasIconLinkKey(reference);
  }

  public boolean hasLabel(String label) {
    return partialApiInfo.hasLabel(label);
  }

  public boolean hasName() {
    return partialApiInfo.hasName();
  }

  public Boolean isPreferred() {
    return partialApiInfo.isPreferred();
  }

  public void putIconLink(String reference, URL link) {
    partialApiInfo.putIconLink(reference, link);
  }

  public void setDescription(String description) {
    partialApiInfo.setDescription(description);
  }

  public void setDiscoveryLink(URL discoveryLink) {
    partialApiInfo.setDiscoveryLink(discoveryLink);
  }

  public void setDisplayName(String displayName) {
    partialApiInfo.setDisplayName(displayName);
  }

  public void setDocumentationLink(URL documentationLink) {
    partialApiInfo.setDocumentationLink(documentationLink);
  }

  public void setDownloadLink(URL downloadLink) {
    partialApiInfo.setDownloadLink(downloadLink);
  }

  public void setName(String name) {
    partialApiInfo.setName(name);
  }

  public void setPreferred(boolean preferred) {
    partialApiInfo.setPreferred(preferred);
  }

  public void setPublisher(String publisher) {
    partialApiInfo.setPublisher(publisher);
  }

  public void setRanking(int ranking) {
    partialApiInfo.setRanking(ranking);
  }

  public void setReleaseDate(LocalDate releaseDate) {
    partialApiInfo.setReleaseDate(releaseDate);
  }

  public void setReleaseNotesLink(URL releaseNotesLink) {
    partialApiInfo.setReleaseNotesLink(releaseNotesLink);
  }

  public void setTosLink(URL tosLink) {
    partialApiInfo.setTosLink(tosLink);
  }

  public void setVersion(String version) {
    partialApiInfo.setVersion(version);
  }

  @Override
  public String toString() {
    return "ApiInfo [identifier=" + identifier + "]";
  }

  public ApiInfoImpl withDescription(String description) {
    this.setDescription(description);
    return this;
  }

  public ApiInfoImpl withDiscoveryLink(String discoveryLink)
      throws MalformedURLException {
    this.setDiscoveryLink(new URL(discoveryLink));
    return this;
  }

  public ApiInfoImpl withDisplayName(String displayName) {
    this.setDisplayName(displayName);
    return this;
  }

  public ApiInfoImpl withDocumentationLink(String documentationLink)
      throws MalformedURLException {
    this.setDocumentationLink(new URL(documentationLink));
    return this;
  }

  public ApiInfoImpl withDownloadLink(URL downloadLink) {
    this.setDownloadLink(downloadLink);
    return this;
  }

  public ApiInfoImpl withIconLink(String iconReference, String iconLink)
      throws MalformedURLException {
    this.putIconLink(iconReference, new URL(iconLink));
    return this;
  }

  public ApiInfoImpl withLabel(String label) {
    this.addLabel(label);
    return this;
  }

  public ApiInfoImpl withName(String name) {
    this.setName(name);
    return this;
  }

  public ApiInfoImpl withPreferred(boolean preferred) {
    this.setPreferred(preferred);
    return this;
  }

  public ApiInfoImpl withPublisher(String publisher) {
    this.setPublisher(publisher);
    return this;
  }

  public ApiInfoImpl withRanking(int ranking) {
    this.setRanking(ranking);
    return this;
  }

  public ApiInfoImpl withReleaseDate(LocalDate releaseDate) {
    this.setReleaseDate(releaseDate);
    return this;
  }

  public ApiInfoImpl withReleaseNotesLink(String releaseNotesLink)
      throws MalformedURLException {
    this.setReleaseNotesLink(new URL(releaseNotesLink));
    return this;
  }

  public ApiInfoImpl withTosLink(String tosLink) throws MalformedURLException {
    this.setTosLink(new URL(tosLink));
    return this;
  }

  public ApiInfoImpl withVersion(String version) {
    this.setVersion(version);
    return this;
  }
}
