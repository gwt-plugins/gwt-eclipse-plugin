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
package com.google.gdt.eclipse.gph.install;

/**
 * Holds information about features that will be installed. This includes the feature name,
 * version, and license agreement.
 */
public class P2LicenseInfo {
  private String featureName;
  private String version;
  private String licenseText;
  private String licenseUUID;
  
  /**
   * Create a new P2LicenseInfo.
   * 
   * @param featureName
   * @param version
   * @param licenseText
   * @param licenseUUID
   */
  public P2LicenseInfo(String featureName, String version, String licenseText, String licenseUUID) {
    this.featureName = featureName;
    this.version = version;
    this.licenseText = licenseText;
    this.licenseUUID = licenseUUID;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    P2LicenseInfo other = (P2LicenseInfo) obj;
    if (featureName == null) {
      if (other.featureName != null)
        return false;
    } else if (!featureName.equals(other.featureName))
      return false;
    if (licenseUUID == null) {
      if (other.licenseUUID != null)
        return false;
    } else if (!licenseUUID.equals(other.licenseUUID))
      return false;
    return true;
  }

  /**
   * @return the feature name
   */
  public String getFeatureName() {
    return featureName;
  }

  /**
   * @return the feature's license agreement
   */
  public String getLicenseText() {
    return licenseText;
  }

  /**
   * @return the feature's version (can be null)
   */
  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((featureName == null) ? 0 : featureName.hashCode());
    result = prime * result
        + ((licenseUUID == null) ? 0 : licenseUUID.hashCode());
    return result;
  }
  
}
