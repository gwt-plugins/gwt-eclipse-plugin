/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.cloudsdk.eclipse.wtp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

/**
 * Utility class to run gcloud commands.
 */
public class GCloudCommandDelegate {
  public static final String APP_ENGINE_COMPONENT_NAME = "App Engine Command Line Interface";
  public static final String CLOUD_SDK_COMPONENT_NAME = "Cloud SDK for Java Developers";
  public static final String GET_VERSION_CMD = "/bin/gcloud version";
  // TODO: Update this command to specify the json format (--format json) when that feature
  // becomes available
  public static final String GET_COMPONENTS_LIST_CMD = "/bin/gcloud components list";

  /**
   * Returns true if the Cloud SDK and the App Engine component have been installed.
   * Returns false otherwise.
   *
   * @param sdkLocation the location of the Cloud SDK
   * @return true if the Cloud SDK and App Engine component have been installed and false otherwise
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean areCloudSdkAndAppEngineInstalled(String sdkLocation)
      throws IOException, InterruptedException {
    if (!(new File(sdkLocation)).exists()) {
      throw new InvalidPathException(sdkLocation, "Path does not exist");
    }

    Process process = Runtime.getRuntime().exec(sdkLocation + GET_COMPONENTS_LIST_CMD);
    String output = getProcessOutput(process);

    // Check process output for Cloud SDK and App Engine status
    return isComponentInstalled(output, APP_ENGINE_COMPONENT_NAME)
        && isComponentInstalled(output, CLOUD_SDK_COMPONENT_NAME);
  }

  @VisibleForTesting
  public static boolean isComponentInstalled(String output, String componentName) {
    // Sample output:
    // -------------------------------------------------------------------------------
    // |                                  Packages                                   |
    // |-----------------------------------------------------------------------------|
    // | Status        | Name                                    | ID         | Size |
    // |---------------+-----------------------------------------+------------+------|
    // | Not Installed | Cloud SDK for Go Developers             | pkg-go     |      |
    // ...
    // | Installed     | Cloud SDK for Java Developers           | pkg-java   |      |
    // -------------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------------------------
    // |                                 Individual Components                                  |
    // |----------------------------------------------------------------------------------------|
    // | Status           | Name                                        | ID         |     Size |
    // |------------------+---------------------------------------------+------------+----------|
    // | Update Available | App Engine Command Line Interface (Preview) | app        |   < 1 MB |
    // | Update Available | BigQuery Command Line Tool                  | bq         |   < 1 MB |
    // ...
    // | Not Installed    | App Engine SDK for Go                       | gae-go     |          |
    // ...
    // | Installed        | App Engine SDK for Java                     | gae-java   | 161.2 MB |
    // ...
    // ------------------------------------------------------------------------------------------
    //

    // TODO: update the parsing to use a Json object when the ability to specify the
    // json format becomes available
    Pattern pattern =
        Pattern.compile("\\|\\s*(Update Available|Installed)\\s*\\|\\s*" + componentName);
    return pattern.matcher(output).find();
  }

  private static String getProcessOutput(Process process) throws IOException, InterruptedException {
    InputStream diffStream = process.getInputStream();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ByteStreams.copy(diffStream, bos);
    process.waitFor();
    return new String(bos.toByteArray());
  }
}
