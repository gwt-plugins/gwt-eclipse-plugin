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
package com.google.appengine.eclipse.datatools.utils;

import com.google.appengine.eclipse.core.datatools.SqlConnectionProperties;
import com.google.appengine.eclipse.core.datatools.SqlConnectionProperties.Vendor;
import com.google.appengine.eclipse.core.sql.SqlUtilities;
import com.google.appengine.eclipse.datatools.GoogleDatatoolsPluginLog;

import org.eclipse.core.runtime.IPath;
import org.eclipse.datatools.connectivity.ConnectionProfileException;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;
import org.eclipse.datatools.connectivity.drivers.IDriverMgmtConstants;
import org.eclipse.datatools.connectivity.drivers.IPropertySet;
import org.eclipse.datatools.connectivity.drivers.PropertySetImpl;
import org.eclipse.datatools.connectivity.drivers.XMLFileManager;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCConnectionProfileConstants;
import org.eclipse.datatools.connectivity.drivers.jdbc.IJDBCDriverDefinitionConstants;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Utils for datatools plugin.
 */
public class DatatoolsUtils {

  public static final String DTP_MYSQL_VENDOR_PROP_ID = "MySql";

  public static final String DTP_MYSQL_VERSION_PROP_ID = "5.1";

  public static final String DTP_BRIDGE_PROP_DEFN_ID =
      "org.eclipse.datatools.connectivity.driverDefinitionID";

  public static final String DTP_MYSQL_CONNECTION_PROFILE =
      "org.eclipse.datatools.enablement.mysql.connectionProfile";

  public static final String DTP_GOOGLE_CLOUD_SQL_CONNECTION_PROFILE =
      "com.google.appengine.eclipse.datatools.connectionProfile";

  public static final String DTP_MYSQLBRIDGE_PROP_DEFN_TYPE =
      "org.eclipse.datatools.enablement.mysql.5_1.driverTemplate";

  public static final String DTP_GOOGLEBRIDGE_PROP_DEFN_TYPE =
      "org.eclipse.datatools.connectivity.googlecloudsql.driverTemplate";

  public static final String GOOGLESQL_INSTANCENAME_PROP_ID =
      "org.eclipse.datatools.connectivity.googlecloudsql.instancename";

  public static final String GOOGLE_SQL_DRIVER_CATEGORY_ID =
      "com.google.appengine.eclipse.datatools.driverCategory";

  public static final String GOOGLE_SQL_WIZARD_PAGE_ID =
      "com.google.appengine.eclipse.datatools.googleSqlProfileDetailsWizardPage";

  public static void changeDriverFilePermissions() {
    IPath path = XMLFileManager.getStorageLocation();
    path = path.append(IDriverMgmtConstants.DRIVER_FILE);
    File file = path.toFile();
    if (file.exists()) {
      try {
        Runtime.getRuntime().exec("chmod 600 " + path.toString());
      } catch (IOException e) {
        GoogleDatatoolsPluginLog.logError("Could not change permissions on the driver file");
      }
    }
  }

  public static  String getDriverName(String driverName) {
    return "Driver (" + driverName + ")";
  }
  
  public static  String getDriverId(String driverId) {
    return "Driver (" + driverId + ")";
  }
  
  public static  String getProfileId(String displayableId) {
    return "Profile ID (" + displayableId + ")";
  }

  public static  String getProfileName(String displayableId) {
    return "Profile (" + displayableId + ")";
  }

  public static void setJdbcUrl(IConnectionProfile profile) {
    Properties properties = profile.getBaseProperties();
    String url = SqlUtilities.getCloudSqlJdbcUrl(
        properties.getProperty(GOOGLESQL_INSTANCENAME_PROP_ID),
        properties.getProperty(IJDBCDriverDefinitionConstants.DATABASE_NAME_PROP_ID),
        true);
    properties.setProperty(IJDBCConnectionProfileConstants.URL_PROP_ID, url);
    profile.setBaseProperties(properties);
    changeDriverFilePermissions();
  }
  
  /**
   * Create properties for a DTP driver.
   * @param connectionProperties
   * @return Properties for a DTP driver.
   */
  public static Properties getSqlBaseProperties(
      SqlConnectionProperties connectionProperties) {
    return getSqlBaseProperties(connectionProperties.getJarPath(),
        connectionProperties.getDatabaseName(), connectionProperties.getDriverClass(),
        connectionProperties.getUsername(), connectionProperties.getPassword(),
        connectionProperties.getInstanceName(), connectionProperties.getJdbcUrl(),
        connectionProperties.getVendor());
  }
  /**
   * Create properties object corresponding to the function arguments. 
   * @return Properties populated by the function arguments.
   */
  public static Properties getSqlBaseProperties(String jarPath, String database, 
      String driverClass, String username, String password, String instance, String jdbcUrl,
      Vendor vendor) {
    Properties baseProperties = new Properties();
    baseProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_JARLIST, jarPath);
    baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_NAME_PROP_ID, database);
    baseProperties.setProperty(IJDBCConnectionProfileConstants.DRIVER_CLASS_PROP_ID, driverClass);
    baseProperties.setProperty(IJDBCConnectionProfileConstants.PASSWORD_PROP_ID, password);
    baseProperties.setProperty(IJDBCConnectionProfileConstants.SAVE_PASSWORD_PROP_ID,
        String.valueOf(true));
    baseProperties.setProperty(IJDBCConnectionProfileConstants.USERNAME_PROP_ID, username);
    
    // We use the Mysql vendor here to use the Mysql catalog loader.
    baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VENDOR_PROP_ID,
        DTP_MYSQL_VENDOR_PROP_ID);
    baseProperties.setProperty(IJDBCConnectionProfileConstants.DATABASE_VERSION_PROP_ID,
        DTP_MYSQL_VERSION_PROP_ID);
    
    if (vendor == Vendor.GOOGLE) {
      baseProperties.setProperty(GOOGLESQL_INSTANCENAME_PROP_ID, instance);
      baseProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE,
          DTP_GOOGLEBRIDGE_PROP_DEFN_TYPE);
      
    } else if (vendor == Vendor.MYSQL) {
      baseProperties.setProperty(IJDBCConnectionProfileConstants.URL_PROP_ID, jdbcUrl);
      baseProperties.setProperty(IDriverMgmtConstants.PROP_DEFN_TYPE, 
          DTP_MYSQLBRIDGE_PROP_DEFN_TYPE);      
    }
    
    return baseProperties;
  }
  
  /**
   * Creates a driver instance and adds it to DTP.
   * @param baseProperties
   * @param displayableId
   */
  public static void addDriverInstanceToDatatools(Properties baseProperties, String displayableId) {
    IPropertySet propertySet = new PropertySetImpl(getDriverName(displayableId), 
        getDriverId(displayableId));
    propertySet.setBaseProperties(baseProperties);
    DriverInstance di = new DriverInstance(propertySet);
    DriverManager.getInstance().addDriverInstance(di);
  }
  
  /**
   * Creates DTP connection profile.
   * @param baseProperties
   * @param displayableId This used to generate the profile id, 
   * @param vendor
   */
  private static void createProfileHelper(Properties baseProperties, String displayableId, 
      Vendor vendor) {
    // Create a profile using the driver.
    ProfileManager profileManager = ProfileManager.getInstance();
    IConnectionProfile icp = profileManager.getProfileByName(getProfileName(displayableId));
    baseProperties.setProperty(DatatoolsUtils.DTP_BRIDGE_PROP_DEFN_ID, getDriverId(displayableId));
    
    try {
      IConnectionProfile oldProfile = profileManager.getProfileByName(
          getProfileName(displayableId));
      
      if (profileManager.getProfileByName(getProfileName(displayableId)) != null) {
        profileManager.deleteProfile(oldProfile);
      }
      
      String profileId = (vendor == Vendor.GOOGLE) ?
          DTP_GOOGLE_CLOUD_SQL_CONNECTION_PROFILE : DTP_MYSQL_CONNECTION_PROFILE;

      // Create the connection profile, and connect it.
      // (But don't use the auto-connect setting, because that will result in
      // profile not getting created if auto-connect fails.)
      profileManager.createProfile(getProfileName(displayableId), getProfileId(displayableId), 
          profileId, baseProperties).connect();
    } catch (ConnectionProfileException e) {
      GoogleDatatoolsPluginLog.logError(e, "Could not create DTP connection profile");
    }
  }
  
  /**
   * Create DTP connection profile for the given connection properties.
   * @param connectionProperties
   */
  public static void createProfile(SqlConnectionProperties connectionProperties) {
    Properties baseProperties = DatatoolsUtils.getSqlBaseProperties(connectionProperties);
    String displayableId = connectionProperties.getDisplayableConnectionPropertiesId();
    Vendor vendor = connectionProperties.getVendor();
    DatatoolsUtils.addDriverInstanceToDatatools(baseProperties, displayableId);
    createProfileHelper(baseProperties, displayableId, vendor);
  }
}
