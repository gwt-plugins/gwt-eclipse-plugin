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
package com.google.gdt.eclipse.suite.wizards;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.resources.ProjectResources;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.osgi.service.prefs.BackingStoreException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Web application project creator.
 */
@SuppressWarnings("restriction")
public class WebAppTemplateProjectCreator implements IWebAppProjectCreator {
  /**
   * FilenameFilter that matches files that have a ".java" extension.
   */
  private static final FilenameFilter javaSourceFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".java");
    }
  };

  /**
   * Returns <code>true</code> if the location URI maps onto the workspace's location URI.
   */
  private static boolean isWorkspaceRootLocationURI(URI locationURI) {
    return ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(locationURI);
  }

  /**
   * Recursively find the .java files in the output directory and reformat them.
   *
   * @throws CoreException
   */
  private static void reformatJavaFiles(File outDir) throws CoreException {
    // If a default JRE has not yet been detected (e.g. brand new workspace),
    // the compiler source compliance may be set to 1.3 (the default value from
    // code). This is a problem because the generated files do not compile
    // against 1.3 (e.g. they use annotations), and thus the formatting will not
    // succeed. We work around this using a trick from the
    // CompliancePreferencePage: Ensure there is a default JRE which in
    // turn updates the compliance level.
    JavaRuntime.getDefaultVMInstall();

    List<File> javaFiles = ProjectResources.findFilesInDir(outDir, javaSourceFilter);

    for (File file : javaFiles) {
      ProjectResources.reformatJavaSource(file);
    }
  }

  private List<IPath> containerPaths = new ArrayList<>();
  private URI locationURI;
  private String packageBaseName;
  private String projectBaseName;
  private IJavaProject[] createdJavaProjects;
  private IProgressMonitor monitor;
  private Sdk gwtSdk;
  private ProjectTemplate projectTemplate;

  protected WebAppTemplateProjectCreator(ProjectTemplate projectTemplate) {
    this.projectTemplate = projectTemplate;
    // Initialize location URI to the workspace
    IPath workspaceLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    if (workspaceLoc != null) {
      locationURI = URIUtil.toURI(workspaceLoc);
    }
  }

  @Override
  public void addContainerPath(IPath containerPath) {
    containerPaths.add(containerPath);
  }

  @Override
  public void addNature(String natureId) {
  }

  /**
   * Creates the project per the current configuration. Note that the caller must have a workspace lock in order to
   * successfully execute this method.
   *
   * @throws BackingStoreException
   * @throws IOException
   */
  @Override
  public void create(IProgressMonitor monitor)
      throws CoreException, SdkException, ClassNotFoundException, BackingStoreException, IOException {
    this.monitor = monitor;

    List<String> projectNames = projectTemplate.getProjectNames(projectBaseName);
    IProject[] project = new IProject[projectNames.size()];
    createdJavaProjects = new IJavaProject[projectNames.size()];
    for(int i=0;i<projectNames.size();i++)
    {
      project[i] = createProject(monitor, projectNames.get(i));
      File dir = URIUtil.toPath(project[i].getLocationURI()).toFile();
      projectTemplate.copyProject(i, dir);
      replacePackageNames(project[i], dir);

      /*
       * Refresh contents; if this project was generated via GWT's WebAppCreator, then these files would have been created
       * directly on the file system Although, this refresh should have been done via the project.open() call, which is
       * part of the createProject call above.
       */
      project[i].refreshLocal(IResource.DEPTH_INFINITE, monitor);

      // Set all of the natures on the project
      NatureUtils.addNatures(project[i], projectTemplate.getNatureIds(i));

      // Create the java project
      createdJavaProjects[i] = JavaCore.create(project[i]);

      // Create a source folder and add it to the raw classpath
      IResource warFolder = project[i].findMember(WebAppUtilities.DEFAULT_WAR_DIR_NAME);
      boolean createWarFolders = (warFolder != null);
      createFolders(project[i], createWarFolders, monitor);
      if (createWarFolders) {
        // Set the WAR source/output directory to "/war"
        WebAppUtilities.setDefaultWarSettings(project[i]);

        // Set the default output directory
        WebAppUtilities.setOutputLocationToWebInfClasses(createdJavaProjects[i], monitor);
        /**
         * Copy files into the web-inf lib folder. This code assumes that it is running in a context that has a workspace
         * lock.
         */
        Sdk gwtSdk = getGWTSdk();
        setGwtSdk(gwtSdk);
        if (gwtSdk != null) {
          new GWTUpdateWebInfFolderCommand(createdJavaProjects[i], gwtSdk).execute();
        }
      }
      reformatJavaFiles(dir);

      // Allow other extensions to run after project creation
      includeExtensionPartipants();
    }
  }

  /**
   * Replaces all occurrences of the PACKAGENAME with the given package name.
   * @param iProject
   * @param dir
   * @throws IOException
   */
  private void replacePackageNames(IProject iProject, File dir) throws IOException {
    Stack<File> files = new Stack<>();
    files.add(dir);
    while(files.isEmpty() == false)
    {
      File file = files.pop();
      if(file.getName().contains("_PACKAGENAME_"))
      {
        String name = file.getName();
        name = name.replace("_PACKAGENAME_", packageBaseName.replace('.', File.separatorChar));
        File newFile = new File(file.getParentFile(), name);
        newFile.getParentFile().mkdirs();
        file.renameTo(newFile);
        file = newFile;
      }
      if(file.getName().contains("_.._"))
      {
        String name = file.getName();
        name = name.replace("_.._", "../");
        File newFile = new File(file.getParentFile(), name);
        newFile.getParentFile().mkdirs();
        file.renameTo(newFile);
        file = newFile;
      }
      if(file.isDirectory())
      {
        File[] cf = file.listFiles();
        files.addAll(Arrays.asList(cf));
      }
      else
      {
        replaceInFile(file.toString(), "\\$\\{PACKAGENAME\\}", packageBaseName);
        replaceInFile(file.toString(), "\\$\\{PROJECTNAME\\}", projectBaseName);
      }
    }
  }

  /**
   * Replaces a pattern in a File. The File is not allowed to be larger then 1.000.000 bytes.
   * @param file the file, where the replacement is made.
   * @param pattern a regular expression to be replaced in the whole file.
   * @param replacement the replacement Text.
   * @throws IOException
   */
  public static void replaceInFile(String file, String pattern, String replacement) throws IOException {
    Charset cset = Charset.defaultCharset();
    java.nio.file.Path path = Paths.get(file);
    if (Files.exists(path)) {
      if (Files.size(path) > 1000000) {
        throw new IOException("File to large");
      }
      byte[] data = Files.readAllBytes(path);
      String str = new String(data, cset);
      String newStr = str.replaceAll(pattern, replacement);
      BufferedWriter writer = Files.newBufferedWriter(path, cset);
      writer.write(newStr);
      writer.close();
    }
  }

  /**
   * @param gwtSdk
   */
  private void setGwtSdk(Sdk gwtSdk) {
    this.gwtSdk = gwtSdk;
  }

  @Override
  public Sdk getGwtSdk() {
    return gwtSdk;
  }

  private void includeExtensionPartipants() throws CoreException {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = extensionRegistry
        .getExtensionPoint("com.gwtplugins.gdt.eclipse.suite.webAppCreatorParticipant");
    if (extensionPoint == null) {
      return;
    }
    IExtension[] extensions = extensionPoint.getExtensions();
    for (IExtension extension : extensions) {
      IConfigurationElement[] configurationElements = extension.getConfigurationElements();
      for (IConfigurationElement configurationElement : configurationElements) {
        Object createExecutableExtension = configurationElement.createExecutableExtension("class");
        Participant participant = (Participant) createExecutableExtension;
        participant.updateWebAppProjectCreator(this);
      }
    }
  }

  @Override
  public void setBuildAnt(boolean buildAnt) {
  }

  @Override
  public void setBuildMaven(boolean buildMaven) {
  }

  @Override
  public void setGenerateEmptyProject(boolean generateEmptyProject) {
  }

  @Override
  public void setLocationURI(URI locationURI) {
    this.locationURI = locationURI;
  }

  @Override
  public void setPackageName(String packageName) {
    this.packageBaseName = packageName;
  }

  @Override
  public void setProjectName(String projectName) {
    this.projectBaseName = projectName;
  }

  protected IFolder createFolders(IProject project, boolean createWarFolders, IProgressMonitor monitor)
      throws CoreException {
    IFolder srcFolder = project.getFolder("src");
    ResourceUtils.createFolderIfNonExistent(srcFolder, monitor);

    if (createWarFolders) {
      // create <WAR>/WEB-INF/lib
      ResourceUtils.createFolderStructure(project, new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/lib"));
    }

    return srcFolder;
  }

  protected IProject createProject(IProgressMonitor monitor, String projectName) throws CoreException {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = workspaceRoot.getProject(projectName);
    if (!project.exists()) {
      URI uri;
      if (isWorkspaceRootLocationURI(locationURI)) {
        // If you want to put a project in the workspace root then the location
        // needs to be null...
        uri = null;
      } else {
        // Non-default paths need to include the project name
        IPath path = URIUtil.toPath(locationURI).append(projectName);
        uri = URIUtil.toURI(path);
      }

      BuildPathsBlock.createProject(project, uri, monitor);
    }
    return project;
  }

  protected IPath findContainerPath(String containerId) {
    for (IPath containerPath : containerPaths) {
      if (SdkClasspathContainer.isContainerPath(containerId, containerPath)) {
        return containerPath;
      }
    }
    return null;
  }

  protected Sdk getGWTSdk() {
    return getSdk(GWTRuntimeContainer.CONTAINER_ID, GWTPreferences.getSdkManager());
  }

  protected void setProjectClasspath(IJavaProject javaProject, IFolder srcFolder, IProgressMonitor monitor)
      throws JavaModelException {
    List<IClasspathEntry> classpathEntries = new ArrayList<>();
    for(IClasspathEntry cp : javaProject.getRawClasspath())
    {
      classpathEntries.add(cp);
    }
    classpathEntries.add(JavaCore.newSourceEntry(srcFolder.getFullPath()));

    // Add the "test" folder as a src path, if it exists
    IProject project = javaProject.getProject();
    IFolder testFolder = project.getFolder("test");
    if (testFolder.exists()) {
      classpathEntries.add(JavaCore.newSourceEntry(testFolder.getFullPath(), new IPath[0],
          project.getFullPath().append("test-classes")));
    }
    classpathEntries.add(JavaCore.newProjectEntry(new Path("/t1"), null, true, null, false));

    // Add our container entries to the path
    for (IPath containerPath : containerPaths) {
      classpathEntries.add(JavaCore.newContainerEntry(containerPath));
    }

    classpathEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));

    javaProject.setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
  }

  private Sdk getSdk(String containerId, SdkManager<? extends Sdk> sdkManager) {
    Sdk sdk = null;
    IPath containerPath = findContainerPath(containerId);
    if (containerPath != null) {
      sdk = sdkManager.findSdkForPath(containerPath);
    }

    return sdk;
  }
  /**
   * Return the Java project created. This will only work half way through the process.
   */
  @Override
  public IJavaProject[] getCreatedJavaProjects() {
    return createdJavaProjects;
  }

  /**
   * Build a Maven project.
   */
  @Override
  public boolean isBuildMaven() {
    return false;
  }

  /**
   * Build a Ant project.
   */
  @Override
  public boolean isBuiltAnt() {
    return false;
  }

  @Override
  public IProgressMonitor getProgressMonitor() {
    return monitor;
  }

}
