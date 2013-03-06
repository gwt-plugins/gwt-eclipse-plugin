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
package com.google.gwt.eclipse.core.preferences;

import com.google.gwt.eclipse.core.GWTPlugin;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Sets the default preferences values.
 */
public class GwtPreferenceInitializer extends AbstractPreferenceInitializer {

  public GwtPreferenceInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
    Preferences prefs = GWTPlugin.getDefault().getPluginPreferences();
    prefs.setDefault(
        GwtPreferenceConstants.UIBINDER_WIZARD_GENERATE_CONTENT_DEFAULT, true);
  }
}
