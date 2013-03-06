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

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.gdt.eclipse.core.sdk.SdkFactory;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkSet;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import java.net.URL;

/**
 * Helper class that assists in the auto-registration of SDK bundles.
 */
public class GAESdkRegistrant {

  /**
   * Registers an App Engine SDK for use by GPE, ensures unique naming and
   * validity prior to registration.
   * 
   * @param sdkUrl - File URL pointing to the root of a AppEngine SDK
   *          installation
   * @throws CoreException
   */
  public static void registerSdk(URL sdkUrl) throws CoreException {
    SdkManager<GaeSdk> sdkManager = GaePreferences.getSdkManager();
    SdkSet<GaeSdk> sdks = sdkManager.getSdks();
    String uniqueName = SdkUtils.generateUniqueSdkNameFrom("App Engine", sdks);
    SdkFactory<GaeSdk> factory = GaeSdk.getFactory();
    String sdkLocation = sdkUrl.getPath();
    GaeSdk newSdk = factory.newInstance(uniqueName, new Path(sdkLocation));
    assert (newSdk.validate().isOK());
    sdks.add(newSdk);
    sdkManager.setSdks(sdks);
  }
}
