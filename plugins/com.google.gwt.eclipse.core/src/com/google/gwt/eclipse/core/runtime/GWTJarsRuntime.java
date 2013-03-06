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

import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GWT runtime that is based on .jar files in the file system.
 */
public class GWTJarsRuntime extends GWTRuntime {
  private static final File[] NO_FILES = new File[0];

  // FIXME: Make private/protected
  public GWTJarsRuntime(String name, IPath location) {
    super(name, location);
  }

  @Override
  public URLClassLoader createClassLoader() throws SdkException,
      MalformedURLException {
    URL[] urls;
    if (validate().isOK()) {
      urls = getBuildClasspathUrls().toArray(new URL[0]);
    } else {
      urls = new URL[0];
    }

    // FIXME: If return type was not URLClassLoader, could return
    // getClass().getClassLoader().
    return new URLClassLoader(urls, null);
  }

  public IClasspathEntry[] getClasspathEntries() {
    // Note that the GWT SDK puts the javadoc in "doc/javadoc", whereas GAE uses
    // "docs/javadoc".
    IPath gwtJavadocLocation = getInstallationPath().append(
        new Path("doc/javadoc"));
    IClasspathAttribute[] extraAttributes = new IClasspathAttribute[0];

    if (gwtJavadocLocation.toFile().exists()) {
      extraAttributes = new IClasspathAttribute[] {JavaCore.newClasspathAttribute(
          IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
          gwtJavadocLocation.toFile().toURI().toString())};
    }

    List<IClasspathEntry> buildClasspathEntries = new ArrayList<IClasspathEntry>();
    if (validate().isOK()) {
      List<IPath> buildClasspaths = getBuildClasspaths();
      for (IPath buildClasspath : buildClasspaths) {
        if (buildClasspath.lastSegment().startsWith("gwt-")) {
          buildClasspathEntries.add(JavaCore.newLibraryEntry(buildClasspath,
              null, null, new IAccessRule[0], extraAttributes, false));
        } else {
          buildClasspathEntries.add(JavaCore.newLibraryEntry(buildClasspath,
              Util.findSourcesJarForClassesJar(buildClasspath), null,
              new IAccessRule[0], new IClasspathAttribute[0], false));
        }
      }
    }

    return buildClasspathEntries.toArray(NO_ICLASSPATH_ENTRIES);
  }

  @Override
  public File getDevJar() {
    File devJar = getInstallationPath().append(
        Util.getDevJarName(getInstallationPath())).toFile();
    if (devJar.exists()) {
      return devJar;
    }

    return null;
  }

  public File[] getWebAppClasspathFiles(IProject project) {
    if (validate().isOK()) {
      return new File[] {getInstallationPath().append("gwt-servlet.jar").toFile()};
    }

    return NO_FILES;
  }

  @Override
  public boolean supportsTransitionalOOPHM() {
    IPath sdkLocation = getInstallationPath();
    IPath gwtUserPath = sdkLocation.append(GWT_OOPHM_JAR);
    return gwtUserPath.toFile().exists();
  }

  @Override
  public IStatus validate() {
    IPath sdkLocation = getInstallationPath();
    File sdkDir = sdkLocation.toFile();
    if (!sdkDir.exists()) {
      return Util.newErrorStatus("SDK path '" + sdkLocation.toOSString()
          + "' does not exist");
    }

    IPath gwtUserPath = sdkLocation.append(GWT_USER_JAR);
    if (!gwtUserPath.toFile().exists()) {
      return Util.newErrorStatus(gwtUserPath.toOSString() + " is missing");
    }

    IPath gwtDevPath = sdkLocation.append(Util.getDevJarName(sdkLocation));
    if (!gwtDevPath.toFile().exists()) {
      return Util.newErrorStatus(gwtDevPath.toOSString() + " is missing");
    }

    IPath gwtServletPath = sdkLocation.append("gwt-servlet.jar");
    if (!gwtServletPath.toFile().exists()) {
      return Util.newErrorStatus(gwtServletPath.toOSString() + " is missing");
    }

    return Status.OK_STATUS;
  }

  private List<IPath> getBuildClasspaths() {
    ArrayList<IPath> classpathEntries = new ArrayList<IPath>();

    classpathEntries.add(getInstallationPath().append(GWT_USER_JAR));
    classpathEntries.add(getInstallationPath().append(
        Util.getDevJarName(getInstallationPath())));

    for (String validationJarName : Util.getValidationJarNames(getInstallationPath())) {
      classpathEntries.add(getInstallationPath().append(validationJarName));
    }

    return classpathEntries;
  }

  private List<URL> getBuildClasspathUrls() {
    List<IPath> buildClasspaths = getBuildClasspaths();
    List<URL> buildClasspathUrls = new ArrayList<URL>();
    for (IPath buildClasspath : buildClasspaths) {
      try {
        buildClasspathUrls.add(buildClasspath.toFile().toURL());
      } catch (MalformedURLException e) {
        GWTPluginLog.logError(e);
      }
    }
    return buildClasspathUrls;
  }
}

