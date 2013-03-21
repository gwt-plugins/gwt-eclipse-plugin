/*******************************************************************************
 * 
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.mobile.android.wizards.helpers;

import com.android.AndroidConstants;
import com.android.resources.Density;
import com.android.SdkConstants;

/**
 * Used for Project Creation
 */

public class ProjectCreationConstants {

  public static final String SHARED_FOLDER_NAME = "shared"; //$NON-NLS-1$

  public static final String PARAM_SDK_TOOLS_DIR = "ANDROID_SDK_TOOLS"; //$NON-NLS-1$
  public static final String PARAM_ACTIVITY = "ACTIVITY_NAME"; //$NON-NLS-1$
  public static final String PARAM_APPLICATION = "APPLICATION_NAME"; //$NON-NLS-1$
  public static final String PARAM_PACKAGE = "PACKAGE"; //$NON-NLS-1$
  public static final String PARAM_PROJECT = "PROJECT_NAME"; //$NON-NLS-1$
  public static final String PARAM_SDK_TARGET = "SDK_TARGET"; //$NON-NLS-1$
  public static final String PARAM_MIN_SDK_VERSION = "MIN_SDK_VERSION"; //$NON-NLS-1$
  public static final String PARAM_C2DM_EMAIL = "C2DM_EMAIL"; //$NON-NLS-N$
  public static final String PARAM_PROJECT_PATH = "PROJECT_PATH"; //$NON-NLS-N$
  
  public static final String PARAM_GCM_API_KEY = "GCM_API_KEY"; //$NON-NLS-N$
  public static final String PARAM_GCM_PROJECT_NUMBER = "GCM_PROJECT_NUMBER"; //$NON-NLS-N$

  public static final String MAIN_MENU_FILE = "activity_main.xml"; //$NON-NLS-1$

  public static final String STRING_RSRC_PREFIX = "@string/"; //$NON-NLS-1$
  public static final String STRING_APP_NAME = "app_name"; //$NON-NLS-1$
  public static final String STRINGS_FILE = "strings.xml"; //$NON-NLS-1$
  public static final String STYLES_FILE = "styles.xml";

  public static final String WS_SEP = "/";

  public static final String BIN_DIRECTORY = SdkConstants.FD_OUTPUT + WS_SEP;
  public static final String LIB_DIRECTORY = SdkConstants.FD_LIB + WS_SEP;
  public static final String RES_DIRECTORY = SdkConstants.FD_RESOURCES + WS_SEP;
  public static final String ASSETS_DIRECTORY = SdkConstants.FD_ASSETS + WS_SEP;

  public static final String DRAWABLE_XHDPI_DIRECTORY = AndroidConstants.FD_RES_DRAWABLE
      + "-" + Density.XHIGH.getResourceValue() + WS_SEP; //$NON-NLS-1$
  public static final String DRAWABLE_HDPI_DIRECTORY = AndroidConstants.FD_RES_DRAWABLE
      + "-" + Density.HIGH.getResourceValue() + WS_SEP; //$NON-NLS-1$
  public static final String DRAWABLE_MDPI_DIRECTORY = AndroidConstants.FD_RES_DRAWABLE
      + "-" + Density.MEDIUM.getResourceValue() + WS_SEP; //$NON-NLS-1$
  public static final String DRAWABLE_LDPI_DIRECTORY = AndroidConstants.FD_RES_DRAWABLE
      + "-" + Density.LOW.getResourceValue() + WS_SEP; //$NON-NLS-1$

  public static final String LAYOUT_DIRECTORY = AndroidConstants.FD_RES_LAYOUT + WS_SEP;
  public static final String MENU_DIRECTORY = AndroidConstants.FD_RES_MENU + WS_SEP;
  public static final String VALUES_DIRECTORY = AndroidConstants.FD_RES_VALUES + WS_SEP;
  public static final String GEN_SRC_DIRECTORY = SdkConstants.FD_GEN_SOURCES + WS_SEP;

  public static final String[] DEFAULT_DIRECTORIES = new String[] {
      BIN_DIRECTORY, RES_DIRECTORY, ASSETS_DIRECTORY};
  public static final String[] RES_DENSITY_ENABLED_DIRECTORIES = new String[] {
      DRAWABLE_XHDPI_DIRECTORY, DRAWABLE_HDPI_DIRECTORY, DRAWABLE_MDPI_DIRECTORY,
      DRAWABLE_LDPI_DIRECTORY, LAYOUT_DIRECTORY, MENU_DIRECTORY, VALUES_DIRECTORY};

  public static final String PH_USES_SDK = "USES-SDK"; //$NON-NLS-1$

  public static final String TEMPLATES_DIRECTORY = "templates/"; //$NON-NLS-1$
  public static final String TEMPLATE_USES_SDK = TEMPLATES_DIRECTORY + "uses-sdk.template"; //$NON-NLS-1$
  public static final String PREFS_DIRECTORY = ".settings/"; //$NON-NLS-N$

  public static final String APP_ENGINE_PROJECT = "appEngineProject"; //$NON-NLS-N$
  public static final String JDT_APT_PREFS = "org.eclipse.jdt.apt.core.prefs"; //$NON-NLS-N$
  public static final String JDT_PREFS = "org.eclipse.jdt.core.prefs"; //$NON-NLS-N$
  public static final String FACTORYPATH_FILE = ".factorypath"; //$NON-NLS-N$
  public static final String APT_FOLDER = ".apt_generated"; //$NON-NLS-N$

  public static final String CONFIG_DIRECTORY = "config/";

}
