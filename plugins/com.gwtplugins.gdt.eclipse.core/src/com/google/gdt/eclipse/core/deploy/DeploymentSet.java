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
package com.google.gdt.eclipse.core.deploy;

import java.util.List;

/**
 * Describes what to deploy: 0-1 frontends, 0-n backends.
 */
public class DeploymentSet {
  
  private final boolean deployFrontend;
  private final List<String> backendNames;
  
  public DeploymentSet(boolean deployFrontend, List<String> backendNames) {
    this.deployFrontend = deployFrontend;
    this.backendNames = backendNames;
  }
  
  public List<String> getBackendNames() {
    return backendNames;
  }

  public boolean getDeployFrontend() {
    return deployFrontend;
  }
  
  public int getDeploymentUnitsCount() {
    int deploymentUnits = 0;
    if (deployFrontend) {
      deploymentUnits++;
    }
    deploymentUnits += backendNames.size();
    return deploymentUnits;
  }
}
