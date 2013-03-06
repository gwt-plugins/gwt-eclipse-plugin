/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 *  All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * Define the interface for ManagedApiProjects. This type is intended to wrap an
 * IJavaProject and provide additional managed API-specific capabilities.
 */
public interface ManagedApiProject {

  /**
   * Set the ManagedApiProject status to true.
   * 
   * @throws CoreException
   */
  void addManagedApiProjectState() throws CoreException;

  /**
   * clear all the observers for this project.
   */
  void clearManagedApiProjectObservers();

  /**
   * Create a managed api from a relative path. The path should be relative to
   * the ManagedApiRoot (e.g. .google_apis). Note: this method creates the API
   * but does not link it into the ManagedApiProject.
   * 
   * @param pathRelativeToManagedApiRoot
   * @return the referenced ManagedApi
   */
  ManagedApi createManagedApi(String pathRelativeToManagedApiRoot);

  /**
   * Find a managed API within the current project using a key. The key is a
   * string made up of the name-version of the API instance.
   */
  ManagedApi findManagedApi(String key);

  /**
   * Return the calculated copy-to target dir. Null if none. If the project
   * specifies the default, this method calculates that result and returns it.
   */
  IFolder getCopyToTargetDir() throws CoreException;

  /**
   * Return the default value for the copy-to directory if one exists --
   * otherwise, return null.
   */
  IFolder getDefaultCopyToTargetDir() throws CoreException;

  IJavaProject getJavaProject();

  IFolder getManagedApiRootFolder();

  ManagedApi[] getManagedApis();

  String getPathRelativeToManagedApiRoot(IPackageFragmentRoot fragmentRoot);

  IProject getProject();

  /**
   * Tester method.
   */
  boolean hasCopyToTargetDir() throws CoreException;

  boolean hasManagedApis();

  /**
   * Call this method to install APIs from folders in the root folder. The
   * implementation attempts to converts each folder into a ManagedApi which it
   * then adds to the project using an undoable operation.
   */
  void install(IFolder[] unregisteredManagedApiFolders,
      IProgressMonitor monitor, String operationText)
      throws CoreException, ExecutionException;

  boolean isPackageFragmentRootInManagedApi(IPackageFragmentRoot fragmentRoot);

  /**
   * Returns whether the project specifies use of a default value for the
   * copy-to directory.
   */
  boolean isUseDefaultCopyToTargetDir() throws CoreException;

  void notifyCopyToDirectoryChanged(IFolder originalFolder, IFolder newFolder);

  void notifyManagedApisAdded(ManagedApi[] api);

  void notifyManagedApisRefreshed(ManagedApi[] api);

  /**
   * Provides notification that an API has been removed. This notification is
   * broadcast to observers (see
   * {@link #registerManagedApiProjectObserver(ManagedApiProjectObserver)}).
   */
  void notifyManagedApisRemoved(ManagedApi[] api);

  /**
   * Provides notification that code outside the plugin has removed a
   * ManagedAPI. This method can handle the removal by registering the removal
   * in undo/redo history.
   */
  void notifyUninstalled(
      final ManagedApi[] apis, final IProgressMonitor monitor)
      throws ExecutionException;

  void registerManagedApiProjectObserver(ManagedApiProjectObserver observer);

  void removeManagedApiProjectState();

  /**
   * Set the copy-to target path for the ManagedApiProject. This path represents
   * a directory into which the plugin should copy ManagedApi libraries.
   * 
   * @throws CoreException
   */
  void setCopyToTargetDir(IFolder path) throws CoreException;

  /**
   * Specifies that the default copy-to dir should be used for the project.
   */
  void setDefaultCopyToTargetDir() throws CoreException;

  void setManagedApiRootFolder(IFolder managedApiRootFolder)
      throws CoreException;

  void updateApis(ApiDirectoryListing apiDirectoryListing);

}
