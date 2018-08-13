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
package com.google.gdt.eclipse.maven.sdk;

import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gdt.eclipse.core.sdk.AbstractSdk;
import com.google.gdt.eclipse.maven.Activator;
import com.google.gdt.eclipse.maven.MavenUtils;
import com.google.gwt.eclipse.core.launch.processors.GwtLaunchConfigurationProcessorUtilities;
import com.google.gwt.eclipse.core.runtime.GwtSdk.ProjectBoundSdk;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A project-based GWT SDK for Maven-based project.
 */
public class GWTMavenRuntime extends ProjectBoundSdk {

  public static final String MAVEN_GWT_GROUP_ID = "com.google.gwt";
  public static final String MAVEN_GWT_DEV_JAR_ARTIFACT_ID = "gwt-dev";
  public static final String MAVEN_GWT_USER_ARTIFACT_ID = "gwt-user";
  public static final String MAVEN_GWT_SERVLET_ARTIFACT_ID = "gwt-servlet";

  protected GWTMavenRuntime(IJavaProject javaProject) {
    super(javaProject);
  }

  @Override
  public URLClassLoader createClassLoader() throws SdkException, MalformedURLException {
    if (!validate().isOK()) {
      return new URLClassLoader(new URL[0]);
    }

    try {
      ArrayList<URL> classloaderURLs = new ArrayList<URL>();

      // getDevJar would throw an exception instead of returning null
      // gwt-dev.jar
      classloaderURLs.add(getDevJar().toURI().toURL());

      // findGwtUserClasspathEntry won't be null, because validate passed gwt-user.jar
      classloaderURLs.add(findGwtUserClasspathEntry().getPath().toFile().toURI().toURL());

      // validation jars
      IClasspathEntry javaxValidationJar = findJavaXValidationClasspathEntry();
      // could be null on older GWT projects
      if (javaxValidationJar != null) {
        classloaderURLs.add(javaxValidationJar.getPath().toFile().toURI().toURL());
      }

      return new URLClassLoader(classloaderURLs.toArray(new URL[classloaderURLs.size()]), null);
    } catch (JavaModelException jme) {
      return new URLClassLoader(new URL[0]);
    }
  }

  /**
   * This method's implementation breaks the general contract of
   * {@link com.google.gwt.eclipse.core.runtime.GwtSdk#getClasspathEntries()} .
   *
   * The general contract states that the entries returned should be the raw entries on the build path that correspond
   * to the SDK. This method returns the resolved entry on the build path that corresponds to the gwt-user library. It
   * then returns the path to the gwt-dev library that's a peer of the gwt-user library in the Maven repository. This
   * library may not be on the build classpath.
   *
   * TODO: Reconsider the general contract of this method.
   *
   * TODO: Get rid of this method; I don't think its used at all in the Maven case.
   */
  @Override
  public IClasspathEntry[] getClasspathEntries() {
    try {
      // If containers are being used, we avoid duplicates by using a set
      Set<IClasspathEntry> classpathEntries = new HashSet<IClasspathEntry>();

      IClasspathEntry gwtUser = findGwtUserClasspathEntry();
      if (gwtUser != null) {
        classpathEntries.add(gwtUser);

        File gwtDevJar = getDevJar();

        if (gwtDevJar != null) {
          classpathEntries.add(JavaCore.newLibraryEntry(Path.fromOSString(gwtDevJar.getAbsolutePath()), null, null));
        }
      }

      final List<IClasspathEntry> rawClasspath = Arrays.asList(javaProject.getRawClasspath());

      // Sort the classpath entries so they match the declared order of the
      // raw classpath.
      IClasspathEntry[] classpathEntryArray = classpathEntries.toArray(NO_ICLASSPATH_ENTRIES);
      Collections.sort(Arrays.asList(classpathEntryArray), new Comparator<IClasspathEntry>() {
        @Override
        public int compare(IClasspathEntry o1, IClasspathEntry o2) {
          return rawClasspath.indexOf(o1) - rawClasspath.indexOf(o2);
        }
      });

      return classpathEntryArray;
    } catch (JavaModelException e) {
      Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
          "Unable to generate classpath entries for the maven-based GWT runtime.", e));
    } catch (SdkException sdke) {
      Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
          "Unable to generate classpath entries for the maven-based GWT runtime.", sdke));
    }

    return AbstractSdk.NO_ICLASSPATH_ENTRIES;
  }

  @Override
  public File getDevJar() throws SdkException, JavaModelException {
    IClasspathEntry classpathEntry = findGwtUserClasspathEntry();

    if (classpathEntry == null) {
      throw new SdkException("Unable to locate gwt-user.jar");
    }

    IPath path = MavenUtils.getArtifactPathForPeerMavenArtifact(classpathEntry.getPath(), MAVEN_GWT_GROUP_ID,
        MAVEN_GWT_DEV_JAR_ARTIFACT_ID);
    if (path == null) {
      throw new SdkException("Unable to locate gwt-dev.jar");
    }

    if (!path.toFile().exists()) {
      throw new SdkException(path.toOSString() + " does not exist.");
    }

    return path.toFile();
  }

  /**
   * Maven-based GWT SDKs do not have a clear installation path. So, we say that the installation path corresponds to:
   * <code><repository path>/<group path></code>.
   */
  @Override
  public IPath getInstallationPath() {

    try {
      IClasspathEntry classpathEntry = findGwtUserClasspathEntry();
      if (classpathEntry == null) {
        return null;
      }

      IPath p = classpathEntry.getPath();
      if (p.segmentCount() < 4) {
        return null;
      }
      return p.removeLastSegments(3);
    } catch (JavaModelException e) {
      Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
          "Unable to determine installation path for the maven-based GWT runtime.", e));
    }

    return null;
  }

  /**
   * Maven-based sdks do not contribute libraries that should be placed in the <code>WEB-INF/lib</code> folder.
   */
  @Override
  public File[] getWebAppClasspathFiles(IProject project) {
    return new File[0];
  }

  @Override
  public IStatus validate() {
    try {
      getDevJar();
    } catch (Exception e) {
      return StatusUtilities.newErrorStatus(e, Activator.PLUGIN_ID);
    }
    return StatusUtilities.OK_STATUS;
  }

  /**
   * Find the classpath for get-dev.jar which is used to run super dev mode.
   *
   * @return IClasspathEntry for the path to gwt-dev.jar
   * @throws JavaModelException
   */
  private IClasspathEntry findGwtCodeServerClasspathEntry() throws JavaModelException {
    IType type = javaProject.findType(GwtLaunchConfigurationProcessorUtilities.GWT_CODE_SERVER);
    if (type == null) {
      return null;
    }

    IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) type
        .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
      return JavaCore.newLibraryEntry(packageFragmentRoot.getPath(), null, null);
    }

    return null;
  }

  private IClasspathEntry findGwtUserClasspathEntry() throws JavaModelException {
    /*
     * Note that the type that we're looking for to determine if we're part of the gwt-user library is different than
     * the one that is used by the superclass. This is because the class that the superclass is querying for,
     * "com.google.gwt.core.client.GWT", also exists in the gwt-servlet library, and for some reason, this sometimes
     * ends up on the build path for Maven projects.
     *
     * TODO: See why Maven is putting gwt-servlet on the build path.
     *
     * TODO: Change the class query in the superclass to "com.google.gwt.junit.client.GWTTestCase"
     */
    IType type = javaProject.findType("com.google.gwt.junit.client.GWTTestCase");

    if (type == null) {
      return null;
    }

    IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) type
        .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
      // TODO: If the Maven javadoc and source libs for gwt-dev.jar are
      // available, attach them here.
      return JavaCore.newLibraryEntry(packageFragmentRoot.getPath(), null, null);
    }

    return null;
  }

  private IClasspathEntry findJavaXValidationClasspathEntry() throws JavaModelException {
    IType type = javaProject.findType("javax.validation.Constraint");

    if (type == null) {
      return null;
    }

    IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) type
        .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_BINARY) {
      return JavaCore.newLibraryEntry(packageFragmentRoot.getPath(), null, null);
    }

    return null;
  }

}
