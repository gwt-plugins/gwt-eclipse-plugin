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
package com.google.gwt.eclipse.core.sdk;

import com.google.gdt.eclipse.core.sdk.SdkFactory;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.net.URL;

/**
 * Helper class that handles the auto-registration of GWT SDK bundles.
 */
public class GWTSdkRegistrant {

  /**
   * Registers a GWT SDK for use by GPE.
   *
   * @param sdkUrl - File URL pointing to the root of a GWT SDK installation
   * @throws CoreException
   */
  public static void registerSdk(URL sdkUrl) throws CoreException {
    SdkManager<GWTRuntime> sdkManager = GWTPreferences.getSdkManager();
    SdkSet<GWTRuntime> sdks = sdkManager.getSdks();
    String uniqueName = SdkUtils.generateUniqueSdkNameFrom("GWT", sdks);
    SdkFactory<GWTRuntime> factory = GWTRuntime.getFactory();
    String sdkLocation = sdkUrl.getPath();
    GWTRuntime newSdk = factory.newInstance(uniqueName, new Path(sdkLocation));
    assert (newSdk.validate().isOK());
    sdks.add(newSdk);
    sdkManager.setSdks(sdks);
  }
}
