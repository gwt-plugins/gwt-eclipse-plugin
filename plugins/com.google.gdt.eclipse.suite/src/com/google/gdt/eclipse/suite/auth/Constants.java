/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.suite.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Placeholder constants.
 */
public class Constants {

  // The properties are located in settings.xml
  // The README.md has the settings.xml configuration
  // https://console.developers.google.com/apis/credentials/domainverification?project=ide-plugin
  public static String ID = getId();
  public static String SECRET;

  /**
   * Ugly but it gets the job done providing properties with out a remodel.
   */
  private static String getId() {
    InputStream oauthInput = Constants.class.getResourceAsStream("oauth.properties");
    Properties properties = new Properties();
    try {
      properties.load(oauthInput);
      ID = properties.getProperty("id");
      SECRET = properties.getProperty("secret");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ID;
  }


}
