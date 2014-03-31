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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utils for GoogleLogin.
 */
public class GoogleLoginUtils {
  
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
