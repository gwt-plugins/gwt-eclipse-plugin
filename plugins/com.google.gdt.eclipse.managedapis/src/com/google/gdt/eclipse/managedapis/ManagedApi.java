/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.gdt.googleapi.core.ApiInfo;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A ManagedApi represents an API bundle installed within an Eclipse project.
 * The ManagedApi object provides access to the API information stored in a
 * bundle/package on disk and referenced by a container-type classpath entry.
 */
public interface ManagedApi extends ApiInfo {

  /**
   * Deletes the directory storing the API to which this object refers.
   * 
   * @param monitor
   */
  void delete(IProgressMonitor monitor);

  /**
   * Provides an ImageDescriptor for the ManagedApi if it is included in the
   * bundle; otherwise, return null.
   * 
   * @return the Image descriptor for the icon to replace the standard
   *         representation of the classpath container, or null.
   */
  ImageDescriptor getClasspathContainerIconImageDescriptor();

  /**
   * Provide access to the classpath entries for the ManagedApi.
   * 
   * @return an array of classpath entries for inclusion in the custom classpath
   *         container representation of this ManagedApi.
   * @throws CoreException Thrown if an error occurs in the traversal of the
   *           on-disk resources.
   */
  IClasspathEntry[] getClasspathEntries() throws CoreException;

  /**
   * Access to the root directory for the specific ManagedApi.
   */
  IFolder getRootDirectory();

  /**
   * Access whether a classpath container icon value is included in the
   * ManagedApi.
   */
  boolean hasClasspathContainerIcon();

  /**
   * Provides access to the state of the ManagedApi. A ManagedApi type
   * represents a bundle on disk within the current project. If the on-disk
   * representation is removed (@see delete(IProgressMonitor)), this method will
   * henceforth return true.
   */
  boolean isDeleted();

  /**
   * Flag represents whether the current ManagedApi has a known revision update.
   */
  boolean isRevisionUpdateAvailable();

  /**
   * Flag represents whether the current ManagedApi has a known update.
   */
  boolean isUpdateAvailable();

  /**
   * Set the revisionUpdateAvailable flag.
   */
  void setRevisionUpdateAvailable(boolean revisionUpdateAvailable);

  /**
   * Set the updateAvailable flag.
   */
  void setUpdateAvailable();

  /**
   * Unset the updateAvailable flag.
   */
  void unsetUpdateAvailable();
}