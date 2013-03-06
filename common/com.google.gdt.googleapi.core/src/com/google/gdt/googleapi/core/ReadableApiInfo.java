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

import java.net.URL;
import java.util.Set;

/**
 * Minimal interface for an ApiInfo type.
 */
public interface ReadableApiInfo {

  String getDescription();

  URL getDiscoveryLink();

  String getDisplayName();

  URL getDocumentationLink();

  URL getDownloadLink();

  URL getIconLink(String key);

  Set<String> getIconLinkKeys();

  String[] getLabels();

  String getName();

  String getPublisher();

  Integer getRanking();

  LocalDate getReleaseDate();

  URL getReleaseNotesLink();

  URL getTosLink();

  String getVersion();

  boolean hasIconLinkKey(String key);

  boolean hasLabel(String label);

  Boolean isPreferred();

}