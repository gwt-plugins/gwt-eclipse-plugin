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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.login.GoogleLoginPrefs.Credentials;
import com.google.gdt.eclipse.login.extensions.IClientProvider;
import com.google.gdt.eclipse.login.extensions.LoginListener;
import com.google.gdt.eclipse.login.ui.LoginBrowser;
import com.google.gdt.eclipse.login.ui.LoginTrimContribution;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Class that handles logging in to Google services.
 */
public class GoogleLogin {

  /**
   * Class representing a problem with a response.
   */
  public static class ResponseException extends Exception {
    public ResponseException(String reason) {
      super(reason);
    }
  }

  private static final String
      EXTERNAL_BROWSER_MSG =
          "An embedded browser could not be created for signing in. "
          + "An external web browser has been launched instead. Please sign in using this browser, "
          + "and enter the verification code here";

  private static final String
      GET_EMAIL_URL = "https://www.googleapis.com/userinfo/email";

  private static final String
      LOGIN_NOTIFICATION_EXTENSION_POINT = "loginListener";

  private static GoogleLogin instance;

  private static final JsonFactory jsonFactory = new JacksonFactory();

  private static final String
      OAUTH2_NATIVE_CALLBACK_URL = GoogleOAuthConstants.OOB_REDIRECT_URI;
  private static final HttpTransport transport = new NetHttpTransport();

  static {
    instance = new GoogleLogin();
    instance.loadLogin();
  }

  public static GoogleLogin getInstance() {
    return instance;
  }

  public static void promptToLogIn() {
    promptToLogIn(null);
  }

  public static void promptToLogIn(final String message) {
    if (!instance.isLoggedIn()) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          GoogleLogin.getInstance().logIn(message);
        }
      });
    }
  }

  private static void showNoBrowsersMessageDialog() {
    MessageDialog noBrowsersMd = new MessageDialog(Display.getDefault()
      .getActiveShell(),
        "No browsers found",
        null,
        null,
        MessageDialog.ERROR,
        new String[] {"Ok"},
        0) {

        @Override
      protected Control createMessageArea(Composite parent) {
        super.createMessageArea(parent);

        Link link = new Link(parent, SWT.WRAP);
        link.setText("An embedded browser could not be created for signing in."
            + "\nAn external browser is needed to sign in, however, none are defined in Eclipse." + "\nPlease add a browser in <a href=\"#\">Window -> Preferences -> General -> Web Browser</a> and sign in again.");

        link.addSelectionListener(new SelectionAdapter() {
            @Override
          public void widgetSelected(SelectionEvent e) {
            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                Display.getDefault().getActiveShell(),
                "org.eclipse.ui.browser.preferencePage",
                new String[] {"org.eclipse.ui.browser.preferencePage"}, null);

            if (dialog != null) {
              dialog.open();
            }
          }
        });
        return parent;
      }
    };
    noBrowsersMd.open();
  }

  private Credential access;

  private String accessToken;

  private long accessTokenExpiryTime;

  private Set<String> cachedOAuthScopes;

  private String clientId;

  private String clientSecret;

  // if we connected to the internet
  private boolean connected;

  private String email;

  private boolean isLoggedIn;

  private String refreshToken;

  private LoginTrimContribution trim;

  protected GoogleLogin() {
    isLoggedIn = false;
    email = "";
    connected = true; // assume we're connected until checkCredentials is called
    initializeOauthClientInfo();
  }

  /**
   * See {@link #createRequestFactory(String)}.
   */
  public HttpRequestFactory createRequestFactory() {
    return createRequestFactory(null);
  }

  /**
   * Returns an HttpRequestFactory object that has been signed with the users's
   * authentication headers to use to make http requests. If the user has not
   * signed in, this method will block and pop up the login dialog to the user.
   * If the user cancels signing in, this method will return null.
   * 
   *  If the access token that was used to sign this transport was revoked or
   * has expired, then execute() invoked on Request objects constructed from
   * this transport will throw an exception, for example,
   * "com.google.api.client.http.HttpResponseException: 401 Unauthorized"
   * 
   * @param message The message to display in the login dialog if the user needs
   *          to log in to complete this action. If null, then no message area
   *          is created. See {@link #logIn(String)}
   */
  public HttpRequestFactory createRequestFactory(String message) {
    if (!checkLoggedIn(message)) {
      return null;
    }
    return transport.createRequestFactory(access);
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh token
   * if it is expired.
   * 
   * @return an OAuth2 token, or null if there was an error or if the user
   *         wasn't signed in and canceled signing in.
   * @throws IOException if something goes wrong while fetching the token.
   * 
   */
  public String fetchAccessToken() throws IOException {
    if (!checkLoggedIn(null)) {
      return null;
    }
    if (accessTokenExpiryTime != 0) {
      long currentTime = new GregorianCalendar().getTimeInMillis() / 1000;
      if (currentTime >= accessTokenExpiryTime) {
        fetchOAuth2Token();
      }
    } else {
      fetchOAuth2Token();
    }
    return accessToken;
  }

  public String fetchOAuth2ClientId() {
    return clientId;
  }

  public String fetchOAuth2ClientSecret() {
    return clientSecret;
  }

  public String fetchOAuth2RefreshToken() {
    if (!checkLoggedIn(null)) {
      return null;
    }
    return refreshToken;
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh
   * token. This token is short lived.
   * 
   * @return an OAuth2 token, or null if there was an error or if the user
   *         wasn't signed in and canceled signing in.
   * @throws IOException if something goes wrong while fetching the token.
   * 
   */
  public String fetchOAuth2Token() throws IOException {
    if (!checkLoggedIn(null)) {
      return null;
    }

    try {
      GoogleRefreshTokenRequest request = new GoogleRefreshTokenRequest(
          transport, jsonFactory, refreshToken, clientId, clientSecret);
      GoogleTokenResponse authResponse = request.execute();
      accessToken = authResponse.getAccessToken();
      access.setAccessToken(accessToken);
      accessTokenExpiryTime = new GregorianCalendar().getTimeInMillis() / 1000
          + authResponse.getExpiresInSeconds().longValue();
    } catch (IOException e) {
      GoogleLoginPlugin.logError("Could not obtain an Oauth2 access token.", e);
      throw e;
    }
    saveCredentials();
    return accessToken;
  }

  /**
   * @return the user's email address, or the empty string if the user is logged
   *         out, or null if the user's email couldn't be retrieved
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns true if the plugin was able to connect to the internet to try to ,
   * true if there were no stored credentials, and false if there were stored
   * credentials, but it could not connect to verify.
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * @return true if the user is logged in, false otherwise
   */
  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  /**
   * See {@link #logIn(String)}.
   */
  public boolean logIn() {
    return logIn(null);
  }

  /**
   * Pops up the dialogs to allow the user to sign in. If the user is already
   * signed in, then this does nothing and returns true.
   * 
   * @param message if not null, then this message is displayed above the
   *          embedded browser widget. This is for when the user is presented
   *          the login dialog from doing something other than logging in, such
   *          as accessing Google API services. It should say something like
   *          "Importing a project from Google Project Hosting requires signing
   *          in."
   * 
   * @return true if the user signed in or is already signed in, false otherwise
   */
  public boolean logIn(String message) {

    if (isLoggedIn) {
      return true;
    }

    String authorizeUrl =
        new GoogleAuthorizationCodeRequestUrl(
            clientId, OAUTH2_NATIVE_CALLBACK_URL, getOAuthScopes()).build();

    connected = true;
    final LoginBrowser loginBrowser = new LoginBrowser(
        Display.getDefault().getActiveShell(), authorizeUrl, message);

    int rc = LoginBrowser.BROWSER_ERROR;

    try {
      rc = loginBrowser.open();
    } catch (Throwable e) {
      // the login browser logs its own errors
    }

    String verificationCode;

    if (rc == LoginBrowser.CANCEL) {
      return false;
    } else if (rc == LoginBrowser.BROWSER_ERROR) {
      // if the embedded browser couldn't be opened, have to fall back
      // on an external browser.
      verificationCode = openExternalBrowserForLogin(authorizeUrl);
      if (verificationCode == null) {
        return false;
      }
    } else {
      verificationCode = loginBrowser.getVerificationCode();
    }
    if ((verificationCode == null) || verificationCode.isEmpty()) {
      return false;
    }

    GoogleAuthorizationCodeTokenRequest authRequest = new GoogleAuthorizationCodeTokenRequest(transport,
        jsonFactory,
        clientId,
        clientSecret,
        verificationCode,
        OAUTH2_NATIVE_CALLBACK_URL);
    GoogleTokenResponse authResponse;
    try {
      authResponse = authRequest.execute();
    } catch (IOException e) {
      MessageDialog.openError(Display.getDefault().getActiveShell(),
          "Error while signing in", "An error occured while trying to sign in: "
              + e.getMessage() + ". See the error log for more details.");
      GoogleLoginPlugin.logError(
          "Could not sign in. Make sure that you entered the correct verification code.",
          e);
      return false;
    }
    refreshToken = authResponse.getRefreshToken();
    accessToken = authResponse.getAccessToken();
    access = makeCredential();
    accessTokenExpiryTime = new GregorianCalendar().getTimeInMillis() / 1000
        + authResponse.getExpiresInSeconds().longValue();
    isLoggedIn = true;
    email = queryEmail();
    saveCredentials();
    notifyTrim();
    notifyLoginStatusChange(true);
    return true;
  }

  /**
   * Logs the user out. Pops up a question dialog asking if the user really
   * wants to quit.
   * 
   * @return true if the user logged out, false otherwise
   */
  public boolean logOut() {
    return logOut(true);
  }

  /**
   * Logs the user out.
   * 
   * @param showPrompt if true, opens a prompt asking if the user really wants
   *          to log out. If false, the user is logged out
   * @return true if the user was logged out or is already logged out, and false
   *         if the user chose not to log out
   */
  public boolean logOut(boolean showPrompt) {
    return logOut(showPrompt, true);
  }

  /**
   * When the login trim is instantiated by the UI, it calls this method so that
   * when logIn() is called by something other than the login trim itself, the
   * login trim can be notified to update its UI.
   * 
   * @param trim
   */
  public void setLoginTrimContribution(LoginTrimContribution trim) {
    this.trim = trim;
  }

  private boolean checkLoggedIn(String msg) {
    if (!isLoggedIn) {
      boolean rc = logIn(msg);
      if (!rc) {
        return false;
      }
      notifyTrim();
    }
    return true;
  }

  private Set<String> getOAuthScopes() {
    if (cachedOAuthScopes == null) {
      cachedOAuthScopes = GoogleLoginUtils.queryOAuthScopeExtensions();
    }

    return cachedOAuthScopes;
  }

  private void initializeOauthClientInfo() {
    ExtensionQuery<IClientProvider> extensionQuery = new ExtensionQuery<
        IClientProvider>(
        GoogleLoginPlugin.PLUGIN_ID, "oauthClientProvider", "class");
    for (ExtensionQuery.Data<IClientProvider> data : extensionQuery.getData()) {
      String id = data.getExtensionPointData().getId();
      String secret = data.getExtensionPointData().getSecret();
      if (!StringUtilities.isEmpty(id) && id.trim().length() > 0
          && !StringUtilities.isEmpty(secret) && secret.trim().length() > 0) {
        clientId = id;
        clientSecret = secret;
        return;
      }
    }
  }

  private boolean logOut(boolean showPrompt, boolean doRevoke) {

    if (!isLoggedIn) {
      return true;
    }

    boolean logOut = true;
    if (showPrompt) {
      logOut = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
          "Sign out?", "Are you sure you want to sign out?");
    }

    if (logOut) {
      email = "";
      isLoggedIn = false;

      GoogleLoginPrefs.clearStoredCredentials();

      notifyLoginStatusChange(false);
      return true;
    }
    return false;
  }

  private void notifyLoginStatusChange(boolean login) {
    ExtensionQuery<LoginListener> extensionQuery = new ExtensionQuery<
        LoginListener>(GoogleLoginPlugin.PLUGIN_ID,
        LOGIN_NOTIFICATION_EXTENSION_POINT, "class");

    List<ExtensionQuery.Data<LoginListener>> loginListenerList = extensionQuery
      .getData();
    for (ExtensionQuery.Data<LoginListener> loginListener : loginListenerList) {
      loginListener.getExtensionPointData().statusChanged(login);
    }
  }

  private void notifyTrim() {
    if (trim != null) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          trim.updateUi();
        }
      });
    }
  }

  /**
   * Opens an external browser for the user to login, opens a dialog telling the
   * user to login using that browser, and paste the verification code in a
   * dialog box that pops up.
   */
  private String openExternalBrowserForLogin(String authorizeUrl) {

    IWebBrowser browser = null;
    try {
      // we don't send them to the logout url because we don't want to
      // force them to log out of their normal sessions
      browser = BrowserUtilities.launchBrowser(authorizeUrl);
    } catch (PartInitException e) {
      showNoBrowsersMessageDialog();
      return null;
    } catch (MalformedURLException e) {
      GoogleLoginPlugin.logError("Could not open external browser", e);
    }

    final InputDialog md = new InputDialog(
        Display.getDefault().getActiveShell(), "Sign in to Google Services",
        EXTERNAL_BROWSER_MSG, null, new IInputValidator() {
          public String isValid(String newText) {
            if (!newText.trim().isEmpty()) {
              return null;
            }
            return "Verification code cannot be empty";
          }
        });

    int rc = md.open();
    String verificationCode = md.getValue();

    browser.close();
    md.close();
    return verificationCode;
  }

  private String queryEmail() {
    String url = GET_EMAIL_URL;

    HttpResponse resp = null;
    try {
      HttpRequest get = createRequestFactory().buildGetRequest(
          new GenericUrl(url));
      resp = get.execute();
      Scanner scan = new Scanner(resp.getContent());
      String respStr = "";
      while (scan.hasNext()) {
        respStr += scan.nextLine();
      }

      Map<String, String> params = GoogleLoginUtils.parseUrlParameters(respStr);
      String userEmail = params.get("email");
      if (userEmail == null) {
        throw new Exception("Response from server is invalid.");
      }

      return userEmail;

    } catch (Exception e) {
      // catch exception in case something goes wrong in parsing the response
      GoogleLoginPlugin.logError(
          "Could not parse email after Google service sign-in", e);
    }

    return null;
  }

  private void saveCredentials() {
    if (!isLoggedIn) {
      GoogleLoginPrefs.clearStoredCredentials();
    } else {
      Credentials creds = new Credentials(accessToken, refreshToken, email,
          getOAuthScopes(), accessTokenExpiryTime);
      GoogleLoginPrefs.saveCredentials(creds);
    }
  }

  protected void loadLogin() {

    Credentials prefs = GoogleLoginPrefs.loadCredentials();

    // the stored email can be null in the case where the external browser
    // was launched, because we can't extract the email from the external
    // browser
    if (prefs.refreshToken == null || prefs.storedScopes == null) {
      GoogleLoginPrefs.clearStoredCredentials();
      return;
    }

    accessToken = prefs.accessToken;
    refreshToken = prefs.refreshToken;
    accessTokenExpiryTime = prefs.accessTokenExpiryTime;
    this.email = prefs.storedEmail;

    isLoggedIn = true;

    if (!getOAuthScopes().equals(prefs.storedScopes)) {
      GoogleLoginPlugin.logWarning(
          "OAuth scope set for stored credentials no longer valid, logging out.");
      GoogleLoginPlugin.logWarning(
          getOAuthScopes() + " vs. " + prefs.storedScopes);
      logOut(false);
    }

    access = makeCredential();
  }

  public Credential getCredential() {
    if (access == null) {
      access = makeCredential();
    }
    return access;
  }
  
  public Credential makeCredential() {
    Credential cred = new GoogleCredential.Builder()
      .setJsonFactory(jsonFactory)
      .setTransport(transport)
      .setClientSecrets(clientId, clientSecret).build();
    cred.setAccessToken(accessToken);
    cred.setRefreshToken(refreshToken);
    return cred;
  }

  public void stop() {
    if (GoogleLoginPrefs.getLogoutOnExitPref()) {
      logOut(false);
    }
  }
}
