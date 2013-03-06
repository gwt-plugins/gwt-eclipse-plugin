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
package com.google.gdt.eclipse.managedapis.directory;

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.googleapi.core.ApiDirectoryItem;
import com.google.gdt.googleapi.search.Match;
import com.google.gdt.googleapi.search.Searchable;
import com.google.gdt.googleapi.search.Term;

import org.joda.time.LocalDate;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a model-level representation for an API that is displayed to the
 * user for potential selection. This object may represent an uninstalled API
 * sourced from a directory listing; the API may be installed; and it may or may
 * not represent an installed api for which an upgrade is available.
 */
public class ManagedApiEntry implements Searchable {

  private static void matchField(Term term, String field, String value,
      List<Match> matches) {
    if (value != null) {
      if (value.toLowerCase().contains(term.getText())) {
        matches.add(new Match(term, field));
      }
    }
  }

  boolean selected = false;

  private ApiDirectoryItem directoryEntry;

  private ManagedApi installed;

  /**
   * Produce a ManagedApiEntry from a ApiDirectoryEntry (not installed) and a
   * ManagedApi (installed). Either can be null.
   */
  public ManagedApiEntry(ApiDirectoryItem directoryEntry, ManagedApi installed) {
    this.installed = installed;
    this.directoryEntry = directoryEntry;
    if (isUpdateAvailable()) {
      installed.setUpdateAvailable();
    }
  }

  /**
   * Access the description.
   */
  public String getDescription() {
    String description = null;
    if (null != directoryEntry) {
      description = directoryEntry.getDescription();
    }
    if (null == description && null != installed) {
      description = installed.getDescription();
    }
    return description;
  }

  /**
   * Access the directoryEntry.
   */
  public ApiDirectoryItem getDirectoryEntry() {
    return directoryEntry;
  }

  /**
   * Access the labels of the listed API entry.
   */
  public String[] getDirectoryEntryLabels() {
    String[] labels = new String[0];
    if (null != directoryEntry) {
      labels = directoryEntry.getLabels();
    }
    return labels;
  }

  /**
   * Access the release date of the listed API entry.
   */
  public LocalDate getDirectoryEntryReleaseDate() {
    LocalDate releaseDate = null;
    if (null != directoryEntry) {
      releaseDate = directoryEntry.getReleaseDate();
    }
    return releaseDate;
  }

  /**
   * Access the version of the listed API entry.
   */
  public String getDirectoryEntryVersion() {
    String version = null;
    if (null != directoryEntry) {
      version = directoryEntry.getVersion();
    }
    return version;
  }

  /**
   * Access the display name of the entry
   */
  public String getDisplayName() {
    String displayName = null;
    if (null != directoryEntry) {
      displayName = directoryEntry.getDisplayName();
    }
    if (null == displayName && null != installed) {
      displayName = installed.getDisplayName();
    }
    return displayName;
  }

  /**
   * Access the documentation link of the entry
   */
  public URL getDocumentationLink() {
    URL documentationLink = null;
    if (null != directoryEntry) {
      documentationLink = directoryEntry.getDocumentationLink();
    }
    if (null == documentationLink && null != installed) {
      documentationLink = installed.getDocumentationLink();
    }
    return documentationLink;
  }

  /**
   * Access the icon link of the entry for the given key.
   */
  public URL getIconLink(String reference) {
    URL iconLink = null;
    if (null != directoryEntry) {
      iconLink = directoryEntry.getIconLink(reference);
    }
    if (null == iconLink && null != installed) {
      iconLink = installed.getIconLink(reference);
    }
    return iconLink;
  }

  /**
   * Access the collection of icon link keys for the entry.
   */
  public Set<String> getIconLinkReferences() {
    Set<String> iconLinkReferences = new HashSet<String>();
    if (null != directoryEntry) {
      iconLinkReferences.addAll(directoryEntry.getIconLinkKeys());
    }
    if (null != installed) {
      iconLinkReferences.addAll(installed.getIconLinkKeys());
    }
    return iconLinkReferences;
  }

  /**
   * Determine whether the entry is currently installed.
   */
  public ManagedApi getInstalled() {
    return installed;
  }

  /**
   * Access the release date of the installed API entry.
   */
  public LocalDate getInstalledReleaseDate() {
    LocalDate releaseDate = null;
    if (null != installed) {
      releaseDate = installed.getReleaseDate();
    }
    return releaseDate;
  }

  /**
   * Access the version of the installed API entry.
   */
  public String getInstalledVersion() {
    String version = null;
    if (null != installed) {
      version = installed.getVersion();
    }
    return version;
  }

  /**
   * Access the name of the entry.
   */
  public String getName() {
    String name = null;
    if (null != directoryEntry) {
      name = directoryEntry.getName();
    }
    if (null == name && null != installed) {
      name = installed.getName();
    }
    return name;
  }

  /**
   * Access the publisher of the entry.
   */
  public String getPublisher() {
    String publisher = null;
    if (null != directoryEntry) {
      publisher = directoryEntry.getPublisher();
    }
    if (null == publisher && null != installed) {
      publisher = installed.getPublisher();
    }
    return publisher;
  }

  /**
   * Access the ranking for the entry.
   */
  public int getRanking() {
    int ranking = 0;
    if (null != directoryEntry) {
      ranking = directoryEntry.getRanking();
    } else if (null != installed) {
      ranking = installed.getRanking();
    }
    return ranking;
  }

  /**
   * Access the release notes link of the entry.
   */
  public URL getReleaseNotesLink() {
    URL releaseNotesLink = null;
    if (null != directoryEntry) {
      releaseNotesLink = directoryEntry.getReleaseNotesLink();
    }
    if (null == releaseNotesLink && null != installed) {
      releaseNotesLink = installed.getReleaseNotesLink();
    }
    return releaseNotesLink;
  }

  /**
   * Access the terms of service link for the entry.
   */
  public URL getTosLink() {
    URL tosLink = null;
    if (null != directoryEntry) {
      tosLink = directoryEntry.getTosLink();
    }
    if (null == tosLink && null != installed) {
      tosLink = installed.getTosLink();
    }
    return tosLink;
  }

  /**
   * Determine whether an uninstalled listing exists for this entry.
   */
  public boolean hasDirectoryEntry() {
    return null != directoryEntry;
  }

  /**
   * Returns whether the icon map contains an entry for the provided key.
   */
  public boolean hasIconLinkKey(String key) {
    boolean hasKey = false;
    if (null != directoryEntry) {
      hasKey = directoryEntry.hasIconLinkKey(key);
    }
    if (!hasKey && null != installed) {
      hasKey = installed.hasIconLinkKey(key);
    }
    return hasKey;
  }

  /**
   * Returns whether the entry is currently installed.
   */
  public boolean isInstalled() {
    return null != installed;
  }

  /**
   * Determines whether the user has marked this entry for import.
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Returns whether an update is available for the entry.
   */
  public boolean isUpdateAvailable() {
    return isInstalled() && installed.isRevisionUpdateAvailable();
  }

  public Match[] match(Term[] terms) {
    List<Match> matches = new ArrayList<Match>();
    for (Term term : terms) {
      matchField(term, "displayName", getDisplayName(), matches);
      matchField(term, "description", getDescription(), matches);
      matchField(term, "publisher", getPublisher(), matches);
      for (String label : getDirectoryEntry() != null
          ? getDirectoryEntry().getLabels() : new String[0]) {
        matchField(term, "label", label, matches);
      }
      matchField(term, "version", getDirectoryEntryVersion(), matches);
    }
    return matches.toArray(new Match[matches.size()]);
  }

  /**
   * Sets whether the user has marked this entry for import.
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }
}
