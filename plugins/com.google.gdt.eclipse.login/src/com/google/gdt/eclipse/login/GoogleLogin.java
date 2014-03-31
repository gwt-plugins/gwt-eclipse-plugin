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

import java.io.IOException;
import java.net.MalformedURLException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.login.common.OAuthData;
import com.google.gdt.eclipse.login.common.OAuthDataStore;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gdt.eclipse.login.common.LoggerFacade;
import com.google.gdt.eclipse.login.common.LoginListener;
import com.google.gdt.eclipse.login.common.UiFacade;
import com.google.gdt.eclipse.login.extensions.IClientProvider;
import com.google.gdt.eclipse.login.ui.LoginBrowser;
import com.google.gdt.eclipse.login.ui.LoginTrimContribution;

/**
 * Class that handles logging in to Google services.
 */
public class GoogleLogin {

  /**
   * Class representing a problem with a response.
   */
  @SuppressWarnings("serial")
  public static class ResponseException extends Exception {
    public ResponseException(String reason) {
      super(reason);
    }
  }

  private static final String LOGIN_NOTIFICATION_EXTENSION_POINT = "loginListener";

  private static EclipseUiFacade uiFacade;
  private static GoogleLogin instance;

  static {
    ClientIdSecretPair clientInfo = getClientIdAndSecretFromExtensionPoints();
    uiFacade = new EclipseUiFacade();
    GoogleLoginState state =
        new GoogleLoginState(
            clientInfo.getClientId(),
            clientInfo.getClientSecret(),
            GoogleLoginUtils.queryOAuthScopeExtensions(),
            new EclipsePreferencesOAuthDataStore(),
            uiFacade,
            new EclipseLoggerFacade());
    addLoginListenersFromExtensionPoints(state);
    instance = new GoogleLogin(state);
  }

  private static ClientIdSecretPair getClientIdAndSecretFromExtensionPoints() {
    ExtensionQuery<IClientProvider> clientProviderExtensionQuery =
        new ExtensionQuery<IClientProvider>(
            GoogleLoginPlugin.PLUGIN_ID, "oauthClientProvider", "class");
    for (ExtensionQuery.Data<IClientProvider> data : clientProviderExtensionQuery.getData()) {
      IClientProvider provider = data.getExtensionPointData();
      String clientId = provider.getId();
      String clientSecret = provider.getSecret();
      if (clientId != null && clientId.trim().length() > 0
          && clientSecret != null && clientSecret.trim().length() > 0) {
        return new ClientIdSecretPair(clientId, clientSecret);
      }
    }
    throw new IllegalStateException("No suitable oauthClientProvider extension point found");
  }

  private static void addLoginListenersFromExtensionPoints(GoogleLoginState state) {
    ExtensionQuery<LoginListener> listenerExtensionQuery =
        new ExtensionQuery<LoginListener>(
            GoogleLoginPlugin.PLUGIN_ID, LOGIN_NOTIFICATION_EXTENSION_POINT, "class");
    for (ExtensionQuery.Data<LoginListener> extensionData : listenerExtensionQuery.getData()) {
      state.addLoginListener(extensionData.getExtensionPointData());
    }
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
  
  private final GoogleLoginState delegate;

  protected GoogleLogin(GoogleLoginState delegate) {
    this.delegate = delegate;
  }

  /**
   * See {@link #createRequestFactory(String)}.
   */
  public HttpRequestFactory createRequestFactory() {
    return delegate.createRequestFactory(null);
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
    return delegate.createRequestFactory(message);
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
    return delegate.fetchAccessToken();
  }

  public String fetchOAuth2ClientId() {
    return delegate.fetchOAuth2ClientId();
  }

  public String fetchOAuth2ClientSecret() {
    return delegate.fetchOAuth2ClientSecret();
  }

  public String fetchOAuth2RefreshToken() {
    return delegate.fetchOAuth2RefreshToken();
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
    return delegate.fetchOAuth2Token();
  }

  public Credential getCredential() {
    return delegate.getCredential();
  }

  /**
   * @return the user's email address, or the empty string if the user is logged
   *         out, or null if the user's email couldn't be retrieved
   */
  public String getEmail() {
    return delegate.getEmail();
  }

  /**
   * Returns true if the plugin was able to connect to the internet to try to ,
   * true if there were no stored credentials, and false if there were stored
   * credentials, but it could not connect to verify.
   */
  public boolean isConnected() {
    return delegate.isConnected();
  }

  /**
   * @return true if the user is logged in, false otherwise
   */
  public boolean isLoggedIn() {
    return delegate.isLoggedIn();
  }

  /**
   * See {@link #logIn(String)}.
   */
  public boolean logIn() {
    return delegate.logIn(null);
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
    return delegate.logIn(message);
  }

  /**
   * Logs the user out. Pops up a question dialog asking if the user really
   * wants to quit.
   * 
   * @return true if the user logged out, false otherwise
   */
  public boolean logOut() {
    return delegate.logOut();
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
    return delegate.logOut(showPrompt);
  }

  public Credential makeCredential() {
    return delegate.makeCredential();
  }

  /**
   * When the login trim is instantiated by the UI, it calls this method so that
   * when logIn() is called by something other than the login trim itself, the
   * login trim can be notified to update its UI.
   * 
   * @param trim
   */
  public void setLoginTrimContribution(LoginTrimContribution trim) {
    uiFacade.setLoginTrimContribution(trim);
  }

  public void stop() {
    if (GoogleLoginPrefs.getLogoutOnExitPref()) {
      logOut(false);
    }
  }

  /**
   * Performs the firing of listeners and the updates to the status indicator in the trim
   * contribution that are normally performed as part of a log in or log out, and retrieves any
   * persistently stored credentials upon log in, but does not actually interact with an OAuth
   * server.
   * 
   * @param login
   *     {@code true} if a log in is to be simulated, {@code false} if a log out is to be simulated
   */
  @VisibleForTesting
  public void updateInternalLogin(boolean login) {
    delegate.simulateLoginStatusChange(login);
  }
  
  /**
   * A pair consisting of the OAuth client ID and OAuth client secret for a client application.
   */
  @Immutable
  private static class ClientIdSecretPair {
    private final String clientId;
    private final String clientSecret;
    
    public ClientIdSecretPair(String clientId, String clientSecret) {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
    }
    
    public String getClientId() {
      return clientId;
    }
    
    public String getClientSecret() {
      return clientSecret;
    }
  }
    
  /**
   * An implementation of {@link UiFacade} using Eclipse dialogs and embedded browsers.
   */
  private static class EclipseUiFacade implements UiFacade {

    private static final String EXTERNAL_BROWSER_MSG =
        "An embedded browser could not be created for signing in. "
            + "An external web browser has been launched instead. "
            + "Please sign in using this browser, and enter the verification code here.";
    
    private static final String NO_BROWSER_MSG =
        "An embedded browser could not be created for signing in."
            + "\nAn external browser is needed to sign in, however, none are defined in Eclipse."
            + "\nPlease add a browser in <a href=\"#\">Window -> Preferences -> General -> "
            + "Web Browser</a> and sign in again.";
    
    private LoginTrimContribution trim;

    public void setLoginTrimContribution(LoginTrimContribution trim) {
      this.trim = trim;
    }

    public void notifyStatusIndicator() {
      if (trim != null) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            trim.updateUi();
          }
        });
      }
    }

    @Nullable
    public String obtainVerificationCodeFromUserInteraction(
        String message, GoogleAuthorizationCodeRequestUrl requestUrl) {
      final LoginBrowser loginBrowser =
          new LoginBrowser(Display.getDefault().getActiveShell(), requestUrl, message);

      int rc;
      try {
        rc = loginBrowser.open();
      } catch (Throwable e) {
        rc = LoginBrowser.BROWSER_ERROR;
        // the login browser logs its own errors
      }

      if (rc == LoginBrowser.CANCEL) {
        return null;
      } else if (rc == LoginBrowser.BROWSER_ERROR) {
        // if the embedded browser couldn't be opened, have to fall back
        // on an external browser.
        return Strings.emptyToNull(openExternalBrowserForLogin(requestUrl));
      } else {
        return Strings.emptyToNull(loginBrowser.getVerificationCode());
      }
    }

    /**
     * Opens an external browser for the user to login, issues a request to the OAuth server
     * specified by a given {@link GoogleAuthorizationCodeRequestUrl}, and opens a dialog telling
     * the user to log in using that browser and to paste the verification code displayed in the
     * browser into the dialog. This is a fall-back for the case in which it is impossible to obtain
     * the authorization code that the OAuth server sends back to the browser directly from the
     * browser, without user intervention.
     * 
     * @param requestUrl the given {@code GoogleAuthorizationCodeRequestUrl}
     */
    private String openExternalBrowserForLogin(GoogleAuthorizationCodeRequestUrl requestUrl) {

      IWebBrowser browser = null;
      try {
        // we don't send them to the logout url because we don't want to
        // force them to log out of their normal sessions
        browser = BrowserUtilities.launchBrowser(requestUrl.build());
      } catch (PartInitException e) {
        showNoBrowsersMessageDialog();
        return null;
      } catch (MalformedURLException e) {
        GoogleLoginPlugin.logError("Could not open external browser", e);
      }

      final InputDialog md =
          new InputDialog(
              Display.getDefault().getActiveShell(),
              "Sign in to Google Services",
              EXTERNAL_BROWSER_MSG,
              null,
              new IInputValidator() {
                public String isValid(String newText) {
                  return newText.trim().isEmpty() ? "Verification code cannot be empty" : null;
                }
              });

      md.open();
      String verificationCode = md.getValue();

      browser.close();
      md.close();
      return verificationCode;
    }

    private static void showNoBrowsersMessageDialog() {
      MessageDialog noBrowsersMd =
          new MessageDialog(
              Display.getDefault().getActiveShell(),
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
              link.setText(NO_BROWSER_MSG);
    
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
    
    public void showErrorDialog(String title, String message) {
      MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
    }
    
    public boolean askYesOrNo(String title, String message) {
      return MessageDialog.openQuestion(Display.getDefault().getActiveShell(), title, message);
    }
  }
  
  /**
   * An implementation of the {@link OAuthDataStore} interface using Eclipse preferences.
   */
  private static class EclipsePreferencesOAuthDataStore implements OAuthDataStore {

    public void saveOAuthData(OAuthData credentials) {
      GoogleLoginPrefs.saveOAuthData(credentials);      
    }

    public OAuthData loadOAuthData() {
      return GoogleLoginPrefs.loadOAuthData();
    }

    public void clearStoredOAuthData() {
      GoogleLoginPrefs.clearStoredOAuthData();      
    }    
  }
  
  private static class EclipseLoggerFacade implements LoggerFacade {
    public void logError(String msg, Throwable t) {
      GoogleLoginPlugin.logError(msg, t);
    }

    public void logWarning(String msg) {
      GoogleLoginPlugin.logWarning(msg);
    }    
  }
}
