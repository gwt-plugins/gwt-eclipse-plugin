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

/**
 * Contains connection properties which should be passed on to the SQL plugin.
 */
public class SqlConnectionProperties {
  
  /**
   * Vendor type. Currently only two allowed.
   */
  public enum Vendor {
    GOOGLE,
    MYSQL
  }
  
  private String databaseName = null;
  private String displayableConnectionPropertiesId = null;
  private String driverClass = null;
  private String instanceName = null;
  private String jarPath = null;
  private String jdbcUrl = null;
  private String password = null;
  private String username = null;
  private Vendor vendor;

  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Get a meaningful id for the connection which you can display to the user.
   */
  public String getDisplayableConnectionPropertiesId() {
    return displayableConnectionPropertiesId;
  }

  public String getDriverClass() {
    return driverClass;
  }

  public String getInstanceName() {
    return instanceName;
  }
  
  public String getJarPath() {
    return jarPath;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public Vendor getVendor() {
    return vendor;
  }
  
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * Get a meaningful id for the connection which you can display to the user.
   */
  public void setDisplayableConnectionPropertiesId(String displayableConnectionPropertiesId) {
    this.displayableConnectionPropertiesId = displayableConnectionPropertiesId;
  }

  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }
  
  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }
  
  public void setPassword(String password) {  
    this.password = password;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
  
  public void setVendor(Vendor vendor) {
    this.vendor = vendor;
  }
}
