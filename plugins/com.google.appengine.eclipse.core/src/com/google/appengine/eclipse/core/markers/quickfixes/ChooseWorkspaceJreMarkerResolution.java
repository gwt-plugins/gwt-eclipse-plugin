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
package com.google.appengine.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.SWTUtilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Marker resolution for the problem of using an old JRE. This resolution brings
 * up the JRE preferences page.
 */
public class ChooseWorkspaceJreMarkerResolution implements
    IMarkerResolution {

  private static final String JRE_PREFERENCE_PAGE_ID = "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage";

  public String getLabel() {
    return "Select a newer JRE for your workspace";
  }

  public void run(IMarker marker) {
    PreferencesUtil.createPreferenceDialogOn(SWTUtilities.getShell(),
        JRE_PREFERENCE_PAGE_ID, null, null).open();
  }
}
