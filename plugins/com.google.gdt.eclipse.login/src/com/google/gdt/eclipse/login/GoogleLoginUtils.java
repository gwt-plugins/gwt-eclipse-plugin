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
package com.google.gdt.eclipse.login;

import com.google.gdt.eclipse.core.extensions.ExtensionQuery.Data;
import com.google.gdt.eclipse.core.extensions.ExtensionQueryStringAttr;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utils for GoogleLogin.
 */
public class GoogleLoginUtils {
  
  /**
   * Takes a string that looks like "param1=val1&param2=val2&param3=val3" and
   * puts the key-value pairs into a map. The string is assumed to be UTF-8
   * encoded. If the string has a '?' character, then only the characters after
   * the question mark are considered.
   * 
   * @param params The parameter string.
   * @return A map with the key value pairs
   * @throws UnsupportedEncodingException if UTF-8 encoding is not supported
   */
  public static Map<String, String> parseUrlParameters(String params)
      throws UnsupportedEncodingException {
    Map<String, String> paramMap = new HashMap<String, String>();
    
    int qMark = params.indexOf('?');
    if (qMark > -1) {
      params = params.substring(qMark + 1);
    }
    
    String[] paramArr = params.split("&");
    for (String s : paramArr) {
      String[] keyVal = s.split("=");
      if (keyVal.length == 2) {
        paramMap.put(URLDecoder.decode(keyVal[0], "UTF-8"), URLDecoder.decode(
            keyVal[1], "UTF-8"));
      }
    }
    return paramMap;
  }

  /**
   * Returns a space delimited string of the OAuth scope contributions.
   */
  protected static SortedSet<String> queryOAuthScopeExtensions() {
    ExtensionQueryStringAttr q = new ExtensionQueryStringAttr(
        GoogleLoginPlugin.PLUGIN_ID, "oauthScope", "scope");
    List<Data<String>> data = q.getData();
    SortedSet<String> scopes = new TreeSet<String>();
    for (Data<String> scopeData : data) {
      scopes.add(scopeData.getExtensionPointData().trim());
    }
    return scopes;
  }
}
