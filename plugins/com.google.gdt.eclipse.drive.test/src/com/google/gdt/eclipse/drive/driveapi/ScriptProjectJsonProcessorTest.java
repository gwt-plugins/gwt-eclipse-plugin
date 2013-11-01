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
package com.google.gdt.eclipse.drive.driveapi;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Unit test for {@link ScriptProjectJsonProcessor}.
 */
@RunWith(JUnit4.class)
public class ScriptProjectJsonProcessorTest {
  
  private static final String PROJECT_TO_IMPORT_JSON_FORM =
      "{\"files\":[" 
          + "{\"id\":\"docId1\",\"name\":\"file1\",\"type\":\"server_js\","
              + "\"source\":\"file 1 contents\"},"
          + "{\"id\":\"docId2\",\"name\":\"file2\",\"type\":\"html\","
              + "\"source\":\"file 2 contents\"}"
          + "]}";
  
  private static final Map<String, DriveScriptInfo> PROJECT_TO_IMPORT_JAVA_FORM =
      ImmutableMap.<String, DriveScriptInfo>builder()
          .put("file1.gs", new DriveScriptInfo("file1", "docId1", "server_js", "file 1 contents"))
          .put("file2.html", new DriveScriptInfo("file2", "docId2", "html", "file 2 contents"))
          .build();
  
  private static final Map<String, DriveScriptInfo> PROJECT_TO_SAVE_JAVA_FORM =
      ImmutableMap.<String, DriveScriptInfo>builder()
          .put("file1.gs", new DriveScriptInfo("file1", "docId1", "server_js", "file 1 contents"))
          .put("file2.gs", new DriveScriptInfo("file2", "docId2", "server_js", "file 2 contents"))
          .put("file3.html", new DriveScriptInfo("file3", "docId3", "html", "file 3 contents"))
          .put("file4.html", new DriveScriptInfo("file4", "docId4", "html", "file 4 contents"))
          .put("new_gs.gs", new DriveScriptInfo("new_gs", null, "server_js", "new_gs.gs contents"))
          .put(
              "new_html.html",
              new DriveScriptInfo("new_html", null, "html", "new_html.html contents"))
          .build();
  
  private static final Map<String, DriveScriptInfo> PROJECT_TO_SAVE_JAVA_FORM_NO_EXTENSIONS =
      ImmutableMap.<String, DriveScriptInfo>builder()
          .put("file1", new DriveScriptInfo("file1", "docId1", "server_js", "file 1 contents"))
          .put("file2", new DriveScriptInfo("file2", "docId2", "server_js", "file 2 contents"))
          .put("file3", new DriveScriptInfo("file3", "docId3", "html", "file 3 contents"))
          .put("file4", new DriveScriptInfo("file4", "docId4", "html", "file 4 contents"))
          .put("new_gs", new DriveScriptInfo("new_gs", null, "server_js", "new_gs.gs contents"))
          .put("new_html", new DriveScriptInfo("new_html", null, "html", "new_html.html contents"))
          .build();
  
  private static final String PROJECT_TO_SAVE_JSON_FORM =
      "{\"files\":[" 
          + "{\"id\":\"docId1\",\"name\":\"file1\",\"type\":\"server_js\","
              + "\"source\":\"file 1 contents\"},"
          + "{\"id\":\"docId2\",\"type\":\"server_js\"},"
          + "{\"id\":\"docId3\",\"name\":\"file3\",\"type\":\"html\","
              + "\"source\":\"file 3 contents\"},"
          + "{\"id\":\"docId4\",\"type\":\"html\"},"
          + "{\"name\":\"new_gs\",\"type\":\"server_js\",\"source\":\"new_gs.gs contents\"},"
          + "{\"name\":\"new_html\",\"type\":\"html\",\"source\":\"new_html.html contents\"}"
          + "]}";
  
  @Test
  public void testParse() {
    ScriptProjectJsonProcessor processor = new ScriptProjectJsonProcessor();
    assertEquals(PROJECT_TO_IMPORT_JAVA_FORM, processor.parse(PROJECT_TO_IMPORT_JSON_FORM));
  }

  @Test
  public void testToJson() {
    ScriptProjectJsonProcessor processor = new ScriptProjectJsonProcessor();
    assertEquals(
        PROJECT_TO_SAVE_JSON_FORM,
        processor.toJson(
            PROJECT_TO_SAVE_JAVA_FORM,
            ImmutableList.of("file1.gs", "file3.html", "new_gs.gs", "new_html.html")));
    assertEquals(
        PROJECT_TO_SAVE_JSON_FORM,
        processor.toJson(
            PROJECT_TO_SAVE_JAVA_FORM_NO_EXTENSIONS,
            ImmutableList.of("file1.gs", "file3.html", "new_gs.gs", "new_html.html")));
  }

}
