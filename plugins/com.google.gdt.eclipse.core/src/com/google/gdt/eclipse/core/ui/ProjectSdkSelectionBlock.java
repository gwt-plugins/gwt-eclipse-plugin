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
package com.google.gdt.eclipse.core.ui;

import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Sdk selection block whose default is relative to a project.
 * 
 * @param <T> type of {@link com.google.gdt.eclipse.core.sdk.Sdk}
 */
public abstract class ProjectSdkSelectionBlock<T extends Sdk> extends
    SdkSelectionBlock<T> {

  private final T initialSdk;

  public ProjectSdkSelectionBlock(Composite parent, int style,
      IJavaProject javaProject) {
    super(parent, style);

    this.initialSdk = doFindSdkFor(javaProject);

    T sdk = findRegisteredSdk(initialSdk);
    if (sdk == null) {
      sdk = initialSdk;
    }

    updateSdkBlockControls();
    initializeSdkComboBox();

    setSdkSelectionBlockSelection(sdk);
  }

  public T getInitialSdk() {
    return initialSdk;
  }

  public boolean hasSdkChanged() {
    SdkSelection<T> sdkSelection = getSdkSelection();
    T sdk = findRegisteredSdk(initialSdk);
    if (sdk == null) {
      sdk = initialSdk;
    }

    return sdkSelection.isDefault() != isDefaultSdk(sdk)
        || sdkSelection.getSelectedSdk() != sdk;
  }

  protected abstract T doFindSdkFor(IJavaProject javaProject);

  protected abstract String doGetContainerId();

  /**
   * The sdk provider that accounts for an 
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} that may not be registered with an 
   * {@link com.google.gdt.eclipse.core.sdk.SdkManager}.
   */
  protected T doGetDefaultSdk() {
    return doGetSdkManager().getSdks().getDefault();
  }

  protected abstract SdkManager<T> doGetSdkManager();

  protected List<T> doGetSpecificSdks() {
    List<T> sdks = new ArrayList<T>(doGetSdkManager().getSdks());
    T registeredSdk = findRegisteredSdk(initialSdk);
    if (registeredSdk == null && initialSdk != null) {
      /*
       * Add the initial sdk because it does not correspond to an sdk that we
       * know about.
       */
      sdks.add(initialSdk);
    }

    return sdks;
  }

  private T findRegisteredSdk(T sdk) {
    if (sdk != null) {
      // FIXME: If the Sdk is a classpath container then we should
      // look it up and return null if we don't find it. Otherwise, we should
      // look it up by installation path and return null if it does not match
      // the classpath entries of the registered sdk for that location.
      return SdkUtils.findSdkForInstallationPath(doGetSdkManager().getSdks(),
          sdk.getInstallationPath());
    }

    return null;
  }

  private boolean isDefaultSdk(T sdk) {
    return doGetDefaultSdk() == sdk;
  }
  
  protected void setSdkSelectionBlockSelection(T sdk) {
    if (isDefaultSdk(sdk)) {
      setSelection(-1);
    } else {
      List<T> specificSdks = doGetSpecificSdks();
      setSelection(specificSdks.indexOf(sdk));
    }
  }
}

