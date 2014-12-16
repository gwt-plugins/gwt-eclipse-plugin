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
package com.google.gdt.eclipse.appengine.api;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.appengine.v1beta2.Appengine;
import com.google.api.services.appengine.v1beta2.model.App;
import com.google.api.services.appengine.v1beta2.model.AppsListResponse;
import com.google.gdt.eclipse.core.browser.BrowserUtilities;
import com.google.gdt.eclipse.login.GoogleLogin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Provides App Engine project information using the App Engine API.
 */
public class AppengineApiWrapper {
  protected Appengine appengine;
  protected boolean inTestMode = false;
  private Credential credential;
  private static final String INSERT_ERROR_MESSAGE =
      "Error occured while inserting the new application.";
  private static final String LIST_ERROR_MESSAGE =
      "Error occured while listing the available applications.";
  private static final String APPENGINE_ADMIN_CONSOLE_URL =
      "https://appengine.google.com/dashboard?&app_id=";
  public static final String APPENGINE_CREATE_APP = "https://appengine.google.com/start/createapp";

  // Text that we look for in messages issued by GAE:
  private static final String GAE_DUPLICATE_APP_MESSAGE = "DUPLICATE_OBJECT: App already exists.";
  private static final String GAE_UNAUTHORIZED_APP_MESSAGE =
      "UNAUTHORIZED: User must accept most recent ToS";
  private static final String GAE_REACHED_QUOTA_MESSAGE_PREFIX = "ACCESS_DENIED: User";
  private static final String GAE_REACHED_QUOTA_MESSAGE_SUFFIX = "may not own any more free apps";
  private static final String GAE_INVALID_APP_MESSAGE = "INVALID_REQUEST: Invalid appId:";

  // Text that we present to GPE users
  private static final String GPE_DUPLICATE_APP_MESSAGE = "This app id already exists.";
  private static final String GPE_UNAUTHORIZED_APP_MESSAGE =
      "You must accept the most recent Terms of Service to use App Engine. "
          + "Please go to https://cloud.google.com/console to accept it.";
  private static final String GPE_REACHED_QUOTA_MESSAGE =
      "Error creating application: Did you run out of free applications?";
  private static final String GPE_INVALID_APP_MESSAGE = "This app id is not valid.";

  private static String parseIOException(IOException e, String defaultMessage) {
    try {
      String ioExceptionMessage = e.getMessage();

      // Removing first line with client error status code; 400 Bad Request
      String serverErrorJson = ioExceptionMessage.substring(ioExceptionMessage.indexOf("\n") + 1);

      Gson gson = new GsonBuilder().create();
      AppEngineErrorInfo errorInfo = gson.fromJson(serverErrorJson, AppEngineErrorInfo.class);
      String errorMessage = errorInfo.getMessage();

      if (errorMessage.isEmpty()) {
        return defaultMessage;
      } else if (errorMessage.equals(GAE_DUPLICATE_APP_MESSAGE)) {
        return GPE_DUPLICATE_APP_MESSAGE;
      } else if (errorMessage.equals(GAE_UNAUTHORIZED_APP_MESSAGE)) {
        return GPE_UNAUTHORIZED_APP_MESSAGE;
      } else if (errorMessage.startsWith(GAE_REACHED_QUOTA_MESSAGE_PREFIX)
          && errorMessage.endsWith(GAE_REACHED_QUOTA_MESSAGE_SUFFIX)) {
        return GPE_REACHED_QUOTA_MESSAGE;
      } else if (errorMessage.startsWith(GAE_INVALID_APP_MESSAGE)) {
        return GPE_INVALID_APP_MESSAGE;
      } else {
        return errorMessage;
      }

    } catch (NullPointerException ex) {
      // Occurs when the message or exception is null
      return defaultMessage;
    } catch (IndexOutOfBoundsException ex) {
      // Occurs when getting the substring of ioExceptionMessage
      return defaultMessage;
    }

  }

  /**
   * When the user is logged in, this function returns an updated list of App Engine applications.
   * If the user is not logged in and forceLogin is true, the user is prompted to login in.
   *
   * @param forceLogin If true, the user is prompted to login if the user is not yet logged in.
   * @return An array of available App Engine applications if applications exist, an empty array is
   *         no application exists or null if the API call failed.
   * @throws IOException
   */
  @Nullable
  public String[] getApplications(boolean forceLogin) throws IOException {
    if (!checkLogin(forceLogin)) {
      return null;
    }
    try {
      return listApps();
    } catch (IOException e) {
      throw new IOException(parseIOException(e, LIST_ERROR_MESSAGE));
    }
  }

  /**
   *
   * When the user is logged in, this function creates an App Engine application (with only the App
   * ID set and domain if it is a domain application) and inserts it into the logged in user's
   * account. If the user is not logged in and forceLogin is true, the user is prompted to login in.
   *
   * @param appId The new application's ID.
   * @param forceLogin If true, the user is prompted to login if the user is not yet logged in.
   * @throws IOException
   */
  public void insertNewApplication(String appId, boolean forceLogin) throws IOException {
    if (!checkLogin(forceLogin)) {
      return;
    }

    try {
      insertApp(appId);
    } catch (IOException e) {
      throw new IOException(parseIOException(e, INSERT_ERROR_MESSAGE));
    }
  }

  /**
   * Opens a browser to the Google App Engine Admin Console of the selected application.
   *
   * @param appId The new application's ID.
   */
  public void viewConsole(String appId) {
    String url = APPENGINE_ADMIN_CONSOLE_URL + appId;
    BrowserUtilities.launchBrowserAndHandleExceptions(url);
  }

  /**
   * When the user is logged in, this function initializes the App Engine API. If the user is not
   * logged in and forceLogin is true, the user is prompted to login in.
   *
   * @param forceLogin If true, the user is prompted to login if the user is not yet logged in.
   * @return true is the user is logged in and the App Engine API is initialized. False otherwise.
   */
  private boolean checkLogin(boolean forceLogin) {
    if (inTestMode) {
      return true;
    }

    if (GoogleLogin.getInstance().isLoggedIn()) {
      initializeAppEngine();
      return true;
    } else {
      if (forceLogin && GoogleLogin.getInstance().logIn()) {
        initializeAppEngine();
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * If the Google login credentials are available, this function initializes the App Engine API.
   */
  private void initializeAppEngine() {
    if (credential == null) {
      credential = GoogleLogin.getInstance().getCredential();
      appengine =
          new Appengine.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(
              "Google Plugin for Eclipse").build();
    }
  }

  /**
   * This function creates an App Engine application with only the App Id and the domain, if it is a
   * domain application, and inserts it into the logged in user's account.
   *
   * NOTE: The ability to insert app ids no longer exists. See comment in CreateAppIdDialog for
   * more information.
   *
   * @param appId The application id
   * @throws IOException
   */
  private void insertApp(String appId) throws IOException {

    throw new IllegalStateException("The ability to insert apps no longer exists.");
    // App app = new App();
    // app.setAppId(appId);
    // String domain = "";
    // if (appId.contains(":")) {
    // String[] splitApp = appId.split(":");
    // domain = splitApp[0];
    // app.setDomain(domain);
    // }
    //
    // InsertAppRequest a = new InsertAppRequest();
    // a.setApp(app);
    // appengine.apps().insert(a).execute();
  }

  /**
   * This function returns a list of all the apps belonging to the authenticated user.
   *
   * @throws IOException
   */
  private String[] listApps() throws IOException {
    Appengine.Apps.List appsDotList = appengine.apps().list();
    AppsListResponse resp = null;

    try {
      resp = appsDotList.execute();
    } catch (IOException ex) {
      // TODO(nbashirbello): Update this feature when API refreshes token on its own
      // Appengine API does not refresh user token after they expire so it is
      // done here.
      credential.refreshToken();
      resp = appsDotList.execute();
    }

    if (resp == null) {
      return null;
    }

    List<App> appsList = resp.getApps();
    if (appsList == null) {
      return new String[0];
    }

    String[] result = new String[appsList.size()];
    int i = 0;
    for (App app : appsList) {
      result[i] = app.getAppId();
      i++;
    }

    return result;
  }
}
