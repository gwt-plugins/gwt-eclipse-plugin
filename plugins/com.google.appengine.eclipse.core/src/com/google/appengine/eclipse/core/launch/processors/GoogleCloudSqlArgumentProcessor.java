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
package com.google.appengine.eclipse.core.launch.processors;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GoogleCloudSqlProperties;
import com.google.appengine.eclipse.core.sql.SqlUtilities;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the VM arguments for Google Cloud SQL.
 */
public class GoogleCloudSqlArgumentProcessor implements
    ILaunchConfigurationProcessor {
  private static final String ARG_RDBMS_DRIVER = "-Drdbms.driver=";
  private static final String ARG_RDBMS_DRIVER_VALUE = "com.mysql.jdbc.Driver";
  private static final String ARG_RDBMS_EXTRA_PROPERTIES = "-Drdbms.extra.properties=";
  private static final String ARG_RDBMS_HOSTED_INSTANCE = "-Drdbms.hosted.instance=";
  private static final String ARG_RDBMS_SERVER = "-Drdbms.server=";
  private static final String ARG_RDBMS_SERVER_HOSTED_VALUE = "hosted";
  private static final String ARG_RDBMS_SERVER_LOCAL_VALUE = "local";
  private static final String ARG_RDBMS_URL = "-Drdbms.url=";
  private static final String ARG_RDBMS_DATABASE = "-Drdbms.database=";
  private static final String ARG_RDBMS_USER = "-Drdbms.user=";
  private static final String ARG_RDBMS_PASSWORD = "-Drdbms.password=";

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    removeSqlConfigurations(javaProject, vmArgs);

    boolean usingGoogleCloudSql = GoogleCloudSqlProperties.getGoogleCloudSqlEnabled(javaProject.getProject());
    boolean localDevMySqlEnabled = GoogleCloudSqlProperties.getLocalDevMySqlEnabled(javaProject.getProject());
    if (usingGoogleCloudSql && GaeNature.isGaeProject(javaProject.getProject())) {
      if (localDevMySqlEnabled) {
        updateMySqlLaunchCongiguration(launchConfig, javaProject, programArgs,
            vmArgs);
      } else {
        updateGoogleCloudSqlLaunchCongiguration(launchConfig, javaProject,
            programArgs, vmArgs);
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    return null;
  }

  private void remove(List<String> vmArgs, String argument) {
    int argIndex = StringUtilities.indexOfThatStartsWith(vmArgs, argument, 0);
    LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        vmArgs, argIndex, true);
  }

  private void removeSqlConfigurations(IJavaProject javaProject,
      List<String> vmArgs) {
    remove(vmArgs, ARG_RDBMS_SERVER);
    remove(vmArgs, ARG_RDBMS_EXTRA_PROPERTIES);
    remove(vmArgs, ARG_RDBMS_HOSTED_INSTANCE);
    remove(vmArgs, ARG_RDBMS_SERVER);
    remove(vmArgs, ARG_RDBMS_DRIVER);
    remove(vmArgs, ARG_RDBMS_URL);
    remove(vmArgs, ARG_RDBMS_DATABASE);
    remove(vmArgs, ARG_RDBMS_USER);
    remove(vmArgs, ARG_RDBMS_PASSWORD);
  }

  private void updateGoogleCloudSqlLaunchCongiguration(
      ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) {
    String password = GoogleCloudSqlProperties.getTestDatabasePassword(javaProject.getProject());
    String user = GoogleCloudSqlProperties.getTestDatabaseUser(javaProject.getProject());
    String instance = GoogleCloudSqlProperties.getTestInstanceName(javaProject.getProject());
    String database = GoogleCloudSqlProperties.getTestDatabaseName(javaProject.getProject());
    if (instance.trim().isEmpty()) {
      return;
    }
    vmArgs.add(0, ARG_RDBMS_SERVER + ARG_RDBMS_SERVER_HOSTED_VALUE);
    vmArgs.add(0, ARG_RDBMS_HOSTED_INSTANCE + "\"" + instance + "\"");
    vmArgs.add(0, ARG_RDBMS_DATABASE + "\"" + database + "\"");
    vmArgs.add(0, ARG_RDBMS_USER + "\"" + user + "\"");
    vmArgs.add(0, ARG_RDBMS_PASSWORD + "\"" + password + "\"");
  }

  private void updateMySqlLaunchCongiguration(
      ILaunchConfigurationWorkingCopy launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) {

    String rdbmsUrlValue = SqlUtilities.getMySqlUrl(javaProject.getProject());

    if (rdbmsUrlValue.isEmpty()) {
      return;
    }
    String database = GoogleCloudSqlProperties.getMySqlDatabaseName(javaProject.getProject());
    String user = GoogleCloudSqlProperties.getMySqlDatabaseUser(javaProject.getProject());
    String password = GoogleCloudSqlProperties.getMySqlDatabasePassword(javaProject.getProject());
    vmArgs.add(0, ARG_RDBMS_DATABASE + "\"" + database + "\"");
    vmArgs.add(0, ARG_RDBMS_USER + "\"" + user + "\"");
    vmArgs.add(0, ARG_RDBMS_PASSWORD + "\"" + password + "\"");
    vmArgs.add(0, ARG_RDBMS_SERVER + ARG_RDBMS_SERVER_LOCAL_VALUE);
    vmArgs.add(0, ARG_RDBMS_DRIVER + ARG_RDBMS_DRIVER_VALUE);
    vmArgs.add(0, ARG_RDBMS_URL + rdbmsUrlValue);
  }
}
