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
package com.google.gdt.eclipse.managedapis.platform;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.SWTUtilities;
import com.google.gdt.eclipse.core.jobs.DownloadRunnable;
import com.google.gdt.eclipse.core.jobs.UnzipToIFilesRunnable;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiDependencies;
import com.google.gdt.eclipse.managedapis.ManagedApiJsonClasses.ApiRevision;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.ManagedApiUtils;
import com.google.gdt.eclipse.managedapis.directory.ManagedApiEntry;
import com.google.gdt.eclipse.managedapis.impl.ApiPlatformType;
import com.google.gdt.eclipse.managedapis.impl.EclipseJavaProject;
import com.google.gdt.eclipse.managedapis.impl.IconInfo;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiMavenInfo;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;
import com.google.gdt.eclipse.managedapis.impl.ProguardConfig.Info;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.project.MavenProject;
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.widgets.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Job that installs managed apis in the current project.
 * 
 * If the current project is a Maven project, the pom.xml is updated based on the API information.
 * 
 * When it is a non-Maven project, the job composes several sub-actions to install managed apis.
 * When executed, the job downloads the targeted entries, extracts them, creates a classpath
 * container as necessary and refreshes the project to make the changes visible.
 */
public class ManagedApiInstallJob extends Job {

  public static final String LANGUAGE_VERSION_PARAM = "&lv="
      + ManagedApiPlugin.API_CLIENT_LANG_VERSION;
  public static final String DESCRIPTOR_ONLY_PARAM = "&descriptor-only=1";
  public static final String MAVEN_DEP_GROUP_ID = "com.google.apis";
  public static final String API_LIB_INFO_URL =
      "https://developers.google.com/resources/api-libraries/info/";

  private static final int TICKS_CREATE_ROOT_FOLDER = 500;
  private static final int TICKS_DOWNLOAD_API_BUNDLE = 10000;
  private static final int TICKS_DELETE_EXISTING_API_FOLDER = 500;
  private static final int TICKS_CREATE_NEW_API_FOLDER = 500;
  private static final int TICKS_EXTRACT_ZIP = 2500;
  private static final int TICKS_PER_API = TICKS_DOWNLOAD_API_BUNDLE
      + TICKS_DELETE_EXISTING_API_FOLDER + TICKS_CREATE_NEW_API_FOLDER + TICKS_EXTRACT_ZIP;
  private static final int TICKS_REGISTER_APIS = 1000;
  private static final String DEPENDENCIES_PARAM = "&dependencies=1";
  public static final String OLD_MAVEN2_NATURE_ID = "org.maven.ide.eclipse.maven2Nature";
  public static final String MAVEN2_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

  /**
   * Returns the first Dependency in the dependecyList that has the Artifact ID of nameToMatch.
   * Returns null if none exists.
   * 
   * @param dependecyList The list of Dependecy to search
   * @param nameToMatch The name of the Dependecy being search for
   * @return
   */
  private static Dependency findApiMatchingName(List<Dependency> dependecyList, String nameToMatch) {
    if (dependecyList == null) {
      return null;
    }

    for (Dependency dependency : dependecyList) {
      if (nameToMatch.equals(dependency.getArtifactId())) {
        return dependency;
      }
    }
    return null;
  }

  /**
   * Utility method creates a target download File based on standardized naming conventions.
   * 
   * @throws IOException if the file create fails.
   */
  private static File getDestinationFile() throws IOException {
    final File tmpFile = File.createTempFile("eclipse-gpe-managed-apis-", ".zip");
    tmpFile.deleteOnExit();
    return tmpFile;
  }

  /**
   * Returns <code>true</code> if the given project has the Maven 2 nature. This checks for the old
   * maven nature (till m2Eclipse 0.12) and the new Maven nature (m2Eclipse 1.0.0 and up).
   * 
   * @throws CoreException
   */
  private static boolean hasMavenNature(IProject project) throws CoreException {
    if (NatureUtils.hasNature(project, MAVEN2_NATURE_ID)) {
      return true;
    }
    if (NatureUtils.hasNature(project, OLD_MAVEN2_NATURE_ID)) {
      return true;
    }
    return false;
  }

  private List<IStatus> subtaskStati;

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
  public ManagedApiInstallJob(String name, Collection<ManagedApiEntry> entries, IProject project,
      String messageFmt) {
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
    if (project == null) {
      return new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Not able to install APIs in null project");
    }

    if ((entries == null) || (entries.size() == 0)) {
      return new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "No APIs were selected to add to project.");
    }

    IStatus jobStatus = Status.OK_STATUS;
    try {
      jobStatus =
          hasMavenNature(project) ? addApiToMavenProject(monitor)
              : addApiToNonMavenProject(monitor);
    } catch (CoreException e) {
      jobStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Error determining if project has the maven nature.", e);
    }

    final IStatus jobStatusPtr = jobStatus;
    if (!jobStatus.isOK()) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          ManagedApiPlugin.getDefault().getLog().log(jobStatusPtr);
          MessageDialog.openError(SWTUtilities.getShell(), "Google Plugin for Eclipse",
              "There was a problem downloading the API bundles. "
                  + "See the Error Log for more details.");
        }
      });
    }
    return Status.OK_STATUS;
  }

  /**
   * Adds the selected APIs to a maven project by updating the pom.xml file.
   */
  private IStatus addApiToMavenProject(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;

    try {
      IFile pomIfile = project.getFile("pom.xml");
      if (!pomIfile.exists()) {
        return new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
            pomIfile.getFullPath().toString() + " does not exist.");
      }

      // Initialize MavenProject
      IMavenProjectFacade mavenProjectFacade =
          MavenPlugin.getMavenProjectRegistry().getProject(project);
      MavenProject mavenProject = mavenProjectFacade.getMavenProject(new NullProgressMonitor());
      List<Dependency> allDependencies = mavenProject.getModel().getDependencies();
  
      if (subtaskStati == null) {
        subtaskStati = new ArrayList<IStatus>();
      }

      // Update pom.xml with new dependencies
      for (ManagedApiEntry entry : entries) {
        if (entry == null) {
          continue;
        }

        IStatus entryStatus = updateMavenDependencyList(mavenProject, entry, allDependencies);
        if (!entryStatus.isOK()) {
          if (entryStatus.matches(Status.CANCEL)) {
            return entryStatus;
          } else {
            subtaskStati.add(entryStatus);
            continue;
          }
        }
      }

      DefaultModelWriter modelWriter = new DefaultModelWriter();
      modelWriter.write(pomIfile.getRawLocation().toFile(), null, mavenProject.getModel());

      // Refresh pom file
      pomIfile.refreshLocal(IResource.DEPTH_ZERO, monitor);

    } catch (OperationCanceledException e) {
      jobStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Refreshing pom.xml was cancelled. Please refresh manually right-clicking the file and"
              + "selecting Refresh.", e);
    } catch (IOException e) {
      jobStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Error updating pom.xml file with new APIs", e);
    } catch (CoreException e) {
      jobStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Error updating pom.xml file with new APIs", e);
    }

    return jobStatus;
  }

  /**
   * Add the selected APIs to a non-maven project, by downloading the API and adding it to the
   * project's classpath.
   */
  private IStatus addApiToNonMavenProject(IProgressMonitor monitor) {
    IStatus jobStatus = Status.OK_STATUS;
    
    // Calculate total ticks
    int totalTicks =
        TICKS_CREATE_ROOT_FOLDER + (entries.size() * TICKS_PER_API) + TICKS_REGISTER_APIS;
    SubMonitor submon = SubMonitor.convert(monitor, "Install Google APIs", totalTicks);

    List<IFolder> unregisteredApiFolders = new ArrayList<IFolder>();

    try {
      ManagedApiProject managedApiProject =
          ManagedApiProjectImpl.getManagedApiProject(JavaCore.create(project));

      // create root folder (.google-apis) in project root.
      IFolder managedApiRoot = managedApiProject.getManagedApiRootFolder();
      if (managedApiRoot != null) {
        ResourceUtils.createFolderIfNonExistent(managedApiRoot,
            submon.newChild(TICKS_CREATE_ROOT_FOLDER));

        int entryCount = entries != null ? entries.size() : 0;
        if (entryCount > 0) {
          subtaskStati = new ArrayList<IStatus>(entryCount);

          // Download and set up all the API files
          for (ManagedApiEntry entry : entries) {
            monitor.setTaskName(MessageFormat.format(messageFmt, entry.getDisplayName()));
            IStatus entryStatus =
                setUpApiFiles(entry, managedApiRoot, unregisteredApiFolders, submon);
            if (entryStatus.matches(Status.CANCEL)) {
              break;
            }
          }

          // Install the APIs
          if (!jobStatus.matches(Status.CANCEL)) {
            try {
              managedApiProject.install(
                  unregisteredApiFolders.toArray(new IFolder[unregisteredApiFolders.size()]),
                  submon.newChild(TICKS_REGISTER_APIS), getName());
            } catch (ExecutionException e) {
              subtaskStati.add(new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
                  "Failure while installing managed APIs", e));
            }
          }

          if (!jobStatus.matches(Status.CANCEL) && !subtaskStati.isEmpty()) {
            jobStatus =
                new MultiStatus(ManagedApiPlugin.PLUGIN_ID, IStatus.WARNING,
                    subtaskStati.toArray(new IStatus[subtaskStati.size()]),
                    "Adding Google API failed. Refer to details.", null);
          }
        }
      } else {
        jobStatus =
            new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
                "Unexpected failure: ManagedAPI Root folder could not be identified");
      }
    } catch (CoreException e) {
      jobStatus = new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID, "Unexpected failure", e);
    }
    return jobStatus;
  }

  /**
   * Deletes files that are not specified in the descriptor.json files for a managed API.
   * 
   * @param folder The folder files would be deleted from. Usually a folder in the ".google_apis"
   *          folder.
   * @param apiDependencies The dependencies for the managed API.
   * @param descriptorContent The contents of a descriptor.json file.
   */
  private void deleteUnlistedFiles(IFolder folder, ApiDependencies apiDependencies,
      String descriptorContent) {
    Set<String> dependencyFiles = ManagedApiUtils.computeDependecyFileNames(apiDependencies);
    if (dependencyFiles == null) {
      return;
    }

    Gson infoGson =
        new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    Info proguardInfo = infoGson.fromJson(descriptorContent, Info.class);
    String proguardConfig = proguardInfo.getProguardConfig();
    if (proguardConfig.contains("/")) {
      proguardConfig = proguardConfig.substring(proguardConfig.lastIndexOf('/') + 1);
    }

    Gson iconInfoGson =
        new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    IconInfo iconInfo = iconInfoGson.fromJson(descriptorContent, IconInfo.class);

    String x32Path = iconInfo.getIconFiles().getX32();
    if (x32Path.contains("/")) {
      x32Path = x32Path.substring(x32Path.lastIndexOf('/') + 1);
    }

    String x16Path = iconInfo.getIconFiles().getX16();
    if (x16Path.contains("/")) {
      x16Path = x16Path.substring(x16Path.lastIndexOf('/') + 1);
    }

    dependencyFiles.add(ManagedApiJsonClasses.DESCRIPTOR_FILENAME);
    dependencyFiles.add(proguardConfig);
    dependencyFiles.add(x32Path);
    dependencyFiles.add(x16Path);
    dependencyFiles.add("LICENSE.txt");
    dependencyFiles.add("classpath-include");
    dependencyFiles.add("readme.html");

    ResourceUtils.deleteUnlistedFiles(folder.getLocation().toFile(), dependencyFiles);
  }

  /**
   * Downloads the zip file for the specified managed API.
   * 
   * @throws IOException when it can't create the download URL or get the destination file.
   */
  private IStatus downloadApi(ManagedApiEntry entry, File tmpFile, SubMonitor submon)
      throws IOException {
    URL downloadLink = entry.getDirectoryEntry().getDownloadLink();
    if (!downloadLink.toString().contains(ManagedApiProjectImpl.GDATA_FOLDER_NAME)) {
      downloadLink =
          new URL(downloadLink.toString().concat(DEPENDENCIES_PARAM).concat(
              LANGUAGE_VERSION_PARAM));
    }
    DownloadRunnable download = new DownloadRunnable(downloadLink, tmpFile); 
    tmpFile.delete();
    return download.run(submon.newChild(TICKS_DOWNLOAD_API_BUNDLE));
  }

  /**
   * Returns the maven resource information from
   * "https://developers.google.com/resources/api-libraries/info".
   * 
   * @param apiName The name of the API
   * @param apiVersion The version of the API
   * @return ManagedApiMavenInfo for the API with name apiName and apiVersion version.
   * @throws IOException
   */
  private ManagedApiMavenInfo getManagedApiMavenInfo(String apiName, String apiVersion)
      throws IOException {
    String urlString = API_LIB_INFO_URL + apiName + "/" + apiVersion + "/java";
    URL apiInfoUrl = new URL(urlString);
    StringBuilder sb = new StringBuilder();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(apiInfoUrl.openStream()));
    String inputLine;
    while ((inputLine = bufferedReader.readLine()) != null) {
      sb.append(inputLine);
    }
    String apiInfo = sb.toString();
    bufferedReader.close();

    Gson infoGson = new GsonBuilder().create();
    ManagedApiMavenInfo mavenInfo = infoGson.fromJson(apiInfo, ManagedApiMavenInfo.class);

    return mavenInfo;
  }

  private IStatus removeUnwantedDependecies(IFolder targetFolder, String localDescriptorContent,
      IFile localDescriptorFile) throws CoreException {

    IStatus entryStatus = Status.OK_STATUS;
    try {
      // Remove unwanted dependencies.
      ApiDependencies apiDependencies =
          ManagedApiJsonClasses.GSON_CODEC.fromJson(localDescriptorContent, ApiDependencies.class);
      ApiPlatformType platformType = ApiPlatformType.getAndroidPlatformType(project);

      try {
        String parentFolderName =
            localDescriptorFile.getFullPath().removeLastSegments(1).lastSegment();
        IFolder parentFolder = targetFolder.getFolder(parentFolderName);

        if (parentFolder.exists()) {
          deleteUnlistedFiles(parentFolder, apiDependencies, localDescriptorContent);
        }
      } catch (IndexOutOfBoundsException e) {
        ManagedApiPlugin.getDefault().getLog().log(
            new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
                "Exception caught while searching descriptor.json's parent folder", e));
      }

      if (platformType == null) {
        /*
         * It has to be App Engine, since we only allow the addition of Managed APIs to App Engine
         * or Android projects.
         */
        platformType = ApiPlatformType.APPENGINE;
      }

      ResourceUtils.deleteFiles(targetFolder,
          ManagedApiUtils.computeDependenciesToRemove(apiDependencies, platformType));
    } catch (IllegalArgumentException e) {
      entryStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Exception caught during API extraction", e);
    }

    return entryStatus;
  }

  private IStatus setUpApiFiles(ManagedApiEntry entry, IFolder managedApiRoot,
      List<IFolder> unregisteredApiFolders, SubMonitor submon) throws CoreException {
    IStatus entryStatus;
    if (subtaskStati == null) {
      subtaskStati = new ArrayList<IStatus>();
    }

    try {
      // Download ZIP file
      final File tmpFile = getDestinationFile();
      entryStatus = downloadApi(entry, tmpFile, submon);
      if (entryStatus.matches(Status.CANCEL)) {
        return entryStatus;
      } else if (!entryStatus.isOK()) {
        subtaskStati.add(entryStatus);
        return Status.OK_STATUS;
      }

      // Create target folder
      String directoryName = entry.getName() + "-" + entry.getDirectoryEntryVersion();
      IFolder targetFolder = managedApiRoot.getFolder(directoryName);
      ResourceUtils.deleteFileRecursively(new File(targetFolder.getRawLocation().toString()));
      targetFolder.refreshLocal(IResource.DEPTH_INFINITE, submon.newChild(0));
      targetFolder.create(true, true, submon.newChild(TICKS_CREATE_NEW_API_FOLDER));

      // Extract ZIP file
      UnzipToIFilesRunnable unzipRunner = new UnzipToIFilesRunnable(tmpFile, targetFolder);
      entryStatus = unzipRunner.run(submon.newChild(TICKS_EXTRACT_ZIP));
      if (entryStatus.matches(Status.CANCEL)) {
        return entryStatus;
      } else if (!entryStatus.isOK()) {
        subtaskStati.add(entryStatus);
        return Status.OK_STATUS;
      }

      // Modifying target folder (and so classpath container name to
      // include revision and language version in addition to name and
      // version. This will make sure container initializer gets
      // triggered on addition of library with new revision / language
      // version.
      IFile localDescriptorFile =
          ManagedApiUtils.scanManagedApiFiles(
              new EclipseJavaProject(JavaCore.create(project)), targetFolder).getDescriptor();
      if (localDescriptorFile != null) {
        String localDescriptorContent =
            FileUtils.readFully(new FileReader(localDescriptorFile.getLocation().toFile()));
        ApiRevision localRevision =
            ManagedApiJsonClasses.GSON_CODEC.fromJson(localDescriptorContent,
                ApiRevision.class);

        if (localRevision.getRevision() != null && localRevision.getLanguage_version() != null) {
          directoryName +=
              "r" + localRevision.getRevision() + "lv" + localRevision.getLanguage_version();
          IFolder targetFolder2 = managedApiRoot.getFolder(directoryName);
          ResourceUtils.deleteFileRecursively(new File(targetFolder2.getRawLocation().toString()));
          targetFolder2.refreshLocal(IResource.DEPTH_INFINITE, submon.newChild(0));
          targetFolder.copy(targetFolder2.getFullPath(), true, new NullProgressMonitor());
          ResourceUtils.deleteFileRecursively(new File(targetFolder.getRawLocation().toString()));
          targetFolder = targetFolder2;
          entryStatus =
              removeUnwantedDependecies(targetFolder, localDescriptorContent, localDescriptorFile);
          if (entryStatus.isOK()) {
            subtaskStati.add(entryStatus);
          }
        }
      }
      unregisteredApiFolders.add(targetFolder);
    } catch (InvocationTargetException e) {
      entryStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Exception caught while adding API", e);
      subtaskStati.add(entryStatus);
    } catch (IllegalArgumentException e) {
      entryStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Exception caught during API extraction", e);
      subtaskStati.add(entryStatus);
    } catch (IOException e) {
      entryStatus =
          new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
              "Exception caught during API download", e);
      subtaskStati.add(entryStatus);
    }
    return Status.OK_STATUS;
  }
  
  /**
   * Updates the list of dependencies of the maven project with the newly added Managed APIs.
   */
  private IStatus updateMavenDependencyList(MavenProject mavenProject, ManagedApiEntry entry,
      List<Dependency> allDependencies) {
    try {

      ManagedApiMavenInfo mavenInfo =
          getManagedApiMavenInfo(entry.getName(), entry.getDirectoryEntryVersion());

      // Check if API of same or different version already exists
      String artifactId = mavenInfo.getMaven().getArtifactId();
      Dependency matchingDependency = findApiMatchingName(allDependencies, artifactId);
      if (matchingDependency != null) {
        mavenProject.getModel().removeDependency(matchingDependency);
      }

      Dependency dependency = new Dependency();
      dependency.setVersion(mavenInfo.getMaven().getVersion());
      dependency.setGroupId(MAVEN_DEP_GROUP_ID);
      dependency.setArtifactId(artifactId);
      mavenProject.getModel().addDependency(dependency);


    } catch (IOException e) {
      return new Status(IStatus.ERROR, ManagedApiPlugin.PLUGIN_ID,
          "Error occured while attempting to retrieve maven information for " + entry.getName()
              + " API.", e);
    }
    
    return Status.OK_STATUS;
  }
}
