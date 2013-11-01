/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.client.util.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gdt.eclipse.drive.driveapi.DriveScriptInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link AppsScriptProject}.
 */
@RunWith(JUnit4.class)
public class AppsScriptProjectTest {
  
  private static final String PROJECT_ID = "project ID";
  private static final String SCRIPT_FILE_TYPE = "server_js";
  private static final String HTML_FILE_TYPE = "html";
  
  private static final String FILE_1_ID = "file 1 ID";
  private static final String FILE_1_IMPORT_NAME = "f1script";
  private static final String FILE_1_FILE_NAME = FILE_1_IMPORT_NAME + ".gs";
  private static final String FILE_1_ESCAPED_SOURCE = "function f1() {\\n  // text of f1\\n}";
  private static final String FILE_1_SOURCE = FILE_1_ESCAPED_SOURCE.replaceAll("\\\\n", "\n");
  private static final String FILE_1_JSON_FOR_UPDATE =
      updatedFileJson(
          FILE_1_ID, FILE_1_IMPORT_NAME, SCRIPT_FILE_TYPE, FILE_1_ESCAPED_SOURCE);
  private static final String FILE_1_JSON_FOR_RETENTION = retainedFileJson(FILE_1_ID);
  private static final DriveScriptInfo SCRIPT_1_INFO =
      new DriveScriptInfo(FILE_1_IMPORT_NAME, FILE_1_ID, SCRIPT_FILE_TYPE, FILE_1_SOURCE);

  private static final String FILE_2_ID = "file 2 ID";
  private static final String FILE_2_IMPORT_NAME = "f2script";
  private static final String FILE_2_FILE_NAME = FILE_2_IMPORT_NAME + ".gs";
  private static final String FILE_2_ESCAPED_SOURCE = "function f2() {\\n  // text of f2\\n}";
  private static final String FILE_2_SOURCE = FILE_2_ESCAPED_SOURCE.replaceAll("\\\\n", "\n");
  private static final String FILE_2_JSON_FOR_UPDATE =
      updatedFileJson(
          FILE_2_ID, FILE_2_IMPORT_NAME, SCRIPT_FILE_TYPE, FILE_2_ESCAPED_SOURCE);
  private static final String FILE_2_JSON_FOR_RETENTION = retainedFileJson(FILE_2_ID);
  private static final DriveScriptInfo SCRIPT_2_INFO =
      new DriveScriptInfo(FILE_2_IMPORT_NAME, FILE_2_ID, SCRIPT_FILE_TYPE, FILE_2_SOURCE);
  
  private static final String FILE_LIST_JSON_UPDATE_1_UPDATE_2 =
      delimitedList('[', ']', FILE_1_JSON_FOR_UPDATE, FILE_2_JSON_FOR_UPDATE);
  private static final String FILE_LIST_JSON_UPDATE_2_UPDATE_1 =
      delimitedList('[', ']', FILE_2_JSON_FOR_UPDATE, FILE_1_JSON_FOR_UPDATE);

  private static final String FILE_LIST_JSON_UPDATE_1_RETAIN_2 =
      delimitedList('[', ']', FILE_1_JSON_FOR_UPDATE, FILE_2_JSON_FOR_RETENTION);
  private static final String FILE_LIST_JSON_RETAIN_2_UPDATE_1 =
      delimitedList('[', ']', FILE_2_JSON_FOR_RETENTION, FILE_1_JSON_FOR_UPDATE);

  private static final String FILE_LIST_JSON_RETAIN_1_UPDATE_2 =
      delimitedList('[', ']', FILE_1_JSON_FOR_RETENTION, FILE_2_JSON_FOR_UPDATE);
  private static final String FILE_LIST_JSON_UPDATE_2_RETAIN_1 =
      delimitedList('[', ']', FILE_2_JSON_FOR_UPDATE, FILE_1_JSON_FOR_RETENTION);

  private static final String FILE_LIST_JSON_RETAIN_1_RETAIN_2 =
      delimitedList('[', ']', FILE_1_JSON_FOR_RETENTION, FILE_2_JSON_FOR_RETENTION);
  private static final String FILE_LIST_JSON_RETAIN_2_RETAIN_1 =
      delimitedList('[', ']', FILE_2_JSON_FOR_RETENTION, FILE_1_JSON_FOR_RETENTION);
  
  private static final Map<String, DriveScriptInfo> SCRIPT_INFO_MAP =
      ImmutableMap.<String, DriveScriptInfo>builder()
          .put(FILE_1_FILE_NAME, SCRIPT_1_INFO)
          .put(FILE_2_FILE_NAME, SCRIPT_2_INFO)
          .build();
  
  private static final String API_INFO = "API info";
  
  @Test
  public void testMakeAndSimpleGetters_fromDriveProject() {
    AppsScriptProject project =
        AppsScriptProject.make(
            PROJECT_ID, projectJson(FILE_LIST_JSON_UPDATE_1_UPDATE_2), API_INFO);
    String fingerprint = project.getContentHash(ImmutableSet.<String>of());
    
    assertEquals(PROJECT_ID, project.getDriveFileId());
    assertEquals(ImmutableSet.of(FILE_1_FILE_NAME, FILE_2_FILE_NAME), project.getScriptFileNames());
    assertEquals(SCRIPT_1_INFO, project.getScriptInfo(FILE_1_FILE_NAME));
    assertEquals(SCRIPT_2_INFO, project.getScriptInfo(FILE_2_FILE_NAME));
    assertEquals(API_INFO, project.getApiInfoJsonInScript());
    assertEquals(fingerprint, project.getDriveVersionFingerprint());
  }
  
  @Test
  public void testMakeAndSimpleGetters_fromEclipseProject() {
    AppsScriptProject project1 = AppsScriptProject.make(PROJECT_ID, SCRIPT_INFO_MAP, "fingerprint");
    
    assertEquals(PROJECT_ID, project1.getDriveFileId());
    assertEquals(
        ImmutableSet.of(FILE_1_FILE_NAME, FILE_2_FILE_NAME), project1.getScriptFileNames());
    assertEquals(SCRIPT_1_INFO, project1.getScriptInfo(FILE_1_FILE_NAME));
    assertEquals(SCRIPT_2_INFO, project1.getScriptInfo(FILE_2_FILE_NAME));
    assertEquals("fingerprint", project1.getDriveVersionFingerprint());
    assertEquals("", project1.getApiInfoJsonInScript());
  }
  
  @Test
  public void testGetContentHash_fileIdDoesNotMatter() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name", SCRIPT_FILE_TYPE, "content"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id2", "name", SCRIPT_FILE_TYPE, "content"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertTrue(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_nameMatters() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name1", SCRIPT_FILE_TYPE, "content"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name2", SCRIPT_FILE_TYPE, "content"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertFalse(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_typeMatters() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name", SCRIPT_FILE_TYPE, "content"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name", HTML_FILE_TYPE, "content"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertFalse(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_textMatters() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name", SCRIPT_FILE_TYPE, "content1"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id", "name", SCRIPT_FILE_TYPE, "content2"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertFalse(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_additionalFileMatters() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"),
            updatedFileJson("id2", "name2", SCRIPT_FILE_TYPE, "content2"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertFalse(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_fileOrderDoesNotMatter() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"),
            updatedFileJson("id2", "name2", SCRIPT_FILE_TYPE, "content2"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id2", "name2", SCRIPT_FILE_TYPE, "content2"),
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertTrue(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_typeToTextPairingMatters() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"),
            updatedFileJson("id2", "name2", HTML_FILE_TYPE, "content2"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", HTML_FILE_TYPE, "content1"),
            updatedFileJson("id2", "name2", SCRIPT_FILE_TYPE, "content2"));
    String hash1 = project1.getContentHash(ImmutableSet.<String>of());
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertFalse(hash1.equals(hash2));
  }
  
  @Test
  public void testGetContentHash_exclusionWorks() {
    AppsScriptProject project1 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id1", "name1", SCRIPT_FILE_TYPE, "content1"),
            updatedFileJson("id2", "name2", HTML_FILE_TYPE, "content2"),
            updatedFileJson("id3", "name3", SCRIPT_FILE_TYPE, "content3"));
    AppsScriptProject project2 =
        projectForFileJsons(
            "fileId1",
            updatedFileJson("id3", "name3", SCRIPT_FILE_TYPE, "content3"));
    String hash1 = project1.getContentHash(ImmutableSet.of("name1.gs", "name2.html"));
    String hash2 = project2.getContentHash(ImmutableSet.<String>of());
    assertTrue(hash1.equals(hash2));
  }
  
  @Test
  public void testGetProjectJson() {
    AppsScriptProject project = AppsScriptProject.make(PROJECT_ID, SCRIPT_INFO_MAP, "fingerprint");
    
    String computedProjectJson12 =
        project.getProjectJson(ImmutableSet.of(FILE_1_IMPORT_NAME, FILE_2_IMPORT_NAME));
    assertEqualsEither(
        projectJson(FILE_LIST_JSON_UPDATE_1_UPDATE_2),
        projectJson(FILE_LIST_JSON_UPDATE_2_UPDATE_1),
        computedProjectJson12);
    
    String computedProjectJson1 = project.getProjectJson(ImmutableList.of(FILE_1_IMPORT_NAME));
    assertEqualsEither(
        projectJson(FILE_LIST_JSON_UPDATE_1_RETAIN_2),
        projectJson(FILE_LIST_JSON_RETAIN_2_UPDATE_1),
        computedProjectJson1);
    
    String computedProjectJson2 = project.getProjectJson(ImmutableList.of(FILE_2_IMPORT_NAME));
    assertEqualsEither(
        projectJson(FILE_LIST_JSON_RETAIN_1_UPDATE_2),
        projectJson(FILE_LIST_JSON_UPDATE_2_RETAIN_1),
        computedProjectJson2);
    
    String computedProjectJsonNone = project.getProjectJson(ImmutableList.<String>of());
    assertEqualsEither(
        projectJson(FILE_LIST_JSON_RETAIN_1_RETAIN_2),
        projectJson(FILE_LIST_JSON_RETAIN_2_RETAIN_1),
        computedProjectJsonNone);
  }
  
  private static void assertEqualsEither(String expected1, String expected2, String actual) {
    Set<String> expectedSet = ImmutableSet.of(expected1, expected2);
    assertTrue(expectedSet.contains(actual));
  }
  
  private static final AppsScriptProject projectForFileJsons(String fileId, String ... fileJsons) {
    return
        AppsScriptProject.make(fileId, projectJson(delimitedList('[', ']', fileJsons)), "");
  }
  
  private static final String projectJson(String fileListJson) {
    return delimitedList('{', '}', quoted("files") + ":" + fileListJson);
  }
  
  private static final String updatedFileJson(
      String fileId, String fileName, String fileType, String escapedSource) {
    return
        delimitedList(
            '{',
            '}',
            nameValuePair("id", fileId),
            nameValuePair("name", fileName),
            nameValuePair("type", fileType),
            nameValuePair("source", escapedSource));
  }
  
  private static final String retainedFileJson(String fileId) {
    return
        delimitedList(
            '{',
            '}',
            nameValuePair("id", fileId),
            nameValuePair("type", SCRIPT_FILE_TYPE));
  }
  
  private static final String delimitedList(
      char leftDelimiter, char rightDelimiter, String... items) {
    return leftDelimiter + Joiner.on(',').join(Arrays.asList(items)) + rightDelimiter;
  }
  
  private static final String nameValuePair(String name, String value) {
    return quoted(name) + ":" + quoted(value);
  }
  
  private static final String quoted(String contentNotNeedingEscapes) {
    return '"' + contentNotNeedingEscapes + '"';
  }

}
