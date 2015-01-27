/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloudsdk.eclipse.wtp.runtime;

import com.google.cloudsdk.eclipse.wtp.CloudSdkPlugin;
import com.google.cloudsdk.eclipse.wtp.GCloudCommandDelegate;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

import java.io.File;

/**
 * Cloud SDK Runtime delegate.
 */
public class CloudSdkRuntime extends RuntimeDelegate {
  @Override
  public IStatus validate() {
    IStatus status = super.validate();
    if (status != Status.OK_STATUS) {
      return status;
    }

    // Check if the sdk location is a valid path
    String sdkLocation = getRuntime().getLocation().toString();
    if (!(new File(sdkLocation)).exists()) {
      return new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID, 0,
          "Specified Cloud SDK directory does not exist", null);
    }

    // Check that the Cloud SDK and App Engine component are installed
    try {
      if (!GCloudCommandDelegate.areCloudSdkAndAppEngineInstalled(sdkLocation)) {
        return new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID, 0,
            "The Cloud SDK/App Engine component is not installed", null);
      }
    } catch (Exception e) {
      return new Status(IStatus.ERROR, CloudSdkPlugin.PLUGIN_ID, 0,
          "Failed to validate that the Cloud SDK/App Engine component is installed", e);
    }

    return Status.OK_STATUS;
  }
}