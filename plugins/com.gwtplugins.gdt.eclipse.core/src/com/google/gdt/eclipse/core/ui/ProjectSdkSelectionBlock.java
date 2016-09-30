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

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Sdk selection block whose default is relative to a project.
 * 
 * @param <T> type of {@link com.google.gdt.eclipse.core.sdk.Sdk}
 */
public abstract class ProjectSdkSelectionBlock<T extends Sdk> extends SdkSelectionBlock<T> {

  private final T initialProjectBoundSdk;
  private final IJavaProject javaProject;

  public ProjectSdkSelectionBlock(Composite parent, int style, IJavaProject javaProject) {
    super(parent, style);

    this.javaProject = javaProject;
    this.initialProjectBoundSdk = doFindSdkFor(javaProject);

    T sdk = findRegisteredSdk(initialProjectBoundSdk);
    
    // We couldn't find a registered sdk for the project-bound SDK. Just use the project-bound SDK. 
    if (sdk == null) {
      sdk = initialProjectBoundSdk;
    }

    updateSdkBlockControls();
    initializeSdkComboBox();

    if (isProjectUsingDefaultSdk(sdk)) {
      setSelection(-1);
    } else {
      List<T> specificSdks = doGetSpecificSdks();
      setSelection(specificSdks.indexOf(sdk));
    }
  }

  public T getInitialSdk() {
    return initialProjectBoundSdk;
  }

  public boolean hasSdkChanged() {
    SdkSelection<T> sdkSelection = getSdkSelection();
    
    if (sdkSelection.getSelectedSdk() == null) {
      return false;
    }
    
    T sdk = findRegisteredSdk(initialProjectBoundSdk);
    if (sdk == null) {
      sdk = initialProjectBoundSdk;
    }

    return sdkSelection.isDefault() != isProjectUsingDefaultSdk(sdk)
        || !sdkSelection.getSelectedSdk().equals(sdk);
  }

  protected abstract T doFindSdkFor(IJavaProject javaProject);

  protected abstract String doGetContainerId();

  /**
   * The sdk provider that accounts for an
   * {@link com.google.gdt.eclipse.core.sdk.Sdk} that may not be registered with
   * an {@link com.google.gdt.eclipse.core.sdk.SdkManager}.
   */
  protected T doGetDefaultSdk() {
    return doGetSdkManager().getSdks().getDefault();
  }

  protected abstract SdkManager<T> doGetSdkManager();

  protected List<T> doGetSpecificSdks() {
    List<T> sdks = new ArrayList<T>(doGetSdkManager().getSdks());
    T registeredSdk = findRegisteredSdk(initialProjectBoundSdk);
    if (registeredSdk == null && initialProjectBoundSdk != null) {
      /*
       * Add the initial sdk because it does not correspond to an sdk that we
       * know about.
       */
      sdks.add(initialProjectBoundSdk);
    }

    return sdks;
  }

  private T findRegisteredSdk(T projectBoundSdk) {
    
    // First, see if we're dealing with a classpath container
    try {
      IClasspathEntry entry = ClasspathUtilities.findClasspathEntryContainer(
          javaProject.getRawClasspath(), doGetContainerId());
      if (entry != null) {
        T containerSdk = doGetSdkManager().findSdkForPath(entry.getPath());
        if (containerSdk != null) {
          return containerSdk;
        }
      }
    } catch (CoreException ce) {
      CorePluginLog.logError(ce);
    }

    // If we're here, it means that we're not dealing with a project that has
    // a classpath container. Let's see if we can convert the project-bound
    // SDK into an actual registered SDK.
    if (projectBoundSdk != null) {
      return SdkUtils.findSdkForInstallationPath(doGetSdkManager().getSdks(),
          projectBoundSdk.getInstallationPath());
    }

    return null;
  }

  /**
   * Returns true if the given project SDK is actually the default SDK for the
   * workspace AND the project has a classpath container entry for the given SDK
   * that indicates that the workspace default should actually be used (i.e. no
   * trailing segments).
   * 
   * Also returns true if the project SDK is the default SDK for the workspace
   * and the project does not have a classpath container entry for the SDK.
   * 
   * @param projectSdk
   * @return
   */
  private boolean isProjectUsingDefaultSdk(T projectSdk) {
    if (!isDefaultSdk(projectSdk)) {
      return false;
    }

    try {
      IClasspathEntry entry = ClasspathUtilities.findClasspathEntryContainer(
          javaProject.getRawClasspath(), doGetContainerId());
      if (entry != null) {
        if (SdkClasspathContainer.isDefaultContainerPath(doGetContainerId(), entry.getPath())) {
          return true;
        }
      }
    } catch (CoreException ce) {
      CorePluginLog.logError(ce);
    }

    return false;
  }

  /**
   * Returns true if the given SDK matches the workspace default SDK.
   */
  private boolean isDefaultSdk(T sdk) {
    T defaultSdk = doGetDefaultSdk();
    if (defaultSdk == null) {
      return false;
    }
    return doGetDefaultSdk().equals(sdk);
  }
}
