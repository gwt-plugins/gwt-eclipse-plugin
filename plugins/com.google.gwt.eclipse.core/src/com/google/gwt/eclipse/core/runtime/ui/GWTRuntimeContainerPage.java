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
package com.google.gwt.eclipse.core.runtime.ui;

import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainerPage;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.preferences.ui.GwtPreferencePage;
import com.google.gwt.eclipse.core.resources.GWTImages;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;

/**
 * Wizard for selecting a GWT SDK when editing a GWT Classpath Container.
 */
public class GWTRuntimeContainerPage extends
    SdkClasspathContainerPage<GWTRuntime> {

  public GWTRuntimeContainerPage() {
    super("GWT SDK", "GWT SDK", GWTPlugin.getDefault().getImageDescriptor(
        GWTImages.GWT_LOGO), GWTPreferences.getSdkManager(),
        GWTRuntimeContainer.CONTAINER_ID, GwtPreferencePage.ID);
  }

  @Override
  protected GWTRuntime validateJavaProjectAndSelectedSdk() {
    GWTRuntime selectedSdk = super.validateJavaProjectAndSelectedSdk();

    if (selectedSdk == null) {
      // There was an error when validating the Java project and
      // SDK chosen by the user; abort our validation.
      // FIXME: Are we supposed to return null here?
    }

    // Verify that if the user has a pre-GWT 1.6 SDK selected, that their
    // project does not have a configured WAR directory
    if (WebAppUtilities.isWebApp(javaProject.getProject())
        && !selectedSdk.containsSCL()) {
      setErrorMessage("You must use GWT 1.6+ with a Web Application project.");
      setPageComplete(false);
    }

    return selectedSdk;
  }
}
