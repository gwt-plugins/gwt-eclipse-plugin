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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainerInitializer;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Resolves a {@link GWTRuntime} path to an Initializes an
 * {@link org.eclipse.jdt.core.IClasspathContainer IClasspathContainer}.
 * 
 * TODO: Move this and subtypes into the sdk package.
 */
public class GWTRuntimeContainerInitializer extends
    SdkClasspathContainerInitializer<GWTRuntime> {

  @Override
  public String getDescription(IPath containerPath, IJavaProject project) {
    return "GWT " + super.getDescription(containerPath, project);
  }

  @Override
  protected SdkClasspathContainer<GWTRuntime> createClasspathContainer(
      IPath containerPath, GWTRuntime sdk, String description,
      IJavaProject javaProject) {
    return new GWTRuntimeContainer(containerPath, sdk,
        sdk.getClasspathEntries(), description);
  }

  @Override
  protected String getContainerId() {
    return GWTRuntimeContainer.CONTAINER_ID;
  }

  @Override
  protected SdkManager<GWTRuntime> getSdkManager() {
    return GWTPreferences.getSdkManager();
  }

  @Override
  protected SdkClasspathContainer<GWTRuntime> updateClasspathContainer(
      IPath containerPath, GWTRuntime sdk, String description,
      IJavaProject project, IClasspathContainer containerSuggestion) {

    // TODO: Persist the changes to the container

    return new GWTRuntimeContainer(containerPath, sdk,
        containerSuggestion.getClasspathEntries(), description);
  }
}
