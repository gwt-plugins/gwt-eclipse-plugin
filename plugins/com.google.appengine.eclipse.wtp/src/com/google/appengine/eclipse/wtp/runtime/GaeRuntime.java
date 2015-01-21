/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.runtime;

import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IJavaRuntime;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

/**
 * Google App Engine server runtime support.
 */
public final class GaeRuntime extends RuntimeDelegate implements IJavaRuntime {
  /**
   * Server definition attribute id in the server attributes
   */
  public static final String SERVER_DEFINITION_ID = "server_definition_id";
  public static final String GAE_RUNTIME_ID = "com.google.appengine.runtime.id";
  /**
   * Server instance properties attribute id on server attributes
   */
  public static final String SERVER_INSTANCE_PROPERTIES = "gae_server_instance_properties";
  private static final String PROPERTY_VM_INSTALL_TYPE_ID = "vm-install-type-id";
  private static final String PROPERTY_VM_INSTALL_ID = "vm-install-id";
  private static final String PROPERTY_GAE_SDK_NAME = "gae-sdk-name";
  private static final String PROPERTY_GAE_SDK_VERSION = "gae-sdk-version";

  /**
   * @return the name of associated GAE SDK.
   */
  public String getGaeSdkName() {
    return getAttribute(PROPERTY_GAE_SDK_NAME, (String) null);
  }

  /**
   * @return the version of associated GAE SDK.
   */
  public String getGaeSdkVersion() {
    return getAttribute(PROPERTY_GAE_SDK_VERSION, (String) null);
  }


  @Override
  public IVMInstall getVMInstall() {
    if (getVMInstallTypeId() == null) {
      return JavaRuntime.getDefaultVMInstall();
    }
    try {
      IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(getVMInstallTypeId());
      IVMInstall[] vmInstalls = vmInstallType.getVMInstalls();
      int size = vmInstalls.length;
      String id = getVMInstallId();
      for (int i = 0; i < size; i++) {
        if (id.equals(vmInstalls[i].getId())) {
          return vmInstalls[i];
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return null;

  }

  /**
   * Returns VM id
   *
   * @return id
   */
  public String getVMInstallId() {
    return getAttribute(PROPERTY_VM_INSTALL_ID, (String) null);
  }

  /**
   * Returns the vm type id
   *
   * @return id
   */
  public String getVMInstallTypeId() {
    return getAttribute(PROPERTY_VM_INSTALL_TYPE_ID, (String) null);
  }

  /**
   * Is use default VM selected.
   *
   * @return boolean
   */
  @Override
  public boolean isUsingDefaultJRE() {
    return getVMInstallTypeId() == null;
  }

  /**
   * Set GaeSdk to be used.
   *
   * @param sdk
   */
  public void setGaeSdk(GaeSdk sdk) {
    setAttribute(PROPERTY_GAE_SDK_NAME, sdk.getName());
    setAttribute(PROPERTY_GAE_SDK_VERSION, sdk.getVersion());
  }

  /**
   * SetVM to be used.
   *
   * @param vmInstall
   */
  public void setVMInstall(IVMInstall vmInstall) {
    if (vmInstall == null) {
      setVMInstall(null, null);
    } else {
      setVMInstall(vmInstall.getVMInstallType().getId(), vmInstall.getId());
    }
  }

  @Override
  public IStatus validate() {
    String sdkName = getGaeSdkName();
    if (sdkName == null || sdkName.trim().isEmpty()) {
      return StatusUtilities.newErrorStatus(
          "No App Engine SDK associated with this runtime. Please edit the runtime or create new.",
          AppEnginePlugin.PLUGIN_ID);
    }
    GaeSdk sdk = RuntimeUtils.getRuntimeSdkNoFallback(this);
    if (sdk == null) {
      return StatusUtilities.newErrorStatus(
          "App Engine SDK is not found. Please define at least one and associate it with the runtime.",
          AppEnginePlugin.PLUGIN_ID);
    }
    IStatus sdkStatus = sdk.validate();
    if (!sdkStatus.isOK()) {
      return sdkStatus;
    }
    return super.validate();
  }

  private void setVMInstall(String typeId, String id) {
    if (typeId == null) {
      setAttribute(PROPERTY_VM_INSTALL_TYPE_ID, (String) null);
    } else {
      setAttribute(PROPERTY_VM_INSTALL_TYPE_ID, typeId);
    }

    if (id == null) {
      setAttribute(PROPERTY_VM_INSTALL_ID, (String) null);
    } else {
      setAttribute(PROPERTY_VM_INSTALL_ID, id);
    }
  }

}