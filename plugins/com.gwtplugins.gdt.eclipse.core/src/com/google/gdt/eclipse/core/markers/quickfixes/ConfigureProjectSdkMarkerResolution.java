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
package com.google.gdt.eclipse.core.markers.quickfixes;

import com.google.gdt.eclipse.core.CorePlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Marker resolution for the problem of an invalid (or not present) SDK. This
 * resolution will bring up the SDK configuration page so that the user can
 * select/inspect/change the SDK that the project is using.
 */
public class ConfigureProjectSdkMarkerResolution implements IMarkerResolution {

  private String sdkTypeName;
  private String projectPropertyPageID;

  /**
   * Constructs a new instance of this marker resolution.
   * 
   * @param sdkTypeName An enum value representing the SDK's name
   * @param projectPropertyPageID The ID of the Project Property Page that
   *          should be opened to configure the SDK
   */
  public ConfigureProjectSdkMarkerResolution(String sdkTypeName,
      String projectPropertyPageID) {
    this.sdkTypeName = sdkTypeName;
    this.projectPropertyPageID = projectPropertyPageID;
  }

  public String getLabel() {
    return "Select or inspect the " + sdkTypeName + " SDK for your project";
  }

  public void run(final IMarker marker) {
    PreferenceDialog page = PreferencesUtil.createPropertyDialogOn(
        CorePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
        marker.getResource().getProject(), projectPropertyPageID,
        new String[] {projectPropertyPageID}, null);

    page.open();
  }
}
