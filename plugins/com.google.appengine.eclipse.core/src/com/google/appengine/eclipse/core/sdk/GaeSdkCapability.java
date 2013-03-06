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
package com.google.appengine.eclipse.core.sdk;

import com.google.gdt.eclipse.core.sdk.SdkUtils;

/**
 * Enum of GAE SDK capabilities.
 *
 * Simple version checks are supported using min/max ranges but more complex
 * validations can be implemented by overriding the check() method.
 */
public enum GaeSdkCapability {
  /**
   * High replication datastore support.
   */
  HRD("1.5.1"),
  GOOGLE_CLOUD_SQL("1.6.1"),
  OPTIONAL_USER_LIB(AppEngineBridge.MIN_VERSION_FOR_OPT_DATANUCLEUS_LIB);

  private final String minVersion, maxVersion;

  private GaeSdkCapability() {
    this(null);
  }

  private GaeSdkCapability(String minVersion) {
    this(minVersion, null);
  }

  private GaeSdkCapability(String minVersion, String maxVersion) {
    this.minVersion = minVersion;
    this.maxVersion = maxVersion;
  }

  /**
   * Verify whether a capability is supported by the given SDK.
   */
  public boolean check(GaeSdk sdk) {
    String sdkVersion = sdk.getVersion();

    // If no restrictions are specified, assume the feature is always supported.
    return (minVersion == null || SdkUtils.compareVersionStrings(sdkVersion,
        minVersion) >= 0)
        && (maxVersion == null || SdkUtils.compareVersionStrings(sdkVersion,
            maxVersion) <= 0);
  }
}

