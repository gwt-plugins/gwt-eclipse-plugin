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
import com.google.gdt.eclipse.core.sdk.SdkSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

/**
 * Testing-related utility methods dealing with GAE SDKs.
 * 
 * This lives in gae.core so gdt.suite.test can use it.
 */
public class GaeSdkTestUtilities {

  public static void addDefaultSdk() throws CoreException {
    String gaeHome = System.getenv("GAE_HOME");
    if (gaeHome == null) {
      throw new RuntimeException("The GAE_HOME environment variable is not set");
    }

    SdkSet<GaeSdk> sdkSet = GaePreferences.getSdks();
    if (sdkSet.getDefault() == null) {
      assert (sdkSet.size() == 0);

      GaeSdk sdk = GaeSdk.getFactory().newInstance("Default GAE SDK",
          new Path(gaeHome));
      IStatus status = sdk.validate();
      if (!status.isOK()) {
        throw new CoreException(status);
      }

      sdkSet.add(sdk);
      GaePreferences.setSdks(sdkSet);
    }
  }
}
