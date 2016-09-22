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
package com.google.gdt.eclipse.core.sdk;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Base classpath container initializer for {@link Sdk}s.
 * 
 * @param <T> the type of {@link Sdk} associated with this classpath container
 *          initializer
 */
public abstract class SdkClasspathContainerInitializer<T extends Sdk> extends
    ClasspathContainerInitializer {

  @Override
  public boolean canUpdateClasspathContainer(IPath containerPath,
      IJavaProject project) {
    return getContainerId().equals(getComparisonID(containerPath, project));
  }

  @Override
  public String getDescription(IPath containerPath, IJavaProject project) {
    assert (containerPath.segmentCount() > 0);
    StringBuilder sb = new StringBuilder();
    sb.append("SDK ");
    SdkManager<T> sdkManager = getSdkManager();
    T sdk = sdkManager.findSdkForPath(containerPath);
    sb.append("[");
    if (sdk != null) {
      if (sdk.validate().isOK()) {
        sb.append(sdk.getDescription());
      } else {
        sb.append("invalid");
      }
    } else {
      sb.append("missing");
    }
    sb.append("]");

    return sb.toString();
  }

  @Override
  public void initialize(IPath containerPath, final IJavaProject javaProject)
      throws CoreException {
    SdkClasspathContainer<T> sdkClasspathContainer = null;
    final T sdk = resolveSdkFromContainerPath(containerPath, javaProject);
    if (sdk != null) {
      String description = getDescription(containerPath, javaProject);
      sdkClasspathContainer = createClasspathContainer(containerPath, sdk,
          description, javaProject);
    }

    // Container will be set to null if it could not be resolved which will
    // result in a classpath error for the project.
    JavaCore.setClasspathContainer(containerPath,
        new IJavaProject[] {javaProject},
        new IClasspathContainer[] {sdkClasspathContainer}, null);
  }

  @Override
  public void requestClasspathContainerUpdate(IPath containerPath,
      IJavaProject project, IClasspathContainer containerSuggestion)
      throws CoreException {

    SdkClasspathContainer<T> sdkClasspathContainer = null;
    T sdk = resolveSdkFromContainerPath(containerPath, project);
    if (sdk != null) {
      String description = getDescription(containerPath, project);
      sdkClasspathContainer = updateClasspathContainer(containerPath, sdk,
          description, project, containerSuggestion);
    }

    // Container will be set to null if it could not be resolved which will
    // result in a classpath error for the project.
    JavaCore.setClasspathContainer(containerPath, new IJavaProject[] {project},
        new IClasspathContainer[] {sdkClasspathContainer}, null);
  }

  /**
   * Creates a classpath container for the project with the specified container
   * path and sdk.
   * 
   * @param containerPath
   * @param sdk
   * @param description
   * @param project
   * @return a classpath container for the project, container and sdk
   * 
   *         NOTE: We may want to use the RuntimeContainerEntryResolver to
   *         exclude additional items on the build path from the runtime path
   */
  protected abstract SdkClasspathContainer<T> createClasspathContainer(
      IPath containerPath, T sdk, String description, IJavaProject project);

  /**
   * Returns the container ID handled by this container initializer.
   * 
   * @return container ID handled by this container initializer
   */
  protected abstract String getContainerId();

  /**
   * Returns the {@link SdkManager} used by this classpath container
   * initializer.
   * 
   * @return the {@link SdkManager} used by this classpath container initializer
   */
  protected abstract SdkManager<T> getSdkManager();

  /**
   * Resolve the container path to an Sdk instance.
   * 
   * @param containerPath
   * @param project
   * @return the container path for the Sdk
   */
  protected T resolveSdkFromContainerPath(IPath containerPath,
      IJavaProject project) {
    if (containerPath.segmentCount() == 0) {
      // Invalid container path
      return null;
    }

    SdkManager<T> sdkManager = getSdkManager();
    assert (sdkManager != null);
    T sdk = sdkManager.findSdkForPath(containerPath);
    if (sdk != null && !sdk.validate().isOK()) {
      // We found the Sdk but it is no longer valid
      return null;
    }

    return sdk;
  }

  /**
   * Updates a classpath container for the project with the specified container
   * suggestion. The implementor is responsible for persisting the changes to
   * the container.
   * 
   * @param containerPath
   * @param sdk
   * @param description
   * @param project
   * @param containerSuggestion
   * @return a classpath container for the project, container and sdk
   */
  protected abstract SdkClasspathContainer<T> updateClasspathContainer(
      IPath containerPath, T sdk, String description, IJavaProject project,
      IClasspathContainer containerSuggestion);
}
