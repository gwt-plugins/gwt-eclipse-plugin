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

/**
 * Mutable version of an ApiInfo type.
 */
public interface MutableApiInfo extends ReadableApiInfo {

  void addLabel(String label);

  void clearIconLinks();

  void putIconLink(String reference, URL link);

  void setDescription(String description);

  void setDiscoveryLink(URL discoveryLink);

  void setDisplayName(String displayName);

  void setDocumentationLink(URL documentationLink);

  void setDownloadLink(URL downloadLink);

  void setName(String name);

  void setPreferred(boolean preferred);

  void setPublisher(String publisher);

  void setRanking(int ranking);

  void setReleaseDate(LocalDate releaseDate);

  void setReleaseNotesLink(URL releaseNotesLink);

  void setTosLink(URL tosLink);

  void setVersion(String version);

}
