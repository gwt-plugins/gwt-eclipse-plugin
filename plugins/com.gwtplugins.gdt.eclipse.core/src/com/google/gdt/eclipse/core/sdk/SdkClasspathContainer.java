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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Classpath container associated with an {@link Sdk}.
 *
 * @param <T> type of {@link Sdk} associated with this classpath container
 */
public class SdkClasspathContainer<T extends Sdk> implements
    IClasspathContainer {

  /**
   * Descriptor for SDK files that controls where they are copied to and whether
   * they appear on the build path or not.
   */
  public static class ContainerEntry {
    private final String nameRegExp;
    private final boolean copyToWebInfLib;
    private final boolean includedInBuildpath;

    public ContainerEntry(String nameRegExp, boolean copyToWebInfLib,
        boolean includedInBuildpath) {
      this.nameRegExp = nameRegExp;
      this.copyToWebInfLib = copyToWebInfLib;
      this.includedInBuildpath = includedInBuildpath;
    }

    public boolean copyToWebInfLib() {
      return copyToWebInfLib;
    }

    public String getNameRegExp() {
      return nameRegExp;
    }

    public boolean includedInBuildpath() {
      return includedInBuildpath;
    }

    @Override
    public String toString() {
      return nameRegExp
          + (copyToWebInfLib ? ", copy to WEB-INF/lib"
              : ", don't copy to WEB-INF/lib")
          + (includedInBuildpath ? ", included in build path"
              : ", not included in buildpath");
    }
  }

  /**
   * Type of container, default or qualified.
   */
  public enum Type {
    /**
     * Default containers resolve to the workspace.
     */
    DEFAULT,

    /**
     * Named containers resolved to the Sdk name specified in the first segment
     * of the container path.
     */
    NAMED
  }

  /**
   * Computes the container path for a given container ID, Sdk, and container
   * type.
   */
  public static IPath computeContainerPath(String containerId, Sdk sdk,
      Type containerType) {
    if (containerType == Type.DEFAULT) {
      return computeDefaultContainerPath(containerId);
    } else {
      return computeQualifiedContainerPath(containerId, sdk);
    }
  }

  /**
   * Computes a default container path for the given container ID.
   *
   * @param containerId container ID for the given Sdk
   * @return default container path for the given container ID
   */
  public static IPath computeDefaultContainerPath(String containerId) {
    return new Path(containerId);
  }

  /**
   * Computes a qualified container path for the given container ID and Sdk.
   *
   * @param containerId container ID for the given Sdk
   * @param sdk Sdk instance
   * @return qualified container path for the given container ID and Sdk
   */
  public static IPath computeQualifiedContainerPath(String containerId, Sdk sdk) {
    assert (containerId != null && containerId.length() > 0);
    assert (sdk != null);

    return new Path(containerId).append(sdk.getName());
  }

  /**
   * Returns <code>true</code> if the classpath entry is an
   * {@link IClasspathEntry#CPE_CONTAINER} and it has the specified container
   * ID.
   *
   * @param containerId
   * @param classpathEntry
   * @return whether the classpathEntry is a container and has the containerId
   */
  public static boolean isContainerClasspathEntry(String containerId,
      IClasspathEntry classpathEntry) {
    if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
      return false;
    }

    return isContainerPath(containerId, classpathEntry.getPath());
  }

  /**
   * Returns <code>true</code> if the path is a container path for the specified
   * container ID.
   *
   * @param containerId
   * @param path
   * @return whether the path is a container path for the containerId.
   */
  public static boolean isContainerPath(String containerId, IPath path) {
    assert (containerId != null);
    assert (containerId.length() > 0);
    assert (path != null);

    if (path.segmentCount() == 0) {
      return false;
    }

    return path.segment(0).equals(containerId);
  }

  /**
   * Returns <code>true</code> if the specified path is a default container path
   * which means that it has a single segment and that segment matches the
   * container ID.
   *
   * @param containerId container ID that should be associated with the default
   *          path
   * @param path the path to test
   */
  public static boolean isDefaultContainerPath(String containerId, IPath path) {
    // NOTE: Technically we should get a ClasspathContainerInitializer and ask
    // it for the comparison ID, but we have no project to pass to them.
    // If the path has just one segment, the SDK is default and if the path has another segment,
    // we check if it is the datanucleus version number.
    return isContainerPath(containerId, path)
        && (path.segmentCount() == 1 || path.segment(1).matches("v[0-9]{1,2}"));
  }

  private final IPath path;

  private final T sdk;

  private final String description;

  private final IClasspathEntry[] classpathEntries;

  public SdkClasspathContainer(IPath path, T sdk,
      IClasspathEntry[] classpathEntries, String description) {
    assert (!path.isEmpty());
    assert (sdk != null);
    assert (description != null);

    this.classpathEntries = classpathEntries;
    this.path = path;
    this.sdk = sdk;
    this.description = description;
  }

  /**
   * Returns the set of {@link IClasspathEntry IClasspathEntries} associated
   * with this container.
   */
  public IClasspathEntry[] getClasspathEntries() {
    return classpathEntries;
  }

  public String getDescription() {
    return description;
  }

  public int getKind() {
    return IClasspathContainer.K_APPLICATION;
  }

  public IPath getPath() {
    return path;
  }

  public T getSdk() {
    return sdk;
  }
}
