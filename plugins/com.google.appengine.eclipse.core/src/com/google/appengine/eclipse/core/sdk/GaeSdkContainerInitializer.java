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
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainerInitializer;
import com.google.gdt.eclipse.core.sdk.SdkManager;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Initializes or updates an AppEngine library.
 */
public class GaeSdkContainerInitializer extends
    SdkClasspathContainerInitializer<GaeSdk> {

  public static ClasspathContainerInitializer getInitializer() {
    return JavaCore.getClasspathContainerInitializer(GaeSdkContainer.CONTAINER_ID);
  }

  @Override
  public String getDescription(IPath containerPath, IJavaProject project) {
    return "App Engine " + super.getDescription(containerPath, project);
  }

  @Override
  protected SdkClasspathContainer<GaeSdk> createClasspathContainer(
      IPath containerPath, GaeSdk sdk, String description, IJavaProject javaProject) {
    return new GaeSdkContainer(
        containerPath, sdk, sdk.getClasspathEntries(javaProject), description);
  }

  @Override
  protected String getContainerId() {
    return GaeSdkContainer.CONTAINER_ID;
  }

  @Override
  protected SdkManager<GaeSdk> getSdkManager() {
    return GaePreferences.getSdkManager();
  }

  @Override
  protected SdkClasspathContainer<GaeSdk> updateClasspathContainer(
      IPath containerPath, GaeSdk sdk, String description,
      IJavaProject project, IClasspathContainer containerSuggestion) {

    // TODO: Persist the changes to the container

    return new GaeSdkContainer(containerPath, sdk,
        containerSuggestion.getClasspathEntries(), description);
  }
}
