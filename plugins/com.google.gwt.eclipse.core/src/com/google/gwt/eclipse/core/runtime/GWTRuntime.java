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

import com.google.gdt.eclipse.core.ClasspathUtilities;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.extensions.ExtensionQuery;
import com.google.gdt.eclipse.core.sdk.AbstractSdk;
import com.google.gdt.eclipse.core.sdk.SdkFactory;
import com.google.gdt.eclipse.core.sdk.SdkUtils;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.preferences.GWTPreferences;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a GWT runtime and provides a URLClassLoader that can be used to
 * load the gwt-user and gwt-dev classes.
 * 
 * TODO: Move this and subtypes into the sdk package.
 */
public abstract class GWTRuntime extends AbstractSdk {

  /**
   * A factory that returns a project-bound GWT SDK. Extension points can
   * implement this interface to return an externally-computed project-bound GWT
   * SDK in response to calls to {@link GWTRuntime#findSdkFor(IJavaProject)}.
   */
  public interface IProjectBoundSdkFactory {
    ProjectBoundSdk newInstance(IJavaProject javaProject);
  }
  /**
   * Models an {@link com.google.gdt.eclipse.core.sdk.Sdk} that was detected on
   * a project's classpath.
   */
  public static class ProjectBoundSdk extends GWTRuntime {
    private static IPath getAbsoluteLocation(IPath workspaceRelativePath,
        IProject project) {
      IPath relativeSourcePath = workspaceRelativePath.removeFirstSegments(1);
      return project.getFolder(relativeSourcePath).getLocation();
    }

    protected final IJavaProject javaProject;

    protected ProjectBoundSdk(IJavaProject javaProject) {
      super("", null);
      this.javaProject = javaProject;
    }

    /**
     * Returns a {@link ClassLoader} that is backed by the project's runtime
     * classpath.
     * 
     * TODO: This returns a classloader which contains ALL
     * of the jars of the project. Lookups on this thing are going to be SLOW.
     * Can we optimize this? We could create a classloader that just contains
     * the jars that GWT requires. Maybe caching is the right solution here.
     * 
     * TODO: Why can't we just delegate to
     * {@link #getClasspathEntries()} when generating the classloader URLs? Why
     * do we have to add every URL that is part of the project? That would
     * certainly speed up lookups on this classloader. Maybe we cannot do this
     * because project-bound sdks handle the case of source-based runtimes, and
     * in that case, we need all of the dependencies as part of the classloader.
     */
    @Override
    public URLClassLoader createClassLoader() throws SdkException,
        MalformedURLException {
      try {
        String[] defaultRuntimeClasspath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
        URL[] urls = new URL[defaultRuntimeClasspath.length];
        for (int i = 0; i < defaultRuntimeClasspath.length; ++i) {
          File file = new File(defaultRuntimeClasspath[i]);
          urls[i] = file.toURL();
        }
        return new URLClassLoader(urls);
      } catch (CoreException e) {
        throw new SdkException(e);
      }
    }

    /**
     * Returns the classpath entries from the {@link IJavaProject}'s raw
     * classpath that make up the {@link com.google.gdt.eclipse.core.sdk.Sdk}.
     * 
     * TODO: Can we clean up uses of this method? It
     * really only seems to be useful in the case where you want to derive a
     * classpath container from an SDK. I'm not sure if this should be a
     * first-class method on an SDK.
     * 
     * TODO: Get rid of this method. It's only needed when a classpath container
     * needs to be initialized, and classpath containers are never initialized
     * from ProjectBoundSdks.
     */
    public IClasspathEntry[] getClasspathEntries() {
      try {
        // If containers are being used, we avoid duplicates by using a set
        Set<IClasspathEntry> classpathEntries = new HashSet<IClasspathEntry>();

        IClasspathEntry gwtDev = findGwtDevClasspathEntry();
        if (gwtDev != null) {
          classpathEntries.add(gwtDev);
        }

        IClasspathEntry gwtUser = findGwtUserClasspathEntry();
        if (gwtUser != null) {
          classpathEntries.add(gwtUser);
        }

        final List<IClasspathEntry> rawClasspath = Arrays.asList(javaProject.getRawClasspath());

        // Sort the classpath entries so they match the declared order of the
        // raw classpath.
        IClasspathEntry[] classpathEntryArray = classpathEntries.toArray(NO_ICLASSPATH_ENTRIES);
        Collections.sort(Arrays.asList(classpathEntryArray),
            new Comparator<IClasspathEntry>() {
              public int compare(IClasspathEntry o1, IClasspathEntry o2) {
                return rawClasspath.indexOf(o1) - rawClasspath.indexOf(o2);
              }
            });

        return classpathEntryArray;
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }

      return AbstractSdk.NO_ICLASSPATH_ENTRIES;
    }

    @Override
    public File getDevJar() throws SdkException, JavaModelException {
      IPath installPath = computeInstallPath();
      if (installPath != null) {
        return installPath.append(Util.getDevJarName(installPath)).toFile();
      }

      return null;
    }

    @Override
    public IPath getInstallationPath() {
      if (usesGwtDevProject()) {
        // Project backed sdks use the workspace location as the install
        // location.
        return ResourcesPlugin.getWorkspace().getRoot().getLocation();
      }

      return computeInstallPath();
    }

    @Override
    public String getName() {
      IPath installationPath = getInstallationPath();
      if (installationPath != null) {
        return installationPath.toOSString();
      } else {
        return "Unknown";
      }
    }

    public File[] getWebAppClasspathFiles(IProject project) {
      IPath installPath = computeInstallPath();
      if (installPath != null) {
        return new File[] {installPath.append("gwt-servlet.jar").toFile()};
      }

      return NO_FILES;
    }

    @Override
    public boolean supportsTransitionalOOPHM() {
      /*
       * Don't worry about transtional OOPHM, between GWT versions 1.7.X and
       * 2.0.
       */
      return false;
    }

    @Override
    public boolean usesGwtDevProject() {
      try {
        IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry resolvedClasspathEntry : resolvedClasspath) {
          String projectName = resolvedClasspathEntry.getPath().segment(0);
          if (projectName.equals(GWTProjectsRuntime.GWT_DEV_FALLBACK_PROJECT_NAME)
              || projectName.equals(GWTProjectsRuntime.getPlatformSpecificDevProjectName())) {
            return true;
          }
        }
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }
      return false;
    }

    @Override
    public IStatus validate() {
      if (getInstallationPath() == null) {
        return StatusUtilities.newErrorStatus(
            "Could not determine an installation path based on the project's classpath.",
            GWTPlugin.PLUGIN_ID);
      }

      if (usesGwtDevProject()) {
        IStatus status = GWTProjectsRuntime.getGwtDevJarStatus(this);
        if (!status.isOK()) {
          return status;
        }
      }

      return StatusUtilities.OK_STATUS;
    }

    /**
     * Returns the value of the gwt_devjar variable or <code>null</code> if the
     * variable is not defined.
     */
    private IPath computeGwtDevJarVariableValue() {
      IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
      IValueVariable valueVariable = variableManager.getValueVariable("gwt_devjar");
      if (valueVariable != null) {
        String value = valueVariable.getValue();
        if (value != null) {
          IPath path = new Path(value);
          return path.removeLastSegments(1);
        }
      }

      return null;
    }

    private IPath computeInstallPath() {
      try {
        IClasspathEntry classpathEntry = findGwtDevClasspathEntry();
        if (classpathEntry == null) {
          classpathEntry = findGwtUserClasspathEntry();
        }

        if (classpathEntry != null) {
          return computeInstallPath(classpathEntry);
        }

        return null;
      } catch (JavaModelException e) {
        GWTPluginLog.logError(e);
      }

      return null;
    }

    private IPath computeInstallPath(IClasspathEntry classpathEntry)
        throws JavaModelException {
      IPath installPath = null;
      switch (classpathEntry.getEntryKind()) {
        case IClasspathEntry.CPE_CONTAINER:
          GWTRuntime sdk = GWTPreferences.getSdkManager().findSdkForPath(
              classpathEntry.getPath());
          if (sdk != null) {
            IClasspathEntry[] classpathEntries = sdk.getClasspathEntries();
            if (classpathEntries.length > 0) {
              installPath = computeInstallPath(classpathEntries[0]);
            }
          }
          break;

        case IClasspathEntry.CPE_LIBRARY:
          installPath = classpathEntry.getPath().removeLastSegments(1);
          break;
        case IClasspathEntry.CPE_PROJECT:
        case IClasspathEntry.CPE_SOURCE:
          installPath = computeGwtDevJarVariableValue();
          if (installPath == null) {
            installPath = computeInstallPathFromProjectOrSourceClasspathEntry(classpathEntry);
          }
          break;
        default:
          break;
      }

      return installPath;
    }

    private IPath computeInstallPathFromProject(IJavaProject jProject)
        throws JavaModelException {
      IPath buildStagingDirectory = null;
      for (IClasspathEntry rawClasspath : jProject.getRawClasspath()) {
        if (rawClasspath.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          IPath sourcePathLocation = getAbsoluteLocation(
              rawClasspath.getPath(), jProject.getProject());

          // Project could be gwt-user or gwt-dev and their source paths vary
          // from 2 to 3 segments - Hack.
          IPath outputLocation = sourcePathLocation.removeLastSegments(3).append(
              GWTProjectsRuntime.STAGING_FOLDER_RELATIVE_LOCATION);
          if (!outputLocation.toFile().exists()) {
            outputLocation = sourcePathLocation.removeLastSegments(2).append(
                GWTProjectsRuntime.STAGING_FOLDER_RELATIVE_LOCATION);
          }

          if (outputLocation.toFile().exists()) {
            buildStagingDirectory = outputLocation;
            break;
          }
        }
      }

      if (buildStagingDirectory != null) {
        return computeStagingDirectoryPath(buildStagingDirectory);
      }

      return null;
    }

    private IPath computeInstallPathFromProjectOrSourceClasspathEntry(
        IClasspathEntry classpathEntry) throws JavaModelException {
      IPath installPath;
      IPath entryPath = classpathEntry.getPath();
      // First segment should be project name
      String projectName = entryPath.segment(0);
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(
          projectName);
      IJavaProject jProject = JavaCore.create(project);
      installPath = computeInstallPathFromProject(jProject);
      return installPath;
    }

    private IClasspathEntry findGwtDevClasspathEntry()
        throws JavaModelException {
      IClasspathEntry gwtDev = ClasspathUtilities.findRawClasspathEntryFor(
          javaProject, "com.google.gwt.dev.About");
      return gwtDev;
    }

    private IClasspathEntry findGwtUserClasspathEntry()
        throws JavaModelException {
      IClasspathEntry gwtUser = ClasspathUtilities.findRawClasspathEntryFor(
          javaProject, "com.google.gwt.core.client.GWT");
      return gwtUser;
    }
  }

  public static final String GWT_DEV_NO_PLATFORM_JAR = "gwt-dev.jar";

  public static final String GWT_OOPHM_JAR = "gwt-dev-oophm.jar";

  public static final String GWT_USER_JAR = "gwt-user.jar";

  public static final String VALIDATION_API_JAR_PREFIX = "validation-api-";

  private static final SdkFactory<GWTRuntime> factory = new SdkFactory<GWTRuntime>() {
    public GWTRuntime newInstance(String name, IPath sdkHome) {
      if (isProjectBasedSdk(sdkHome)) {
        return new GWTProjectsRuntime(name, sdkHome);
      }

      return new GWTJarsRuntime(name, sdkHome);
    }
  };

  private static final File[] NO_FILES = new File[0];

  /**
   * Finds the {@link GWTRuntime} used by the specified project. Note that the
   * SDK need not have been registered.
   */
  public static GWTRuntime findSdkFor(IJavaProject javaProject)
      throws JavaModelException {

    ExtensionQuery<GWTRuntime.IProjectBoundSdkFactory> extQuery = new ExtensionQuery<GWTRuntime.IProjectBoundSdkFactory>(
        GWTPlugin.PLUGIN_ID, "gwtProjectBoundSdkFactory", "class");
    List<ExtensionQuery.Data<GWTRuntime.IProjectBoundSdkFactory>> sdkFactories = extQuery.getData();
    for (ExtensionQuery.Data<GWTRuntime.IProjectBoundSdkFactory> sdkFactory : sdkFactories) {

      GWTRuntime externalGWTRuntime = sdkFactory.getExtensionPointData().newInstance(
          javaProject);
      if (externalGWTRuntime != null && externalGWTRuntime.validate().isOK()) {
        return externalGWTRuntime;
      }
    }

    ProjectBoundSdk projectBoundSdk = new ProjectBoundSdk(javaProject);
    if (projectBoundSdk.getInstallationPath() != null) {
      return projectBoundSdk;
    }

    return null;
  }

  public static SdkFactory<GWTRuntime> getFactory() {
    return factory;
  }

  protected static IPath computeStagingDirectoryPath(IPath rootPath) {
    File stagingDirectory = rootPath.toFile();

    // Find the staging output directory: gwt-<platform>-<version>
    final File[] buildDirs = stagingDirectory.listFiles(new FileFilter() {
      public boolean accept(File file) {
        return (file.isDirectory() && file.getName().startsWith("gwt-"));
      }
    });
    if (buildDirs != null && buildDirs.length > 0) {
      return new Path(buildDirs[0].getAbsolutePath());
    }

    return null;
  }

  /**
   * Returns <code>true</code> if the SDK home is a project based SDK.
   * 
   * @param sdkHome
   * @return whether the SDK home is a project based SDK
   */
  private static boolean isProjectBasedSdk(IPath sdkHome) {
    IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    return location.equals(sdkHome);
  }

  protected GWTRuntime(String name, IPath location) {
    super(name, location);
  }

  /*
   * TODO: need to compute this only once and cache the result. Ideally, this
   * should be done at the same time that "validateAndSetVersion" is called.
   * Unfortunately, whenever we're interacting with the runtime list on the main
   * preference page, those values are extracted straight from a string, and
   * validateAndSetVersion is never called beforehand.
   * 
   * Perhaps the version should not be stored in the prefs string, only the
   * name/location. That way we can just call validateAndSetVersion after
   * parsing the prefs string.
   */
  public boolean containsSCL() {
    try {
      URLClassLoader cl = createClassLoader();
      cl.loadClass("com.google.gwt.core.ext.ServletContainerLauncher");
      return true;
    } catch (ClassNotFoundException e) {
      /*
       * No need for logging an exception here; we're just checking to see if
       * this runtime supports SCLs or not.
       */
    } catch (MalformedURLException e) {
      GWTPluginLog.logError(e);
    } catch (SdkException e) {
      GWTPluginLog.logError(e);
    }

    return false;
  }

  // FIXME: Why is this returning URLClassLoader instead of ClassLoader
  public abstract URLClassLoader createClassLoader() throws SdkException,
      MalformedURLException;

  public abstract File getDevJar() throws SdkException, JavaModelException;

  public String getVersion() {
    URLClassLoader cl;
    final String exceptionMessage = "Cannot get version of GWT SDK \""
        + getName() + "\", ensure it is configured properly";
    try {
      cl = createClassLoader();
      // Extract version from gwt-dev-<platform>
      Class<?> about = cl.loadClass("com.google.gwt.dev.About");
      Field versionField = about.getField("GWT_VERSION_NUM");
      return SdkUtils.cleanupVersion((String) versionField.get(null));
    } catch (MalformedURLException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (SdkException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (ClassNotFoundException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (SecurityException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (NoSuchFieldException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (IllegalArgumentException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    } catch (IllegalAccessException e) {
      GWTPluginLog.logError(e, exceptionMessage);
    }

    return "";
  }

  /**
   * Ask this runtime whether it supports "transitional" OOPHM. This is the
   * range of source code after GWT 1.7 but before the GWT 2.0 release. This
   * form of OOPHM requires either the gwt-dev-oophm.jar, or the gwt-dev-oophm
   * project.
   * 
   * TODO: Remove this once pre-GWT 2.0 releases are deprecated.
   */
  public abstract boolean supportsTransitionalOOPHM();

  /**
   * Returns <code>true</code> if the {@link GWTRuntime} references the gwt-dev
   * project.
   */
  public boolean usesGwtDevProject() {
    // Overridden in subclasses.
    return false;
  }

  public abstract IStatus validate();
}

