/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.mobile.android;

import com.google.gdt.eclipse.core.pde.BundleUtilities;

import org.eclipse.ui.AbstractSourceProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Exports the adtPresentVar variable.
 * 
 * If this plugin has all of its dependencies satisfied, including those that
 * are optionally based on ADT, then adtPresentVar is set to adtAvailable.
 * Otherwise, it is set up adtUnavailable. 
 * 
 */
public class AdtPresentVarSourceProvider extends AbstractSourceProvider {

  private static final String VARIABLE_NAME = GdtAndroidPlugin.PLUGIN_ID
      + ".adtPresentVar";

  private static final String VALUE_ADT_AVAILABLE = "adtAvailable";

  private static final String VALUE_ADT_UNAVAILABLE = "adtUnavailable";

  public void dispose() {
    // Not needed
  }

  public Map<String, String> getCurrentState() {
    Map<String, String> map = new HashMap<String, String>(1);
    if (BundleUtilities.areBundlesDependenciesSatisfied(GdtAndroidPlugin.PLUGIN_ID)) {
      map.put(VARIABLE_NAME, VALUE_ADT_AVAILABLE);
    } else {
      map.put(VARIABLE_NAME, VALUE_ADT_UNAVAILABLE);
    }
    return map;
  }

  public String[] getProvidedSourceNames() {
    return new String[] {VARIABLE_NAME};
  }

}
