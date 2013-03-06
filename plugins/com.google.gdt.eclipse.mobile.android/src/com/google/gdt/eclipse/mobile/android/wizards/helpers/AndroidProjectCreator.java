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
package com.google.gdt.eclipse.mobile.android.wizards.helpers;

import com.google.gdt.eclipse.mobile.android.GdtAndroidPlugin;

import com.android.SdkConstants;
import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.AndroidNature;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.io.StreamException;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Creates a sample Android Project
 */

@SuppressWarnings("restriction")
public class AndroidProjectCreator {

  private static String[] ICON_FILES = new String[] {"ic_launcher.png"};
  private static String[] ICON_XHDPI_FILES = new String[] {"ic_launcher_xhdpi.png"};
  private static String[] ICON_HDPI_FILES = new String[] {"ic_launcher_hdpi.png"};
  private static String[] ICON_MDPI_FILES = new String[] {"ic_launcher_mdpi.png"};
  private static String[] ICON_LDPI_FILES = new String[] {"ic_launcher_ldpi.png"};

  private static String ICON_LAUNCHER_FILE = "ic_launcher-web.png";

  private static final String TEMPLATES_DIRECTORY = "templates/android/"; //$NON-NLS-1$

  public static AndroidProjectCreator createNewAndroidProjectCreator() {
    return new AndroidProjectCreator();
  }

  private String projectName;

  private IAndroidTarget sdkTarget;
  private Map<String, Object> androidProjectParameters;

  private HashMap<String, String> androidProjectDictionary;

  private IProject androidProject;

  private IProjectDescription androidProjectDescription;

  /**
   * Create the project
   * 
   * @throws CoreException
   * @throws IOException
   * @throws StreamException
   */
  public IProject create(IProgressMonitor monitor) throws CoreException, IOException,
      StreamException {

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    androidProject = workspace.getRoot().getProject(projectName);
    androidProjectDescription = workspace.newProjectDescription(androidProject.getName());
    IPath path = (IPath) androidProjectParameters.get(ProjectCreationConstants.PARAM_PROJECT_PATH);
    IPath defaultLocation = Platform.getLocation();
    if (!path.equals(defaultLocation)) {
      androidProjectDescription.setLocation(path.append(new Path(androidProject.getName())));
    } else {
      androidProjectDescription.setLocation(null);
    }

    // Create project and open it
    androidProject.create(androidProjectDescription, new SubProgressMonitor(monitor, 10));
    if (monitor.isCanceled())
      throw new OperationCanceledException();

    androidProject.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 10));
    // Add the Java and android nature to the project
    AndroidNature.setupProjectNatures(androidProject, monitor);

    // Create folders in the project if they don't already exist
    addDefaultDirectories(androidProject, AdtConstants.WS_ROOT,
        ProjectCreationConstants.DEFAULT_DIRECTORIES, monitor);
    String[] sourceFolders = new String[] {
        SdkConstants.FD_SOURCES, ProjectCreationConstants.GEN_SRC_DIRECTORY};
    addDefaultDirectories(androidProject, AdtConstants.WS_ROOT, sourceFolders, monitor);

    // Create the resource folders in the project if they don't already exist.
    addDefaultDirectories(androidProject, ProjectCreationConstants.RES_DIRECTORY,
        ProjectCreationConstants.RES_DENSITY_ENABLED_DIRECTORIES, monitor);

    // Setup class path: mark folders as source folders
    IJavaProject androidJavaProject = JavaCore.create(androidProject);
    String justProjectName = projectName.replace(" ", "");
    justProjectName = justProjectName.substring(0, 1).toUpperCase() + justProjectName.substring(1);

    androidProjectParameters.put("CLASSNAME", justProjectName); //$NON-NLS-N$
    // for now, always create new project
    addManifest(androidProject, androidProjectParameters, androidProjectDictionary, monitor);

    // add the default app icon
    addIcon(androidProject, monitor);

    // Create the default package components
    Map<String, String> replacements = new HashMap<String, String>();

    replacements.put("@PackageName@", //$NON-NLS-N$
        (String) androidProjectParameters.get(ProjectCreationConstants.PARAM_PACKAGE));

    replacements.put("@ClassName@", justProjectName); //$NON-NLS-N$
    replacements.put("@ClassNameLowercase@", justProjectName.toLowerCase()); //$NON-NLS-N$

    addMainActivityClass(androidProject, sourceFolders[0],
        (String) androidProjectParameters.get(ProjectCreationConstants.PARAM_PACKAGE),
        "MainActivity.java", replacements, monitor); //$NON-NLS-N$

    // add the string definition file if needed
    if (androidProjectDictionary.size() > 0) {
      addStringDictionaryFile(androidProject, androidProjectDictionary, monitor);
    }

    // add the main menu resource file
    addMainMenuFile(androidProject, monitor);
    addLayoutFile(androidProject, "activity_main.xml", monitor);
    // add the default proguard config
    File libFolder = new File(
        (String) androidProjectParameters.get(ProjectCreationConstants.PARAM_SDK_TOOLS_DIR),
        SdkConstants.FD_LIB);
    addLocalFile(androidProject, new File(libFolder, SdkConstants.FN_PROJECT_PROGUARD_FILE),
        monitor);

    addPrefsFile(androidProject, monitor);

    // Set output location
    androidJavaProject.setOutputLocation(
        androidProject.getFolder(ProjectCreationConstants.BIN_DIRECTORY).getFullPath(), monitor);

    setupSourceFolders(androidJavaProject, sourceFolders, monitor);

    Sdk.getCurrent().initProject(androidProject, sdkTarget);

    // Fix the project to make sure all properties are as expected.
    // Necessary for existing projects and good for new ones to.
    ProjectHelper.fixProject(androidProject);
    return androidProject;
  }

  public void setAndroidProjectDictionary(HashMap<String, String> androidProjectDictionary) {
    this.androidProjectDictionary = androidProjectDictionary;
  }

  public void setAndroidProjectParameters(Map<String, Object> parameters) {
    this.androidProjectParameters = parameters;
    projectName = (String) androidProjectParameters.get(ProjectCreationConstants.PARAM_PROJECT);
    sdkTarget = (IAndroidTarget) androidProjectParameters.get(ProjectCreationConstants.PARAM_SDK_TARGET);
  }

  /**
   * Adds default directories to the project.
   * 
   * @param project The Java Project to update.
   * @param parentFolder The path of the parent folder. Must end with a
   *          separator.
   * @param folders Folders to be added.
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to create the directories in the
   *           project.
   */
  private void addDefaultDirectories(IProject project, String parentFolder, String[] folders,
      IProgressMonitor monitor) throws CoreException {
    for (String name : folders) {
      if (name.length() > 0) {
        IFolder folder = project.getFolder(parentFolder + name);
        if (!folder.exists()) {
          folder.create(true /* force */, true /* local */, new SubProgressMonitor(monitor, 10));
        }
      }
    }
  }

  private IClasspathEntry[] addEntryToClasspath(IClasspathEntry[] entries, IClasspathEntry newEntry) {
    int n = entries.length;
    IClasspathEntry[] newEntries = new IClasspathEntry[n + 1];
    System.arraycopy(entries, 0, newEntries, 0, n);
    newEntries[n] = newEntry;
    return newEntries;
  }

  /**
   * Creates a file from a data source.
   * 
   * @param dest the file to write
   * @param source the content of the file.
   * @param monitor the progress monitor
   * @throws CoreException
   */
  private void addFile(IFile dest, byte[] source, IProgressMonitor monitor) throws CoreException {
    if (source != null) {
      // Save in the project
      InputStream stream = new ByteArrayInputStream(source);
      dest.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }
  }

  /**
   * Adds default application icon to the project.
   * 
   * @param project The Java Project to update.
   * @param legacy whether we're running in legacy mode (no density support)
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to update the project.
   */
  private void addIcon(IProject project, IProgressMonitor monitor) throws CoreException {
    // TODO (rdayal): Clean this up.
    IFile xhdpiFile, hdpiFile, mdpiFile, ldpiFile;
    for (int i = 0; i < ICON_FILES.length; i++) {
      // xhigh, high, medium, and low densities
      xhdpiFile = project.getFile(ProjectCreationConstants.RES_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.DRAWABLE_XHDPI_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ICON_FILES[i]);
      hdpiFile = project.getFile(ProjectCreationConstants.RES_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.DRAWABLE_HDPI_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ICON_FILES[i]);
      mdpiFile = project.getFile(ProjectCreationConstants.RES_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.DRAWABLE_MDPI_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ICON_FILES[i]);
      ldpiFile = project.getFile(ProjectCreationConstants.RES_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.DRAWABLE_LDPI_DIRECTORY
          + ProjectCreationConstants.WS_SEP + ICON_FILES[i]);

      if (!xhdpiFile.exists()) {
        addFile(xhdpiFile,
            ProjectResourceUtils.getResource(TEMPLATES_DIRECTORY + ICON_XHDPI_FILES[i]), monitor);
      }

      if (!hdpiFile.exists()) {
        addFile(hdpiFile,
            ProjectResourceUtils.getResource(TEMPLATES_DIRECTORY + ICON_HDPI_FILES[i]), monitor);
      }

      if (!mdpiFile.exists()) {
        addFile(mdpiFile,
            ProjectResourceUtils.getResource(TEMPLATES_DIRECTORY + ICON_MDPI_FILES[i]), monitor);
      }

      if (!ldpiFile.exists()) {
        addFile(ldpiFile,
            ProjectResourceUtils.getResource(TEMPLATES_DIRECTORY + ICON_LDPI_FILES[i]), monitor);
      }

      // Finally, add web launcher file
      IFile launcherFile = project.getFile(ICON_LAUNCHER_FILE);
      if (!launcherFile.exists()) {
        addFile(launcherFile,
            ProjectResourceUtils.getResource(TEMPLATES_DIRECTORY + ICON_LAUNCHER_FILE), monitor);
      }      
    }

  }

  /**
   * Adds a layout file
   */
  private void addLayoutFile(IProject project, String fileName, IProgressMonitor monitor)
      throws CoreException, IOException {
    // create the layout file
    IFolder layoutfolder = project.getFolder(ProjectCreationConstants.RES_DIRECTORY).getFolder(
        ProjectCreationConstants.LAYOUT_DIRECTORY);
    IFile file = layoutfolder.getFile(fileName);
    if (!file.exists()) {
      copyFile(
          ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY + fileName + ".layout"),
          file, monitor);
    }
  }

  /**
   * Adds a file to the root of the project
   * 
   * @param project the project to add the file to.
   * @param source the file to add. It'll keep the same filename once copied
   *          into the project.
   * @throws FileNotFoundException
   * @throws CoreException
   */
  private void addLocalFile(IProject project, File source, IProgressMonitor monitor)
      throws FileNotFoundException, CoreException {
    IFile dest = project.getFile(source.getName());
    if (dest.exists() == false) {
      FileInputStream stream = new FileInputStream(source);
      dest.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }
  }

  /**
   * Adds the main activity class, naming it to correspond to project name
   * 
   * @throws CoreException
   * @throws IOException
   */
  private void addMainActivityClass(IProject project, String sourceFolder, String packageName,
      String fileName, Map<String, String> replacements, IProgressMonitor monitor)
      throws CoreException, IOException {

    // create the java package directories.
    IFolder pkgFolder = project.getFolder(sourceFolder);

    String[] components = packageName.split(AdtConstants.RE_DOT);
    for (String component : components) {
      pkgFolder = pkgFolder.getFolder(component);
      if (!pkgFolder.exists()) {
        pkgFolder.create(true /* force */, true /* local */, new SubProgressMonitor(monitor, 10));
      }
    }

    IFile file = pkgFolder.getFile(fileName);
    if (!file.exists()) {
      copyFile(ProjectResourceUtils.getResourceAsString(
          TEMPLATES_DIRECTORY + "MainActivity.java", replacements), file, monitor); //$NON-NLS-N$
    }
  }

  /**
   * Adds the menu resource file.
   * 
   * @param project The Java Project to update.
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to update the project.
   * @throws IOException if the method fails to create the files in the project.
   */
  private void addMainMenuFile(IProject project, IProgressMonitor monitor) throws CoreException,
      IOException {

    // create the IFile object and check if the file doesn't already exist.
    IFile file = project.getFile(ProjectCreationConstants.RES_DIRECTORY
        + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.MENU_DIRECTORY
        + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.MAIN_MENU_FILE);
    if (!file.exists()) {
      // get the main_menus.xml template
      Map<String, String> replacements = new HashMap<String, String>();
      String mainMenuTemplate = ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
          + "activity_main.xml", replacements); //$NON-NLS-N$
      // Save in the project as UTF-8
      InputStream stream = new ByteArrayInputStream(mainMenuTemplate.getBytes("UTF-8")); //$NON-NLS-1$
      file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }
  }

  /**
   * Adds the manifest to the project.
   * 
   * @param project The Java Project to update.
   * @param parameters Template Parameters.
   * @param dictionary String List to be added to a string definition file. This
   *          map will be filled by this method.
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to update the project.
   * @throws IOException if the method fails to create the files in the project.
   */
  private void addManifest(IProject project, Map<String, Object> parameters,
      Map<String, String> dictionary, IProgressMonitor monitor) throws CoreException, IOException {

    // get IFile to the manifest and check if it's not already there.
    IFile file = project.getFile(SdkConstants.FN_ANDROID_MANIFEST_XML);
    if (!file.exists()) {

      // Read manifest template
      String manifestTemplate = ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
          + "AndroidManifest.xml"); //$NON-NLS-N$

      // Replace all keyword parameters
      manifestTemplate = replaceParameters(manifestTemplate, parameters);

      if (manifestTemplate == null) {
        // Inform the user there will be not manifest.
        GdtAndroidPlugin.getLogger().logError(
            "Failed to generate the Android manifest. Missing template %s");
        // Abort now, there's no need to continue
        return;
      }

      String minSdkVersion = (String) parameters.get(ProjectCreationConstants.PARAM_MIN_SDK_VERSION);
      if (minSdkVersion != null && minSdkVersion.length() > 0) {
        String usesSdkTemplate = AdtPlugin.readEmbeddedTextFile(ProjectCreationConstants.TEMPLATE_USES_SDK);
        if (usesSdkTemplate != null) {
          String usesSdk = replaceParameters(usesSdkTemplate, parameters);
          manifestTemplate = manifestTemplate.replaceAll(ProjectCreationConstants.PH_USES_SDK,
              usesSdk);
        }
      } else {
        manifestTemplate = manifestTemplate.replaceAll(ProjectCreationConstants.PH_USES_SDK, "");
      }

      // Save in the project as UTF-8
      InputStream stream = new ByteArrayInputStream(manifestTemplate.getBytes("UTF-8")); //$NON-NLS-1$
      file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }
  }

  private void addPrefsFile(IProject project, IProgressMonitor monitor) throws CoreException,
      IOException {
    IFolder pkgFolder = project.getFolder(ProjectCreationConstants.PREFS_DIRECTORY);
    if (!pkgFolder.exists()) {
      pkgFolder.create(true /* force */, true /* local */, new SubProgressMonitor(monitor, 10));
    }
    IFile file = project.getFile(ProjectCreationConstants.PREFS_DIRECTORY
        + GdtAndroidPlugin.PLUGIN_ID + ".prefs"); //$NON-NLS-N$
    String prefString = ProjectCreationConstants.APP_ENGINE_PROJECT
        + "=" + projectName + "-AppEngine"; //$NON-NLS-N$
    if (!file.exists()) {
      copyFile(prefString, file, monitor);
    }
    // apt prefs
    IFile jdtAptPrefs = project.getFile(ProjectCreationConstants.PREFS_DIRECTORY
        + ProjectCreationConstants.JDT_APT_PREFS);
    if (!jdtAptPrefs.exists()) {
      copyFile(
          ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
              + ProjectCreationConstants.JDT_APT_PREFS), jdtAptPrefs, monitor);
    }
    IFile jdtPrefs = project.getFile(ProjectCreationConstants.PREFS_DIRECTORY
        + ProjectCreationConstants.JDT_PREFS);
    if (!jdtPrefs.exists()) {
      copyFile(
          ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
              + ProjectCreationConstants.JDT_PREFS), jdtPrefs, monitor);
    }
  }

  /**
   * Adds the string resource file.
   * 
   * @param project The Java Project to update.
   * @param strings The list of strings to be added to the string file.
   * @param monitor An existing monitor.
   * @throws CoreException if the method fails to update the project.
   * @throws IOException if the method fails to create the files in the project.
   */
  private void addStringDictionaryFile(IProject project, Map<String, String> strings,
      IProgressMonitor monitor) throws CoreException, IOException {

    // create the IFile object and check if the file doesn't already exist.
    IFile file = project.getFile(ProjectCreationConstants.RES_DIRECTORY
        + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.VALUES_DIRECTORY
        + ProjectCreationConstants.WS_SEP + ProjectCreationConstants.STRINGS_FILE);
    if (!file.exists()) {
      // get the strings.xml template
      Collections.singletonMap("@AppName@", projectName);
      String stringTemplate = ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
          + "strings.xml", Collections.singletonMap("@AppName@", projectName)); //$NON-NLS-N$
      // Save in the project as UTF-8
      InputStream stream = new ByteArrayInputStream(stringTemplate.getBytes("UTF-8")); //$NON-NLS-1$
      file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }

    // create the IFile object and check if the file doesn't already exist.
    file = project.getFile(ProjectCreationConstants.RES_DIRECTORY + ProjectCreationConstants.WS_SEP
        + ProjectCreationConstants.VALUES_DIRECTORY + ProjectCreationConstants.WS_SEP
        + ProjectCreationConstants.STYLES_FILE);
    if (!file.exists()) {
      // get the strings.xml template
      Map<String, String> replacements = new HashMap<String, String>();
      String stringTemplate = ProjectResourceUtils.getResourceAsString(TEMPLATES_DIRECTORY
          + "styles.xml", replacements); //$NON-NLS-N$
      // Save in the project as UTF-8
      InputStream stream = new ByteArrayInputStream(stringTemplate.getBytes("UTF-8")); //$NON-NLS-1$
      file.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
    }
  }

  /**
   * Copies the given file from our resource folder to the new project. Expects
   * the file to the US-ASCII or UTF-8 encoded.
   * 
   * @throws CoreException from IFile if failing to create the new file.
   * @throws MalformedURLException from URL if failing to interpret the URL.
   * @throws FileNotFoundException from RandomAccessFile.
   * @throws IOException from RandomAccessFile.length() if can't determine the
   *           length.
   */
  private void copyFile(String resourceFile, IFile destFile, IProgressMonitor monitor)
      throws CoreException, IOException {

    // Save in the project as UTF-8
    InputStream stream = new ByteArrayInputStream(resourceFile.getBytes("UTF-8")); //$NON-NLS-1$
    destFile.create(stream, false /* force */, new SubProgressMonitor(monitor, 10));
  }

  /**
   * Removes the corresponding source folder from the class path entries if
   * found.
   * 
   * @param entries The class path entries to read. A copy will be returned.
   * @param folder The parent source folder to remove.
   * @return A new class path entries array.
   */
  private IClasspathEntry[] removeSourceClasspath(IClasspathEntry[] entries, IContainer folder) {
    if (folder == null) {
      return entries;
    }
    IClasspathEntry source = JavaCore.newSourceEntry(folder.getFullPath());
    int n = entries.length;
    for (int i = n - 1; i >= 0; i--) {
      if (entries[i].equals(source)) {
        IClasspathEntry[] newEntries = new IClasspathEntry[n - 1];
        if (i > 0)
          System.arraycopy(entries, 0, newEntries, 0, i);
        if (i < n - 1)
          System.arraycopy(entries, i + 1, newEntries, i, n - i - 1);
        n--;
        entries = newEntries;
      }
    }
    return entries;
  }

  /**
   * Replaces placeholders found in a string with values.
   * 
   * @param str the string to search for placeholders.
   * @param parameters a map of <placeholder, Value> to search for in the string
   * @return A new String object with the placeholder replaced by the values.
   */
  private String replaceParameters(String str, Map<String, Object> parameters) {

    if (parameters == null) {
      GdtAndroidPlugin.getLogger().logError(
          "NPW replace parameters: null parameter map. String: '%s'", str); //$NON-NLS-1$
      return str;
    } else if (str == null) {
      GdtAndroidPlugin.getLogger().logError("NPW replace parameters: null template string"); //$NON-NLS-1$
      return str;
    }

    for (Entry<String, Object> entry : parameters.entrySet()) {
      if (entry != null && entry.getValue() instanceof String) {
        Object value = entry.getValue();
        if (value == null) {
          GdtAndroidPlugin.getLogger().logError(
              "NPW replace parameters: null value for key '%s' in template '%s'", //$NON-NLS-1$
              entry.getKey(), str);
        } else {
          str = str.replaceAll(entry.getKey(), (String) value);
        }
      }
    }

    return str;
  }

  /**
   * Adds the given folder to the project's class path.
   * 
   * @param javaProject The Java Project to update.
   * @param sourceFolder Template Parameters.
   * @param monitor An existing monitor.
   * @throws CoreException
   */
  private void setupSourceFolders(IJavaProject javaProject, String[] sourceFolders,
      IProgressMonitor monitor) throws CoreException {
    IProject project = javaProject.getProject();
    // get the list of entries.
    IClasspathEntry[] entries = javaProject.getRawClasspath();

    // remove the project as a source folder (This is the default)
    entries = removeSourceClasspath(entries, project);

    // add the source folders.
    for (String sourceFolder : sourceFolders) {
      IFolder srcFolder = project.getFolder(sourceFolder);

      // remove it first in case.
      entries = removeSourceClasspath(entries, srcFolder);
      entries = addEntryToClasspath(entries, JavaCore.newSourceEntry(srcFolder.getFullPath()));
    }

    // add .apt_generated to classpath
    IClasspathAttribute[] attributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(
        "optional", "true")}; //$NON-NLS-N$
    IFolder aptFolder = project.getFolder(ProjectCreationConstants.APT_FOLDER);
    IClasspathEntry entry = JavaCore.newSourceEntry(aptFolder.getFullPath(),
        ClasspathEntry.INCLUDE_ALL, ClasspathEntry.EXCLUDE_NONE, null, attributes);
    entries = addEntryToClasspath(entries, entry);

    javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
  }

}
