/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.core.properties;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Gets and sets Google Cloud SQL Service project properties.
 */
public final class GoogleCloudSqlProperties {

  private static final String GOOGLE_CLOUD_SQL_ENABLED = "googleCloudSqlEnabled";

  // If false, user wants to use Google Cloud SQL Service with local appengine.
  private static final String LOCAL_DEV_MYSQL_ENABLED = "localDevMySqlEnabled";

  private static final String MYSQL_DATABASE_NAME = "mySqlDatabaseName";

  private static final String MYSQL_DATABASE_PASSWORD = "mySqlDatabasePassword";

  private static final String MYSQL_DATABASE_USER = "mySqlDatabaseUser";

  private static final String MYSQL_HOST_NAME = "mySqlHostName";

  private static final String MYSQL_IS_CONFIGURED = "mySqlIsConfigured";

  private static final String MYSQL_JDBC_JAR = "mySqlJdbcJar";

  private static final String MYSQL_PORT = "mySqlPort";

  private static final String PROD_DATABASE_NAME = "prodDatabaseName";

  private static final String PROD_DATABASE_PASSWORD = "prodDatabasePassword";

  private static final String PROD_DATABASE_USER = "prodDatabaseUser";

  private static final String PROD_INSTANCE_NAME = "prodInstanceName";

  private static final String PROD_IS_CONFIGURED = "prodIsConfigured";

  private static final String TEST_DATABASE_NAME = "testDatabaseName";

  private static final String TEST_DATABASE_PASSWORD = "testDatabasePassword";

  private static final String TEST_DATABASE_USER = "testDatabaseUser";

  private static final String TEST_INSTANCE_NAME = "testInstanceName";

  private static final String TEST_IS_CONFIGURED = "testIsConfigured";

  public static Boolean getGoogleCloudSqlEnabled(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(GOOGLE_CLOUD_SQL_ENABLED, false);
  }

  public static boolean getLocalDevMySqlEnabled(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(LOCAL_DEV_MYSQL_ENABLED, true);
  }

  public static String getMySqlDatabaseName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(MYSQL_DATABASE_NAME, "");
  }

  public static String getMySqlDatabasePassword(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(MYSQL_DATABASE_PASSWORD, "");
  }

  public static String getMySqlDatabaseUser(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(MYSQL_DATABASE_USER, "root");
  }

  public static String getMySqlHostName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(MYSQL_HOST_NAME, "localhost");
  }

  public static boolean getMySqlIsConfigured(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(MYSQL_IS_CONFIGURED, false);
  }

  public static String getMySqlJdbcJar(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(MYSQL_JDBC_JAR, "");
  }

  public static int getMySqlPort(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getInt(MYSQL_PORT, 3306);
  }

  public static String getProdDatabaseName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(PROD_DATABASE_NAME, "");
  }

  public static String getProdDatabasePassword(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(PROD_DATABASE_PASSWORD, "");
  }

  public static String getProdDatabaseUser(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(PROD_DATABASE_USER, "");
  }

  public static String getProdInstanceName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(PROD_INSTANCE_NAME, "");
  }

  public static boolean getProdIsConfigured(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(PROD_IS_CONFIGURED, false);
  }

  public static String getTestDatabaseName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(TEST_DATABASE_NAME, "");
  }

  public static String getTestDatabasePassword(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(TEST_DATABASE_PASSWORD, "");
  }

  public static String getTestDatabaseUser(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(TEST_DATABASE_USER, "");
  }

  public static String getTestInstanceName(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.get(TEST_INSTANCE_NAME, "");
  }

  public static boolean getTestIsConfigured(IProject project) {
    IEclipsePreferences prefs = getProjectProperties(project);
    return prefs.getBoolean(TEST_IS_CONFIGURED, false);
  }

  public static void jobSetGoogleCloudSqlEnabled(
      final IProject project, final boolean cloudSqlEnabled) {
    Job job = new WorkspaceJob(GaeProjectProperties.PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setGoogleCloudSqlEnabled(project, cloudSqlEnabled);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    GaeProjectProperties.startWorkspaceJob(job, project);
  }

  public static void jobSetLocalDevMySqlEnabled(final IProject project,
      final boolean isDevMySqlEnabled) {
    Job job = new WorkspaceJob(GaeProjectProperties.PREFERENCES_JOB_NAME) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          setLocalDevMySqlEnabled(project, isDevMySqlEnabled);
          return Status.OK_STATUS;
        } catch (BackingStoreException e) {
          return StatusUtilities.newErrorStatus(e, AppEngineCorePlugin.PLUGIN_ID);
        }
      }
    };
    GaeProjectProperties.startWorkspaceJob(job, project);
  }

  public static void setGoogleCloudSqlEnabled(IProject project, Boolean googleCloudSqlEnabled)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(GOOGLE_CLOUD_SQL_ENABLED, googleCloudSqlEnabled.toString());
    prefs.flush();
  }

  public static void setLocalDevMySqlEnabled(IProject project, Boolean mySqlEnabled)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(LOCAL_DEV_MYSQL_ENABLED, mySqlEnabled.toString());
    prefs.flush();
  }

  public static void setMySqlDatabaseName(IProject project, String databaseName)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_DATABASE_NAME, databaseName);
    prefs.flush();
  }

  public static void setMySqlDatabasePassword(IProject project, String password)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_DATABASE_PASSWORD, password);
    prefs.flush();
  }

  public static void setMySqlDatabaseUser(IProject project, String string)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_DATABASE_USER, string);
    prefs.flush();
  }

  public static void setMySqlHostName(IProject project, String host) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_HOST_NAME, host);
    prefs.flush();
  }

  public static void setMySqlIsConfigured(IProject project, Boolean isConfigured)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_IS_CONFIGURED, isConfigured.toString());
    prefs.flush();
  }

  public static void setMySqlJdbcJar(IProject project, String jdbcJar) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(MYSQL_JDBC_JAR, jdbcJar);
    prefs.flush();
  }

  public static void setMySqlPort(IProject project, int port) throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.putInt(MYSQL_PORT, port);
    prefs.flush();
  }

  public static void setProdDatabaseName(IProject project, String database)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(PROD_DATABASE_NAME, database);
    prefs.flush();
  }

  public static void setProdDatabasePassword(IProject project, String password)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(PROD_DATABASE_PASSWORD, password);
    prefs.flush();
  }

  public static void setProdDatabaseUser(IProject project, String username)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(PROD_DATABASE_USER, username);
    prefs.flush();
  }

  public static void setProdInstanceName(IProject project, String instance)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(PROD_INSTANCE_NAME, instance);
    prefs.flush();
  }

  public static void setProdIsConfigured(IProject project, Boolean isConfigured)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(PROD_IS_CONFIGURED, isConfigured.toString());
    prefs.flush();
  }

  public static void setTestDatabaseName(IProject project, String database)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(TEST_DATABASE_NAME, database);
    prefs.flush();
  }

  public static void setTestDatabasePassword(IProject project, String password)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(TEST_DATABASE_PASSWORD, password);
    prefs.flush();
  }

  public static void setTestDatabaseUser(IProject project, String username)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(TEST_DATABASE_USER, username);
    prefs.flush();
  }

  public static void setTestInstanceName(IProject project, String instance)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(TEST_INSTANCE_NAME, instance);
    prefs.flush();
  }

  public static void setTestIsConfigured(IProject project, Boolean isConfigured)
      throws BackingStoreException {
    IEclipsePreferences prefs = getProjectProperties(project);
    prefs.put(TEST_IS_CONFIGURED, isConfigured.toString());
    prefs.flush();
  }

  private static IEclipsePreferences getProjectProperties(IProject project) {
    IScopeContext projectScope = new ProjectScope(project);
    return projectScope.getNode(AppEngineCorePlugin.PLUGIN_ID);
  }

  private GoogleCloudSqlProperties() {
    // Not instantiable
  }
}
