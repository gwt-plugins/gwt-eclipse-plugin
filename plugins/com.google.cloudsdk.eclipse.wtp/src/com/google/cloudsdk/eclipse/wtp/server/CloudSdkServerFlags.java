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
package com.google.cloudsdk.eclipse.wtp.server;

import com.google.cloudsdk.eclipse.wtp.server.ServerFlagsInfo.Flag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Holds the list of "gcloud app run" flags for the Cloud SDK server.
 */
public class CloudSdkServerFlags {
  private static final String FLAGS_FILE = "server-flags.json";

  private static List<Flag> sFlags = null;

  // TODO: when the json formatting option is available for "gcloud preview app run --help"
  // replace the "server-flags.json" with its output
  public static List<Flag> getFlags() throws FileNotFoundException {
    if (sFlags == null) {
      InputStream input = CloudSdkServerFlags.class.getResourceAsStream(FLAGS_FILE);
      Scanner scanner = new Scanner(input);
      String content = scanner.useDelimiter("\\Z").next();
      scanner.close();
      Gson gson = new GsonBuilder().create();
      ServerFlagsInfo serverFlagsInfo = gson.fromJson(content, ServerFlagsInfo.class);
      sFlags = Collections.unmodifiableList(serverFlagsInfo.getFlags());
    }
    return sFlags;
  }
}
