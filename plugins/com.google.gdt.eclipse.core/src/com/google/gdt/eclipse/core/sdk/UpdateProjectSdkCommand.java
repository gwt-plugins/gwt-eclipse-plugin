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

import com.google.gdt.eclipse.core.ClasspathUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command for updating a project's SDK by removing any old SDK entries and
 * adding a new classpath container for the new SDK and optionally updating
 * WEB-INF folder.
 *
 * @param <T> type of Sdk
 */
public abstract class UpdateProjectSdkCommand<T extends Sdk> {
  /**
   * A classpath update type.  These constants control how an Sdk will
   * be added to the classpath.
   */
  public enum UpdateType {
    DEFAULT_CONTAINER, NAMED_CONTAINER, RAW,
  }

  /**
   * Returns the type of update to be performed on the classpath.
   */
  protected static <T extends Sdk> UpdateType computeUpdateType(T oldSdk,
      T newSdk, boolean isNewSdkDefault, SdkSet<T> sdkSet, String containerId) {
    UpdateType updateType = isNewSdkDefault ? UpdateType.DEFAULT_CONTAINER
        : UpdateType.NAMED_CONTAINER;
    if (oldSdk != null) {
      IClasspathEntry[] oldClasspathEntries = oldSdk.getClasspathEntries();
      /*
       * If the old SDK was not a container then we will use raw jars/projects.
       * The new SDK will never be a container - those only exist on a project's
       * classpath.
       *
       * TODO: We need a better way to model the sdk install vs. and sdk on the
       * project's classpath.  They are currently mixed.
       */
      if (oldClasspathEntries.length != 1
          || ClasspathUtilities.findClasspathEntryContainer(
              oldClasspathEntries, containerId) == null) {
        updateType = UpdateType.RAW;
      }
    }

    if (updateType != UpdateType.RAW && newSdk != null) {
      if (sdkSet.findSdk(newSdk.getName()) == null) {
        updateType = UpdateType.RAW;
      }
    }

    return updateType;
  }

  private final IJavaProject javaProject;
  private final T newSdk;
  private final T oldSdk;
  private final UpdateType updateType;
  private final UpdateWebInfFolderCommand updateWebInfCommand;

  public UpdateProjectSdkCommand(IJavaProject javaProject, T oldSdk, T newSdk,
      UpdateType updateType, UpdateWebInfFolderCommand updateWebInfCommand) {
    this.oldSdk = oldSdk;
    this.newSdk = newSdk;
    this.javaProject = javaProject;
    this.updateType = updateType;
    this.updateWebInfCommand = updateWebInfCommand;
  }

  public final void execute() throws CoreException, FileNotFoundException,
      BackingStoreException {
    List<IClasspathEntry> classpathEntriesToRemove = computeBuildClasspathEntriesToRemove();
    List<IClasspathEntry> buildClasspathEntries = new ArrayList<IClasspathEntry>(
        Arrays.asList(javaProject.getRawClasspath()));

    buildClasspathEntries.removeAll(classpathEntriesToRemove);

    if (updateWebInfCommand != null) {
      updateWebInfCommand.execute();
    }

    // Add the SDK container path
    buildClasspathEntries.addAll(computeBuildClasspathEntriesToAdd());

    // Update the build classpath
    ClasspathUtilities.setRawClasspath(javaProject, buildClasspathEntries);
  }

  protected List<IClasspathEntry> computeBuildClasspathEntriesToAdd() {
    if (newSdk != null) {
      if (updateType == UpdateType.RAW) {
        return Arrays.asList(newSdk.getClasspathEntries());
      } else {
        IPath containerPath = SdkClasspathContainer.computeContainerPath(
            getContainerId(), newSdk,
            updateType == UpdateType.DEFAULT_CONTAINER
                ? SdkClasspathContainer.Type.DEFAULT
                : SdkClasspathContainer.Type.NAMED);
        return Collections.singletonList(JavaCore.newContainerEntry(containerPath));
      }
    }
    return Collections.emptyList();
  }

  /**
   * Computes the set of {@link IClasspathEntry IClasspathEntries} that need to
   * be removed from the project's classpath. This may include classpath
   * containers or individual libraries based on how the SDK was previously
   * added.
   */
  protected List<IClasspathEntry> computeBuildClasspathEntriesToRemove()
      throws CoreException {
    if (oldSdk == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(oldSdk.getClasspathEntries());
  }

  protected abstract String getContainerId();

  protected IJavaProject getJavaProject() {
    return javaProject;
  }

  protected T getNewSdk() {
    return newSdk;
  }

  protected UpdateType getUpdateType() {
    return updateType;
  }
}
