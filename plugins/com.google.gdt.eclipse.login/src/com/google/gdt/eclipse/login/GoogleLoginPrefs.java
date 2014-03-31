/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.login;

import com.google.common.base.Joiner;
import com.google.gdt.eclipse.login.common.OAuthData;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Handles preferences for Google Login plugin.
 * 
 */
public class GoogleLoginPrefs {
  
  // Delimiter for the list of scopes. 
  private static final String SCOPE_DELIMITER = " ";

  private static final String OAUTH_DATA_EMAIL_KEY = "credentials_email";
  private static final String OAUTH_DATA_ACCESS_TOKEN_KEY = "credentials_access_token";
  private static final String OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY =
      "credentials_access_token_expiry_time";
  private static final String OAUTH_DATA_REFRESH_TOKEN_KEY = "credentials_refresh_token";
  private static final String ICON_ONLY_KEY = "icon_only";
  private static final String LOGOUT_ON_EXIT_KEY = "logout_on_exit";
  private static final String OAUTH_SCOPES_KEY = "oauth_scopes";

  private static final String PREFERENCES_PATH = "/com/google/gdt/eclipse/login";

  public static void clearStoredOAuthData() {
    Preferences prefs = getPrefs();
    prefs.remove(OAUTH_DATA_ACCESS_TOKEN_KEY);
    prefs.remove(OAUTH_DATA_REFRESH_TOKEN_KEY);
    prefs.remove(OAUTH_DATA_EMAIL_KEY);
    prefs.remove(OAUTH_SCOPES_KEY);
    prefs.remove(OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY);
    flushPrefs(prefs);
  }

  public static boolean getIconOnlyPref() {
    return getPrefs().getBoolean(ICON_ONLY_KEY, false);
  }

  // Based on the value of the entry with key "logout_on_exit" in
  // ~/.java/.userPrefs/com/google/gdt/eclipse/login/prefs.xml
  public static boolean getLogoutOnExitPref() {
    return getPrefs().getBoolean(LOGOUT_ON_EXIT_KEY, false);
  }

  public static OAuthData loadOAuthData() {
    Preferences prefs = getPrefs();

    String accessToken = prefs.get(OAUTH_DATA_ACCESS_TOKEN_KEY, null);
    String refreshToken = prefs.get(OAUTH_DATA_REFRESH_TOKEN_KEY, null);
    String storedEmail = prefs.get(OAUTH_DATA_EMAIL_KEY, null);
    String storedScopesString = prefs.get(OAUTH_SCOPES_KEY, null);
    
    // Use a set to ensure uniqueness.
    SortedSet<String> storedScopes = new TreeSet<String>();
    if (storedScopesString != null) {
      for (String scope : storedScopesString.split(SCOPE_DELIMITER)) {
        storedScopes.add(scope);
      }
    }
    long accessTokenExpiryTime = 0;
    String accessTokenExpiryTimeString = prefs.get(OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY, null);
    if (accessTokenExpiryTimeString != null) {
      accessTokenExpiryTime = Long.parseLong(accessTokenExpiryTimeString);
    }
    return new OAuthData(
        accessToken, refreshToken, storedEmail, storedScopes, accessTokenExpiryTime);
  }

  public static void saveOAuthData(OAuthData dataToBeSaved) {
    Preferences prefs = getPrefs();
    prefs.put(OAUTH_DATA_ACCESS_TOKEN_KEY, dataToBeSaved.getAccessToken());
    prefs.put(OAUTH_DATA_REFRESH_TOKEN_KEY, dataToBeSaved.getRefreshToken());
    prefs.put(
        OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY,
        Long.toString(dataToBeSaved.getAccessTokenExpiryTime()));

    // we save the scopes so that if the user updates the plugin and the
    // scopes change, we can force the plugin to log out.
    Joiner joiner = Joiner.on(SCOPE_DELIMITER);
    prefs.put(OAUTH_SCOPES_KEY, joiner.join(dataToBeSaved.getStoredScopes()));

    String storedEmail = dataToBeSaved.getStoredEmail();
    if (storedEmail != null) {
      prefs.put(OAUTH_DATA_EMAIL_KEY, storedEmail);
    }
    flushPrefs(prefs);
  }

  public static void saveIconOnlyPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(ICON_ONLY_KEY, logoutOnExit);
    flushPrefs(prefs);
  }

  /**
   * Sets and saves to the preferences store if the user should be logged out
   * when Eclipse extis.
   */
  public static void saveLogoutOnExitPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(LOGOUT_ON_EXIT_KEY, logoutOnExit);
    flushPrefs(prefs);
  }

  private static void flushPrefs(Preferences prefs) {
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      GoogleLoginPlugin.logError("Could not flush preferences while saving login credentials", e);
    }
  }

  private static Preferences getPrefs() {
    return Preferences.userRoot().node(PREFERENCES_PATH);
  }

}
