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

import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectoryFactory;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListing;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiListingSource;
import com.google.gdt.eclipse.managedapis.directory.StructuredApiCollection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A factory used to produce ManagedApiListingSources, and by extension the
 * listings themselves.
 */
public class ManagedApiListingSourceFactory {
  private ApiDirectoryFactory apiDirectoryFactory;
  private ManagedApiProject project = null;

  public ManagedApiListingSource buildManagedApiListingSource() {
    final ApiDirectory apiDirectory = apiDirectoryFactory.buildApiDirectory();
    return new ManagedApiListingSource() {

      private ManagedApiListing listing;

      public ManagedApiListing getManagedApiListing() {
        return listing;
      }

      public IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading APIs", 100);
        IStatus jobStatus = checkConfiguration()
            ? Status.OK_STATUS
            : new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
                "Unable to run ManagedApiListingSource without proper configuration");

        StructuredApiCollection structuredApiListing = new LatestVersionOnlyStructuredApiCollection();

        monitor.setTaskName("Read installed APIs");
        ManagedApi[] installedApis;
        installedApis = project.getManagedApis();
        structuredApiListing.addAll(installedApis);
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        monitor.worked(5);

        SubMonitor loadApiDirectoryMon = SubMonitor.convert(monitor,
            "Load API Directory", 90);
        jobStatus = apiDirectory.run(loadApiDirectoryMon);
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        if (!Status.OK_STATUS.equals(jobStatus)) {
          return jobStatus;
        }

        monitor.setTaskName("Merge API listing data");
        structuredApiListing.addAll(apiDirectory.getApiDirectoryListing().getItems());
        monitor.worked(5);
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }

        listing = structuredApiListing.getListing();
        monitor.done();

        return jobStatus;
      }

      private boolean checkConfiguration() {
        boolean configured = true;
        configured &= null != project;
        configured &= null != apiDirectory;
        return configured;
      }

    };
  }

  /**
   * Provides a general mechanism for setting the ApiDirectoryFactory.
   */
  public void setApiDirectoryFactory(ApiDirectoryFactory apiDirectoryFactory) {
    this.apiDirectoryFactory = apiDirectoryFactory;
  }

  /**
   * Sets the ApiDirectoryFactory to use a URL.
   */
  public void setDirectoryLink(final String directoryLink) {
    apiDirectoryFactory = new ApiDirectoryFactory() {
      public ApiDirectory buildApiDirectory() {
        RemoteApiDirectory directory = new RemoteApiDirectory();
        directory.setDirectoryLink(directoryLink);
        return directory;
      }
    };
  }

  /**
   * Sets the ApiDirectoryFactory to use a local file.
   */
  public void setDirectorySourceFile(final String sourceFile) {
    apiDirectoryFactory = new ApiDirectoryFactory() {
      public ApiDirectory buildApiDirectory() {
        LocalApiDirectory directory = new LocalApiDirectory();
        directory.setSourceFile(sourceFile);
        return directory;
      }
    };
  }

  /**
   * Provides access to the current project (as a ManagedApiProject)
   */
  public void setProject(ManagedApiProject project) {
    this.project = project;
  }
}
