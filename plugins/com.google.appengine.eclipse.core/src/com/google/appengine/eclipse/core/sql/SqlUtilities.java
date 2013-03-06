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

package com.google.appengine.eclipse.core.sql;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.gdt.eclipse.login.GoogleLogin;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWTException;

import java.io.IOException;

/**
 * Contains the functions to get mysql, prod and test google cloud sql.
 */
public class SqlUtilities {
  private static final String MYSQL_URL_VALUE_FORMAT = "jdbc:mysql://%s:%s/%s?";
  private static final String MYSQL_URL_VALUE_USER_FORMAT = "user=%s&";
  private static final String MYSQL_URL_VALUE_PASSWORD_FORMAT = "password=%s&";

  private static final String
      CLOUDSQL_URL_VALUE_FORMAT = "jdbc:google:rdbms://%s/%s?";
  private static final String
      CLOUDSQL_URL_VALUE_OAUTH2_FORMAT = "oauth2RefreshToken=%s&";
  private static final String
      CLOUDSQL_URL_VALUE_OAUTH2_ACCESS_TOKEN_FORMAT = "oauth2AccessToken=%s&";
  private static final String
      CLOUDSQL_URL_VALUE_CLIENTID_FORMAT = "oauth2ClientId=%s&";
  private static final String
      CLOUDSQL_URL_VALUE_CLIENTSECRET_FORMAT = "oauth2ClientSecret=%s&";

  private static boolean isDialogShown = false;
  private static Object mutex = new Object();

  /**
   * It returns empty string ("") if it cannot form the url. If the
   * aurthorization arguments cannot be found, it returns the url without the
   * authorization arguments.
   * 
   * @param instance should be non null.
   * @param database should be non null.
   * @return cloud sql jdbc url.
   */
  public static String getCloudSqlJdbcUrl(
      String instance, String database, boolean withAuthorizationArguments) {
    if (instance.trim().isEmpty() || database.trim().isEmpty()) {
      return "";
    }

    StringBuilder rdbmsUrlValue = new StringBuilder(String.format(
        CLOUDSQL_URL_VALUE_FORMAT, instance, database));
    if (withAuthorizationArguments) {
      synchronized (mutex) {
        if (!isDialogShown) {
          isDialogShown = true;
        } else {
          try {
            mutex.wait();
          } catch (InterruptedException e) {
            AppEngineCorePluginLog.logError(e);
          }
          if (!GoogleLogin.getInstance().isLoggedIn()) {
            mutex.notify();
            return getCloudSqlJdbcUrl(instance, database, false);
          }
        }
      }
      GoogleLogin.promptToLogIn("Log in before connecting.");
      String refreshToken = "";
      String accessToken = "";
      String clientID = "";
      String clientSecret = "";
      try {
        refreshToken = GoogleLogin.getInstance().fetchOAuth2RefreshToken();
        accessToken = GoogleLogin.getInstance().fetchAccessToken();
        clientID = GoogleLogin.getInstance().fetchOAuth2ClientId();
        clientSecret = GoogleLogin.getInstance().fetchOAuth2ClientSecret();
      } catch (SWTException e) {
        // Do nothing. User is not logged in and she cancelled when prompted.
      } catch (IOException e) {
        AppEngineCorePluginLog.logError(e);
      }
      synchronized (mutex) {
        isDialogShown = false;
        mutex.notify();
        if (!GoogleLogin.getInstance().isLoggedIn()) {
          return getCloudSqlJdbcUrl(instance, database, false);
        }
      }
      if (!refreshToken.isEmpty()) {
        rdbmsUrlValue.append(
            String.format(CLOUDSQL_URL_VALUE_OAUTH2_FORMAT, refreshToken));
      }
      if (!accessToken.isEmpty()) {
        rdbmsUrlValue.append(String.format(
            CLOUDSQL_URL_VALUE_OAUTH2_ACCESS_TOKEN_FORMAT, accessToken));
      }
      if (!clientID.isEmpty()) {
        rdbmsUrlValue.append(
            String.format(CLOUDSQL_URL_VALUE_CLIENTID_FORMAT, clientID));
      }
      if (!clientSecret.isEmpty()) {
        rdbmsUrlValue.append(String.format(
            CLOUDSQL_URL_VALUE_CLIENTSECRET_FORMAT, clientSecret));
      }
    }

    // The last char will be either ? or & which is not required.
    rdbmsUrlValue.deleteCharAt(rdbmsUrlValue.length() - 1);
    return rdbmsUrlValue.toString();
  }

  /**
   * Returns empty string ("") if it cannot form the url.
   * 
   * @param host should be non null.
   * @param database should be non null.
   * @param port should be non null.
   * @param user should be non null. If there is no user name then set it to "".
   * @param password should be non null. If there is no password set it to "".
   * @return mysql jdbc url.
   */
  public static String getMySqlJdbcUrl(
      String host, String database, String port, String user, String password) {
    if (host.trim().isEmpty() || database.trim().isEmpty()
        || port.trim().isEmpty()) {
      return "";
    }

    StringBuilder rdbmsUrlValue = new StringBuilder(String.format(
        MYSQL_URL_VALUE_FORMAT, host, port, database));

    if (!user.isEmpty()) {
      rdbmsUrlValue.append(String.format(MYSQL_URL_VALUE_USER_FORMAT, user));
    }
    if (!password.isEmpty()) {
      rdbmsUrlValue.append(
          String.format(MYSQL_URL_VALUE_PASSWORD_FORMAT, password));
    }
    // The last char will be either ? or & which is not required.
    rdbmsUrlValue.deleteCharAt(rdbmsUrlValue.length() - 1);
    return rdbmsUrlValue.toString();
  }

  /**
   * @param project
   * @return mysql jdbc url for the project.
   */
  public static String getMySqlUrl(IProject project) {
    String user = GoogleCloudSqlProperties.getMySqlDatabaseUser(project);
    String database = GoogleCloudSqlProperties.getMySqlDatabaseName(project);
    String password = GoogleCloudSqlProperties.getMySqlDatabasePassword(
        project);
    String host = GoogleCloudSqlProperties.getMySqlHostName(project);
    String port = new Integer(GoogleCloudSqlProperties.getMySqlPort(project))
      .toString();

    return getMySqlJdbcUrl(host, database, port, user, password);
  }

  /**
   * The url is returned without authorization arguments, username and password.
   * 
   * @param project
   * @return production jdbc url for project.
   */
  public static String getProdJdbcUrl(IProject project) {
    String instance = GoogleCloudSqlProperties.getProdInstanceName(project);
    String database = GoogleCloudSqlProperties.getProdDatabaseName(project);

    return getCloudSqlJdbcUrl(instance, database, false);
  }

}
