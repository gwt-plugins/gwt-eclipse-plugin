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
package com.google.gdt.eclipse.managedapis.impl;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.jobs.DownloadRunnable;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiLogger;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
import com.google.gdt.eclipse.managedapis.directory.ApiDirectoryFactory;
import com.google.gdt.googleapi.core.ApiDirectoryListing;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * ManagedApiChecker tests periodically to see whether any open projects have
 * APIs that can be upgraded.
 */
public class ManagedApiChecker {

  private class ManagedApiCheckerJob extends Job {

    public ManagedApiCheckerJob(String name) {
      super(name);
    }

    private IconCache loadIconCache(IProgressMonitor monitor) {
      IconCache iconCache = null;
      File iconCacheZip;
      try {
        iconCacheZip = File.createTempFile("temp",
            Long.toString(System.nanoTime()), iconCacheParentDirectory);

        URL downloadLink = new URL(
            ManagedApiPlugin.getManagedApiIconBundleHref());

        (new DownloadRunnable(downloadLink, iconCacheZip)).run(monitor);

        File iconCacheRoot = File.createTempFile("temp",
            Long.toString(System.nanoTime()), iconCacheParentDirectory);
        iconCacheRoot.delete();
        iconCacheRoot.mkdir();

        iconCache = IconCache.createIconCache(iconCacheZip, iconCacheRoot);
      } catch (IOException e) {
        ManagedApiLogger.log(ManagedApiLogger.INFO, "Failed to load icon cache");
      }

      return iconCache;
    }

    private void loadManagedApis(IProgressMonitor monitor) {
      ApiDirectory apiDirectory = factory.buildApiDirectory();
      apiDirectory.run(monitor);
      ApiDirectoryListing listing = apiDirectory.getApiDirectoryListing();

      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      for (IProject project : workspaceRoot.getProjects()) {
        try {
          if (project.exists()
              && NatureUtils.hasNature(project, JavaCore.NATURE_ID)) {
            IJavaProject javaProject = JavaCore.create(project);
            ManagedApiProject apiProject = ManagedApiProjectImpl.getManagedApiProject(javaProject);
            apiProject.updateApis(listing);
          }
        } catch (CoreException e) {
          // Thrown if the project doesn't exist or is not open, OK to skip
        }
      }

      if (!monitor.isCanceled()) {
        checkerJob.schedule(checkFrequency);
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      loadManagedApis(monitor);
      return StatusUtilities.OK_STATUS;
    }
  }

  private File iconCacheParentDirectory;

  private ApiDirectoryFactory factory;

  private long checkFrequency;

  private final ManagedApiCheckerJob checkerJob;

  private ManagedApiPlugin plugin;

  public ManagedApiChecker(ApiDirectoryFactory factory,
      long checkFrequencyInMillis, String jobName, ManagedApiPlugin plugin) {
    super();
    this.factory = factory;
    this.checkFrequency = checkFrequencyInMillis;
    this.plugin = plugin;
    checkerJob = new ManagedApiCheckerJob(jobName);
    checkerJob.setSystem(true);
  }

  public void startChecking() {
    IPath pluginPath = plugin.getStateLocation();
    File pluginStateLocationDirectory = pluginPath.toFile();
    iconCacheParentDirectory = new File(pluginStateLocationDirectory,
        "icon_caches");
    iconCacheParentDirectory.mkdir();

    checkerJob.schedule();
  }

  public void stopChecking() {
    checkerJob.cancel();
    plugin.setIconCache(null);
    ResourceUtils.deleteFileRecursively(iconCacheParentDirectory);
  }
}
