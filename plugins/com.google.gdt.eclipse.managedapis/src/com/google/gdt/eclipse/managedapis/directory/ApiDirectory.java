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
package com.google.gdt.eclipse.managedapis.directory;

import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Defines the methods necessary for an API directory. The run method loads the
 * directory, making the listing available. Check the return status on the
 * directory prior to using the object for a listing.
 */
public interface ApiDirectory {

  static final IStatus STATUS_UNINITIALIZED = new Status(IStatus.INFO,
      ManagedApiPlugin.PLUGIN_ID, "NOT INITIALIZED");

  static final IStatus STATUS_DIRTY = new Status(IStatus.INFO,
      ManagedApiPlugin.PLUGIN_ID, "CACHE EXPIRED");

  /**
   * Provides access to the listing.
   * 
   * @return a listing
   */
  ApiDirectoryListing getApiDirectoryListing();

  /**
   * Get the status of the API directory.
   * 
   * @return values should be one of: @TODO: add potential values
   */
  IStatus getStatus();

  /**
   * Initializes, and loads the ApiDirectory to make access to listings
   * possible.
   * 
   * @param monitor
   * @return OK_STATUS if successful
   */
  IStatus run(IProgressMonitor monitor);

}
