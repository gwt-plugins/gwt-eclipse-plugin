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

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.util.Data;
import com.google.api.client.util.FieldInfo;
import com.google.api.client.util.Types;

import java.util.Map;

/**
 * Provides a method for obtaining a string representation of an {@link HttpRequest}. The logic is
 * extracted from the logging logic interleaved in {@link HttpRequest#execute()}.
 */
public class HttpRequestUtils {

  private HttpRequestUtils() { // prevent instantiation
  }
  
  public static String toString(HttpRequest request) {
    StringBuilder resultBuilder = new StringBuilder();
    
    resultBuilder.append(request.getRequestMethod());
    resultBuilder.append(' ');
    resultBuilder.append(request.getUrl().build());
    resultBuilder.append('\n');
    
    HttpHeaders headers = request.getHeaders();
    for (Map.Entry<String, Object> headerEntry : request.getHeaders().entrySet()) {
      String name = headerEntry.getKey();
      Object value = headerEntry.getValue();
      if (value != null) {
        // compute the display name from the declared field name to fix capitalization
        String displayName = name;
        FieldInfo fieldInfo = headers.getClassInfo().getFieldInfo(name);
        if (fieldInfo != null) {
          displayName = fieldInfo.getName();
        }
        Class<? extends Object> valueClass = value.getClass();
        if (value instanceof Iterable<?> || valueClass.isArray()) {
          for (Object repeatedValue : Types.iterableOf(value)) {
            addHeader(resultBuilder, displayName, repeatedValue);
          }
        } else {
          addHeader(resultBuilder, displayName, value);
        }
      }
    }

    return resultBuilder.toString();

  }

  private static void addHeader(StringBuilder target, String name, Object value) {
    // ignore nulls
    if (value == null || Data.isNull(value)) {
      return;
    }
    // compute value
    String stringValue =
        value instanceof Enum<?> ? FieldInfo.of((Enum<?>) value).getName() : value.toString();
    // log header
    String loggedStringValue = stringValue;
    if (("Authorization".equalsIgnoreCase(name) || "Cookie".equalsIgnoreCase(name))) {
      loggedStringValue = "<Not Logged>";
    }
    if (target != null) {
      target.append(name).append(": ");
      target.append(loggedStringValue);
      target.append('\n');
    }
  }
}
