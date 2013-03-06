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
package com.google.appengine.eclipse.core.sdk;

import com.google.appengine.eclipse.core.AppEngineCorePlugin;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.preferences.ui.GaePreferencePage;
import com.google.appengine.eclipse.core.resources.GaeImages;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainerPage;

/**
 * Wizard for selecting a GAE SDK when editing a GAE Classpath Container.
 */
public class GaeSdkClasspathContainerPage extends
    SdkClasspathContainerPage<GaeSdk> {

  public GaeSdkClasspathContainerPage() {
    super("App Engine SDK", "App Engine SDK",
        AppEngineCorePlugin.getDefault().getImageDescriptor(
            GaeImages.APP_ENGINE_DEPLOY_LARGE), GaePreferences.getSdkManager(),
        GaeSdkContainer.CONTAINER_ID, GaePreferencePage.ID);
  }
}
