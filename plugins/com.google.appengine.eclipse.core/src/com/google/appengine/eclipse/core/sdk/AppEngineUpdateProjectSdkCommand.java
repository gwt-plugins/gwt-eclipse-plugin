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
import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand;
import com.google.gdt.eclipse.core.sdk.UpdateWebInfFolderCommand;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Command for updating a project to use an SDK container.
 */
public class AppEngineUpdateProjectSdkCommand extends
    UpdateProjectSdkCommand<GaeSdk> {

  public static <T extends Sdk> UpdateType computeUpdateType(GaeSdk oldSdk,
      GaeSdk newSdk, boolean isDefault) {
    return computeUpdateType(oldSdk, newSdk, isDefault,
        GaePreferences.getSdks(), GaeSdkContainer.CONTAINER_ID);
  }

  static IPath computeAppEngineSdkInstallPath(IJavaProject javaProject)
      throws CoreException {
    assert (ClasspathUtilities.findClasspathEntryContainer(
        javaProject.getRawClasspath(), GaeSdkContainer.CONTAINER_ID) == null);
    for (IClasspathEntry entry : javaProject.getRawClasspath()) {
      entry = JavaCore.getResolvedClasspathEntry(entry);
      IPath entryPath = entry.getPath();
      String fileName = entryPath.lastSegment();
      if (fileName.matches("appengine\\-tools\\-api.*\\.jar")) {
        if (entryPath.segmentCount() > 2) {
          // TODO: We should check that the jar exists on disk. Throw a
          // CoreException with a more informative message.
          return entryPath.removeLastSegments(2);
        }
      }
    }
    return null;
  }

  public AppEngineUpdateProjectSdkCommand(IJavaProject javaProject,
      GaeSdk oldSdk, GaeSdk newSdk, UpdateType updateType,
      UpdateWebInfFolderCommand updateWebInfFolderCommand) {
    super(javaProject, oldSdk, newSdk, updateType, updateWebInfFolderCommand);
  }

  @Override
  protected String getContainerId() {
    return GaeSdkContainer.CONTAINER_ID;
  }
}
