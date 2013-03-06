/*******************************************************************************
 * Copyright 2012 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiDependencies;
import com.google.gdt.eclipse.managedapis.impl.ApiFileType;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;
import com.google.gdt.eclipse.managedapis.impl.EclipseJavaProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiImpl;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiResourceVisitor;
import com.google.gdt.eclipse.managedapis.impl.ProguardConfig;
import com.google.gdt.eclipse.managedapis.impl.ProguardState;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of utility classes that are useful for working with managed APIs.
 */
public class ManagedApiUtils {

  /**
   * Given a root folder, search for the descriptor.json file for a managed API.
   * Once it is found, deserialize the dependency information.
   * 
   * Returns null if the folder does not exist, descriptor.json cannot be found
   * under the file tree, or the dependency information cannot be deserialized
   * correctly.
   */
  public static ApiDependencies findAndReadDependencyFile(File folder)
      throws IllegalArgumentException, InvocationTargetException,
      CoreException, IOException {
    if (!folder.exists()) {
      return null;
    }
    ApiDependencies apiDependencies = null;
    for (File resource : folder.listFiles()) {
      if (resource.isDirectory()) {
        apiDependencies = findAndReadDependencyFile(resource);
        if (apiDependencies != null) {
          return apiDependencies;
        }
      } else if (resource.getName().equals(
          ManagedApiJsonClasses.DESCRIPTOR_FILENAME)) {
        return ManagedApiJsonClasses.GSON_CODEC.fromJson(
            ResourceUtils.readFileContents(resource), ApiDependencies.class);
      }
    }
    return null;
  }

  /**
   * Scans the a root folder of a managed API in a Managed API project. Returns
   * a {@see ManagedApiResourceVisitor}.
   * 
   * @param project The project
   * @param rootDir An IFolder representing the root directory for a managed API
   *          (should be something like <code>.google_apis/apiname..</code>).
   */
  public static ManagedApiResourceVisitor scanManagedApiFiles(
      final EclipseProject project, final IFolder rootDir) throws CoreException {
    ManagedApiResourceVisitor visitor = new ManagedApiResourceVisitor();
    visitor.setProject(project);
    visitor.setRootDir(rootDir);
    rootDir.accept(visitor);
    return visitor;
  }

  /**
   * Given a set of dependencies for an API and a platform type, return the
   * filenames for the dependencies that need to be added for the API. This
   * method only returns the filenames for those files that are of type
   * 'binary'; it does not return the filenames for files of any other type.
   * 
   * Returns null if apiDepenencies is null.
   * 
   * @param apiDependencies The dependencies of a managed API
   * @param platformType The platform type: {@see ApiPlatformType}.
   */
  public static List<String> computeBinaryDepsToAdd(
      ApiDependencies apiDependencies, ApiPlatformType platformType) {

    if (apiDependencies == null) {
      return null;
    }

    List<String> libJarsList = new ArrayList<String>();

    for (ApiDependencies.ApiDependency dependency : apiDependencies.getDependencies()) {

      if (!platformType.matches(dependency.getEnvironments())) {
        continue;
      }

      for (ApiDependencies.File file : dependency.getFiles()) {
        if (file.getPath() != null
            && ApiFileType.BINARY.matches((file.getType()))) {
          libJarsList.add(file.getPath().substring(
              file.getPath().lastIndexOf('/') + 1));
        }
      }
    }
    return libJarsList;
  }

  /**
   * Given a set of dependencies for an API and a platform type, return the
   * filenames for the dependencies that are not needed for the API. This method
   * will return filenames for all file types, not just binary.
   * 
   * Returns null if apiDepenencies is null.
   * 
   * @param apiDependencies The dependencies of a managed API
   * @param platformType The platform type: {@see ApiPlatformType}.
   */
  public static List<String> computeDependenciesToRemove(
      ApiDependencies apiDependencies, ApiPlatformType platformType)
      throws JavaModelException {

    if (apiDependencies == null) {
      return null;
    }

    List<String> dependencyToRemoveList = new ArrayList<String>();

    for (ApiDependencies.ApiDependency dependency : apiDependencies.getDependencies()) {

      if (platformType.matches(dependency.getEnvironments())) {
        continue;
      }

      /*
       * If the platform type does not match any of the specified environments
       * for the dependency, then we need to add this dependency to the removal
       * list.
       */

      for (ApiDependencies.File file : dependency.getFiles()) {
        if (file.getPath() != null) {
          dependencyToRemoveList.add(file.getPath().substring(
              file.getPath().lastIndexOf('/') + 1));
        }
      }

    }

    return dependencyToRemoveList;
  }

  /**
   * Generate the current state of Proguard information for an Android project.
   * 
   * The state is based on any Managed APIs that have been added to his project,
   * in addition to any other folders with managed API-like information.
   * 
   * [TODO: We're having to make a distinction between Managed APIs and those
   * generated for Cloud Endpoints here because of the divergent code paths. We
   * need to consolidate the Cloud Endpoints code so that that the addition of a
   * Cloud Endpoint library is treated just like that of a Managed API.]
   * 
   * @param androidProject The android project. If null, this method returns
   *          null.
   * @param otherApiFolders Any other root folders containing API information.
   *          If they do not exist, they will be ignored.
   * 
   * @return A {@link ProguardState} object representing the current Proguard
   *         state.
   * 
   * @throws CoreException
   * @throws IOException
   */
  public static ProguardState generateCurrentProguardState(
      IProject androidProject, IFolder... otherApiFolders)
      throws CoreException, IOException {

    if (androidProject == null) {
      return null;
    }

    if (ApiPlatformType.getAndroidPlatformType(androidProject) == null) {
      return null;
    }

    IJavaProject jp = JavaCore.create(androidProject);
    EclipseProject ep = new EclipseJavaProject(JavaCore.create(androidProject));

    // Check to see if there's existing Proguard information for the project
    // based on
    // managed API info
    ManagedApiProject managedApiProject = ManagedApiProjectImpl.getManagedApiProject(jp);
    ProguardState beforeState = null;
    if (managedApiProject.hasManagedApis()) {
      for (ManagedApi api : managedApiProject.getManagedApis()) {
        ProguardConfig config = new ProguardConfig(api, new EclipseJavaProject(
            managedApiProject.getJavaProject()));
        if (config.hasProguardConfig()) {
          beforeState = ProguardState.createFromCurrent(config, ep);
          break;
        }
      }
    }

    /*
     * There's no existing Proguard information based on existing Managed APIs.
     * There may be Proguard information from Cloud Endpoint libraries that have
     * already been generated
     */
    if (beforeState == null) {
      for (IFolder managedApiFolder : otherApiFolders) {

        if (!managedApiFolder.exists()) {
          continue;
        }

        ManagedApi managedApi = ManagedApiImpl.createManagedApi(ep,
            managedApiFolder);
        if (managedApi != null) {
          ProguardConfig config = new ProguardConfig(managedApi,
              new EclipseJavaProject(managedApiProject.getJavaProject()));
          if (config.hasProguardConfig()) {
            beforeState = ProguardState.createFromCurrent(config, ep);
            break;
          }
        }
      }
    }

    if (beforeState == null) {
      beforeState = ProguardState.createFromCurrent(null, ep);
    }

    return beforeState;
  }
}
