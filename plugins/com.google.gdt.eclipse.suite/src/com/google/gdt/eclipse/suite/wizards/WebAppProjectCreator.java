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

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.appengine.eclipse.core.resources.GaeProject;
import com.google.appengine.eclipse.core.resources.GaeProjectResources;
import com.google.appengine.eclipse.core.sdk.AppEngineUpdateWebInfFolderCommand;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.appengine.eclipse.core.sdk.GaeSdkCapability;
import com.google.appengine.eclipse.core.sdk.GaeSdkContainer;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gdt.eclipse.core.WebAppUtilities.FileInfo;
import com.google.gdt.eclipse.core.natures.NatureUtils;
import com.google.gdt.eclipse.core.projects.IWebAppProjectCreator;
import com.google.gdt.eclipse.core.resources.ProjectResources;
import com.google.gdt.eclipse.core.sdk.Sdk;
import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gdt.eclipse.core.sdk.SdkClasspathContainer;
import com.google.gdt.eclipse.core.sdk.SdkManager;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.ProjectMigrator;
import com.google.gdt.eclipse.suite.launch.WebAppLaunchUtil;
import com.google.gdt.eclipse.suite.preferences.GdtPreferences;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntimeContainer;
import com.google.gwt.eclipse.core.runtime.tools.WebAppProjectCreatorRunner;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;
import com.google.gwt.eclipse.core.util.GwtVersionUtil;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.osgi.service.prefs.BackingStoreException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.management.ReflectionException;

/**
 * Web application project creator.
 */
@SuppressWarnings("restriction")
public class WebAppProjectCreator implements IWebAppProjectCreator {

  public static final IWebAppProjectCreator.Factory FACTORY = new IWebAppProjectCreator.Factory() {
    @Override
    public IWebAppProjectCreator create() {
      return new WebAppProjectCreator();
    }
  };

  private static final String FACET_JST_JAVA = "jst.java";

  /**
   * FilenameFilter that matches files that have a ".java" extension.
   */
  private static final FilenameFilter javaSourceFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".java");
    }
  };

  static String generateClassName(String projectName) {
    return StringUtilities.capitalize(sanitizeProjectName(projectName));
  }

  static String generateServletClassName(String projectName) {
    return generateClassName(projectName) + "Servlet";
  }

  static String generateServletName(String projectName) {
    return StringUtilities.capitalize(sanitizeProjectName(projectName));
  }

  static String generateServletPath(String projectName) {
    return generateServletName(projectName).toLowerCase();
  }

  /**
   * Deletes all files with a .launch extension located in the specified directory.
   */
  private static void deleteAllLaunchConfigurations(File dir) {
    File[] launchConfigurationFiles = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".launch");
      }
    });

    for (File launchConfigurationFile : launchConfigurationFiles) {
      launchConfigurationFile.delete();
    }
  }

  private static void deleteAllShellScripts(String projectName, File dir) {
    final String shellScriptPrefix = projectName + "-";
    File[] launchConfigurationFiles = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(shellScriptPrefix);
      }
    });

    for (File launchConfigurationFile : launchConfigurationFiles) {
      launchConfigurationFile.delete();
    }
  }

  private static void deleteBuildScript(File outDir) {
    File buildScript = new File(outDir, "build.xml");
    if (buildScript.exists()) {
      buildScript.delete();
    }
  }

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

  private static String sanitizeProjectName(String projectName) {
    assert (projectName != null && projectName.length() > 0);

    String sanitized = null;

    // Replace first character if it's invalid
    char firstChar = projectName.charAt(0);
    if (Character.isJavaIdentifierStart(firstChar)) {
      sanitized = String.valueOf(firstChar);
    } else {
      sanitized = "_";
    }

    // Replace remaining invalid characters
    for (int i = 1; i < projectName.length(); i++) {
      char ch = projectName.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        sanitized += String.valueOf(ch);
      } else {
        sanitized += "_";
      }
    }

    return sanitized;
  }

  private String appId;

  private List<IPath> containerPaths = new ArrayList<IPath>();

  private final List<FileInfo> fileInfos = new ArrayList<FileInfo>();

  private boolean isGenerateEmptyProject;

  private URI locationURI;

  private List<String> natureIds = new ArrayList<String>();

  private String packageName;

  private String projectName;

  private String[] templates = new String[] {"sample"};

  private String[] templateSources;

  private boolean buildAnt;

  private boolean buildMaven;

  private IJavaProject createdJavaProject;

  private IProgressMonitor monitor;

  protected WebAppProjectCreator() {
    // Always a java project
    natureIds.add(JavaCore.NATURE_ID);

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
  public void addFile(IPath path, InputStream inputStream) {
    fileInfos.add(new FileInfo(path, inputStream));
  }

  @Override
  public void addFile(IPath path, String content) throws UnsupportedEncodingException {
    fileInfos.add(new FileInfo(path, new ByteArrayInputStream(content.getBytes("UTF-8"))));
  }

  @Override
  public void addNature(String natureId) {
    natureIds.add(natureId);
  }

  /**
   * Creates the project per the current configuration. Note that the caller must have a workspace
   * lock in order to successfully execute this method.
   *
   * @throws BackingStoreException
   * @throws IOException
   */
  @Override
  public void create(IProgressMonitor monitor) throws CoreException, SdkException,
      ClassNotFoundException, BackingStoreException, IOException {
    this.monitor = monitor;
    boolean useGwt = natureIds.contains(GWTNature.NATURE_ID);
    boolean useGae = natureIds.contains(GaeNature.NATURE_ID);

    if (useGae) {
      createGaeProject(useGwt);
    }

    // TODO: Add code to update the progress monitor
    if (useGwt) {
      // Let GWT create the source files that we want, we will overwrite the
      // .project and .classpath files anyway

      IPath locationPath = URIUtil.toPath(locationURI);

      if (!isGenerateEmptyProject) {
        createGWTProject(monitor, packageName, locationPath.toOSString());
      } else if (!useGae) {
        // Add "empty" web.xml since the project is using GWT but not using
        // Google App Engine and GPE has already generated an empty project.
        addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/web.xml"),
            ProjectResources.createWebXmlSource());
      }

      IPath projDirPath = locationPath.append(projectName);

      // Wipe out the existing .project file
      projDirPath.append(".project").toFile().delete();

      // Wipe out the existing .classpath file
      projDirPath.append(".classpath").toFile().delete();

      // Wipe out the generated README. If we're using the legacy GWT project
      // creation tools, this will silently return false (the README isn't
      // there), but that's okay.
      projDirPath.append("README.txt").toFile().delete();
    }

    IProject project = createProject(monitor);

    if (isGenerateEmptyProject) {
      IPath classSourcePath = new Path("src/" + packageName.replace('.', '/'));
      ResourceUtils.createFolderStructure(project, classSourcePath);
    }

    /*
     * Refresh contents; if this project was generated via GWT's WebAppCreator, then these files
     * would have been created directly on the file system Although, this refresh should have been
     * done via the project.open() call, which is part of the createProject call above.
     */
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    // Create files
    createFiles(project);

    // Set all of the natures on the project
    NatureUtils.addNatures(project, natureIds);

    // Create the java project
    createdJavaProject = JavaCore.create(project);

    // Create a source folder and add it to the raw classpath
    IResource warFolder = project.findMember(WebAppUtilities.DEFAULT_WAR_DIR_NAME);
    boolean createWarFolders = (warFolder != null);
    IFolder srcFolder = createFolders(project, createWarFolders, monitor);
    if (createWarFolders) {
      // Set the WAR source/output directory to "/war"
      WebAppUtilities.setDefaultWarSettings(project);

      // Set the default output directory
      WebAppUtilities.setOutputLocationToWebInfClasses(createdJavaProject, monitor);

      /*
       * Copy files into the web-inf lib folder. This code assumes that it is running in a context
       * that has a workspace lock.
       */
      Sdk gwtSdk = getGWTSdk();
      if (gwtSdk != null) {
        new GWTUpdateWebInfFolderCommand(createdJavaProject, gwtSdk).execute();
      }

      Sdk gaeSdk = getGaeSdk();
      if (gaeSdk != null) {
        new AppEngineUpdateWebInfFolderCommand(createdJavaProject, gaeSdk).execute();
      }
    }

    // Set the project migrator version of the project to the current version,
    // so it will get future migrations but not existing migrations
    GdtPreferences.setProjectMigratorVersion(project, ProjectMigrator.CURRENT_VERSION);

    if (useGae) {
      setGaeDefaults(createdJavaProject);
    }

    if (!buildMaven) { // Do not do this when using Maven, b/c Maven will configure the Classpath
      setProjectClasspath(createdJavaProject, srcFolder, monitor);
    }

    if (useGae) {
      // Update WEB-INF folder to get the latest datanucleus jars.
      new AppEngineUpdateWebInfFolderCommand(createdJavaProject, getGaeSdk()).execute();
    }

    if (!buildMaven) { // This would have to come after maven extension configuration
      createLaunchConfig(project);
    }

    // Created a faceted project. This is long-running and hence run in a
    // separate job.
    jobSetupFacets(project);

    // Add the App Engine App ID to the project and create a new App Engine/Cloud project
    if (useGae) {
      saveAppIdToAppEngineWebXml(project);
    }

    // Allow other extensions to run after project creation
    includeExtensionPartipants();
  }

  private void includeExtensionPartipants() throws CoreException {
    IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint =
        extensionRegistry
            .getExtensionPoint("com.google.gdt.eclipse.suite.webAppCreatorParticipant");
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

  @Inject
  public void doSomething(IExtensionRegistry registry) {
    registry.getConfigurationElementsFor("com.google.gdt.eclipse.suite.webAppCreatorParticipant");
  }

  public List<IPath> getContainerPaths() {
    return containerPaths;
  }

  public List<FileInfo> getFileInfos() {
    return fileInfos;
  }

  public URI getLocationURI() {
    return locationURI;
  }

  public List<String> getNatureIds() {
    return natureIds;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getProjectName() {
    return projectName;
  }

  public String[] getTemplates() {
    String[] result;
    if (templates != null) {
      result = new String[templates.length];
      System.arraycopy(templates, 0, result, 0, templates.length);
    } else {
      result = null;
    }
    return result;
  }

  public String[] getTemplateSources() {
    String[] result;
    if (templateSources != null) {
      result = new String[templateSources.length];
      System.arraycopy(templateSources, 0, result, 0, templateSources.length);
    } else {
      result = null;
    }
    return result;
  }

  @Override
  public void setAppId(String appId) {
    this.appId = appId;
  }

  @Override
  public void setBuildAnt(boolean buildAnt) {
    this.buildAnt = buildAnt;
    if (buildAnt) {
      addTemplate("ant");
    }
  }

  @Override
  public void setBuildMaven(boolean buildMaven) {
    this.buildMaven = buildMaven;
    if (buildMaven) {
      addTemplate("maven");
    }
  }

  public void addTemplate(String template) {
    String[] result = Arrays.copyOf(templates, templates.length + 1);
    result[templates.length] = template;
    templates = result;
  }

  public void setContainerPaths(List<IPath> containerPaths) {
    this.containerPaths = containerPaths;
  }

  @Override
  public void setGenerateEmptyProject(boolean generateEmptyProject) {
    this.isGenerateEmptyProject = generateEmptyProject;
  }

  @Override
  public void setLocationURI(URI locationURI) {
    this.locationURI = locationURI;
  }

  public void setNatureIds(List<String> natureIds) {
    this.natureIds = natureIds;
  }

  @Override
  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  @Override
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public void setTemplates(String... templates) {
    if (templates == null) {
      this.templates = null;
    } else {
      this.templates = new String[templates.length];
      System.arraycopy(templates, 0, this.templates, 0, templates.length);
    }
  }

  @Override
  public void setTemplateSources(String... templateSources) {
    if (templateSources == null) {
      this.templateSources = null;
    } else {
      this.templateSources = new String[templateSources.length];
      System.arraycopy(templateSources, 0, this.templateSources, 0, templateSources.length);
    }
  }

  protected void createFiles(IProject project) throws CoreException {
    for (FileInfo fileInfo : fileInfos) {
      ResourceUtils.createFolderStructure(project, fileInfo.path.removeLastSegments(1));
      ResourceUtils.createFile(project.getFullPath().append(fileInfo.path), fileInfo.inputStream);
    }
  }

  protected IFolder createFolders(IProject project, boolean createWarFolders,
      IProgressMonitor monitor) throws CoreException {
    IFolder srcFolder = project.getFolder("src");
    ResourceUtils.createFolderIfNonExistent(srcFolder, monitor);

    if (createWarFolders) {
      // create <WAR>/WEB-INF/lib
      ResourceUtils.createFolderStructure(project, new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME
          + "/WEB-INF/lib"));
    }

    return srcFolder;
  }

  protected void createGaeProject(boolean useGwt) throws CoreException, FileNotFoundException,
      UnsupportedEncodingException {
    addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/appengine-web.xml"),
        GaeProjectResources.createAppEngineWebXmlSource(useGwt));

    IPath gaeSdkContainerPath = findContainerPath(GaeSdkContainer.CONTAINER_ID);
    if (gaeSdkContainerPath == null) {
      throw new CoreException(new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID,
          "Missing GAE SDK container path"));
    }

    GaeSdk gaeSdk = GaePreferences.getSdkManager().findSdkForPath(gaeSdkContainerPath);
    if (gaeSdk != null) {
      // Add jdoconfig.xml
      addFile(new Path("src/META-INF/jdoconfig.xml"),
          GaeProjectResources.createJdoConfigXmlSource(getGaeDatanucleusVersion(gaeSdk)));
      // Add persistence.xml
      addFile(new Path("src/META-INF/persistence.xml"),
          GaeProjectResources.createPersistenceXmlSource(getGaeDatanucleusVersion(gaeSdk)));

      IPath installationPath = gaeSdk.getInstallationPath();
      File log4jPropertiesFile = installationPath.append("config/user/log4j.properties").toFile();
      if (log4jPropertiesFile.exists()) {
        // Add the log4j.properties file
        addFile(new Path("src/log4j.properties"), new FileInputStream(log4jPropertiesFile));
      }

      File loggingPropertiesFile =
          installationPath.append("config/user/logging.properties").toFile();
      if (loggingPropertiesFile.exists()) {
        // Add the logging.properties file
        addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/logging.properties"),
            new FileInputStream(loggingPropertiesFile));
      }
    }

    if (isGenerateEmptyProject) {
      addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/web.xml"),
          GaeProjectResources.createEmptyWebXml());
    }

    if (!useGwt && !isGenerateEmptyProject) {
      String servletName = WebAppProjectCreator.generateServletName(projectName);
      String servletPath = WebAppProjectCreator.generateServletPath(projectName);
      String servletPackageName = packageName;
      String servletSimpleClassName = WebAppProjectCreator.generateServletClassName(projectName);
      String servletQualifiedClassName = servletPackageName + "." + servletSimpleClassName;

      // Add index.html
      addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/index.html"),
          GaeProjectResources.createWelcomePageSource(servletName, servletPath));

      // Add web.xml
      addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/WEB-INF/web.xml"),
          ProjectResources.createWebXmlSource(servletName, servletPath, servletQualifiedClassName));
      // Add servlet source
      IPath servletClassSourcePath =
          new Path("src/" + servletQualifiedClassName.replace('.', '/') + ".java");
      addFile(servletClassSourcePath,
          GaeProjectResources.createSampleServletSource(servletPackageName, servletSimpleClassName));

      // Add default favicon.ico
      addFile(new Path(WebAppUtilities.DEFAULT_WAR_DIR_NAME + "/favicon.ico"),
          GaeProjectResources.createFavicon());
    }
  }

  protected void createLaunchConfig(IProject project) throws CoreException {
    // If the default SDK is GWT 2.7 or greater, turn on GWT super dev mode by default.
    boolean turnOnGwtSuperDevMode =
        GwtVersionUtil.isGwtVersionGreaterOrEqualTo27(JavaCore.create(project));

    ILaunchConfigurationWorkingCopy wc =
        WebAppLaunchUtil.createLaunchConfigWorkingCopy(project.getName(), project,
            WebAppLaunchUtil.determineStartupURL(project, false), false, turnOnGwtSuperDevMode);
    ILaunchGroup[] groups = DebugUITools.getLaunchGroups();

    ArrayList<String> groupsNames = new ArrayList<String>();
    for (ILaunchGroup group : groups) {
      if (IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP.equals(group.getIdentifier())
          || IDebugUIConstants.ID_RUN_LAUNCH_GROUP.equals(group.getIdentifier())) {
        groupsNames.add(group.getIdentifier());
      }
    }

    wc.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, groupsNames);
    wc.doSave();
  }

  protected IProject createProject(IProgressMonitor monitor) throws CoreException {
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

  protected Sdk getGaeSdk() {
    return getSdk(GaeSdkContainer.CONTAINER_ID, GaePreferences.getSdkManager());
  }

  protected Sdk getGWTSdk() {
    return getSdk(GWTRuntimeContainer.CONTAINER_ID, GWTPreferences.getSdkManager());
  }

  protected void setProjectClasspath(IJavaProject javaProject, IFolder srcFolder,
      IProgressMonitor monitor) throws JavaModelException {
    List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
    classpathEntries.add(JavaCore.newSourceEntry(srcFolder.getFullPath()));

    // Add the "test" folder as a src path, if it exists
    IProject project = javaProject.getProject();
    IFolder testFolder = project.getFolder("test");
    if (testFolder.exists()) {
      classpathEntries.add(JavaCore.newSourceEntry(testFolder.getFullPath(), new IPath[0], project
          .getFullPath().append("test-classes")));
    }

    // Add our container entries to the path
    for (IPath containerPath : containerPaths) {
      classpathEntries.add(JavaCore.newContainerEntry(containerPath));
    }

    classpathEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));

    javaProject.setRawClasspath(classpathEntries.toArray(new IClasspathEntry[0]), monitor);
  }

  private void createGWTProject(IProgressMonitor monitor, String packageName, String outDirPath)
      throws SdkException, JavaModelException, CoreException {

    /*
     * The project name will be used as the entry point name. When invoking GWT's WebAppCreator, the
     * entry point name will be passed in as the last component in the module name. The
     * WebAppCreator will use the last component of the module name as the generated Eclipse project
     * name, which gives us the desired effect.
     *
     * FIXME: The project name may not be a valid entry point name. We need to scan the project name
     * token-by-token, and translate its tokens into a valid Java identifier name. Some examples:
     *
     * If the first token in the project name is a lower-case letter, then the translated character
     * should be made upper case.
     *
     * If the first token in the project name is a non-alpha character it should be deleted in the
     * translation.
     */

    final String entryPoint = generateClassName(projectName);
    final String qualifiedModuleName = packageName + "." + entryPoint;

    IPath gwtContainerPath = findContainerPath(GWTRuntimeContainer.CONTAINER_ID);
    assert (gwtContainerPath != null);

    GWTRuntime runtime = GWTPreferences.getSdkManager().findSdkForPath(gwtContainerPath);
    assert (runtime != null);

    // Get a reference to the gwt-dev-<platform>.jar
    File gwtDevJar = runtime.getDevJar();

    // Need to set gwt.devjar property before calling projectCreator and
    // applicationCreator
    System.setProperty("gwt.devjar", gwtDevJar.toString());

    File outDir = new File(outDirPath, projectName);

    WebAppProjectCreatorRunner.createProject(qualifiedModuleName, outDir.getAbsolutePath(),
        runtime, monitor, templateSources, templates);

    reformatJavaFiles(outDir);
    String version = runtime.getVersion();
    if (!SdkUtils.isInternal(version)
        && SdkUtils.compareVersionStrings(version,
            WebAppProjectCreatorRunner.GWT_VERSION_WITH_TEMPLATES) <= -1) {
      deleteAllLaunchConfigurations(outDir);
      deleteAllShellScripts(projectName, outDir);
      deleteBuildScript(outDir);
    }
  }

  private String getGaeDatanucleusVersion(GaeSdk sdk) {
    if (sdk.getCapabilities().contains(GaeSdkCapability.OPTIONAL_USER_LIB)) {
      try {
        return sdk.getAppEngineBridge().getLatestVersion("datanucleus");
      } catch (CoreException e) {
        GdtPlugin.getLogger().logError(e, "Could not get appengine bridge from sdk");
      } catch (ReflectionException e) {
        GdtPlugin.getLogger().logError(e.getLocalizedMessage(), e.getTargetException());
      }
    }
    return "";
  }

  private Sdk getSdk(String containerId, SdkManager<? extends Sdk> sdkManager) {
    Sdk sdk = null;
    IPath gaeContainerPath = findContainerPath(containerId);
    if (gaeContainerPath != null) {
      sdk = sdkManager.findSdkForPath(gaeContainerPath);
    }

    return sdk;
  }

  private void jobSetupFacets(final IProject project) {
    // Facet setup is done in a workspace job since this can be long running,
    // hence shouldn't be from the UI thread.
    WorkspaceJob setupFacetsJob = new WorkspaceJob("Setting up facets") {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          // Create faceted project
          IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
          // Add Java facet by default
          IProjectFacet javaFacet = ProjectFacetsManager.getProjectFacet(FACET_JST_JAVA);
          facetedProject.installProjectFacet(javaFacet.getDefaultVersion(), null, monitor);
          return Status.OK_STATUS;
        } catch (CoreException e) {
          // Log and continue
          GdtPlugin.getLogger().logError(e);
          return new Status(IStatus.ERROR, GdtPlugin.PLUGIN_ID, e.toString(), e);
        }
      }
    };
    setupFacetsJob.schedule();
  }

  private void saveAppIdToAppEngineWebXml(IProject project) throws IOException, CoreException {
    if (appId == null || appId.isEmpty()) {
      return;
    }

    GaeProject gaeProject = GaeProject.create(project);
    if (gaeProject != null) {
      gaeProject.setAppId(appId, true);
    }
  }

  private void setGaeDefaults(IJavaProject javaProject) throws BackingStoreException {
    GaeSdk sdk = (GaeSdk) getGaeSdk();

    // Enable HRD by default for new projects, if supported.
    if (sdk != null && sdk.getCapabilities().contains(GaeSdkCapability.HRD)) {
      GaeProjectProperties.setGaeHrdEnabled(javaProject.getProject(), true);
    }

    // Choose the highest version for each library by default.
    if (sdk != null) {
      GaeProjectProperties.setGaeDatanucleusVersion(javaProject.getProject(),
          getGaeDatanucleusVersion(sdk));
    }
  }

  /**
   * Return the Java project created. This will only work half way through the process.
   */
  @Override
  public IJavaProject getCreatedJavaProject() {
    return createdJavaProject;
  }

  /**
   * Build a Maven project.
   */
  @Override
  public boolean getBuildMaven() {
    return buildMaven;
  }

  /**
   * Build a Ant project.
   */
  @Override
  public boolean getBuiltAnt() {
    return buildAnt;
  }

  @Override
  public IProgressMonitor getProgressMonitor() {
    return monitor;
  }

}
