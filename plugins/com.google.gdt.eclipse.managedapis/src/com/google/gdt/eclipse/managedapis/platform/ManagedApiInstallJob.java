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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.jobs.DownloadRunnable;
import com.google.gdt.eclipse.core.jobs.UnzipToIFilesRunnable;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiDependencies;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiRevision;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;
import com.google.gdt.eclipse.managedapis.impl.EclipseJavaProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;

import org.apache.tools.ant.util.FileUtils;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Job that composes several sub-actions to install managed apis in the current
 * project. The job takes a ManagedApiEntry and a project and when executed,
 * downloads the targeted entries, extracts them, creates a classpath container
 * as necessary and refreshes the project to make the changes visible.
 */
public class ManagedApiInstallJob extends Job {
  private static final int TICKS_CREATE_ROOT_FOLDER = 500;
  private static final int TICKS_DOWNLOAD_API_BUNDLE = 10000;
  private static final int TICKS_DELETE_EXISTING_API_FOLDER = 500;
  private static final int TICKS_CREATE_NEW_API_FOLDER = 500;
  private static final int TICKS_EXTRACT_ZIP = 2500;
  private static final int TICKS_PER_API = TICKS_DOWNLOAD_API_BUNDLE
      + TICKS_DELETE_EXISTING_API_FOLDER + TICKS_CREATE_NEW_API_FOLDER
      + TICKS_EXTRACT_ZIP;
  private static final int TICKS_REGISTER_APIS = 1000;
  private static final String DEPENDENCIES_PARAM = "&dependencies=1";

  /**
   * Utility method creates a target download File based on standardized naming
   * conventions.
   * 
   * @throws IOException if the file create fails.
   */
  private static File getDestinationFile() throws IOException {
    final File tmpFile = File.createTempFile("eclipse-gpe-managed-apis-",
        ".zip");
    tmpFile.deleteOnExit();
    return tmpFile;
  }

  private Collection<ManagedApiEntry> entries;
  private IProject project;
  private String messageFmt;

  /**
   * Define the installation job.
   * 
   * @param name the job name
   * @param entries the APIs to import
   * @param project the project on which to import the specified APIs.
   */
  public ManagedApiInstallJob(String name, Collection<ManagedApiEntry> entries,
      IProject project, String messageFmt) {
    super(name);
    assert project != null;
    assert entries != null;
    this.messageFmt = messageFmt;

    this.entries = entries;
    this.project = project;
  }

  /**
   * The execution method on the Job.
   */
  @Override
  public IStatus run(IProgressMonitor monitor) {

    IStatus jobStatus = Status.OK_STATUS;

    // calculate total ticks
    int totalTicks = TICKS_CREATE_ROOT_FOLDER
        + (entries.size() * TICKS_PER_API) + TICKS_REGISTER_APIS;
    SubMonitor submon = SubMonitor.convert(monitor, "Install Google APIs",
        totalTicks);

    List<IFolder> unregisteredApiFolders = new ArrayList<IFolder>();

    if (project == null) {
      return new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Not able to install APIs in null project");
    }

    try {
      ManagedApiProject managedApiProject = ManagedApiProjectImpl.getManagedApiProject(JavaCore.create(project));

      // create root folder (.google-apis) in project root.
      IFolder managedApiRoot = managedApiProject.getManagedApiRootFolder();
      if (managedApiRoot != null) {
        ResourceUtils.createFolderIfNonExistent(managedApiRoot,
            submon.newChild(TICKS_CREATE_ROOT_FOLDER));

        int entryCount = entries != null ? entries.size() : 0;
        if (entryCount > 0) {
          List<IStatus> subtaskStati = new ArrayList<IStatus>(entryCount);
          for (ManagedApiEntry entry : entries) {
            monitor.setTaskName(MessageFormat.format(messageFmt,
                entry.getDisplayName()));
            IStatus entryStatus;
            try {
              // Download ZIP file
              URL downloadLink = entry.getDirectoryEntry().getDownloadLink();
              if (!downloadLink.toString().contains(
                  ManagedApiProjectImpl.GDATA_FOLDER_NAME)) {
                downloadLink = new URL(downloadLink.toString().concat(
                    DEPENDENCIES_PARAM));
              }
              final File tmpFile = getDestinationFile();
              DownloadRunnable download = new DownloadRunnable(downloadLink,
                  tmpFile);
              entryStatus = download.run(submon.newChild(TICKS_DOWNLOAD_API_BUNDLE));
              if (entryStatus != Status.OK_STATUS) {
                if (entryStatus == Status.CANCEL_STATUS) {
                  jobStatus = Status.CANCEL_STATUS;
                  break;
                } else {
                  subtaskStati.add(entryStatus);
                  continue;
                }
              }

              // Create target folder
              String directoryName = entry.getName() + "-"
                  + entry.getDirectoryEntryVersion();
              IFolder targetFolder = managedApiRoot.getFolder(directoryName);
              ResourceUtils.deleteFileRecursively(new File(
                  targetFolder.getRawLocation().toString()));
              targetFolder.refreshLocal(IResource.DEPTH_INFINITE,
                  submon.newChild(0));
              targetFolder.create(true, true,
                  submon.newChild(TICKS_CREATE_NEW_API_FOLDER));

              // Extract ZIP file
              UnzipToIFilesRunnable unzipRunner = new UnzipToIFilesRunnable(
                  tmpFile, targetFolder);
              entryStatus = unzipRunner.run(submon.newChild(TICKS_EXTRACT_ZIP));
              if (entryStatus != Status.OK_STATUS) {
                if (entryStatus == Status.CANCEL_STATUS) {
                  jobStatus = Status.CANCEL_STATUS;
                  break;
                } else {
                  subtaskStati.add(entryStatus);
                  continue;
                }
              }

              // Modifying taget folder (and so classpath container name to
              // include revision and language version in addition to name and
              // version. This will make sure container initializer gets
              // triggered on addition of library with new revision / language
              // version.
              IFile localDescriptorFile = ManagedApiUtils.scanManagedApiFiles(
                  new EclipseJavaProject(JavaCore.create(project)),
                  targetFolder).getDescriptor();
              if (localDescriptorFile != null) {
                String localDescriptorContent = FileUtils.readFully(new FileReader(
                    localDescriptorFile.getLocation().toFile()));
                ApiRevision localRevision = ManagedApiJsonClasses.GSON_CODEC.fromJson(
                    localDescriptorContent,
                    ApiRevision.class);
                if (localRevision.getRevision() != null
                    && localRevision.getLanguage_version() != null) {
                  directoryName += "r" + localRevision.getRevision() + "lv"
                      + localRevision.getLanguage_version();
                  IFolder targetFolder2 = managedApiRoot.getFolder(directoryName);
                  ResourceUtils.deleteFileRecursively(new File(
                      targetFolder2.getRawLocation().toString()));
                  targetFolder2.refreshLocal(IResource.DEPTH_INFINITE,
                      submon.newChild(0));
                  targetFolder.copy(targetFolder2.getFullPath(), true,
                      new NullProgressMonitor());
                  ResourceUtils.deleteFileRecursively(new File(
                      targetFolder.getRawLocation().toString()));
                  targetFolder = targetFolder2;

                  // Remove unwanted dependencies.
                  ApiDependencies apiDependencies = ManagedApiJsonClasses.GSON_CODEC.fromJson(
                      localDescriptorContent, ApiDependencies.class);
                  ApiPlatformType platformType = ApiPlatformType.getAndroidPlatformType(project);
                  
                  if (platformType == null) {
                    /*
                     * It has to be App Engine, since we only allow the addition of Managed
                     * APIs to App Engine or Android projects.
                     */
                    platformType = ApiPlatformType.APPENGINE;
                  }
                  
                  ResourceUtils.deleteFiles(targetFolder,
                      ManagedApiUtils.computeDependenciesToRemove(apiDependencies, platformType));
                }
              }
              unregisteredApiFolders.add(targetFolder);
              tmpFile.delete();
            } catch (InvocationTargetException e) {
              entryStatus = new Status(IStatus.ERROR,
                  ManagedApiPlugin.PLUGIN_ID,
                  "Exception caught while adding API", e);
              subtaskStati.add(entryStatus);
            } catch (IllegalArgumentException e) {
              entryStatus = new Status(IStatus.ERROR,
                  ManagedApiPlugin.PLUGIN_ID,
                  "Exception caught during API extraction", e);
              subtaskStati.add(entryStatus);
            } catch (IOException e) {
              entryStatus = new Status(IStatus.ERROR,
                  ManagedApiPlugin.PLUGIN_ID,
                  "Exception caught during API download", e);
              subtaskStati.add(entryStatus);
            }
          }

          if (jobStatus != Status.CANCEL_STATUS) {
            try {
              managedApiProject.install(
                  unregisteredApiFolders.toArray(new IFolder[unregisteredApiFolders.size()]),
                  submon.newChild(TICKS_REGISTER_APIS), getName());
            } catch (ExecutionException e) {
              subtaskStati.add(new Status(IStatus.ERROR,
                  ManagedApiPlugin.PLUGIN_ID,
                  "Failure while installing managed APIs", e));
            }
          }

          if (jobStatus != Status.CANCEL_STATUS && !subtaskStati.isEmpty()) {
            jobStatus = new MultiStatus(ManagedApiPlugin.PLUGIN_ID,
                IStatus.WARNING,
                subtaskStati.toArray(new IStatus[subtaskStati.size()]),
                "Adding Google API failed. Refer to details.", null);
          }
        }
      } else {
        jobStatus = new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
            "Unexpected failure: ManagedAPI Root folder could not be identified");
      }
    } catch (CoreException e) {
      jobStatus = new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Unexpected failure", e);
    }

    final IStatus jobStatusPtr = jobStatus;
    if (!jobStatus.isOK()) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          ManagedApiPlugin.getDefault().getLog().log(jobStatusPtr);
          MessageDialog.openError(SWTUtilities.getShell(),
              "Google Plugin for Eclipse",
              "There was a problem downloading the API bundles. "
                  + "See the Error Log for more details.");
        }
      });
    }
    return Status.OK_STATUS;
  }
}
