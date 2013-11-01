/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.login;

import com.google.gdt.eclipse.login.GoogleLoginPrefs.Credentials;

import java.util.HashSet;
import java.util.Set;

/**
 * A testing utility used to perform a Google login for a fictional user. The user's account is
 * gpe.team.1@gmail.com.
 */
public class TestGoogleLogin {

  private static final String USER_ACCESS_TOKEN =
      "ya29.AHES6ZTVKEAKi2avC2oiI4sQ1KOrYQsx8i14tf6iA5jlhQ";

  private static final String USER_REFRESH_TOKEN = "1/1p76tQ6yjvxXh2E3K0urmdfEgKvatecHd_eUTj0tqF4";

  private static final String USER_STORED_EMAIL = "gpe.team.1@gmail.com";

  private static final String[] USER_STORED_SCOPES = {
      "https://www.googleapis.com/auth/projecthosting",
      "https://www.googleapis.com/auth/userinfo#email",
      "https://www.googleapis.com/auth/appsmarketplace",
      "https://www.googleapis.com/auth/sqlservice",
      "https://www.googleapis.com/auth/appengine.admin",
      "https://www.googleapis.com/auth/drive",
      "https://www.googleapis.com/auth/drive.scripts"};

  /**
   * Returns user's stored email address.
   */
  public static String getEmail() {
    return USER_STORED_EMAIL;
  }

  /**
   * Logs in to Google.
   */
  public static void logIn() {
    Credentials credentials = createCredentials();
    GoogleLoginPrefs.saveCredentials(credentials);
    GoogleLogin.getInstance().updateInternalLogin(true);
  }

  /**
   * Logs out of Google.
   */
  public static void logOut() {
    GoogleLogin.getInstance().logOut(false);
    GoogleLogin.getInstance().updateInternalLogin(false);
  }

  /**
   * Create Credential for gpe.team.1@google.com.
   * 
   * @return The credential.
   */
  private static Credentials createCredentials() {
    Set<String> storedScopes = new HashSet<String>();
    for (String scope : USER_STORED_SCOPES) {
      storedScopes.add(scope);
    }

    Credentials credentials =
        new Credentials(USER_ACCESS_TOKEN, USER_REFRESH_TOKEN, USER_STORED_EMAIL, storedScopes, 0);
    return credentials;
  }
}
