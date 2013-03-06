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

import java.util.Comparator;

/**
 * A comparator for P2 {@link P2LicenseInfo}s.
 */
public class LicenseInfoComparator implements Comparator<P2LicenseInfo> {

  /**
   * Create a new LicenseInfoComparator.
   */
  public LicenseInfoComparator() {
  }
  
  public int compare(P2LicenseInfo arg0, P2LicenseInfo arg1) {
    String name0 = arg0.getFeatureName();
    String name1 = arg1.getFeatureName();
    
    return name0.compareToIgnoreCase(name1);
  }

}
