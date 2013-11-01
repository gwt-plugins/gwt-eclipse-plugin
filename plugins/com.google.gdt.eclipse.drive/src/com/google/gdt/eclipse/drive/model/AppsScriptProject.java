/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.gdt.eclipse.drive.model;

import com.google.api.client.util.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.Hashing;
import com.google.gdt.eclipse.drive.driveapi.DriveScriptInfo;
import com.google.gdt.eclipse.drive.driveapi.ScriptProjectJsonProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.concurrent.Immutable;

/**
 * Contains an abstract representation of an Apps Script project. This representation is common to
 * both Drive Apps Script projects and Eclipse Apps Script projects and is used for communication
 * between Drive and Eclipse.
 */
@Immutable
public class AppsScriptProject {
  
  private static final ScriptProjectJsonProcessor JSON_PROCESSOR = new ScriptProjectJsonProcessor();
  
  private final String fileId;
  private final SortedMap<String, DriveScriptInfo> fileNamesToInfo;
  private final String apiInfoJsonInScript;
  private final String driveVersionFingerprint;
  
  /**
   * Creates a new {@code AppsScriptProject} from Drive data. The inputs are a Drive file ID for the
   * Apps Script project, a JSON representation of the Drive Apps Script project contents, and
   * a JSON-in-script representation of the Google APIs used by the project.
   * 
   * @param fileId the Drive file ID
   * @param importedProjectJson the JSON representation of project contents
   * @param apiInfoJsonInScript
   *     the JSON-in-script representation of the Google APIs used by the project
   * @return the new {@code AppsScriptProject}
   */
  public static AppsScriptProject make(
      String fileId, String importedProjectJson, String apiInfoJsonInScript) {
    SortedMap<String, DriveScriptInfo> fileNamesToInfo =
        JSON_PROCESSOR.parse(Preconditions.checkNotNull(importedProjectJson));
    return new AppsScriptProject(
        Preconditions.checkNotNull(fileId),
        Preconditions.checkNotNull(fileNamesToInfo),
        Preconditions.checkNotNull(apiInfoJsonInScript),
        computeContentHash(fileNamesToInfo, ImmutableSet.<String>of()));
  }
  
  /**
   * Creates a new {@code AppsScriptProject} from Eclipse data. The inputs are a Drive file ID, a
   * map from script names to {@link DriveScriptInfo} objects, and a Drive version fingerprint.
   * A {@code DriveScriptInfo} holds the contents and metadata for one script.
   * 
   * @param fileId the Drive file ID
   * @param scriptNamesToInfo the map
   * @param driveVersionFingerprint
   *     the Drive version fingerprint of the latest Drive project version with which this
   *     {@code AppsScriptProject} is synchronized
   * @return the new {@code AppsScriptProject}
   */
  public static AppsScriptProject make(
      String fileId,
      Map<String, DriveScriptInfo> scriptNamesToInfo,
      String driveVersionFingerprint) {
    return new AppsScriptProject(
        Preconditions.checkNotNull(fileId),
        ImmutableSortedMap.copyOf(Preconditions.checkNotNull(scriptNamesToInfo)),
        "",
        Preconditions.checkNotNull(driveVersionFingerprint));
  }

  private AppsScriptProject(
      String fileId,
      SortedMap<String, DriveScriptInfo> fileNamesToInfo,
      String apiInfoJsonInScript,
      String driveVersionFingerprint) {
    this.fileId = fileId;
    this.fileNamesToInfo = fileNamesToInfo;
    this.apiInfoJsonInScript = apiInfoJsonInScript;
    this.driveVersionFingerprint = driveVersionFingerprint;
  }

  public String getDriveFileId() {
    return fileId;
  }
  
  public Set<String> getScriptFileNames() {
    return fileNamesToInfo.keySet();
  }
  
  /**
   * Obtains the {@link DriveScriptInfo} of the script file with a specified file name.
   * 
   * @param fileName the specified file name
   * @return the {@code DriveScriptInfo} associated with that file name
   * @throws IllegalArgumentException
   *     if this {@code AppsScriptProject} does not have a file with the specified file name
   */
  public DriveScriptInfo getScriptInfo(String fileName) throws IllegalArgumentException {
    DriveScriptInfo scriptInfo = fileNamesToInfo.get(fileName);
    if (scriptInfo == null) {
      throw new IllegalArgumentException(
          "AppsScriptProject does not have a file named <" + fileName + ">");
    }
    return scriptInfo;
  }
  
  /**
   * Obtains a JSON representation of modifications to this {@code AppsScriptProject}.
   * 
   * @param filesToSave
   *     the Eclipse names of files for which the JSON representation should include new content
   * @return a JSON representation of this {@code AppsScriptProject}
   */
  public String getProjectJson(Collection<String> filesToSave) {
    return JSON_PROCESSOR.toJson(fileNamesToInfo, filesToSave);
  }
  
  /**
   * Obtains a JSON-in-script representation of information about the APIs used in this
   * {@code AppsScriptProject}.
   * 
   * @return
   *     a JSON-in-script representation of information about the APIs used in this
   *     {@code AppsScriptProject}
   */
  public String getApiInfoJsonInScript() {
    return apiInfoJsonInScript;
  }

  /**
   * Retrieves the stored fingerprint of the last Drive project version with which this
   * {@code AppsScriptProject} is synchronized. In the case of an {@code AppsScriptProject}
   * created from a Drive project by the {@link #make(String, String, String)} method, the
   * fingerprint is computed by the {@code make} method from the contents of the project. In the
   * case of an {@code AppsScriptProject} created from an Eclipse project by the
   * {@link #make(String, Map, String)} method, a fingerprint that had been persistently stored in
   * Eclipse project preferences is passed into the {@code make} method.
   * 
   * @return the Drive version fingerprint
   */
  public String getDriveVersionFingerprint() {
    return driveVersionFingerprint;
  }
  
  private static final char[] HEX_DIGITS =
      {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
  
  /**
   * Obtains a fingerprint based on the content of this {@code AppsScriptProject}, ignoring the
   * files named in a specified set of excluded file names.
   * 
   * (When computing the fingerprint of a Drive project to compare it to the last-synchronized
   * version fingerprint of an Eclipse project, we want to ignore files that the user has deleted
   * from Eclipse, but deliberately retained in Drive, so this method is called with the set of
   * names of the deleted-but-retained files.)
   * 
   * @param excludedFiles the specified set of excluded file names
   * @return the fingerprint, consisting of 32 hexadecimal digits
   */
  public String getContentHash(Set<String> excludedFiles) {
    return computeContentHash(fileNamesToInfo, excludedFiles);
  }
  
  // Returns the hexadecimal representation of the 128-bit MD5 hash of a string of the form
  //   file_name_1=>file_content_1#file_name_2=>file_content_2#...
  // where the file names are sorted.
  private static String computeContentHash(
      SortedMap<String, DriveScriptInfo> fileNamesToInfo, Set<String> excludedFiles) {
    StringBuilder contentStringBuilder = new StringBuilder();
    for (Entry<String, DriveScriptInfo> entry : fileNamesToInfo.entrySet()) {
      // Since fileNamesToInfo is a SortedMap, the order of the entries is well-defined.
      String fileName = entry.getKey();
      if (!excludedFiles.contains(fileName) ) {
        contentStringBuilder.append(fileName);
        contentStringBuilder.append("=>");
        contentStringBuilder.append(entry.getValue().getContents());
        contentStringBuilder.append("#");
      }
    }
    byte[] contentBytes = contentStringBuilder.toString().getBytes(Charsets.UTF_8);
    
    byte[] hash = Hashing.md5().hashBytes(contentBytes).asBytes();
    char[] resultChars = new char[2 * hash.length];
    int cursor = 0;
    for (byte b : hash) {
      resultChars[cursor] = HEX_DIGITS[(b & 0xf0) >> 4];
      resultChars[cursor + 1] = HEX_DIGITS[b & 0x0f];
      cursor += 2;
    }
    return new String(resultChars);
  }

  @Override
  public String toString() {
    return
        Objects.toStringHelper(this)
            .add("fileId", fileId)
            .add("fileNamesToInfo", fileNamesToInfo)
            .add("apiInfoJsonInScript", apiInfoJsonInScript)
            .add("driveVersionFingerprint", driveVersionFingerprint)
            .toString();
  }
}
