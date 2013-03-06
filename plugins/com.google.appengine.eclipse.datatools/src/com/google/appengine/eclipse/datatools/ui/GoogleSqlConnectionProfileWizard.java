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
package com.google.appengine.eclipse.datatools.ui;

import com.google.appengine.eclipse.core.datatools.SqlConnectionProperties;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.AppEngineBridge;
import com.google.appengine.eclipse.datatools.utils.DatatoolsUtils;

import org.eclipse.datatools.connectivity.ui.wizards.ExtensibleNewConnectionProfileWizard;

import java.util.Properties;

/**
 * The Google Cloud SQL connection profile wizard.
 */
public class GoogleSqlConnectionProfileWizard extends ExtensibleNewConnectionProfileWizard {

  public GoogleSqlConnectionProfileWizard() {
    super(new GoogleSqlProfileDetailsWizardPage(DatatoolsUtils.GOOGLE_SQL_WIZARD_PAGE_ID));
    createDefaultDriverInstance();
  }
  
  private Properties createDefaultDriverInstance() {
    Properties baseProperties = new Properties();

    String jarPath;
    if (GaePreferences.getDefaultSdk() != null) {
      jarPath = GaePreferences.getDefaultSdk().getInstallationPath()
          + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR_PATH_IN_SDK
          + AppEngineBridge.APPENGINE_CLOUD_SQL_JAR;
    } else {
      jarPath = AppEngineBridge.APPENGINE_CLOUD_SQL_JAR;
    }
    
    baseProperties = DatatoolsUtils.getSqlBaseProperties(jarPath, "database", 
        "com.google.cloud.sql.jdbc.internal.googleapi.GoogleApiDriver", "", "",
        "your_instance_name", null,
        SqlConnectionProperties.Vendor.GOOGLE);
    
    String driverId = DatatoolsUtils.getDriverId(SqlConnectionProperties.Vendor.GOOGLE.name());
    
    DatatoolsUtils.addDriverInstanceToDatatools(baseProperties, 
        SqlConnectionProperties.Vendor.GOOGLE.name());

    return baseProperties;
  }
}
