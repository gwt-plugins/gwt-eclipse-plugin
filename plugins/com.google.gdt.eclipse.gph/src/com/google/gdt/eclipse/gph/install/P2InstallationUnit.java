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

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around an update site URL and a list of features to install.
 */
public class P2InstallationUnit {
  private String name;
  private String updateSite;
  private List<P2InstallationFeature> features;
  
  public P2InstallationUnit(String name, String updateSite, List<P2InstallationFeature> features) {
    this.name = name;
    this.updateSite = updateSite;
    
    this.features = new ArrayList<P2InstallationFeature>();
    
    for (P2InstallationFeature f : features) {
      this.features.add(new P2InstallationFeature(f.getFeatureLabel(), f.getFeatureId()));
    }
  }
  
  public List<P2InstallationFeature> getFeatures() {
    return features;
  }

  public String getInstallationUnitName() {
    return name;
  }

  public String getUpdateSite() {
    return updateSite;
  }
  
  public URI getUpdateSiteURI() {
    try {
      return new URI(getUpdateSite());
    } catch (URISyntaxException e) {
      ProjectHostingUIPlugin.logError(e);
      
      return null;
    }
  }
  
  @Override
  public String toString() {
    return getUpdateSite();
  }
  
}
