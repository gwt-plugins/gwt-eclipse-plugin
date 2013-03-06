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
package com.google.appengine.eclipse.core.datatools;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sql.SqlUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import java.util.List;

/**
 * Populates the objects in the extension point
 * com.google.appengine.eclipse.core.gaeSqlToolsExtension and calls
 * createProfileDefinition().
 */
public final class SqlConnectionExtensionPopulator {

  /**
   * There types of connections cloud sql production, cloud sql testing and
   * local mysql can be specified in the preferences ui.
   */
  public enum ConnectionType {
    CONNECTION_TYPE_TEST ("GoogleCloudSQL.DevInstance"),
    CONNECTION_TYPE_PROD ("GoogleCloudSQL.AppEngineInstance"),
    CONNECTION_TYPE_LOCAL_MYSQL ("MySQL.DevInstance");

    private String displayableStringForType;

    private ConnectionType(String displayableString) {
      displayableStringForType = displayableString;
    }

    public String getDisplayableStringForType() {
      return displayableStringForType;
    }
  }

  private static final String GAE_MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";
  private static final String GAE_CLOUD_SQL_DRIVER_CLASS =
      "com.google.cloud.sql.jdbc.internal.googleapi.GoogleApiDriver";
  private static final String GAE_SQL_CONNECTION_PROPERTIES_EXTENSION_POINT =
      "gaeSqlToolsExtension";

  public static void populateCloudSQLBridgeExtender(IJavaProject javaProject) {
    String cloudSqlJarPath = getCloudSqlJarPath(javaProject);
    populateCloudSQLBridgeExtender(javaProject, cloudSqlJarPath);
  }

  public static void populateCloudSQLBridgeExtender(IJavaProject javaProject, ConnectionType type) {
    String jarPath = getJarPathForType(javaProject, type);
    populateCloudSQLBridgeExtender(javaProject, type, jarPath);
  }

  public static void populateCloudSQLBridgeExtender(
      IJavaProject javaProject, ConnectionType connectionType, String jarPath) {

    ExtensionQuery<GaeSqlToolsExtension> extensionQuery =
        new ExtensionQuery<GaeSqlToolsExtension>(
            AppEngineCorePlugin.PLUGIN_ID, GAE_SQL_CONNECTION_PROPERTIES_EXTENSION_POINT,
            "class");

    List<ExtensionQuery.Data<GaeSqlToolsExtension>>
        sqlPropertylUpdateListenerList = extensionQuery.getData();

    for (ExtensionQuery.Data<GaeSqlToolsExtension> bridge :
        sqlPropertylUpdateListenerList) {
      try {
        SqlConnectionProperties connectionProperties;
        connectionProperties =
            getSqlConnectionPropertiesByType(javaProject, connectionType, jarPath);
        bridge.getExtensionPointData().updateConnectionProperties(connectionProperties);
      } catch (Exception e) {
        // This could happen is it could instantiate the object of the class defined by 
        // the extension point. 
        AppEngineCorePluginLog.logError(e, "Error while populating connection");
      }
    }
    return;
  }

  public static void populateCloudSQLBridgeExtender(
      IJavaProject javaProject, String cloudSqlJarPath) {
    IProject project = javaProject.getProject();

    if (!GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(project)) {
      return;
    }

    if (GoogleCloudSqlProperties.getLocalDevMySqlEnabled(project)) {
      if (GoogleCloudSqlProperties.getMySqlIsConfigured(project)) {
        populateCloudSQLBridgeExtender(javaProject, ConnectionType.CONNECTION_TYPE_LOCAL_MYSQL);
      }
    } else {
      if (GoogleCloudSqlProperties.getTestIsConfigured(project)) {
        populateCloudSQLBridgeExtender(
            javaProject, ConnectionType.CONNECTION_TYPE_TEST, cloudSqlJarPath);
      }
    }

    if (GoogleCloudSqlProperties.getProdIsConfigured(project)) {
      populateCloudSQLBridgeExtender(
          javaProject, ConnectionType.CONNECTION_TYPE_PROD, cloudSqlJarPath);
    }
  }

  private static String getCloudSqlJarPath(IJavaProject javaProject) {
    String jarPath = "";
    try {
      jarPath = GaeSdk.findSdkFor(javaProject).getInstallationPath()
          + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR_PATH_IN_SDK
          + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR;
    } catch (JavaModelException e) {
      AppEngineCorePluginLog.logError(e);
    }
    return jarPath;
  }

  private static String getDisplaybleConnectionId(String projectName, ConnectionType type) {
    return projectName + "." + type.getDisplayableStringForType();
  }

  private static String getJarPathForType(IJavaProject javaProject, ConnectionType connectionType) {
    switch (connectionType) {
      case CONNECTION_TYPE_LOCAL_MYSQL:
        return GoogleCloudSqlProperties.getMySqlJdbcJar(javaProject.getProject());
      case CONNECTION_TYPE_PROD:
      case CONNECTION_TYPE_TEST:
        return getCloudSqlJarPath(javaProject);
    }
    return null;
  }

  private static SqlConnectionProperties getSqlConnectionPropertiesByType(
      IJavaProject javaProject, ConnectionType connectionType, String jarPath) {
    if (connectionType == ConnectionType.CONNECTION_TYPE_LOCAL_MYSQL) {
      return setLocalMySqlConnectionProperties(javaProject.getProject(), jarPath);
    } else if (connectionType == ConnectionType.CONNECTION_TYPE_PROD) {
      return setProdCloudSqlConnectorsProperties(javaProject, jarPath);
    } else if (connectionType == ConnectionType.CONNECTION_TYPE_TEST) {
      return setTestCloudSqlConnectorsProperties(javaProject, jarPath);
    }
    return null;
  }

  private static SqlConnectionProperties setLocalMySqlConnectionProperties(
      IProject project, String jarPath) {
    SqlConnectionProperties sqlConnectionProperties = new SqlConnectionProperties();

    String username = GoogleCloudSqlProperties.getMySqlDatabaseUser(project);
    String databaseName = GoogleCloudSqlProperties.getMySqlDatabaseName(project);
    String password = GoogleCloudSqlProperties.getMySqlDatabasePassword(project);
    String driverURL = SqlUtilities.getMySqlUrl(project);
    String driverClass = GAE_MYSQL_DRIVER_CLASS;
    String connectionId = getDisplaybleConnectionId(project.getName(),
        ConnectionType.CONNECTION_TYPE_LOCAL_MYSQL);

    sqlConnectionProperties.setUsername(username);
    sqlConnectionProperties.setPassword(password);
    sqlConnectionProperties.setJdbcUrl(driverURL);
    sqlConnectionProperties.setDatabaseName(databaseName);
    sqlConnectionProperties.setJarPath(jarPath);
    sqlConnectionProperties.setDriverClass(driverClass);
    sqlConnectionProperties.setDisplayableConnectionPropertiesId(connectionId);
    sqlConnectionProperties.setInstanceName(null);
    sqlConnectionProperties.setVendor(SqlConnectionProperties.Vendor.MYSQL);

    return sqlConnectionProperties;
  }

  private static SqlConnectionProperties setProdCloudSqlConnectorsProperties(
      IJavaProject javaProject, String jarPath) {
    IProject project = javaProject.getProject();
    SqlConnectionProperties sqlConnectionProperties = new SqlConnectionProperties();

    String username = GoogleCloudSqlProperties.getProdDatabaseUser(project);
    String instanceName = GoogleCloudSqlProperties.getProdInstanceName(project);
    String databaseName = GoogleCloudSqlProperties.getProdDatabaseName(project);
    String password = GoogleCloudSqlProperties.getProdDatabasePassword(project);
    String driverClass = GAE_CLOUD_SQL_DRIVER_CLASS;
    String connectionId = getDisplaybleConnectionId(project.getName(),
        ConnectionType.CONNECTION_TYPE_PROD);

    sqlConnectionProperties.setUsername(username);
    sqlConnectionProperties.setPassword(password);
    sqlConnectionProperties.setDatabaseName(databaseName);
    sqlConnectionProperties.setJarPath(jarPath);
    sqlConnectionProperties.setDriverClass(driverClass);
    sqlConnectionProperties.setDisplayableConnectionPropertiesId(connectionId);
    sqlConnectionProperties.setInstanceName(instanceName);
    sqlConnectionProperties.setVendor(SqlConnectionProperties.Vendor.GOOGLE);

    return sqlConnectionProperties;
  }

  private static SqlConnectionProperties setTestCloudSqlConnectorsProperties(
      IJavaProject javaProject, String jarPath) {
    IProject project = javaProject.getProject();

    SqlConnectionProperties sqlConnectionProperties = new SqlConnectionProperties();

    String username = GoogleCloudSqlProperties.getTestDatabaseUser(project);
    String instanceName = GoogleCloudSqlProperties.getTestInstanceName(project);
    String databaseName = GoogleCloudSqlProperties.getTestDatabaseName(project);
    String password = GoogleCloudSqlProperties.getTestDatabasePassword(project);
    String driverClass = GAE_CLOUD_SQL_DRIVER_CLASS;
    String connectionId = getDisplaybleConnectionId(project.getName(),
        ConnectionType.CONNECTION_TYPE_TEST);

    sqlConnectionProperties.setUsername(username);
    sqlConnectionProperties.setPassword(password);
    sqlConnectionProperties.setDatabaseName(databaseName);
    sqlConnectionProperties.setJarPath(jarPath);
    sqlConnectionProperties.setDriverClass(driverClass);
    sqlConnectionProperties.setDisplayableConnectionPropertiesId(connectionId);
    sqlConnectionProperties.setInstanceName(instanceName);
    sqlConnectionProperties.setVendor(SqlConnectionProperties.Vendor.GOOGLE);

    return sqlConnectionProperties;
  }
}
