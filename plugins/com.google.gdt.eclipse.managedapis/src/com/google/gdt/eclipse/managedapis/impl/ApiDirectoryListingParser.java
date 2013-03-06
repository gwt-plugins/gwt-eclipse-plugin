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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.googleapi.core.ApiDirectoryListing;
import com.google.gdt.googleapi.core.ApiDirectoryListingJsonCodec;
import com.google.gson.JsonParseException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Reads a directory listing in raw format (JSON) and produces a listing of
 * objects.
 */
public class ApiDirectoryListingParser {
  private ApiDirectoryListingJsonCodec codec = new ApiDirectoryListingJsonCodec();
  private ApiDirectoryListing listing;
  private String serializedListing;

  /**
   * Provides access to the listing following a call to run().
   */
  public ApiDirectoryListing getListing() {
    return listing;
  }

  /**
   * Parses the listing to make it available using the getListing() method.
   * Returns the status of the parse job.
   * 
   * @param monitor
   * @return OK_STATUS if successful
   */
  public IStatus run(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;
    try {
      listing = codec.toApiDirectoryListing(serializedListing, null);
    } catch (JsonParseException e) {
      jobStatus = new Status(Status.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Malformed Listing", e);
    }
    return jobStatus;
  }

  /**
   * Inject the serialized listing to parse.
   * 
   * @param serializedListing the raw listing.
   */
  public void setSerializedListing(String serializedListing) {
    this.serializedListing = serializedListing;
  }
}
