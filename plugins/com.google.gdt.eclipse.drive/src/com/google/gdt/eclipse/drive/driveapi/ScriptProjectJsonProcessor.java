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

package com.google.gdt.eclipse.drive.driveapi;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gdt.eclipse.drive.DrivePlugin;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.concurrent.Immutable;

/**
 * Translates between JSON representations of a Drive Apps Script project and maps from script file
 * names to script file contents. The JSON representation of a Drive Apps Script project looks like
 * this (but without any whitespace outside of string constants):
 * <pre>
 * {
 *   "files": [
 *     {
 *       "id": "sOmEdRiVeScRiPtId1234567890",
 *       "name": "first.gs",
 *       "type": "server_js",
 *       "source": "function f() {\n  \n}\n",
 *     },
 *     {
 *       "id": "aNoThErDrIvEsCrIpTiD0987654321",
 *       "name": "second.gs",
 *       "type": "html",
 *       "source": "function g() {\n  \n}\n",
 *     }
 *   ]
 * }
 * </pre>
 */
@Immutable
public class ScriptProjectJsonProcessor {
  
  private static final Gson gson = new Gson();

  /**
   * Translates the JSON representation of a Drive Apps Script project into a sorted map from script
   * file names without extensions to {@link DriveScriptInfo} objects.
   * 
   * @param importedProjectJson the JSON representation of the Drive Apps Script project
   * @return
   *     a sorted map whose keys are script file names and whose values are the corresponding
   *     {@link DriveScriptInfo} objects
   */
  public SortedMap<String, DriveScriptInfo> parse(String importedProjectJson) {
    ScriptProject parsedProject;
    try {
      parsedProject = gson.fromJson(importedProjectJson, ScriptProject.class);
    } catch (JsonSyntaxException e) {
      DrivePlugin.logError(e.getMessage() + ": <" + importedProjectJson + ">" , e);
      return ImmutableSortedMap.of();
    }
    SortedMap<String, DriveScriptInfo> result = Maps.newTreeMap();
    for (ScriptFile file : parsedProject.files) {
      String nameWithExtension = FileTypes.fileNameWithExtension(file.name, file.type);
      DriveScriptInfo info = new DriveScriptInfo(file.name, file.id, file.type, file.source);
      result.put(nameWithExtension, info);
    }
    return result;
  }
  
  /**
   * Translates a map, from script file names with or without extensions to {@link ScriptInfo}
   * objects, into the JSON representation of an Apps Script project to be uploaded to Drive.
   * The Drive API for Apps Script treats the JSON as follows:
   * <ul>
   *   <li>Any file in the Drive Apps Script project whose file ID does not occur in the "id"
   *       field of one of the elements of the "files" field is deleted.
   *   <li>Any element of the "files" field whose "id" field is missing is treated as a request to
   *       insert a new file.
   *   <li>Any element of the "files" field whose "id", "name", and "source" fields are all present
   *       is treated as a request to replace the name and contents of the Drive Apps Script file
   *       that has the same file ID (although the name will typically be the same).
   *   <li>Any element of the "files" field whose "id" field is present, but whose "name" and
   *       "source" fields are absent is treated as a request to retain the Drive Apps Script file
   *       with the same file ID, without any changes to the name or content of the file.
   * </ul>
   * 
   * @param scriptNamesToScriptInfo the map
   * @param modifiedFiles the Eclipse file names of files that are to be written to Drive
   * @return the JSON string
   */
  public String toJson(
      Map<String, DriveScriptInfo> scriptNamesToScriptInfo, Collection<String> modifiedFiles) {
    Set<String> modifiedFileDriveNames = Sets.newHashSet();
    for (String eclipseName : modifiedFiles) {
      modifiedFileDriveNames.add(FileTypes.stripExtension(eclipseName));
    }
    ScriptProject scriptList = new ScriptProject();
    scriptList.files = Lists.newArrayListWithCapacity(scriptNamesToScriptInfo.size());
    for (DriveScriptInfo scriptInfo : scriptNamesToScriptInfo.values()) {
      String driveName = scriptInfo.getImportName();
      ScriptFile file = new ScriptFile();
      file.id = scriptInfo.getDocumentId();
      file.type = scriptInfo.getType();
      if (modifiedFileDriveNames.contains(driveName)) {
        file.name = driveName;
        file.source = scriptInfo.getContents();
      }
      scriptList.files.add(file);
    }
    return gson.toJson(scriptList);
  }
  
  /**
   * A Java object representing an individual Apps Script script within a {@link ScriptProject}.
   */
  private static class ScriptFile {
    public String id;
    public String name;
    public String type;
    public String source;
  }
  
  /**
   * A Java object representing the contents of an Apps Script project, created by the {@link Gson}
   * parser from a JSON string downloaded from Drive, or serialized by the {@code Gson} serializer
   * to produce a JSON string to be uploaded to Drive.
   */
  private static class ScriptProject {
    public List<ScriptFile> files;
  }
  
}
