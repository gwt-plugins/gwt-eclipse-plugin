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
package com.google.gdt.eclipse.drive.test;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.util.Charsets;
import com.google.gdt.eclipse.drive.driveapi.DriveServiceFacade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link HttpTransport} object that should be passed to a {@link DriveServiceFacade}
 * constructor to cause GET requests to an App Script project's export link to result in a
 * response with mocked App Script project content. The mocked App Script project contains two
 * scripts, one with name {@link #getScriptFileName}{@code (1)} and contents
 * {@link #getScriptText}{@code (1)} and one with name {@link #getScriptFileName}{@code (2)} and
 * contents {@link #getScriptText}{@code (2)}.
 * 
 * The {@link #buildRequest} method builds a {@link LowLevelHttpRequest} object whose
 * {@code execute} method returns a {@link LowLevelHttpResponse} with the mocked content.
 */

public final class MockAppsScriptExportingTransport extends HttpTransport {
  
  /**
   * @param method ignored
   * @param url ignored
   * @return
   *     a {@link LowLevelHttpRequest} object whose {@code execute} method returns a
   *     {@link LowLevelHttpResponse} with the mocked content
   */
  @Override
  protected LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
    return
        new LowLevelHttpRequest() {
          @Override public void addHeader(String name, String value) throws IOException {
          }

          @Override public LowLevelHttpResponse execute() throws IOException {
            return new MockResponse();
          }
        };
  }
  
  /**
   * Reports the text of script {@code i} in the Apps Script project in leaf 0 of the mocked file
   * system.
   * 
   * @param i either 1 or 2
   * @return the text of the script
   */
  public static String getScriptText(int i) {
    return "function f" + i + "() {\n\n}\n";
  }

  /**
   * Reports the file name (with no extension) of script {@code i} in the Apps Script project in
   * leaf 0 of the mocked file system.
   * 
   * @param i either 1 or 2
   * @return the file name
   */
  public static String getScriptFileName(int i) {
    return "script" + i;
  }

  private static final class MockResponse extends LowLevelHttpResponse {

    @Override public int getStatusCode() throws IOException {
      return 200;
    }

    @Override public String getContentType() throws IOException {
      return DriveServiceFacade.JSON_MIME_TYPE;
    }

    @Override public InputStream getContent() throws IOException {
      return new ByteArrayInputStream(makeLeaf0Json().getBytes(Charsets.UTF_8));
    }

    @Override public String getContentEncoding() throws IOException {
      return "UTF8";
    }

    @Override public String getReasonPhrase() throws IOException {
      return "just because";
    }

    @Override public int getHeaderCount() throws IOException {
      return 0;
    }

    @Override public long getContentLength() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override public String getHeaderName(int arg0) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override public String getHeaderValue(int arg0) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override public String getStatusLine() throws IOException {
      throw new UnsupportedOperationException();
    }    
  }
  
  private static String makeLeaf0Json() {
    StringBuilder resultBuilder = new StringBuilder();
    resultBuilder.append("{\"files\":[");
    appendScriptJson(resultBuilder, 1);
    resultBuilder.append(",");
    appendScriptJson(resultBuilder, 2);
    resultBuilder.append("]}");
    return resultBuilder.toString();
  }
  
  private static void appendScriptJson(StringBuilder target, int scriptNumber) {
    target.append("{\"id\":\"fake script id ");
    target.append(scriptNumber);
    target.append("\",\"name\":\"");
    target.append(MockAppsScriptExportingTransport.getScriptFileName(scriptNumber));
    target.append("\",\"type\":\"server_js\",\"source\":\"");
    // The call on replaceAll below replaces each newline character with a backslash followed by
    // 'n'. (The Java string literal "\\n" passed as the first argument denotes a string that
    // contains a backslash followed by an 'n', which replaceAll treats as a regular expression
    // matching a newline character. The Java string literal "\\\\n" passed as the second argument
    // denotes a string that contains two backslashes followed by an 'n', which replaceAll treats as
    // a replacement string consisting of a single backslash and an 'n'.)
    target.append(getScriptText(scriptNumber).replaceAll("\\n", "\\\\n"));
    target.append("\"}");
  }
}