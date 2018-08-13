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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.JavaProjectUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.GWTProjectUtilities;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a GWT runtime that is based on the Eclipse projects containing the GWT trunk.
 */
public class GWTProjectsRuntime extends GwtSdk {

  public static final String LOCATION = "Workspace";

  public static final String VERSION = "0.0.0";

  static final String GWT_DEV_FALLBACK_PROJECT_NAME = "gwt-dev";

  /*
   * This location (path on disk) is relative to the folder that is the grandparent of a source
   * folder in either the gwt-user or gwt-dev projects.
   */
  static final String STAGING_FOLDER_RELATIVE_LOCATION = "build/staging";

  private static final String GWT_USER_PROJECT_NAME = "gwt-user";

  private static final String JAVADOC_SOURCE_FOLDER_NAME = "javadoc";

  private static final File[] NO_FILES = new File[0];

  private static final URL[] NO_URLS = new URL[0];

  private static final String SUPER_SOURCE_FOLDER_NAME = "super";

  private static final String TEST_SUPER_SOURCE_FOLDER_NAME = "test-super";

  /**
   * FIXME - Were it not for the super source stuff, we would need this method. Can't we provide a
   * way for users to state which folders are super-source, etc?
   */
  public static List<IRuntimeClasspathEntry> getGWTRuntimeProjectSourceEntries(
      IJavaProject project, boolean includeTestSourceEntries) throws SdkException {

    assert (isGWTRuntimeProject(project) && project.exists());

    String projectName = project.getProject().getName();
    List<IRuntimeClasspathEntry> sourceEntries = new ArrayList<IRuntimeClasspathEntry>();

    IClasspathEntry[] gwtUserJavaProjClasspathEntries = null;

    try {
      gwtUserJavaProjClasspathEntries = project.getRawClasspath();
    } catch (JavaModelException e) {
      throw new SdkException("Cannot extract raw classpath from " + projectName + " project.");
    }

    Set<IPath> absoluteSuperSourcePaths = new HashSet<IPath>();

    for (IClasspathEntry curClasspathEntry : gwtUserJavaProjClasspathEntries) {
      if (curClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
        IPath sourcePath = curClasspathEntry.getPath();

        if (isJavadocPath(sourcePath)) {
          // Ignore javadoc paths.
          continue;
        }

        if (GWTProjectUtilities.isTestPath(sourcePath) && !includeTestSourceEntries) {
          // Ignore test paths, unless it is specified explicitly that we should
          // include them.
          continue;
        }

        sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(sourcePath));

        // Figure out the location of the super source path.

        IPath absoluteSuperSourcePath =
            sourcePath.removeLastSegments(1).append(SUPER_SOURCE_FOLDER_NAME);
        IPath relativeSuperSourcePath = absoluteSuperSourcePath.removeFirstSegments(1);

        if (absoluteSuperSourcePaths.contains(absoluteSuperSourcePath)) {
          // I've already included this path.
          continue;
        }

        if (project.getProject().getFolder(relativeSuperSourcePath).exists()) {
          /*
           * We've found the super source path, and we've not added it already. The existence test
           * uses a relative path, but the creation of a runtime classpath entry requires an
           * absolute path.
           */
          sourceEntries.add(JavaRuntime.newArchiveRuntimeClasspathEntry(absoluteSuperSourcePath));
          absoluteSuperSourcePaths.add(absoluteSuperSourcePath);
        }

        IPath absoluteTestSuperSourcePath =
            sourcePath.removeLastSegments(1).append(TEST_SUPER_SOURCE_FOLDER_NAME);
        IPath relativeTestSuperSourcePath = absoluteTestSuperSourcePath.removeFirstSegments(1);
        if (absoluteSuperSourcePaths.contains(absoluteTestSuperSourcePath)) {
          // I've already included this path.
          continue;
        }

        if (includeTestSourceEntries
            && project.getProject().getFolder(relativeTestSuperSourcePath).exists()) {
          /*
           * We've found the super source path, and we've not added it already. The existence test
           * uses a relative path, but the creation of a runtime classpath entry requires an
           * absolute path.
           */
          sourceEntries.add(JavaRuntime
              .newArchiveRuntimeClasspathEntry(absoluteTestSuperSourcePath));
          absoluteSuperSourcePaths.add(absoluteTestSuperSourcePath);
        }
      }
    }

    if (absoluteSuperSourcePaths.isEmpty()) {
      GWTPluginLog.logError("There were no super source folders found for the project '{0}'",
          project.getProject().getName());
    }

    return sourceEntries;
  }

  public static boolean isGWTRuntimeProject(IJavaProject project) {
    String projectName = project.getProject().getName();
    return (GWT_USER_PROJECT_NAME.equals(projectName)
        || projectName.equals(GWT_DEV_FALLBACK_PROJECT_NAME) || projectName
          .equals(getPlatformSpecificDevProjectName()));
  }

  /**
   * Convenience method to synthesize a GWT Contributor Runtime. The runtime is not actually
   * persisted to the user's set of SDKs.
   *
   * This method can be useful when working with the GWT Runtime projects (as they do not have an
   * SDK themselves).
   */
  public static GWTProjectsRuntime syntheziseContributorRuntime() {
    return (GWTProjectsRuntime) GwtSdk.getFactory().newInstance("temp contributor SDK",
        ResourcesPlugin.getWorkspace().getRoot().getLocation());
  }

  /**
   * @return a status with error severity if the JAR does not exist, otherwise OK severity (in the
   *         case of a JavaModelException, it is logged and OK is returned)
   */
  static IStatus getGwtDevJarStatus(GwtSdk sdk) {
    try {
      if (sdk.getDevJar() == null) {
        return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
            "The gwt-dev JAR is missing, ensure your GWT trunk has been built from the command line.");
      }
    } catch (SdkException e) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, e.getMessage());
    } catch (JavaModelException e) {
      GWTPluginLog.logWarning(e, "Could not get GWT dev JAR status");
    }

    return StatusUtilities.newOkStatus("", GWTPlugin.PLUGIN_ID);
  }

  static String getPlatformSpecificDevProjectName() {
    return GWT_DEV_FALLBACK_PROJECT_NAME + '-' + Util.getPlatformName();
  }

  static String getVersionImpl() {
    return VERSION;
  }

  private static IPath getAbsoluteLocation(IPath workspaceRelativePath, IProject project) {
    IPath relativeSourcePath = workspaceRelativePath.removeFirstSegments(1);
    return project.getFolder(relativeSourcePath).getLocation();
  }

  private static boolean isJavadocPath(IPath path) {
    return (JAVADOC_SOURCE_FOLDER_NAME.equals(path.lastSegment()));
  }

  GWTProjectsRuntime(String name, IPath location) {
    super(name, location);
  }

  @Override
  public URLClassLoader createClassLoader() throws SdkException, MalformedURLException {
    IJavaProject userProject = findUserProject();
    if (userProject != null) {
      IRuntimeClasspathEntry outputEntry = JavaRuntime.newDefaultProjectClasspathEntry(userProject);
      try {
        IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry =
            JavaRuntime.resolveRuntimeClasspathEntry(outputEntry, userProject);
        List<URL> urls = new ArrayList<URL>();
        for (IRuntimeClasspathEntry entry : resolveRuntimeClasspathEntry) {
          urls.add(new File(entry.getLocation()).toURI().toURL());
        }

        return new URLClassLoader(urls.toArray(NO_URLS), null);

      } catch (CoreException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClasspathEntry[] getClasspathEntries() {
    IJavaProject devProject = findDevProject();
    IJavaProject userProject = findUserProject();
    if (devProject != null && userProject != null) {
      return new IClasspathEntry[] {JavaCore.newProjectEntry(devProject.getPath()),
          JavaCore.newProjectEntry(userProject.getPath())};
    }

    return NO_ICLASSPATH_ENTRIES;
  }

  @Override
  public File getDevJar() throws SdkException /* throws Exception */, JavaModelException {

    IStringVariableManager variableManager = getVariableManager();
    IValueVariable valueVariable = variableManager.getValueVariable("gwt_devjar");
    if (valueVariable != null) {
      String value = valueVariable.getValue();
      if (value != null) {
        IPath path = new Path(value);
        File file = path.toFile();
        if (!file.exists()) {
          throw new SdkException("gwt_devjar Run/Debug variable points to a non-existent jar: "
              + value);
        }

        return file;
      }
    }

    // We're going to have to search down the trunk to find the built gwt-dev
    // .jar. This assumes that the user has built the trunk at least once

    // TODO: can we remove the check for gwt.devjar from applicationCreator?

    IProject userProject =
        ResourcesPlugin.getWorkspace().getRoot().getProject(GWT_USER_PROJECT_NAME);
    if (!userProject.exists()) {
      throw new SdkException("The project ' " + userProject.getName()
          + "' does not exist in the workspace.");
    }

    IJavaProject javaUserProject = JavaCore.create(userProject);

    if (!javaUserProject.exists()) {
      throw new SdkException("The project ' " + userProject.getName() + "' is not a Java project.");
    }

    IClasspathEntry[] rawClasspaths = javaUserProject.getRawClasspath();

    File stagingDir = null;
    IPath stagingPathLocation = null;

    for (IClasspathEntry rawClasspath : rawClasspaths) {
      if (rawClasspath.getEntryKind() == IClasspathEntry.CPE_SOURCE) {

        IPath sourcePathLocation = getAbsoluteLocation(rawClasspath.getPath(), userProject);
        stagingPathLocation =
            sourcePathLocation.removeLastSegments(2).append(STAGING_FOLDER_RELATIVE_LOCATION);

        stagingDir = stagingPathLocation.toFile();
        if (stagingDir.exists()) {
          break;
        }
      }
    }

    if (stagingPathLocation == null) {
      throw new SdkException("Contributor SDK build directory not found; Project '"
          + userProject.getName() + "' does not have any source folders.");
    }

    if (stagingDir == null || !stagingDir.exists()) {
      throw new SdkException("Contributor SDK build directory not found (expected at "
          + stagingPathLocation.toString() + ")");
    }

    // Find the staging output directory: gwt-<platform>-<version>
    final File[] buildDirs = stagingDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return (file.isDirectory() && file.getName().startsWith("gwt-"));
      }
    });
    if (buildDirs.length == 0) {
      throw new SdkException("Contributor SDK build directory not found (expected at "
          + stagingDir.toString() + File.separator + "gwt-<platform>-<version>)");
    }

    // Find the gwt-dev .jar
    File[] gwtDevJars = buildDirs[0].listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        String name = file.getName();
        IPath sdkLocationPath = Path.fromOSString(buildDirs[0].getAbsolutePath());
        return (name.equalsIgnoreCase(Util.getDevJarName(sdkLocationPath)));
      }
    });
    if (gwtDevJars.length == 0) {
      throw new SdkException("Contributor SDK build directory missing required JAR files");
    }

    return gwtDevJars[0];
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public File[] getWebAppClasspathFiles(IProject project) {
    try {
      File devJar = getDevJar();
      File servletJar = new File(devJar.getParentFile(), "gwt-servlet.jar");
      return new File[] {servletJar};
    } catch (JavaModelException e) {
      e.printStackTrace();
    } catch (SdkException e) {
      e.printStackTrace();
    }

    return NO_FILES;
  }

  @Override
  public boolean usesGwtDevProject() {
    return true;
  }

  @Override
  public IStatus validate() {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IPathVariableManager pathVariableManager = workspace.getPathVariableManager();
    URI gwtUri = pathVariableManager.getURIValue("GWT_ROOT");
    if (gwtUri == null) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Path variable 'GWT_ROOT' is not defined");
    }

    IPath gwtRoot = URIUtil.toPath(gwtUri);
    if (gwtRoot == null) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Path variable 'GWT_ROOT' is not defined");
    }

    if (!gwtRoot.toFile().exists()) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID,
          "Path variable 'GWT_ROOT' points to an invalid location");
    }

    if (findDevProject() == null) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, "Could not find and gwt-dev-* project");
    }

    if (findUserProject() == null) {
      return new Status(IStatus.ERROR, GWTPlugin.PLUGIN_ID, "Could not find the gwt-user project");
    }

    return getGwtDevJarStatus(this);
  }

  private IJavaProject findDevProject() {
    IJavaProject devProject =
        JavaProjectUtilities.findJavaProject(getPlatformSpecificDevProjectName());
    if (devProject == null) {
      devProject = JavaProjectUtilities.findJavaProject(GWT_DEV_FALLBACK_PROJECT_NAME);
    }
    return devProject;
  }

  private IJavaProject findUserProject() {
    return JavaProjectUtilities.findJavaProject(GWT_USER_PROJECT_NAME);
  }

  private IStringVariableManager getVariableManager() {
    return VariablesPlugin.getDefault().getStringVariableManager();
  }

}
