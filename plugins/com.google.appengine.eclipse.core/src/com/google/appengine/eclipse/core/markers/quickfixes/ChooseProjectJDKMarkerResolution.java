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

import com.google.appengine.eclipse.core.AppEngineCorePlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Marker resolution for the problem of using an App Engine project with a JRE
 * and JSPs. This resolution brings up the Java Build Path dialog and gives the
 * user the opportunity to switch their project JRE.
 * 
 * TODO: Consider extending WorkbenchMarkerResolution so that multiple
 * quick-fixes can be applied at the same time.
 */
@SuppressWarnings("restriction")
public class ChooseProjectJDKMarkerResolution implements IMarkerResolution {

  public String getLabel() {
    return "Select a JDK-based 'JRE System Library' for your project";
  }

  public void run(IMarker marker) {
    final IProject project = marker.getResource().getProject();
    PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(
        AppEngineCorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
        project, BuildPathsPropertyPage.PROP_ID,
        new String[] {BuildPathsPropertyPage.PROP_ID}, null);
    page.open();
  }
}
