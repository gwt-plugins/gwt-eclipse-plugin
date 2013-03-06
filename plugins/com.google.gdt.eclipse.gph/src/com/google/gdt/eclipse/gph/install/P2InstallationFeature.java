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
 * Represents a feature to be installed.
 */
public class P2InstallationFeature {
  private String featureId;
  private String featureLabel;
  private boolean installed;

  public P2InstallationFeature(String featureLabel, String featureId) {
    this.featureLabel = featureLabel;
    this.featureId = featureId;
  }

  public String getFeatureId() {
    return featureId;
  }

  public String getFeatureLabel() {
    return featureLabel;
  }

  public boolean isInstalled() {
    return installed;
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

}
